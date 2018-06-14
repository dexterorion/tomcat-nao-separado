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
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.security.PrivilegedSetTccl;


/**
 * Handle incoming TCP connections.
 *
 * This class implement a simple server model: one listener thread accepts on a socket and
 * creates a new worker thread for each incoming connection.
 *
 * More advanced Endpoints will reuse the threads, use queues, etc.
 *
 * @author James Duncan Davidson
 * @author Jason Hunter
 * @author James Todd
 * @author Costin Manolache
 * @author Gal Shachor
 * @author Yoav Shapira
 * @author Remy Maucherat
 */
public class JIoEndpoint extends AbstractEndpoint<Socket> {


    // -------------------------------------------------------------- Constants

    private static final Log log = LogFactory.getLog(JIoEndpoint.class);

    // ----------------------------------------------------------------- Fields

    /**
     * Associated server socket.
     */
    private ServerSocket serverSocket = null;


    // ------------------------------------------------------------ Constructor

    public JIoEndpoint() {
        // Set maxConnections to zero so we can tell if the user has specified
        // their own value on the connector when we reach bind()
        setMaxConnections(0);
        // Reduce the executor timeout for BIO as threads in keep-alive will not
        // terminate when the executor interrupts them.
        setExecutorTerminationTimeoutMillis(0);
    }

    // ------------------------------------------------------------- Properties

    /**
     * Handling of accepted sockets.
     */
    private JloEndpointHandler handler = null;
    public void setHandler(JloEndpointHandler handler ) { this.setHandlerData(handler); }
    public JloEndpointHandler getHandler() { return getHandlerData(); }

    /**
     * Server socket factory.
     */
    private ServerSocketFactory serverSocketFactory = null;
    public void setServerSocketFactory(ServerSocketFactory factory) { this.setServerSocketFactoryData(factory); }
    public ServerSocketFactory getServerSocketFactory() { return getServerSocketFactoryData(); }

    /**
     * Port in use.
     */
    @Override
    public int getLocalPort() {
        ServerSocket s = getServerSocket();
        if (s == null) {
            return -1;
        } else {
            return s.getLocalPort();
        }
    }

    /*
     * Optional feature support.
     */
    @Override
    public boolean getUseSendfile() { return false; }
    @Override
    public boolean getUseComet() { return false; }
    @Override
    public boolean getUseCometTimeout() { return false; }
    @Override
    public boolean getDeferAccept() { return false; }
    @Override
    public boolean getUsePolling() { return false; }


    // ------------------------------------------------ Handler Inner Interface

    public void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }


    // -------------------- Public methods --------------------

    @Override
    public void bind() throws Exception {

        // Initialize thread count defaults for acceptor
        if (getAcceptorThreadCount() == 0) {
            setAcceptorThreadCount(1);
        }
        // Initialize maxConnections
        if (getMaxConnections() == 0) {
            // User hasn't set a value - use the default
            setMaxConnections(getMaxThreadsExecutor(true));
        }

        if (getServerSocketFactoryData() == null) {
            if (isSSLEnabled()) {
                setServerSocketFactoryData(getHandlerData().getSslImplementation().getServerSocketFactory(this));
            } else {
                setServerSocketFactoryData(new DefaultServerSocketFactory(this));
            }
        }

        if (getServerSocket() == null) {
            try {
                if (getAddress() == null) {
                    setServerSocket(getServerSocketFactoryData().createSocket(getPort(),
                            getBacklog()));
                } else {
                    setServerSocket(getServerSocketFactoryData().createSocket(getPort(),
                            getBacklog(), getAddress()));
                }
            } catch (BindException orig) {
                String msg;
                if (getAddress() == null)
                    msg = orig.getMessage() + " <null>:" + getPort();
                else
                    msg = orig.getMessage() + " " +
                            getAddress().toString() + ":" + getPort();
                BindException be = new BindException(msg);
                be.initCause(orig);
                throw be;
            }
        }

    }

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

            startAcceptorThreads();

            // Start async timeout thread
            Thread timeoutThread = new Thread(new JloEndpointAsyncTimeout(this),
                    getName() + "-AsyncTimeout");
            timeoutThread.setPriority(getThreadPriority());
            timeoutThread.setDaemon(true);
            timeoutThread.start();
        }
    }

    @Override
    public void stopInternal() {
        releaseConnectionLatch();
        if (!isPaused()) {
            pause();
        }
        if (isRunning()) {
            setRunning(false);
            unlockAccept();
        }
        shutdownExecutor();
    }

    /**
     * Deallocate APR memory pools, and close server socket.
     */
    @Override
    public void unbind() throws Exception {
        if (isRunning()) {
            stop();
        }
        if (getServerSocket() != null) {
            try {
                if (getServerSocket() != null)
                    getServerSocket().close();
            } catch (Exception e) {
                log.error(getSm().getString("endpoint.err.close"), e);
            }
            setServerSocket(null);
        }
        getHandlerData().recycle();
    }


    @Override
    protected AbstractEndpointAcceptor createAcceptor() {
        return new JloEndpointAcceptor(this);
    }


    /**
     * Configure the socket.
     */
    protected boolean setSocketOptions(Socket socket) {
        try {
            // 1: Set socket options: timeout, linger, etc
            getSocketProperties().setProperties(socket);
        } catch (SocketException s) {
            //error here is common if the client has reset the connection
            if (log.isDebugEnabled()) {
                log.debug(getSm().getString("endpoint.err.unexpected"), s);
            }
            // Close the socket
            return false;
        } catch (Throwable t) {
            ExceptionUtils2.handleThrowable(t);
            log.error(getSm().getString("endpoint.err.unexpected"), t);
            // Close the socket
            return false;
        }
        return true;
    }


    /**
     * Process a new connection from a new client. Wraps the socket so
     * keep-alive and other attributes can be tracked and then passes the socket
     * to the executor for processing.
     *
     * @param socket    The socket associated with the client.
     *
     * @return          <code>true</code> if the socket is passed to the
     *                  executor, <code>false</code> if something went wrong or
     *                  if the endpoint is shutting down. Returning
     *                  <code>false</code> is an indication to close the socket
     *                  immediately.
     */
    protected boolean processSocket(Socket socket) {
        // Process the request from this socket
        try {
            SocketWrapper<Socket> wrapper = new SocketWrapper<Socket>(socket);
            wrapper.setKeepAliveLeft(getMaxKeepAliveRequests());
            wrapper.setSecure(isSSLEnabled());
            // During shutdown, executor may be null - avoid NPE
            if (!isRunning()) {
                return false;
            }
            getExecutor().execute(new JloEndpointSocketProcessor(this, wrapper));
        } catch (RejectedExecutionException x) {
            log.warn("Socket processing request was rejected for:"+socket,x);
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


    /**
     * Process an existing async connection. If processing is required, passes
     * the wrapped socket to an executor for processing.
     *
     * @param socket    The socket associated with the client.
     * @param status    Only OPEN and TIMEOUT are used. The others are used for
     *                  Comet requests that are not supported by the BIO (JIO)
     *                  Connector.
     */
    @Override
    public void processSocketAsync(SocketWrapper<Socket> socket,
            SocketStatus status) {
        try {
            synchronized (socket) {
                if (getWaitingRequestsData().remove(socket)) {
                	JloEndpointSocketProcessor proc = new JloEndpointSocketProcessor(this, socket,status);
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    try {
                        //threads should not be created by the webapp classloader
                        if (Constants34.isSecurityEnabled()) {
                            PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                                    getClass().getClassLoader());
                            AccessController.doPrivileged(pa);
                        } else {
                            Thread.currentThread().setContextClassLoader(
                                    getClass().getClassLoader());
                        }
                        // During shutdown, executor may be null - avoid NPE
                        if (!isRunning()) {
                            return;
                        }
                        getExecutor().execute(proc);
                        //TODO gotta catch RejectedExecutionException and properly handle it
                    } finally {
                        if (Constants34.isSecurityEnabled()) {
                            PrivilegedAction<Void> pa = new PrivilegedSetTccl(loader);
                            AccessController.doPrivileged(pa);
                        } else {
                            Thread.currentThread().setContextClassLoader(loader);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            ExceptionUtils2.handleThrowable(t);
            // This means we got an OOM or similar creating a thread, or that
            // the pool and its queue are full
            log.error(getSm().getString("endpoint.process.fail"), t);
        }
    }

    private ConcurrentLinkedQueue<SocketWrapper<Socket>> waitingRequests =
        new ConcurrentLinkedQueue<SocketWrapper<Socket>>();

    @Override
    public Log getLog() {
        return log;
    }
	public ServerSocket getServerSocket() {
		return getServerSocketData();
	}
	public void setServerSocket(ServerSocket serverSocket) {
		this.setServerSocketData(serverSocket);
	}
	public ServerSocket getServerSocketData() {
		return serverSocket;
	}
	public void setServerSocketData(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}
	public JloEndpointHandler getHandlerData() {
		return handler;
	}
	public void setHandlerData(JloEndpointHandler handler) {
		this.handler = handler;
	}
	public ServerSocketFactory getServerSocketFactoryData() {
		return serverSocketFactory;
	}
	public void setServerSocketFactoryData(ServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}
	public ConcurrentLinkedQueue<SocketWrapper<Socket>> getWaitingRequestsData() {
		return waitingRequests;
	}
	public void setWaitingRequestsData(ConcurrentLinkedQueue<SocketWrapper<Socket>> waitingRequests) {
		this.waitingRequests = waitingRequests;
	}
}
