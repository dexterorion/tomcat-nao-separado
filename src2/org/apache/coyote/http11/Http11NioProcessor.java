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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;

import javax.net.ssl.SSLEngine;

import org.apache.coyote.ActionCode;
import org.apache.coyote.ErrorState;
import org.apache.coyote.RequestInfo;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioEndpointKeyAttachment;
import org.apache.tomcat.util.net.NioEndpointSendfileData;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SecureNioChannel;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;

import org.apache.coyote.Constants24;

/**
 * Processes HTTP requests.
 *
 * @author Remy Maucherat
 * @author Filip Hanik
 */
public class Http11NioProcessor extends AbstractHttp11Processor<NioChannel> {

    private static final Log log = LogFactory.getLog(Http11NioProcessor.class);
    @Override
    protected Log getLog() {
        return log;
    }


    /**
     * SSL information.
     */
    private SSLSupport sslSupport;

    // ----------------------------------------------------------- Constructors


    public Http11NioProcessor(int maxHttpHeaderSize, NioEndpoint endpoint,
            int maxTrailerSize, int maxExtensionSize, int maxSwallowSize) {

        super(endpoint);

        inputBuffer = new InternalNioInputBuffer(getRequest(), maxHttpHeaderSize);
        getRequest().setInputBuffer(inputBuffer);

        outputBuffer = new InternalNioOutputBuffer(getResponse(), maxHttpHeaderSize);
        getResponse().setOutputBuffer(outputBuffer);

        initializeFilters(maxTrailerSize, maxExtensionSize, maxSwallowSize);
    }


    // ----------------------------------------------------- Instance Variables
    /**
     * Input.
     */
    private InternalNioInputBuffer inputBuffer = null;


    /**
     * Output.
     */
    private InternalNioOutputBuffer outputBuffer = null;


    /**
     * Sendfile data.
     */
    private NioEndpointSendfileData sendfileData = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Process pipelined HTTP requests using the specified input and output
     * streams.
     *
     * @throws IOException error during an I/O operation
     */
    @Override
    public SocketState event(SocketStatus status) throws IOException {

        long soTimeout = getEndpoint().getSoTimeout();

        RequestInfo rp = getRequest().getRequestProcessor();
        final NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
        try {
            rp.setStage(Constants24.getStageService());
            if (!getAdapter().event(getRequest(), getResponse(), status)) {
                setErrorState(ErrorState.CLOSE_NOW, null);
            }
            if (!getErrorState().isError()) {
                if (attach != null) {
                    attach.setComet(isComet());
                    if (isComet()) {
                        Integer comettimeout = (Integer) getRequest().getAttribute(
                                Constants24.getCometTimeoutAttr());
                        if (comettimeout != null) {
                            attach.setTimeout(comettimeout.longValue());
                        }
                    } else {
                        //reset the timeout
                        if (isKeepAlive()) {
                            attach.setTimeout(getKeepAliveTimeout());
                        } else {
                            attach.setTimeout(soTimeout);
                        }
                    }

                }
            }
        } catch (InterruptedIOException e) {
            setErrorState(ErrorState.CLOSE_NOW, e);
        } catch (Throwable t) {
            ExceptionUtils2.handleThrowable(t);
            // 500 - Internal Server Error
            getResponse().setStatus(500);
            setErrorState(ErrorState.CLOSE_NOW, t);
            log.error(getSm().getString("http11processor.request.process"), t);
            getAdapter().log(getRequest(), getResponse(), 0);
        }

        rp.setStage(Constants24.getStageEnded());

        if (getErrorState().isError() || status==SocketStatus.STOP) {
            return SocketState.CLOSED;
        } else if (!isComet()) {
            if (isKeepAlive()) {
                inputBuffer.nextRequest();
                outputBuffer.nextRequest();
                return SocketState.OPEN;
            } else {
                return SocketState.CLOSED;
            }
        } else {
            return SocketState.LONG;
        }
    }


    @Override
    protected void resetTimeouts() {
        final NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
        if (!getErrorState().isError() && attach != null &&
                getAsyncStateMachine().isAsyncDispatching()) {
            long soTimeout = getEndpoint().getSoTimeout();

            //reset the timeout
            if (isKeepAlive()) {
                attach.setTimeout(getKeepAliveTimeout());
            } else {
                attach.setTimeout(soTimeout);
            }
        }
    }


    @Override
    protected boolean disableKeepAlive() {
        return false;
    }


    @Override
    protected void setRequestLineReadTimeout() throws IOException {
        // socket.setTimeout()
        //     - timeout used by poller
        // socket.getSocket().getIOChannel().socket().setSoTimeout()
        //     - timeout used for blocking reads

        // When entering the processing loop there will always be data to read
        // so no point changing timeouts at this point

        // For the second and subsequent executions of the processing loop, a
        // non-blocking read is used so again no need to set the timeouts

        // Because NIO supports non-blocking reading of the request line and
        // headers the timeouts need to be set when returning the socket to
        // the poller rather than here.

        // NO-OP
    }


    @Override
    protected boolean handleIncompleteRequestLineRead() {
        // Haven't finished reading the request so keep the socket
        // open
        setOpenSocket(true);
        // Check to see if we have read any of the request line yet
        if (inputBuffer.getParsingRequestLinePhase() < 2) {
            if (getSocketWrapper().getLastAccess() > -1 || isKeptAlive()) {
                // Haven't read the request line and have previously processed a
                // request. Must be keep-alive. Make sure poller uses keepAlive.
                getSocketWrapper().setTimeout(getEndpoint().getKeepAliveTimeout());
            }
        } else {
            if (getEndpoint().isPaused()) {
                // Partially processed the request so need to respond
                getResponse().setStatus(503);
                setErrorState(ErrorState.CLOSE_CLEAN, null);
                getAdapter().log(getRequest(), getResponse(), 0);
                return false;
            } else {
                // Need to keep processor associated with socket
                setReadComplete(false);
                // Make sure poller uses soTimeout from here onwards
                getSocketWrapper().setTimeout(getEndpoint().getSoTimeout());
            }
        }
        return true;
    }


    @Override
    protected void setSocketTimeout(int timeout) throws IOException {
        getSocketWrapper().getSocket().getIOChannel().socket().setSoTimeout(timeout);
    }


    @Override
    protected void setCometTimeouts(SocketWrapper<NioChannel> socketWrapper) {
        // Comet support
        SelectionKey key = socketWrapper.getSocket().getIOChannel().keyFor(
                socketWrapper.getSocket().getPoller().getSelector());
        if (key != null) {
            NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment) key.attachment();
            if (attach != null)  {
                attach.setComet(isComet());
                if (isComet()) {
                    Integer comettimeout = (Integer) getRequest().getAttribute(
                            Constants24.getCometTimeoutAttr());
                    if (comettimeout != null) {
                        attach.setTimeout(comettimeout.longValue());
                    }
                }
            }
        }
    }


    @Override
    protected boolean breakKeepAliveLoop(SocketWrapper<NioChannel> socketWrapper) {
        setOpenSocket(isKeepAlive());
        // Do sendfile as needed: add socket to sendfile and end
        if (sendfileData != null && !getErrorState().isError()) {
            ((NioEndpointKeyAttachment) socketWrapper).setSendfileData(sendfileData);
            sendfileData.setKeepAlive(isKeepAlive());
            SelectionKey key = socketWrapper.getSocket().getIOChannel().keyFor(
                    socketWrapper.getSocket().getPoller().getSelector());
            //do the first write on this thread, might as well
            if (socketWrapper.getSocket().getPoller().processSendfile(key,
                    (NioEndpointKeyAttachment) socketWrapper, true)) {
                setSendfileInProgress(true);
            } else {
                // Write failed
                if (log.isDebugEnabled()) {
                    log.debug(getSm().getString("http11processor.sendfile.error"));
                }
                setErrorState(ErrorState.CLOSE_NOW, null);
            }
            return true;
        }
        return false;
    }


    @Override
    public void recycleInternal() {
        setSocketWrapper(null);
        sendfileData = null;
    }


    // ----------------------------------------------------- ActionHook Methods

    /**
     * Send an action to the connector.
     *
     * @param actionCode Type of the action
     * @param param Action parameter
     */
    @Override
    @SuppressWarnings("incomplete-switch") // Other cases are handled by action()
    public void actionInternal(ActionCode actionCode, Object param) {

        switch (actionCode) {
        case REQ_HOST_ADDR_ATTRIBUTE: {
            // Get remote host address
            if ((getRemoteAddr() == null) && (getSocketWrapper() != null)) {
                InetAddress inetAddr = getSocketWrapper().getSocket().getIOChannel().socket().getInetAddress();
                if (inetAddr != null) {
                    setRemoteAddr(inetAddr.getHostAddress());
                }
            }
            getRequest().remoteAddr().setString(getRemoteAddr());
            break;
        }
        case REQ_LOCAL_NAME_ATTRIBUTE: {
            // Get local host name
            if ((getLocalName() == null) && (getSocketWrapper() != null)) {
                InetAddress inetAddr = getSocketWrapper().getSocket().getIOChannel().socket().getLocalAddress();
                if (inetAddr != null) {
                    setLocalName(inetAddr.getHostName());
                }
            }
            getRequest().localName().setString(getLocalName());
            break;
        }
        case REQ_HOST_ATTRIBUTE: {
            // Get remote host name
            if ((getRemoteHost() == null) && (getSocketWrapper() != null)) {
                InetAddress inetAddr = getSocketWrapper().getSocket().getIOChannel().socket().getInetAddress();
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
            if (getLocalAddr() == null) {
                setLocalAddr(getSocketWrapper().getSocket().getIOChannel().socket().getLocalAddress().getHostAddress());
            }

            getRequest().localAddr().setString(getLocalAddr());
            break;
        }
        case REQ_REMOTEPORT_ATTRIBUTE: {
            if ((getRemotePort() == -1 ) && (getSocketWrapper() !=null)) {
                setRemotePort(getSocketWrapper().getSocket().getIOChannel().socket().getPort());
            }
            getRequest().setRemotePort(getRemotePort());
            break;
        }
        case REQ_LOCALPORT_ATTRIBUTE: {
            if ((getLocalPort() == -1 ) && (getSocketWrapper() !=null)) {
                setLocalPort(getSocketWrapper().getSocket().getIOChannel().socket().getLocalPort());
            }
            getRequest().setLocalPort(getLocalPort());
            break;
        }
        case REQ_SSL_ATTRIBUTE: {
            try {
                if (sslSupport != null) {
                    Object sslO = sslSupport.getCipherSuite();
                    if (sslO != null) {
                        getRequest().setAttribute
                            (SSLSupport.CIPHER_SUITE_KEY, sslO);
                    }
                    sslO = sslSupport.getPeerCertificateChain(false);
                    if (sslO != null) {
                        getRequest().setAttribute
                            (SSLSupport.CERTIFICATE_KEY, sslO);
                    }
                    sslO = sslSupport.getKeySize();
                    if (sslO != null) {
                        getRequest().setAttribute
                            (SSLSupport.KEY_SIZE_KEY, sslO);
                    }
                    sslO = sslSupport.getSessionId();
                    if (sslO != null) {
                        getRequest().setAttribute
                            (SSLSupport.SESSION_ID_KEY, sslO);
                    }
                    getRequest().setAttribute(SSLSupport.SESSION_MGR, sslSupport);
                }
            } catch (Exception e) {
                log.warn(getSm().getString("http11processor.socket.ssl"), e);
            }
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
                SecureNioChannel sslChannel = (SecureNioChannel) getSocketWrapper().getSocket();
                SSLEngine engine = sslChannel.getSslEngine();
                if (!engine.getNeedClientAuth()) {
                    // Need to re-negotiate SSL connection
                    engine.setNeedClientAuth(true);
                    try {
                        sslChannel.rehandshake(getEndpoint().getSoTimeout());
                        sslSupport = ((NioEndpoint)getEndpoint()).getHandler()
                                .getSslImplementation().getSSLSupport(
                                        engine.getSession());
                    } catch (IOException ioe) {
                        log.warn(getSm().getString("http11processor.socket.sslreneg",ioe));
                    }
                }

                try {
                    // use force=false since re-negotiation is handled above
                    // (and it is a NO-OP for NIO anyway)
                    Object sslO = sslSupport.getPeerCertificateChain(false);
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
        case AVAILABLE: {
            getRequest().setAvailable(inputBuffer.available());
            break;
        }
        case COMET_BEGIN: {
            setComet(true);
            break;
        }
        case COMET_END: {
            setComet(false);
            break;
        }
        case COMET_CLOSE: {
            if (getSocketWrapper()==null || getSocketWrapper().getSocket().getAttachment(false)==null) {
                return;
            }
            NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
            attach.setCometOps(NioEndpoint.getOpCallback());
            RequestInfo rp = getRequest().getRequestProcessor();
            if (rp.getStage() != Constants24.getStageService()) {
                // Close event for this processor triggered by request
                // processing in another processor, a non-Tomcat thread (i.e.
                // an application controlled thread) or similar.
                getSocketWrapper().getSocket().getPoller().add(getSocketWrapper().getSocket());
            }
            break;
        }
        case COMET_SETTIMEOUT: {
            if (param==null) {
                return;
            }
            if (getSocketWrapper()==null || getSocketWrapper().getSocket().getAttachment(false)==null) {
                return;
            }
            NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
            long timeout = ((Long)param).longValue();
            //if we are not piggy backing on a worker thread, set the timeout
            RequestInfo rp = getRequest().getRequestProcessor();
            if ( rp.getStage() != Constants24.getStageService() ) {
                attach.setTimeout(timeout);
            }
            break;
        }
        case ASYNC_COMPLETE: {
            if (getAsyncStateMachine().asyncComplete()) {
                ((NioEndpoint)getEndpoint()).processSocket(getSocketWrapper().getSocket(),
                        SocketStatus.OPEN_READ, true);
            }
            break;
        }
        case ASYNC_SETTIMEOUT: {
            if (param==null) {
                return;
            }
            if (getSocketWrapper()==null || getSocketWrapper().getSocket().getAttachment(false)==null) {
                return;
            }
            NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
            long timeout = ((Long)param).longValue();
            //if we are not piggy backing on a worker thread, set the timeout
            attach.setTimeout(timeout);
            break;
        }
        case ASYNC_DISPATCH: {
            if (getAsyncStateMachine().asyncDispatch()) {
                ((NioEndpoint)getEndpoint()).processSocket(getSocketWrapper().getSocket(),
                        SocketStatus.OPEN_READ, true);
            }
            break;
        }
        }
    }


    // ------------------------------------------------------ Protected Methods


    @Override
    protected void prepareRequestInternal() {
        sendfileData = null;
    }

    @Override
	public boolean prepareSendfile(OutputFilter[] outputFilters) {
        String fileName = (String) getRequest().getAttribute(
                Constants24.getSendfileFilenameAttr());
        if (fileName != null) {
            // No entity body sent here
            outputBuffer.addActiveFilter(outputFilters[Constants26.getVoidFilter()]);
            setContentDelimitation(true);
            sendfileData = new NioEndpointSendfileData();
            sendfileData.setFileName(fileName);
            sendfileData.setPos(((Long) getRequest().getAttribute(
                    Constants24.getSendfileFileStartAttr())).longValue());
            sendfileData.setLength(((Long) getRequest().getAttribute(
                    Constants24.getSendfileFileEndAttr())).longValue() - sendfileData.getPos());
            return true;
        }
        return false;
    }

    @Override
    protected AbstractInputBuffer<NioChannel> getInputBuffer() {
        return inputBuffer;
    }

    @Override
    protected AbstractOutputBuffer<NioChannel> getOutputBuffer() {
        return outputBuffer;
    }

    /**
     * Set the SSL information for this HTTP connection.
     */
    @Override
    public void setSslSupport(SSLSupport sslSupport) {
        this.sslSupport = sslSupport;
    }
}
