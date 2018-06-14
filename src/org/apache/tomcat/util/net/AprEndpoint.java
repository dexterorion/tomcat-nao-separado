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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.security.PrivilegedSetTccl;


/**
 * APR tailored thread pool, providing the following services:
 * <ul>
 * <li>Socket acceptor thread</li>
 * <li>Socket poller thread</li>
 * <li>Sendfile thread</li>
 * <li>Worker threads pool</li>
 * </ul>
 *
 * When switching to Java 5, there's an opportunity to use the virtual
 * machine's thread pool.
 *
 * @author Mladen Turk
 * @author Remy Maucherat
 */
public class AprEndpoint extends AbstractEndpoint<Long> {


    // -------------------------------------------------------------- Constants


    private static final Log log = LogFactory.getLog(AprEndpoint.class);
    
    // ----------------------------------------------------------------- Fields
    /**
     * Root APR memory pool.
     */
    private long rootPool = 0;


    public long getRootPool() {
		return rootPool;
	}
	public void setRootPool(long rootPool) {
		this.rootPool = rootPool;
	}
	public long getServerSock() {
		return serverSock;
	}
	public void setServerSock(long serverSock) {
		this.serverSock = serverSock;
	}
	public long getServerSockPool() {
		return serverSockPool;
	}
	public void setServerSockPool(long serverSockPool) {
		this.serverSockPool = serverSockPool;
	}
	public long getSslContext() {
		return sslContext;
	}
	public void setSslContext(long sslContext) {
		this.sslContext = sslContext;
	}

	/**
     * Server socket "pointer".
     */
    private long serverSock = 0;


    /**
     * APR memory pool for the server socket.
     */
    private long serverSockPool = 0;


    /**
     * SSL context.
     */
    private long sslContext = 0;


    private ConcurrentLinkedQueue<SocketWrapper<Long>> waitingRequests =
        new ConcurrentLinkedQueue<SocketWrapper<Long>>();

    private final Map<Long,AprEndpointAprSocketWrapper> connections =
            new ConcurrentHashMap<Long, AprEndpointAprSocketWrapper>();

    // ------------------------------------------------------------ Constructor

    public AprEndpoint() {
        // Need to override the default for maxConnections to align it with what
        // was pollerSize (before the two were merged)
        setMaxConnections(8 * 1024);
    }

    // ------------------------------------------------------------- Properties


    /**
     * Defer accept.
     */
    private boolean deferAccept = true;
    public void setDeferAccept(boolean deferAccept) { this.deferAccept = deferAccept; }
    @Override
    public boolean getDeferAccept() { return deferAccept; }


    /**
     * Size of the sendfile (= concurrent files which can be served).
     */
    private int sendfileSize = 1 * 1024;
    public void setSendfileSize(int sendfileSize) { this.sendfileSize = sendfileSize; }
    public int getSendfileSize() { return sendfileSize; }


    /**
     * Handling of accepted sockets.
     */
    private AprEndpointHandler handler = null;
    public void setHandler(AprEndpointHandler handler ) { this.handler = handler; }
    public AprEndpointHandler getHandler() { return handler; }


    /**
     * Poll interval, in microseconds. The smaller the value, the more CPU the poller
     * will use, but the more responsive to activity it will be.
     */
    private int pollTime = 2000;
    public int getPollTime() { return pollTime; }
    public void setPollTime(int pollTime) { if (pollTime > 0) { this.pollTime = pollTime; } }


    /**
     * Use sendfile for sending static files.
     */
    private boolean useSendfile = Library.isAPR_HAS_SENDFILE();
    public void setUseSendfile(boolean useSendfile) { this.useSendfile = useSendfile; }
    @Override
    public boolean getUseSendfile() { return useSendfile; }


    /**
     * Allow comet request handling.
     */
    private boolean useComet = true;
    public void setUseComet(boolean useComet) { this.useComet = useComet; }
    @Override
    public boolean getUseComet() { return useComet; }
    @Override
    public boolean getUseCometTimeout() { return false; }
    @Override
    public boolean getUsePolling() { return true; }


    /**
     * Sendfile thread count.
     */
    private int sendfileThreadCount = 0;
    public void setSendfileThreadCount(int sendfileThreadCount) { this.sendfileThreadCount = sendfileThreadCount; }
    public int getSendfileThreadCount() { return sendfileThreadCount; }


    /**
     * The socket poller.
     */
    private AprEndpointPoller poller = null;
    public AprEndpointPoller getPoller() {
        return poller;
    }


    /**
     * The socket poller.
     */
    private AprEndpointAsyncTimeout asyncTimeout = null;
    public AprEndpointAsyncTimeout getAsyncTimeout() {
        return asyncTimeout;
    }


    /**
     * The static file sender.
     */
    private AprEndpointSendfile sendfile = null;
    public AprEndpointSendfile getSendfile() {
        return sendfile;
    }


    /**
     * SSL protocols.
     */
    private String SSLProtocol = "all";
    public String getSSLProtocol() { return SSLProtocol; }
    public void setSSLProtocol(String SSLProtocol) { this.SSLProtocol = SSLProtocol; }


    /**
     * SSL password (if a cert is encrypted, and no password has been provided, a callback
     * will ask for a password).
     */
    private String SSLPassword = null;
    public String getSSLPassword() { return SSLPassword; }
    public void setSSLPassword(String SSLPassword) { this.SSLPassword = SSLPassword; }


    /**
     * SSL cipher suite.
     */
    private String SSLCipherSuite = "ALL";
    public String getSSLCipherSuite() { return SSLCipherSuite; }
    public void setSSLCipherSuite(String SSLCipherSuite) { this.SSLCipherSuite = SSLCipherSuite; }


    /**
     * SSL certificate file.
     */
    private String SSLCertificateFile = null;
    public String getSSLCertificateFile() { return SSLCertificateFile; }
    public void setSSLCertificateFile(String SSLCertificateFile) { this.SSLCertificateFile = SSLCertificateFile; }


    /**
     * SSL certificate key file.
     */
    private String SSLCertificateKeyFile = null;
    public String getSSLCertificateKeyFile() { return SSLCertificateKeyFile; }
    public void setSSLCertificateKeyFile(String SSLCertificateKeyFile) { this.SSLCertificateKeyFile = SSLCertificateKeyFile; }


    /**
     * SSL certificate chain file.
     */
    private String SSLCertificateChainFile = null;
    public String getSSLCertificateChainFile() { return SSLCertificateChainFile; }
    public void setSSLCertificateChainFile(String SSLCertificateChainFile) { this.SSLCertificateChainFile = SSLCertificateChainFile; }


    /**
     * SSL CA certificate path.
     */
    private String SSLCACertificatePath = null;
    public String getSSLCACertificatePath() { return SSLCACertificatePath; }
    public void setSSLCACertificatePath(String SSLCACertificatePath) { this.SSLCACertificatePath = SSLCACertificatePath; }


    /**
     * SSL CA certificate file.
     */
    private String SSLCACertificateFile = null;
    public String getSSLCACertificateFile() { return SSLCACertificateFile; }
    public void setSSLCACertificateFile(String SSLCACertificateFile) { this.SSLCACertificateFile = SSLCACertificateFile; }


    /**
     * SSL CA revocation path.
     */
    private String SSLCARevocationPath = null;
    public String getSSLCARevocationPath() { return SSLCARevocationPath; }
    public void setSSLCARevocationPath(String SSLCARevocationPath) { this.SSLCARevocationPath = SSLCARevocationPath; }


    /**
     * SSL CA revocation file.
     */
    private String SSLCARevocationFile = null;
    public String getSSLCARevocationFile() { return SSLCARevocationFile; }
    public void setSSLCARevocationFile(String SSLCARevocationFile) { this.SSLCARevocationFile = SSLCARevocationFile; }


    /**
     * SSL verify client.
     */
    private String SSLVerifyClient = "none";
    public String getSSLVerifyClient() { return SSLVerifyClient; }
    public void setSSLVerifyClient(String SSLVerifyClient) { this.SSLVerifyClient = SSLVerifyClient; }


    /**
     * SSL verify depth.
     */
    private int SSLVerifyDepth = 10;
    public int getSSLVerifyDepth() { return SSLVerifyDepth; }
    public void setSSLVerifyDepth(int SSLVerifyDepth) { this.SSLVerifyDepth = SSLVerifyDepth; }


    /**
     * SSL allow insecure renegotiation for the the client that does not
     * support the secure renegotiation.
     */
    private boolean SSLInsecureRenegotiation = false;
    public void setSSLInsecureRenegotiation(boolean SSLInsecureRenegotiation) { this.SSLInsecureRenegotiation = SSLInsecureRenegotiation; }
    public boolean getSSLInsecureRenegotiation() { return SSLInsecureRenegotiation; }

    private boolean SSLHonorCipherOrder = false;
    /**
     * Set to <code>true</code> to enforce the <i>server's</i> cipher order
     * instead of the default which is to allow the client to choose a
     * preferred cipher.
     */
    public void setSSLHonorCipherOrder(boolean SSLHonorCipherOrder) { this.SSLHonorCipherOrder = SSLHonorCipherOrder; }
    public boolean getSSLHonorCipherOrder() { return SSLHonorCipherOrder; }

    /**
     * Disables compression of the SSL stream. This thwarts CRIME attack
     * and possibly improves performance by not compressing uncompressible
     * content such as JPEG, etc.
     */
    private boolean SSLDisableCompression = false;

    /**
     * Set to <code>true</code> to disable SSL compression. This thwarts CRIME
     * attack.
     */
    public void setSSLDisableCompression(boolean SSLDisableCompression) { this.SSLDisableCompression = SSLDisableCompression; }
    public boolean getSSLDisableCompression() { return SSLDisableCompression; }

    /**
     * Port in use.
     */
    @Override
    public int getLocalPort() {
        long s = serverSock;
        if (s == 0) {
            return -1;
        } else {
            long sa;
            try {
                sa = Address.get(Socket.getAprLocal(), s);
                Sockaddr addr = Address.getInfo(sa);
                return addr.getPort();
            } catch (Exception e) {
                return -1;
            }
        }
    }


    /**
     * This endpoint does not support <code>-1</code> for unlimited connections,
     * nor does it support setting this attribute while the endpoint is running.
     *
     * {@inheritDoc}
     */
    @Override
    public void setMaxConnections(int maxConnections) {
        if (maxConnections == -1) {
            log.warn(getSm().getString("endpoint.apr.maxConnections.unlimited",
                    Integer.valueOf(getMaxConnections())));
            return;
        }
        if (isRunning()) {
            log.warn(getSm().getString("endpoint.apr.maxConnections.running",
                    Integer.valueOf(getMaxConnections())));
            return;
        }
        super.setMaxConnections(maxConnections);
    }


    // --------------------------------------------------------- Public Methods

    /**
     * Number of keepalive sockets.
     */
    public int getKeepAliveCount() {
        if (poller == null) {
            return 0;
        }

        return poller.getConnectionCount();
    }


    /**
     * Number of sendfile sockets.
     */
    public int getSendfileCount() {
        if (sendfile == null) {
            return 0;
        }

        return sendfile.getSendfileCount();
    }


    // ----------------------------------------------- Public Lifecycle Methods


    /**
     * Initialize the endpoint.
     */
    @Override
    public void bind() throws Exception {

        // Create the root APR memory pool
        try {
            rootPool = Pool.create(0);
        } catch (UnsatisfiedLinkError e) {
            throw new Exception(getSm().getString("endpoint.init.notavail"));
        }

        // Create the pool for the server socket
        serverSockPool = Pool.create(rootPool);
        // Create the APR address that will be bound
        String addressStr = null;
        if (getAddress() != null) {
            addressStr = getAddress().getHostAddress();
        }
        int family = Socket.getAprInet();
        if (Library.isAPR_HAVE_IPV6()) {
            if (addressStr == null) {
                if (!OS.isBsd() && !OS.isWin32() && !OS.isWin64())
                    family = Socket.getAprUnspec();
            } else if (addressStr.indexOf(':') >= 0) {
                family = Socket.getAprUnspec();
            }
         }

        long inetAddress = Address.info(addressStr, family,
                getPort(), 0, rootPool);
        // Create the APR server socket
        serverSock = Socket.create(Address.getInfo(inetAddress).getFamily(),
                Socket.getSockStream(),
                Socket.getAprProtoTcp(), rootPool);
        if (OS.isUnix()) {
            Socket.optSet(serverSock, Socket.getAprSoReuseaddr(), 1);
        }
        // Deal with the firewalls that tend to drop the inactive sockets
        Socket.optSet(serverSock, Socket.getAprSoKeepalive(), 1);
        // Bind the server socket
        int ret = Socket.bind(serverSock, inetAddress);
        if (ret != 0) {
            throw new Exception(getSm().getString("endpoint.init.bind", "" + ret, Error.strerror(ret)));
        }
        // Start listening on the server socket
        ret = Socket.listen(serverSock, getBacklog());
        if (ret != 0) {
            throw new Exception(getSm().getString("endpoint.init.listen", "" + ret, Error.strerror(ret)));
        }
        if (OS.isWin32() || OS.isWin64()) {
            // On Windows set the reuseaddr flag after the bind/listen
            Socket.optSet(serverSock, Socket.getAprSoReuseaddr(), 1);
        }

        // Sendfile usage on systems which don't support it cause major problems
        if (useSendfile && !Library.isAPR_HAS_SENDFILE()) {
            useSendfile = false;
        }

        // Initialize thread count default for acceptor
        if (getAcceptorThreadCount() == 0) {
            // FIXME: Doesn't seem to work that well with multiple accept threads
        	setAcceptorThreadCount(1);
        }

        // Delay accepting of new connections until data is available
        // Only Linux kernels 2.4 + have that implemented
        // on other platforms this call is noop and will return APR_ENOTIMPL.
        if (deferAccept) {
            if (Socket.optSet(serverSock, Socket.getAprTcpDeferAccept(), 1) == Status.getAprEnotimpl()) {
                deferAccept = false;
            }
        }

        // Initialize SSL if needed
        if (isSSLEnabled()) {

            if (SSLCertificateFile == null) {
                // This is required
                throw new Exception(getSm().getString("endpoint.apr.noSslCertFile"));
            }

            // SSL protocol
            int value = SSL.getSslProtocolNone();
            if (SSLProtocol == null || SSLProtocol.length() == 0) {
                value = SSL.getSslProtocolAll();
            } else {
                for (String protocol : SSLProtocol.split("\\+")) {
                    protocol = protocol.trim();
                    if ("SSLv2".equalsIgnoreCase(protocol)) {
                        value |= SSL.getSslProtocolSslv2();
                    } else if ("SSLv3".equalsIgnoreCase(protocol)) {
                        value |= SSL.getSslProtocolSslv3();
                    } else if ("TLSv1".equalsIgnoreCase(protocol)) {
                        value |= SSL.getSslProtocolTlsv1();
                    } else if ("TLSv1.1".equalsIgnoreCase(protocol)) {
                        value |= SSL.getSslProtocolTlsv11();
                    } else if ("TLSv1.2".equalsIgnoreCase(protocol)) {
                        value |= SSL.getSslProtocolTlsv12();
                    } else if ("all".equalsIgnoreCase(protocol)) {
                        value |= SSL.getSslProtocolAll();
                    } else {
                        // Protocol not recognized, fail to start as it is safer than
                        // continuing with the default which might enable more than the
                        // is required
                        throw new Exception(getSm().getString(
                                "endpoint.apr.invalidSslProtocol", SSLProtocol));
                    }
                }
            }

            // Create SSL Context
            try {
                sslContext = SSLContext.make(rootPool, value, SSL.getSslModeServer());
            } catch (Exception e) {
                // If the sslEngine is disabled on the AprLifecycleListener
                // there will be an Exception here but there is no way to check
                // the AprLifecycleListener settings from here
                throw new Exception(
                        getSm().getString("endpoint.apr.failSslContextMake"), e);
            }

            if (SSLInsecureRenegotiation) {
                boolean legacyRenegSupported = false;
                try {
                    legacyRenegSupported = SSL.hasOp(SSL.getSslOpAllowUnsafeLegacyRenegotiation());
                    if (legacyRenegSupported)
                        SSLContext.setOptions(sslContext, SSL.getSslOpAllowUnsafeLegacyRenegotiation());
                } catch (UnsatisfiedLinkError e) {
                    // Ignore
                }
                if (!legacyRenegSupported) {
                    // OpenSSL does not support unsafe legacy renegotiation.
                    log.warn(getSm().getString("endpoint.warn.noInsecureReneg",
                                          SSL.versionString()));
                }
            }

            // Set cipher order: client (default) or server
            if (SSLHonorCipherOrder) {
                boolean orderCiphersSupported = false;
                try {
                    orderCiphersSupported = SSL.hasOp(SSL.getSslOpCipherServerPreference());
                    if (orderCiphersSupported)
                        SSLContext.setOptions(sslContext, SSL.getSslOpCipherServerPreference());
                } catch (UnsatisfiedLinkError e) {
                    // Ignore
                }
                if (!orderCiphersSupported) {
                    // OpenSSL does not support ciphers ordering.
                    log.warn(getSm().getString("endpoint.warn.noHonorCipherOrder",
                                          SSL.versionString()));
                }
            }

            // Disable compression if requested
            if (SSLDisableCompression) {
                boolean disableCompressionSupported = false;
                try {
                    disableCompressionSupported = SSL.hasOp(SSL.getSslOpNoCompression());
                    if (disableCompressionSupported)
                        SSLContext.setOptions(sslContext, SSL.getSslOpNoCompression());
                } catch (UnsatisfiedLinkError e) {
                    // Ignore
                }
                if (!disableCompressionSupported) {
                    // OpenSSL does not support ciphers ordering.
                    log.warn(getSm().getString("endpoint.warn.noDisableCompression",
                                          SSL.versionString()));
                }
            }

            // List the ciphers that the client is permitted to negotiate
            SSLContext.setCipherSuite(sslContext, SSLCipherSuite);
            // Load Server key and certificate
            SSLContext.setCertificate(sslContext, SSLCertificateFile, SSLCertificateKeyFile, SSLPassword, SSL.getSslAidxRsa());
            // Set certificate chain file
            SSLContext.setCertificateChainFile(sslContext, SSLCertificateChainFile, false);
            // Support Client Certificates
            SSLContext.setCACertificate(sslContext, SSLCACertificateFile, SSLCACertificatePath);
            // Set revocation
            SSLContext.setCARevocation(sslContext, SSLCARevocationFile, SSLCARevocationPath);
            // Client certificate verification
            value = SSL.getSslCverifyNone();
            if ("optional".equalsIgnoreCase(SSLVerifyClient)) {
                value = SSL.getSslCverifyOptional();
            } else if ("require".equalsIgnoreCase(SSLVerifyClient)) {
                value = SSL.getSslCverifyRequire();
            } else if ("optionalNoCA".equalsIgnoreCase(SSLVerifyClient)) {
                value = SSL.getSslCverifyOptionalNoCa();
            }
            SSLContext.setVerify(sslContext, value, SSLVerifyDepth);
            // For now, sendfile is not supported with SSL
            useSendfile = false;
        }
    }


    /**
     * Start the APR endpoint, creating acceptor, poller and sendfile threads.
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

            // Start poller thread
            poller = new AprEndpointPoller(this);
            poller.init();
            Thread pollerThread = new Thread(poller, getName() + "-Poller");
            pollerThread.setPriority(getThreadPriority());
            pollerThread.setDaemon(true);
            pollerThread.start();

            // Start sendfile thread
            if (useSendfile) {
                sendfile = new AprEndpointSendfile(this);
                sendfile.init();
                Thread sendfileThread =
                        new Thread(sendfile, getName() + "-Sendfile");
                sendfileThread.setPriority(getThreadPriority());
                sendfileThread.setDaemon(true);
                sendfileThread.start();
            }

            startAcceptorThreads();

            // Start async timeout thread
            asyncTimeout = new AprEndpointAsyncTimeout(this);
            Thread timeoutThread = new Thread(asyncTimeout,
                    getName() + "-AsyncTimeout");
            timeoutThread.setPriority(getThreadPriority());
            timeoutThread.setDaemon(true);
            timeoutThread.start();
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
            poller.stop();
            asyncTimeout.stop();
            unlockAccept();
            for (AbstractEndpointAcceptor acceptor : getAcceptors()) {
                long waitLeft = 10000;
                while (waitLeft > 0 &&
                        acceptor.getState() != AcceptorState.ENDED &&
                        serverSock != 0) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    waitLeft -= 50;
                }
                if (waitLeft == 0) {
                    log.warn(getSm().getString("endpoint.warn.unlockAcceptorFailed",
                            acceptor.getThreadName()));
                   // If the Acceptor is still running force
                   // the hard socket close.
                   if (serverSock != 0) {
                       Socket.shutdown(serverSock, Socket.getAprShutdownRead());
                       serverSock = 0;
                   }
                }
            }
            try {
                poller.destroy();
            } catch (Exception e) {
                // Ignore
            }
            poller = null;
            connections.clear();
            if (useSendfile) {
                try {
                    sendfile.destroy();
                } catch (Exception e) {
                    // Ignore
                }
                sendfile = null;
            }
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

        // Destroy pool if it was initialised
        if (serverSockPool != 0) {
            Pool.destroy(serverSockPool);
            serverSockPool = 0;
        }

        // Close server socket if it was initialised
        if (serverSock != 0) {
            Socket.close(serverSock);
            serverSock = 0;
        }

        sslContext = 0;

        // Close all APR memory pools and resources if initialised
        if (rootPool != 0) {
            Pool.destroy(rootPool);
            rootPool = 0;
        }

        handler.recycle();
    }


    // ------------------------------------------------------ Protected Methods

    @Override
    protected AbstractEndpointAcceptor createAcceptor() {
        return new AprEndpointAcceptor(this);
    }


    /**
     * Process the specified connection.
     */
    protected boolean setSocketOptions(long socket) {
        // Process the connection
        int step = 1;
        try {

            // 1: Set socket options: timeout, linger, etc
            if (getSocketProperties().getSoLingerOn() && getSocketProperties().getSoLingerTime() >= 0)
                Socket.optSet(socket, Socket.getAprSoLinger(), getSocketProperties().getSoLingerTime());
            if (getSocketProperties().getTcpNoDelay())
                Socket.optSet(socket, Socket.getAprTcpNodelay(), (getSocketProperties().getTcpNoDelay() ? 1 : 0));
            Socket.timeoutSet(socket, getSocketProperties().getSoTimeout() * 1000);

            // 2: SSL handshake
            step = 2;
            if (sslContext != 0) {
                SSLSocket.attach(sslContext, socket);
                if (SSLSocket.handshake(socket) != 0) {
                    if (log.isDebugEnabled()) {
                        log.debug(getSm().getString("endpoint.err.handshake") + ": " + SSL.getLastError());
                    }
                    return false;
                }
            }

        } catch (Throwable t) {
            ExceptionUtils2.handleThrowable(t);
            if (log.isDebugEnabled()) {
                if (step == 2) {
                    log.debug(getSm().getString("endpoint.err.handshake"), t);
                } else {
                    log.debug(getSm().getString("endpoint.err.unexpected"), t);
                }
            }
            // Tell to close the socket
            return false;
        }
        return true;
    }


    /**
     * Allocate a new poller of the specified size.
     */
    protected long allocatePoller(int size, long pool, int timeout) {
        try {
            return Poll.create(size, pool, 0, timeout * 1000);
        } catch (Error e) {
            if (Status.APR_STATUS_IS_EINVAL(e.getError())) {
                log.info(getSm().getString("endpoint.poll.limitedpollsize", "" + size));
                return 0;
            } else {
                log.error(getSm().getString("endpoint.poll.initfail"), e);
                return -1;
            }
        }
    }

    /**
     * Process given socket. This is called when the socket has been
     * accepted.
     */
    protected boolean processSocketWithOptions(long socket) {
        try {
            // During shutdown, executor may be null - avoid NPE
            if (isRunning()) {
                if (log.isDebugEnabled()) {
                    log.debug(getSm().getString("endpoint.debug.socket",
                            Long.valueOf(socket)));
                }
                AprEndpointAprSocketWrapper wrapper =
                        new AprEndpointAprSocketWrapper(Long.valueOf(socket));
                wrapper.setKeepAliveLeft(getMaxKeepAliveRequests());
                wrapper.setSecure(isSSLEnabled());
                connections.put(Long.valueOf(socket), wrapper);
                getExecutor().execute(new AprEndpointSocketWithOptionsProcessor(this, wrapper));
            }
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
     * Process given socket. Called in non-comet mode, typically keep alive
     * or upgraded protocol.
     */
    public boolean processSocket(long socket, SocketStatus status) {
        try {
            Executor executor = getExecutor();
            if (executor == null) {
                log.warn(getSm().getString("endpoint.warn.noExector",
                        Long.valueOf(socket), null));
            } else {
                SocketWrapper<Long> wrapper =
                        connections.get(Long.valueOf(socket));
                // Make sure connection hasn't been closed
                if (wrapper != null) {
                    executor.execute(new AprEndpointSocketProcessor(this, wrapper, status));
                }
            }
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


    @Override
    public void processSocketAsync(SocketWrapper<Long> socket,
            SocketStatus status) {
        try {
            synchronized (socket) {
                if (waitingRequests.remove(socket)) {
                	AprEndpointSocketProcessor proc = new AprEndpointSocketProcessor(this, socket, status);
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
                        Executor executor = getExecutor();
                        if (executor == null) {
                            log.warn(getSm().getString("endpoint.warn.noExector",
                                    socket, status));
                            return;
                        } else {
                            executor.execute(proc);
                        }
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
        } catch (RejectedExecutionException x) {
            log.warn("Socket processing request was rejected for: "+socket, x);
        } catch (Throwable t) {
            ExceptionUtils2.handleThrowable(t);
            // This means we got an OOM or similar creating a thread, or that
            // the pool and its queue are full
            log.error(getSm().getString("endpoint.process.fail"), t);
        }
    }

    public void closeSocket(long socket) {
        // If not running the socket will be destroyed by
        // parent pool or acceptor socket.
        // In any case disable double free which would cause JVM core.

        connections.remove(Long.valueOf(socket));

        // While the connector is running, destroySocket() will call
        // countDownConnection(). Once the connector is stopped, the latch is
        // removed so it does not matter that destroySocket() does not call
        // countDownConnection() in that case
        AprEndpointPoller poller = this.poller;
        if (poller != null) {
            if (!poller.close(socket)) {
                destroySocket(socket);
            }
        }
    }

    /*
     * This method should only be called if there is no chance that the socket
     * is currently being used by the Poller. It is generally a bad idea to call
     * this directly from a known error condition.
     */
    public void destroySocket(long socket) {
        connections.remove(Long.valueOf(socket));
        if (log.isDebugEnabled()) {
            String msg = getSm().getString("endpoint.debug.destroySocket",
                    Long.valueOf(socket));
            if (log.isTraceEnabled()) {
                log.trace(msg, new Exception());
            } else {
                log.debug(msg);
            }
        }
        // Be VERY careful if you call this method directly. If it is called
        // twice for the same socket the JVM will core. Currently this is only
        // called from Poller.closePollset() to ensure kept alive connections
        // are closed when calling stop() followed by start().
        if (socket != 0) {
            Socket.destroy(socket);
            countDownConnection();
        }
    }

    @Override
    protected Log getLog() {
        return log;
    }
	public ConcurrentLinkedQueue<SocketWrapper<Long>> getWaitingRequests() {
		return waitingRequests;
	}
	public void setWaitingRequests(
			ConcurrentLinkedQueue<SocketWrapper<Long>> waitingRequests) {
		this.waitingRequests = waitingRequests;
	}
	public Map<Long, AprEndpointAprSocketWrapper> getConnections() {
		return connections;
	}
	public void setPoller(AprEndpointPoller poller) {
		this.poller = poller;
	}
	public void setAsyncTimeout(AprEndpointAsyncTimeout asyncTimeout) {
		this.asyncTimeout = asyncTimeout;
	}
	public void setSendfile(AprEndpointSendfile sendfile) {
		this.sendfile = sendfile;
	}
}
