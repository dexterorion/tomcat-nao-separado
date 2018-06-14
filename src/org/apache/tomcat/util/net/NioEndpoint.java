/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.tomcat.util.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.X509KeyManager;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.net.jsse.NioX509KeyManager;

/**
 * NIO tailored thread pool, providing the following services:
 * <ul>
 * <li>Socket acceptor thread</li>
 * <li>Socket poller thread</li>
 * <li>Worker threads pool</li>
 * </ul>
 *
 * When switching to Java 5, there's an opportunity to use the virtual machine's
 * thread pool.
 *
 * @author Mladen Turk
 * @author Remy Maucherat
 * @author Filip Hanik
 */
public class NioEndpoint extends AbstractEndpoint<NioChannel> {

	// -------------------------------------------------------------- Constants

	private static final Log log = LogFactory.getLog(NioEndpoint.class);

	private static final int OP_REGISTER = 0x100; // register interest op
	private static final int OP_CALLBACK = 0x200; // callback interest op

	// ----------------------------------------------------------------- Fields

	private NioSelectorPool selectorPool = new NioSelectorPool();

	/**
	 * Server socket "pointer".
	 */
	private ServerSocketChannel serverSock = null;

	/**
	 * use send file
	 */
	private boolean useSendfile = true;

	/**
	 * The size of the OOM parachute.
	 */
	private int oomParachute = 1024 * 1024;
	/**
	 * The oom parachute, when an OOM error happens, will release the data,
	 * giving the JVM instantly a chunk of data to be able to recover with.
	 */
	private byte[] oomParachuteData = null;

	/**
	 * Make sure this string has already been allocated
	 */
	private static final String oomParachuteMsg = "SEVERE:Memory usage is low, parachute is non existent, your system may start failing.";

	/**
	 * Keep track of OOM warning messages.
	 */
	private long lastParachuteCheck = System.currentTimeMillis();

	/**
     *
     */
	private volatile CountDownLatch stopLatch = null;

	public static Log getLogVariable(){
		return log;
	}
	
	/**
	 * Cache for SocketProcessor objects
	 */
	private ConcurrentLinkedQueue<NioEndpointSocketProcessor> processorCache = new ConcurrentLinkedQueue1(this);

	/**
	 * Cache for key attachment objects
	 */
	private ConcurrentLinkedQueue<NioEndpointKeyAttachment> keyCache = new ConcurrentLinkedQueue2(this);

	/**
	 * Cache for poller events
	 */
	private ConcurrentLinkedQueue<NioEndpointPollerEvent> eventCache = new ConcurrentLinkedQueue3(this);

	/**
	 * Bytebuffer cache, each channel holds a set of buffers (two, except for
	 * SSL holds four)
	 */
	
	private ConcurrentLinkedQueue<NioChannel> nioChannels = new ConcurrentLinkedQueue4(this);

	// ------------------------------------------------------------- Properties

	/**
	 * Generic properties, introspected
	 */
	@Override
	public boolean setProperty(String name, String value) {
		final String selectorPoolName = "selectorPool.";
		try {
			if (name.startsWith(selectorPoolName)) {
				return IntrospectionUtils.setProperty(selectorPool,
						name.substring(selectorPoolName.length()), value);
			} else {
				return super.setProperty(name, value);
			}
		} catch (Exception x) {
			log.error("Unable to set attribute \"" + name + "\" to \"" + value
					+ "\"", x);
			return false;
		}
	}

	/**
	 * Priority of the poller threads.
	 */
	private int pollerThreadPriority = Thread.NORM_PRIORITY;

	public void setPollerThreadPriority(int pollerThreadPriority) {
		this.pollerThreadPriority = pollerThreadPriority;
	}

	public int getPollerThreadPriority() {
		return pollerThreadPriority;
	}

	/**
	 * Handling of accepted sockets.
	 */
	private NioEndpointHandler handler = null;

	public void setHandler(NioEndpointHandler handler) {
		this.handler = handler;
	}

	public NioEndpointHandler getHandler() {
		return handler;
	}

	/**
	 * Allow comet request handling.
	 */
	private boolean useComet = true;

	public void setUseComet(boolean useComet) {
		this.useComet = useComet;
	}

	@Override
	public boolean getUseComet() {
		return useComet;
	}

	@Override
	public boolean getUseCometTimeout() {
		return getUseComet();
	}

	@Override
	public boolean getUsePolling() {
		return true;
	}
	
	private int pollerThreadCount = Math.min(2, Runtime.getRuntime()
			.availableProcessors());

	public void setPollerThreadCount(int pollerThreadCount) {
		this.pollerThreadCount = pollerThreadCount;
	}

	public int getPollerThreadCount() {
		return pollerThreadCount;
	}

	private long selectorTimeout = 1000;

	public void setSelectorTimeout(long timeout) {
		this.selectorTimeout = timeout;
	}

	public long getSelectorTimeout() {
		return this.selectorTimeout;
	}

	/**
	 * The socket poller.
	 */
	private NioEndpointPoller[] pollers = null;
	private AtomicInteger pollerRotater = new AtomicInteger(0);

	/**
	 * Return an available poller in true round robin fashion
	 */
	public NioEndpointPoller getPoller0() {
		int idx = Math.abs(pollerRotater.incrementAndGet()) % pollers.length;
		return pollers[idx];
	}

	public void setSelectorPool(NioSelectorPool selectorPool) {
		this.selectorPool = selectorPool;
	}

	public void setSocketProperties(SocketProperties socketProperties) {
		this.setSocketProperties(socketProperties);
	}

	public void setUseSendfile(boolean useSendfile) {
		this.useSendfile = useSendfile;
	}

	/**
	 * Is deferAccept supported?
	 */
	@Override
	public boolean getDeferAccept() {
		// Not supported
		return false;
	}

	public void setOomParachute(int oomParachute) {
		this.oomParachute = oomParachute;
	}

	public void setOomParachuteData(byte[] oomParachuteData) {
		this.oomParachuteData = oomParachuteData;
	}

	private SSLContext sslContext = null;

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public void setSSLContext(SSLContext c) {
		sslContext = c;
	}

	private String[] enabledCiphers;
	private String[] enabledProtocols;

	/**
	 * Port in use.
	 */
	@Override
	public int getLocalPort() {
		ServerSocketChannel ssc = serverSock;
		if (ssc == null) {
			return -1;
		} else {
			ServerSocket s = ssc.socket();
			if (s == null) {
				return -1;
			} else {
				return s.getLocalPort();
			}
		}
	}

	// --------------------------------------------------------- OOM Parachute
	// Methods

	protected void checkParachute() {
		boolean para = reclaimParachute(false);
		if (!para && (System.currentTimeMillis() - lastParachuteCheck) > 10000) {
			try {
				log.fatal(oomParachuteMsg);
			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
				System.err.println(oomParachuteMsg);
			}
			lastParachuteCheck = System.currentTimeMillis();
		}
	}

	protected boolean reclaimParachute(boolean force) {
		if (oomParachuteData != null)
			return true;
		if (oomParachute > 0
				&& (force || (Runtime.getRuntime().freeMemory() > (oomParachute * 2))))
			oomParachuteData = new byte[oomParachute];
		return oomParachuteData != null;
	}

	protected void releaseCaches() {
		this.keyCache.clear();
		this.nioChannels.clear();
		this.processorCache.clear();
		if (handler != null)
			handler.recycle();

	}

	// --------------------------------------------------------- Public Methods
	/**
	 * Number of keepalive sockets.
	 */
	public int getKeepAliveCount() {
		if (pollers == null) {
			return 0;
		} else {
			int sum = 0;
			for (int i = 0; i < pollers.length; i++) {
				sum += pollers[i].getKeyCount();
			}
			return sum;
		}
	}

	// ----------------------------------------------- Public Lifecycle Methods

	/**
	 * Initialize the endpoint.
	 */
	@Override
	public void bind() throws Exception {

		serverSock = ServerSocketChannel.open();
		getSocketProperties().setProperties(serverSock.socket());
		InetSocketAddress addr = (getAddress() != null ? new InetSocketAddress(
				getAddress(), getPort()) : new InetSocketAddress(getPort()));
		serverSock.socket().bind(addr, getBacklog());
		serverSock.configureBlocking(true); // mimic APR behavior
		serverSock.socket().setSoTimeout(getSocketProperties().getSoTimeout());

		// Initialize thread count defaults for acceptor, poller
		if (getAcceptorThreadCount() == 0) {
			// FIXME: Doesn't seem to work that well with multiple accept
			// threads
			setAcceptorThreadCount(1);
		}
		if (pollerThreadCount <= 0) {
			// minimum one poller thread
			pollerThreadCount = 1;
		}
		stopLatch = new CountDownLatch(pollerThreadCount);

		// Initialize SSL if needed
		if (isSSLEnabled()) {
			SSLUtil sslUtil = handler.getSslImplementation().getSSLUtil(this);

			sslContext = sslUtil.createSSLContext();
			sslContext.init(wrap(sslUtil.getKeyManagers()),
					sslUtil.getTrustManagers(), null);

			SSLSessionContext sessionContext = sslContext
					.getServerSessionContext();
			if (sessionContext != null) {
				sslUtil.configureSessionContext(sessionContext);
			}
			// Determine which cipher suites and protocols to enable
			enabledCiphers = sslUtil.getEnableableCiphers(sslContext);
			enabledProtocols = sslUtil.getEnableableProtocols(sslContext);
		}

		if (oomParachute > 0)
			reclaimParachute(true);
		selectorPool.open();
	}

	public KeyManager[] wrap(KeyManager[] managers) {
		if (managers == null)
			return null;
		KeyManager[] result = new KeyManager[managers.length];
		for (int i = 0; i < result.length; i++) {
			if (managers[i] instanceof X509KeyManager && getKeyAlias() != null) {
				result[i] = new NioX509KeyManager((X509KeyManager) managers[i],
						getKeyAlias());
			} else {
				result[i] = managers[i];
			}
		}
		return result;
	}

	/**
	 * Start the NIO endpoint, creating acceptor, poller threads.
	 */
	@Override
	public void startInternal() throws Exception {

		if (!isRunning()) {
			setRunning(true);
			setPaused(false);

			// Create worker collection
			if (getExecutor() == null) {
				createExecutor();
			}

			initializeConnectionLatch();

			// Start poller threads
			pollers = new NioEndpointPoller[getPollerThreadCount()];
			for (int i = 0; i < pollers.length; i++) {
				pollers[i] = new NioEndpointPoller(this);
				Thread pollerThread = new Thread(pollers[i], getName()
						+ "-ClientPoller-" + i);
				pollerThread.setPriority(getThreadPriority());
				pollerThread.setDaemon(true);
				pollerThread.start();
			}

			startAcceptorThreads();
		}
	}

	/**
	 * Stop the endpoint. This will cause all processing threads to stop.
	 */
	@Override
	public void stopInternal() {
		releaseConnectionLatch();
		if (!isPaused()) {
			pause();
		}
		if (isRunning()) {
			setRunning(false);
			unlockAccept();
			for (int i = 0; pollers != null && i < pollers.length; i++) {
				if (pollers[i] == null)
					continue;
				pollers[i].destroy();
				pollers[i] = null;
			}
			try {
				stopLatch.await(selectorTimeout + 100, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ignore) {
			}
		}
		eventCache.clear();
		keyCache.clear();
		nioChannels.clear();
		processorCache.clear();
		shutdownExecutor();

	}

	/**
	 * Deallocate NIO memory pools, and close server socket.
	 */
	@Override
	public void unbind() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Destroy initiated for "
					+ new InetSocketAddress(getAddress(), getPort()));
		}
		if (isRunning()) {
			stop();
		}
		// Close server socket
		serverSock.socket().close();
		serverSock.close();
		serverSock = null;
		sslContext = null;
		releaseCaches();
		selectorPool.close();
		if (log.isDebugEnabled()) {
			log.debug("Destroy completed for "
					+ new InetSocketAddress(getAddress(), getPort()));
		}
	}

	// ------------------------------------------------------ Protected Methods

	public int getWriteBufSize() {
		return getSocketProperties().getTxBufSize();
	}

	public int getReadBufSize() {
		return getSocketProperties().getRxBufSize();
	}

	public NioSelectorPool getSelectorPool() {
		return selectorPool;
	}

	@Override
	public boolean getUseSendfile() {
		return useSendfile;
	}

	public int getOomParachute() {
		return oomParachute;
	}

	public byte[] getOomParachuteData() {
		return oomParachuteData;
	}

	@Override
	protected AbstractEndpointAcceptor createAcceptor() {
		return new NioEndpointAcceptor(this);
	}

	/**
	 * Process the specified connection.
	 */
	protected boolean setSocketOptions(SocketChannel socket) {
		// Process the connection
		try {
			// disable blocking, APR style, we are gonna be polling it
			socket.configureBlocking(false);
			Socket sock = socket.socket();
			getSocketProperties().setProperties(sock);

			NioChannel channel = nioChannels.poll();
			if (channel == null) {
				// SSL setup
				if (sslContext != null) {
					SSLEngine engine = createSSLEngine();
					int appbufsize = engine.getSession()
							.getApplicationBufferSize();
					NioEndpointNioBufferHandler bufhandler = new NioEndpointNioBufferHandler(
							Math.max(appbufsize, getSocketProperties()
									.getAppReadBufSize()),
							Math.max(appbufsize, getSocketProperties()
									.getAppWriteBufSize()),
							getSocketProperties().getDirectBuffer());
					channel = new SecureNioChannel(socket, engine, bufhandler,
							selectorPool);
				} else {
					// normal tcp setup
					NioEndpointNioBufferHandler bufhandler = new NioEndpointNioBufferHandler(
							getSocketProperties().getAppReadBufSize(),
							getSocketProperties().getAppWriteBufSize(),
							getSocketProperties().getDirectBuffer());

					channel = new NioChannel(socket, bufhandler);
				}
			} else {
				channel.setIOChannel(socket);
				if (channel instanceof SecureNioChannel) {
					SSLEngine engine = createSSLEngine();
					((SecureNioChannel) channel).reset(engine);
				} else {
					channel.reset();
				}
			}
			getPoller0().register(channel);
		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			try {
				log.error("", t);
			} catch (Throwable tt) {
				ExceptionUtils2.handleThrowable(t);
			}
			// Tell to close the socket
			return false;
		}
		return true;
	}

	protected SSLEngine createSSLEngine() {
		SSLEngine engine = sslContext.createSSLEngine();
		if ("false".equals(getClientAuth())) {
			engine.setNeedClientAuth(false);
			engine.setWantClientAuth(false);
		} else if ("true".equals(getClientAuth())
				|| "yes".equals(getClientAuth())) {
			engine.setNeedClientAuth(true);
		} else if ("want".equals(getClientAuth())) {
			engine.setWantClientAuth(true);
		}
		engine.setUseClientMode(false);
		engine.setEnabledCipherSuites(enabledCiphers);
		engine.setEnabledProtocols(enabledProtocols);

		return engine;
	}

	/**
	 * Returns true if a worker thread is available for processing.
	 * 
	 * @return boolean
	 */
	protected boolean isWorkerAvailable() {
		return true;
	}

	@Override
	public void processSocketAsync(SocketWrapper<NioChannel> socketWrapper,
			SocketStatus socketStatus) {
		dispatchForEvent(socketWrapper.getSocket(), socketStatus, true);
	}

	public boolean dispatchForEvent(NioChannel socket, SocketStatus status,
			boolean dispatch) {
		if (dispatch && status == SocketStatus.OPEN_READ) {
			socket.getPoller().add(socket, OP_CALLBACK);
		} else {
			processSocket(socket, status, dispatch);
		}
		return true;
	}

	public boolean processSocket(NioChannel socket, SocketStatus status,
			boolean dispatch) {
		try {
			NioEndpointKeyAttachment attachment = (NioEndpointKeyAttachment) socket
					.getAttachment(false);
			if (attachment == null) {
				return false;
			}
			attachment.setCometNotify(false); // will get reset upon next reg
			NioEndpointSocketProcessor sc = processorCache.poll();
			if (sc == null)
				sc = new NioEndpointSocketProcessor(this, socket, status);
			else
				sc.reset(socket, status);
			if (dispatch && getExecutor() != null)
				getExecutor().execute(sc);
			else
				sc.run();
		} catch (RejectedExecutionException rx) {
			log.warn("Socket processing request was rejected for:" + socket, rx);
			return false;
		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			// This means we got an OOM or similar creating a thread, or that
			// the pool and its queue are full
			log.error(getSm().getString("endpoint.process.fail"), t);
			return false;
		}
		return true;
	}

	@Override
	protected Log getLog() {
		return log;
	}

	public void closeSocket(SocketChannel socket) {
		try {
			socket.socket().close();
		} catch (IOException ioe) {
			if (log.isDebugEnabled()) {
				log.debug("", ioe);
			}
		}
		try {
			socket.close();
		} catch (IOException ioe) {
			if (log.isDebugEnabled()) {
				log.debug("", ioe);
			}
		}
	}

	public ServerSocketChannel getServerSock() {
		return serverSock;
	}

	public void setServerSock(ServerSocketChannel serverSock) {
		this.serverSock = serverSock;
	}

	public long getLastParachuteCheck() {
		return lastParachuteCheck;
	}

	public void setLastParachuteCheck(long lastParachuteCheck) {
		this.lastParachuteCheck = lastParachuteCheck;
	}

	public CountDownLatch getStopLatch() {
		return stopLatch;
	}

	public void setStopLatch(CountDownLatch stopLatch) {
		this.stopLatch = stopLatch;
	}

	public ConcurrentLinkedQueue<NioEndpointSocketProcessor> getProcessorCache() {
		return processorCache;
	}

	public void setProcessorCache(
			ConcurrentLinkedQueue<NioEndpointSocketProcessor> processorCache) {
		this.processorCache = processorCache;
	}

	public ConcurrentLinkedQueue<NioEndpointKeyAttachment> getKeyCache() {
		return keyCache;
	}

	public void setKeyCache(
			ConcurrentLinkedQueue<NioEndpointKeyAttachment> keyCache) {
		this.keyCache = keyCache;
	}

	public ConcurrentLinkedQueue<NioEndpointPollerEvent> getEventCache() {
		return eventCache;
	}

	public void setEventCache(
			ConcurrentLinkedQueue<NioEndpointPollerEvent> eventCache) {
		this.eventCache = eventCache;
	}

	public ConcurrentLinkedQueue<NioChannel> getNioChannels() {
		return nioChannels;
	}

	public void setNioChannels(ConcurrentLinkedQueue<NioChannel> nioChannels) {
		this.nioChannels = nioChannels;
	}

	public NioEndpointPoller[] getPollers() {
		return pollers;
	}

	public void setPollers(NioEndpointPoller[] pollers) {
		this.pollers = pollers;
	}

	public AtomicInteger getPollerRotater() {
		return pollerRotater;
	}

	public void setPollerRotater(AtomicInteger pollerRotater) {
		this.pollerRotater = pollerRotater;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public String[] getEnabledCiphers() {
		return enabledCiphers;
	}

	public void setEnabledCiphers(String[] enabledCiphers) {
		this.enabledCiphers = enabledCiphers;
	}

	public String[] getEnabledProtocols() {
		return enabledProtocols;
	}

	public void setEnabledProtocols(String[] enabledProtocols) {
		this.enabledProtocols = enabledProtocols;
	}

	public static int getOpRegister() {
		return OP_REGISTER;
	}

	public static int getOpCallback() {
		return OP_CALLBACK;
	}

	public static String getOomparachutemsg() {
		return oomParachuteMsg;
	}	
}
