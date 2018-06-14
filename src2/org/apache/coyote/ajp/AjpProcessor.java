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
package org.apache.coyote.ajp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.coyote.ActionCode;
import org.apache.coyote.ErrorState;
import org.apache.coyote.RequestInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;

import org.apache.coyote.Constants24;


/**
 * Processes AJP requests.
 *
 * @author Remy Maucherat
 * @author Henri Gomez
 * @author Dan Milstein
 * @author Keith Wannamaker
 * @author Kevin Seguin
 * @author Costin Manolache
 * @author Bill Barker
 */
public class AjpProcessor extends AbstractAjpProcessor<Socket> {


    /**
     * Logger.
     */
    private static final Log log = LogFactory.getLog(AjpProcessor.class);
    @Override
    protected Log getLog() {
        return log;
    }

    // ----------------------------------------------------------- Constructors


    public AjpProcessor(int packetSize, JIoEndpoint endpoint) {

        super(packetSize, endpoint);

        getResponse().setOutputBuffer(new AbstractAjpProcessorSocketOutputBuffer<Socket>(this));
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * Input stream.
     */
    private InputStream input;
    
    
    /**
     * Output stream.
     */
    private OutputStream output;
    

    // --------------------------------------------------------- Public Methods


    /**
     * Process pipelined HTTP requests using the specified input and output
     * streams.
     *
     * @throws IOException error during an I/O operation
     */
    @Override
    public SocketState process(SocketWrapper<Socket> socket)
        throws IOException {
        RequestInfo rp = getRequest().getRequestProcessor();
        rp.setStage(Constants24.getStageParse());

        // Setting up the socket
        this.setSocketWrapper(socket);
        input = socket.getSocket().getInputStream();
        output = socket.getSocket().getOutputStream();
        int soTimeout = -1;
        if (getKeepAliveTimeout() > 0) {
            soTimeout = socket.getSocket().getSoTimeout();
        }
        boolean cping = false;

        while (!getErrorState().isError() && !getEndpoint().isPaused()) {
            // Parsing the request header
            try {
                // Set keep alive timeout if enabled
                if (getKeepAliveTimeout() > 0) {
                    socket.getSocket().setSoTimeout(getKeepAliveTimeout());
                }
                // Get first message of the request
                if (!readMessage(getRequestHeaderMessage())) {
                    // This means a connection timeout
                    break;
                }
                // Set back timeout if keep alive timeout is enabled
                if (getKeepAliveTimeout() > 0) {
                    socket.getSocket().setSoTimeout(soTimeout);
                }
                // Check message type, process right away and break if
                // not regular request processing
                int type = getRequestHeaderMessage().getByte();
                if (type == Constants25.getJkAjp13CpingRequest()) {
                    if (getEndpoint().isPaused()) {
                        recycle(true);
                        break;
                    }
                    cping = true;
                    try {
                        output.write(getPongmessagearray());
                    } catch (IOException e) {
                        setErrorState(ErrorState.CLOSE_NOW, e);
                    }
                    continue;
                } else if(type != Constants25.getJkAjp13ForwardRequest()) {
                    // Unexpected packet type. Unread body packets should have
                    // been swallowed in finish().
                    if (log.isDebugEnabled()) {
                        log.debug("Unexpected message: " + type);
                    }
                    setErrorState(ErrorState.CLOSE_NOW, null);
                    break;
                }
                getRequest().setStartTime(System.currentTimeMillis());
            } catch (IOException e) {
                setErrorState(ErrorState.CLOSE_NOW, e);
                break;
            } catch (Throwable t) {
                ExceptionUtils2.handleThrowable(t);
                log.debug(getSm().getString("ajpprocessor.header.error"), t);
                // 400 - Bad Request
                getResponse().setStatus(400);
                setErrorState(ErrorState.CLOSE_CLEAN, t);
                getAdapter().log(getRequest(), getResponse(), 0);
            }

            if (!getErrorState().isError()) {
                // Setting up filters, and parse some request headers
                rp.setStage(Constants24.getStagePrepare());
                try {
                    prepareRequest();
                } catch (Throwable t) {
                    ExceptionUtils2.handleThrowable(t);
                    log.debug(getSm().getString("ajpprocessor.request.prepare"), t);
                    // 500 - Internal Server Error
                    getResponse().setStatus(500);
                    setErrorState(ErrorState.CLOSE_CLEAN, t);
                    getAdapter().log(getRequest(), getResponse(), 0);
                }
            }

            if (!getErrorState().isError() && !cping && getEndpoint().isPaused()) {
                // 503 - Service unavailable
                getResponse().setStatus(503);
                setErrorState(ErrorState.CLOSE_CLEAN, null);
                getAdapter().log(getRequest(), getResponse(), 0);
            }
            cping = false;

            // Process the request in the adapter
            if (!getErrorState().isError()) {
                try {
                    rp.setStage(Constants24.getStageService());
                    getAdapter().service(getRequest(), getResponse());
                } catch (InterruptedIOException e) {
                    setErrorState(ErrorState.CLOSE_NOW, e);
                } catch (Throwable t) {
                    ExceptionUtils2.handleThrowable(t);
                    log.error(getSm().getString("ajpprocessor.request.process"), t);
                    // 500 - Internal Server Error
                    getResponse().setStatus(500);
                    setErrorState(ErrorState.CLOSE_CLEAN, t);
                    getAdapter().log(getRequest(), getResponse(), 0);
                }
            }
            
            if (isAsync() && !getErrorState().isError()) {
                break;
            }

            // Finish the response if not done yet
            if (!isFinished() && getErrorState().isIoAllowed()) {
                try {
                    finish();
                } catch (Throwable t) {
                    ExceptionUtils2.handleThrowable(t);
                    setErrorState(ErrorState.CLOSE_NOW, t);
                }
            }

            // If there was an error, make sure the request is counted as
            // and error, and update the statistics counter
            if (getErrorState().isError()) {
                getResponse().setStatus(500);
            }
            getRequest().updateCounters();

            rp.setStage(Constants24.getStageKeepalive());
            recycle(false);
        }
        
        rp.setStage(Constants24.getStageEnded());

        if (isAsync() && !getErrorState().isError() && !getEndpoint().isPaused()) {
            return SocketState.LONG;
        } else {
            input = null;
            output = null;
            return SocketState.CLOSED;
        }
        
    }

    @Override
    public void recycle(boolean socketClosing) {
        super.recycle(socketClosing);
        if (socketClosing) {
            input = null;
            output = null;
        }
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
    protected void actionInternal(ActionCode actionCode, Object param) {

        switch (actionCode) {
        case ASYNC_COMPLETE: {
            if (getAsyncStateMachine().asyncComplete()) {
                ((JIoEndpoint)getEndpoint()).processSocketAsync(this.getSocketWrapper(),
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
                ((JIoEndpoint)getEndpoint()).processSocketAsync(this.getSocketWrapper(),
                        SocketStatus.OPEN_READ);
            }
            break;
        }
        }
    }


    @Override
    protected void resetTimeouts() {
        // NO-OP. The AJP BIO connector only uses the timeout value on the
        //        SocketWrapper for async timeouts.
    }


    @Override
    protected void output(byte[] src, int offset, int length)
            throws IOException {
        output.write(src, offset, length);
    }


    /**
     * Read at least the specified amount of bytes, and place them
     * in the input buffer.
     */
    protected boolean read(byte[] buf, int pos, int n)
        throws IOException {

        int read = 0;
        int res = 0;
        while (read < n) {
            res = input.read(buf, read + pos, n - read);
            if (res > 0) {
                read += res;
            } else {
                throw new IOException(getSm().getString("ajpprocessor.failedread"));
            }
        }
        
        return true;

    }


    /** Receive a chunk of data. Called to implement the
     *  'special' packet in ajp13 and to receive the data
     *  after we send a GET_BODY packet
     */
    @Override
    public boolean receive() throws IOException {

        setFirst(false);
        getBodyMessage().reset();
        if (!readMessage(getBodyMessage())) {
            // Invalid message
            return false;
        }
        // No data received.
        if (getBodyMessage().getLen() == 0) {
            // just the header
            // Don't mark 'end of stream' for the first chunk.
            return false;
        }
        int blen = getBodyMessage().peekInt();
        if (blen == 0) {
            return false;
        }

        getBodyMessage().getBodyBytes(getBodyBytes());
        setEmpty(false);
        return true;
    }

    /**
     * Read an AJP message.
     *
     * @return true if the message has been read, false if the short read
     *         didn't return anything
     * @throws IOException any other failure, including incomplete reads
     */
    protected boolean readMessage(AjpMessage message)
        throws IOException {

        byte[] buf = message.getBuffer();
        int headerLength = message.getHeaderLength();

        read(buf, 0, headerLength);

        int messageLength = message.processHeader(true);
        if (messageLength < 0) {
            // Invalid AJP header signature
            // TODO: Throw some exception and close the connection to frontend.
            return false;
        }
        else if (messageLength == 0) {
            // Zero length message.
            return true;
        }
        else {
            if (messageLength > buf.length) {
                // Message too long for the buffer
                // Need to trigger a 400 response
                throw new IllegalArgumentException(getSm().getString(
                        "ajpprocessor.header.tooLong",
                        Integer.valueOf(messageLength),
                        Integer.valueOf(buf.length)));
            }
            read(buf, headerLength, messageLength);
            return true;
        }
    }
}
