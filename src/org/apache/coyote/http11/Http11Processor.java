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
package org.apache.coyote.http11;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.coyote.ActionCode;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;


/**
 * Processes HTTP requests.
 *
 * @author Remy Maucherat
 * @author fhanik
 */
public class Http11Processor extends AbstractHttp11Processor<Socket> {

    private static final Log log = LogFactory.getLog(Http11Processor.class);
    @Override
    protected Log getLog() {
        return log;
    }

   // ------------------------------------------------------------ Constructor


    public Http11Processor(int headerBufferSize, JIoEndpoint endpoint,
            int maxTrailerSize, int maxExtensionSize, int maxSwallowSize) {

        super(endpoint);
        
        inputBuffer = new InternalInputBuffer(getRequest(), headerBufferSize);
        getRequest().setInputBuffer(inputBuffer);

        outputBuffer = new InternalOutputBuffer(getResponse(), headerBufferSize);
        getResponse().setOutputBuffer(outputBuffer);

        initializeFilters(maxTrailerSize, maxExtensionSize, maxSwallowSize);
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Input.
     */
    private InternalInputBuffer inputBuffer = null;


    /**
     * Output.
     */
    private InternalOutputBuffer outputBuffer = null;


    /**
     * SSL information.
     */
    private SSLSupport sslSupport;

    
    /**
     * The percentage of threads that have to be in use before keep-alive is
     * disabled to aid scalability.
     */
    private int disableKeepAlivePercentage = 75;

    // --------------------------------------------------------- Public Methods


    /**
     * Set the SSL information for this HTTP connection.
     */
    @Override
    public void setSslSupport(SSLSupport sslSupport) {
        this.sslSupport = sslSupport;
    }


    public int getDisableKeepAlivePercentage() {
        return disableKeepAlivePercentage;
    }


    public void setDisableKeepAlivePercentage(int disableKeepAlivePercentage) {
        this.disableKeepAlivePercentage = disableKeepAlivePercentage;
    }


    @Override
    protected boolean disableKeepAlive() {
        int threadRatio = -1;   
        // These may return zero or negative values
        // Only calculate a thread ratio when both are >0 to ensure we get a
        // sensible result
        int maxThreads, threadsBusy;
        if ((maxThreads = getEndpoint().getMaxThreads()) > 0
                && (threadsBusy = getEndpoint().getCurrentThreadsBusy()) > 0) {
            threadRatio = (threadsBusy * 100) / maxThreads;
        }
        // Disable keep-alive if we are running low on threads      
        if (threadRatio > getDisableKeepAlivePercentage()) {     
            return true;
        }
        
        return false;
    }


    @Override
    protected void setRequestLineReadTimeout() throws IOException {
        
        /*
         * When there is no data in the buffer and this is not the first
         * request on this connection and timeouts are being used the
         * first read for this request may need a different timeout to
         * take account of time spent waiting for a processing thread.
         * 
         * This is a little hacky but better than exposing the socket
         * and the timeout info to the InputBuffer
         */
        if (inputBuffer.getLastValid() == 0 && getSocketWrapper().getLastAccess() > -1) {
            int firstReadTimeout;
            if (getKeepAliveTimeout() == -1) {
                firstReadTimeout = 0;
            } else {
                long queueTime =
                    System.currentTimeMillis() - getSocketWrapper().getLastAccess();

                if (queueTime >= getKeepAliveTimeout()) {
                    // Queued for longer than timeout but there might be
                    // data so use shortest possible timeout
                    firstReadTimeout = 1;
                } else {
                    // Cast is safe since queueTime must be less than
                    // keepAliveTimeout which is an int
                    firstReadTimeout = getKeepAliveTimeout() - (int) queueTime;
                }
            }
            getSocketWrapper().getSocket().setSoTimeout(firstReadTimeout);
            if (!inputBuffer.fill()) {
                throw new EOFException(getSm().getString("iib.eof.error"));
            }
            // Once the first byte has been read, the standard timeout should be
            // used so restore it here.
            if (getEndpoint().getSoTimeout()> 0) {
                setSocketTimeout(getEndpoint().getSoTimeout());
            } else {
                setSocketTimeout(0);
            }
        }
    }


    @Override
    protected boolean handleIncompleteRequestLineRead() {
        // Not used with BIO since it uses blocking reads
        return false;
    }


    @Override
    protected void setSocketTimeout(int timeout) throws IOException {
        getSocketWrapper().getSocket().setSoTimeout(timeout);
    }
    
    
    @Override
    protected void setCometTimeouts(SocketWrapper<Socket> socketWrapper) {
        // NO-OP for BIO
    }


    @Override
    protected boolean breakKeepAliveLoop(SocketWrapper<Socket> socketWrapper) {
        setOpenSocket(isKeepAlive());
        // If we don't have a pipe-lined request allow this thread to be
        // used by another connection
        if (inputBuffer.getLastValid() == 0) {
            return true;
        }
        return false;
    }

    
    @Override
    protected void resetTimeouts() {
        // NOOP for BIO
    }


    @Override
    protected void recycleInternal() {
        // Recycle
        this.setSocketWrapper(null);
        // Recycle ssl info
        sslSupport = null;
    }


    @Override
    public SocketState event(SocketStatus status) throws IOException {
        // Should never reach this code but in case we do...
        throw new IOException(
                getSm().getString("http11processor.comet.notsupported"));
    }

    // ----------------------------------------------------- ActionHook Methods


    /**
     * Send an action to the connector.
     *
     * @param actionCode Type of the action
     * @param param Action parameter
     */
    @SuppressWarnings("incomplete-switch") // Other cases are handled by action()
    @Override
    public void actionInternal(ActionCode actionCode, Object param) {

        switch (actionCode) {
        case REQ_SSL_ATTRIBUTE: {
            try {
                if (sslSupport != null) {
                    Object sslO = sslSupport.getCipherSuite();
                    if (sslO != null)
                        getRequest().setAttribute
                            (SSLSupport.CIPHER_SUITE_KEY, sslO);
                    sslO = sslSupport.getPeerCertificateChain(false);
                    if (sslO != null)
                        getRequest().setAttribute
                            (SSLSupport.CERTIFICATE_KEY, sslO);
                    sslO = sslSupport.getKeySize();
                    if (sslO != null)
                        getRequest().setAttribute
                            (SSLSupport.KEY_SIZE_KEY, sslO);
                    sslO = sslSupport.getSessionId();
                    if (sslO != null)
                        getRequest().setAttribute
                            (SSLSupport.SESSION_ID_KEY, sslO);
                    getRequest().setAttribute(SSLSupport.SESSION_MGR, sslSupport);
                }
            } catch (Exception e) {
                log.warn(getSm().getString("http11processor.socket.ssl"), e);
            }
            break;
        }
        case REQ_HOST_ADDR_ATTRIBUTE: {
            if ((getRemoteAddr() == null) && (getSocketWrapper() != null)) {
                InetAddress inetAddr = getSocketWrapper().getSocket().getInetAddress();
                if (inetAddr != null) {
                    setRemoteAddr(inetAddr.getHostAddress());
                }
            }
            getRequest().remoteAddr().setString(getRemoteAddr());
            break;
        }
        case REQ_LOCAL_NAME_ATTRIBUTE: {
            if ((getLocalName() == null) && (getSocketWrapper() != null)) {
                InetAddress inetAddr = getSocketWrapper().getSocket().getLocalAddress();
                if (inetAddr != null) {
                    setLocalName(inetAddr.getHostName());
                }
            }
            getRequest().localName().setString(getLocalName());
            break;
        }
        case REQ_HOST_ATTRIBUTE: {
            if ((getRemoteHost() == null) && (getSocketWrapper() != null)) {
                InetAddress inetAddr = getSocketWrapper().getSocket().getInetAddress();
                if (inetAddr != null) {
                    setRemoteHost(inetAddr.getHostName());
                }
                if(getRemoteHost() == null) {
                    if(getRemoteAddr() != null) {
                        setRemoteHost(getRemoteAddr());
                    } else { // all we can do is punt
                        getRequest().remoteHost().recycle();
                    }
                }
            }
            getRequest().remoteHost().setString(getRemoteHost());
            break;
        }
        case REQ_LOCAL_ADDR_ATTRIBUTE: {
            if (getLocalAddr() == null)
               setLocalAddr(getSocketWrapper().getSocket().getLocalAddress().getHostAddress());

            getRequest().localAddr().setString(getLocalAddr());
            break;
        }
        case REQ_REMOTEPORT_ATTRIBUTE: {
            if ((getRemotePort() == -1 ) && (getSocketWrapper() !=null)) {
                setRemotePort(getSocketWrapper().getSocket().getPort());
            }
            getRequest().setRemotePort(getRemotePort());
            break;
        }
        case REQ_LOCALPORT_ATTRIBUTE: {
            if ((getLocalPort() == -1 ) && (getSocketWrapper() !=null)) {
                setLocalPort(getSocketWrapper().getSocket().getLocalPort());
            }
            getRequest().setLocalPort(getLocalPort());
            break;
        }
        case REQ_SSL_CERTIFICATE: {
            if (sslSupport != null) {
                /*
                 * Consume and buffer the request body, so that it does not
                 * interfere with the client's handshake messages
                 */
                InputFilter[] inputFilters = inputBuffer.getFilters();
                ((BufferedInputFilter) inputFilters[Constants26.getBufferedFilter()])
                    .setLimit(getMaxSavePostSize());
                inputBuffer.addActiveFilter
                    (inputFilters[Constants26.getBufferedFilter()]);
                try {
                    Object sslO = sslSupport.getPeerCertificateChain(true);
                    if( sslO != null) {
                        getRequest().setAttribute
                            (SSLSupport.CERTIFICATE_KEY, sslO);
                    }
                } catch (Exception e) {
                    log.warn(getSm().getString("http11processor.socket.ssl"), e);
                }
            }
            break;
        }
        case ASYNC_COMPLETE: {
            if (getAsyncStateMachine().asyncComplete()) {
                ((JIoEndpoint) getEndpoint()).processSocketAsync(this.getSocketWrapper(),
                        SocketStatus.OPEN_READ);
            }
            break;
        }
        case ASYNC_SETTIMEOUT: {
            if (param == null) return;
            long timeout = ((Long)param).longValue();
            // if we are not piggy backing on a worker thread, set the timeout
            getSocketWrapper().setTimeout(timeout);
            break;
        }
        case ASYNC_DISPATCH: {
            if (getAsyncStateMachine().asyncDispatch()) {
                ((JIoEndpoint) getEndpoint()).processSocketAsync(this.getSocketWrapper(),
                        SocketStatus.OPEN_READ);
            }
            break;
        }
        }
    }


    // ------------------------------------------------------ Protected Methods


    @Override
    protected void prepareRequestInternal() {
        // NOOP for BIO
    }

    @Override
	public boolean prepareSendfile(OutputFilter[] outputFilters) {
        // Should never, ever call this code
        Exception e = new Exception();
        log.error(getSm().getString("http11processor.neverused"), e);
        return false;
    }

    @Override
    protected AbstractInputBuffer<Socket> getInputBuffer() {
        return inputBuffer;
    }

    @Override
    protected AbstractOutputBuffer<Socket> getOutputBuffer() {
        return outputBuffer;
    }

    /**
     * Set the socket buffer flag.
     */
    @Override
    public void setSocketBuffer(int socketBuffer) {
        super.setSocketBuffer(socketBuffer);
        outputBuffer.setSocketBuffer(socketBuffer);
    }
}
