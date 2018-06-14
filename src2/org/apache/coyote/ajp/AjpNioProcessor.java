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

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

import org.apache.coyote.ActionCode;
import org.apache.coyote.ErrorState;
import org.apache.coyote.RequestInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioEndpointKeyAttachment;
import org.apache.tomcat.util.net.NioSelectorPool;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;

import org.apache.coyote.Constants24;


/**
 * Processes AJP requests using NIO.
 */
public class AjpNioProcessor extends AbstractAjpProcessor<NioChannel> {


    /**
     * Logger.
     */
    private static final Log log = LogFactory.getLog(AjpNioProcessor.class);
    @Override
    protected Log getLog() {
        return log;
    }

    // ----------------------------------------------------------- Constructors


    public AjpNioProcessor(int packetSize, NioEndpoint endpoint) {

        super(packetSize, endpoint);

        getResponse().setOutputBuffer(new AbstractAjpProcessorSocketOutputBuffer<NioChannel>(this));

        pool = endpoint.getSelectorPool();
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * Selector pool for the associated endpoint.
     */
    private NioSelectorPool pool;


    // --------------------------------------------------------- Public Methods


    /**
     * Process pipelined HTTP requests using the specified input and output
     * streams.
     *
     * @throws IOException error during an I/O operation
     */
    @Override
    public SocketState process(SocketWrapper<NioChannel> socket)
        throws IOException {
        RequestInfo rp = getRequest().getRequestProcessor();
        rp.setStage(Constants24.getStageParse());

        // Setting up the socket
        this.setSocketWrapper(socket);
        
        long soTimeout = getEndpoint().getSoTimeout();
        boolean cping = false;

        while (!getErrorState().isError() && !getEndpoint().isPaused()) {
            // Parsing the request header
            try {
                // Get first message of the request
                int bytesRead = readMessage(getRequestHeaderMessage(), false);
                if (bytesRead == 0) {
                    break;
                }
                // Set back timeout if keep alive timeout is enabled
                if (getKeepAliveTimeout() > 0) {
                    socket.setTimeout(soTimeout);
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
                        output(getPongmessagearray(), 0, getPongmessagearray().length);
                    } catch (IOException e) {
                        setErrorState(ErrorState.CLOSE_NOW, null);
                    }
                    recycle(false);
                    continue;
                } else if(type != Constants25.getJkAjp13ForwardRequest()) {
                    // Unexpected packet type. Unread body packets should have
                    // been swallowed in finish().
                    if (log.isDebugEnabled()) {
                        log.debug("Unexpected message: " + type);
                    }
                    setErrorState(ErrorState.CLOSE_NOW, null);
                    recycle(true);
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
            // Set keep alive timeout if enabled
            if (getKeepAliveTimeout() > 0) {
                socket.setTimeout(getKeepAliveTimeout());
            }

            recycle(false);
        }

        rp.setStage(Constants24.getStageEnded());

        if (!getErrorState().isError() && !getEndpoint().isPaused()) {
            if (isAsync()) {
                return SocketState.LONG;
            } else {
                return SocketState.OPEN;
            }
        } else {
            return SocketState.CLOSED;
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
                ((NioEndpoint)getEndpoint()).processSocket(this.getSocketWrapper().getSocket(),
                        SocketStatus.OPEN_READ, false);
            }
            break;
        }
        case ASYNC_SETTIMEOUT: {
            if (param == null) return;
            long timeout = ((Long)param).longValue();
            final NioEndpointKeyAttachment ka =
                    (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
            ka.setTimeout(timeout);
            break;
        }
        case ASYNC_DISPATCH: {
            if (getAsyncStateMachine().asyncDispatch()) {
                ((NioEndpoint)getEndpoint()).processSocket(this.getSocketWrapper().getSocket(),
                        SocketStatus.OPEN_READ, true);
            }
            break;
        }
        }
    }


    @Override
    protected void resetTimeouts() {
        // The NIO connector uses the timeout configured on the wrapper in the
        // poller. Therefore, it needs to be reset once asycn processing has
        // finished.
        final NioEndpointKeyAttachment attach =
                (NioEndpointKeyAttachment)getSocketWrapper().getSocket().getAttachment(false);
        if (!getErrorState().isError() && attach != null &&
                getAsyncStateMachine().isAsyncDispatching()) {
            long soTimeout = getEndpoint().getSoTimeout();

            //reset the timeout
            if (getKeepAliveTimeout() > 0) {
                attach.setTimeout(getKeepAliveTimeout());
            } else {
                attach.setTimeout(soTimeout);
            }
        }

    }


    @Override
    protected void output(byte[] src, int offset, int length)
            throws IOException {
        
    	NioEndpointKeyAttachment att =
                (NioEndpointKeyAttachment) getSocketWrapper().getSocket().getAttachment(false);
        if ( att == null ) throw new IOException("Key must be cancelled");

        ByteBuffer writeBuffer =
                getSocketWrapper().getSocket().getBufHandler().getWriteBuffer();

        writeBuffer.put(src, offset, length);
        
        writeBuffer.flip();

        long writeTimeout = att.getWriteTimeout();
        Selector selector = null;
        try {
            selector = pool.get();
        } catch ( IOException x ) {
            //ignore
        }
        try {
            pool.write(writeBuffer, getSocketWrapper().getSocket(), selector,
                    writeTimeout, true);
        }finally { 
            writeBuffer.clear();
            if ( selector != null ) pool.put(selector);
        }
    }


    /**
     * Read the specified amount of bytes, and place them in the input buffer.
     */
    protected int read(byte[] buf, int pos, int n, boolean blockFirstRead)
        throws IOException {

        int read = 0;
        int res = 0;
        boolean block = blockFirstRead;
        
        while (read < n) {
            res = readSocket(buf, read + pos, n - read, block);
            if (res > 0) {
                read += res;
            } else if (res == 0 && !block) {
                break;
            } else {
                throw new IOException(getSm().getString("ajpprocessor.failedread"));
            }
            block = true;
        }
        return read;
    }

    private int readSocket(byte[] buf, int pos, int n, boolean block)
            throws IOException {
        int nRead = 0;
        ByteBuffer readBuffer =
                getSocketWrapper().getSocket().getBufHandler().getReadBuffer();
        readBuffer.clear();
        readBuffer.limit(n);
        if ( block ) {
            Selector selector = null;
            try {
                selector = pool.get();
            } catch ( IOException x ) {
                // Ignore
            }
            try {
                NioEndpointKeyAttachment att =
                        (NioEndpointKeyAttachment) getSocketWrapper().getSocket().getAttachment(false);
                if ( att == null ) throw new IOException("Key must be cancelled.");
                nRead = pool.read(readBuffer, getSocketWrapper().getSocket(),
                        selector, att.getTimeout());
            } catch ( EOFException eof ) {
                nRead = -1;
            } finally { 
                if ( selector != null ) pool.put(selector);
            }
        } else {
            nRead = getSocketWrapper().getSocket().read(readBuffer);
        }
        if (nRead > 0) {
            readBuffer.flip();
            readBuffer.limit(nRead);
            readBuffer.get(buf, pos, nRead);
            return nRead;
        } else if (nRead == -1) {
            //return false;
            throw new EOFException(getSm().getString("iib.eof.error"));
        } else {
            return 0;
        }
    }


    /** Receive a chunk of data. Called to implement the
     *  'special' packet in ajp13 and to receive the data
     *  after we send a GET_BODY packet
     */
    @Override
    public boolean receive() throws IOException {

        setFirst(false);
        getBodyMessage().reset();
        
        readMessage(getBodyMessage(), true);

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
     * @return The number of bytes read
     * @throws IOException any other failure, including incomplete reads
     */
    protected int readMessage(AjpMessage message, boolean blockFirstRead)
        throws IOException {

        byte[] buf = message.getBuffer();
        int headerLength = message.getHeaderLength();

        int bytesRead = read(buf, 0, headerLength, blockFirstRead);

        if (bytesRead == 0) {
            return 0;
        }
        
        int messageLength = message.processHeader(true);
        if (messageLength < 0) {
            // Invalid AJP header signature
            throw new IOException(getSm().getString("ajpmessage.invalidLength",
                    Integer.valueOf(messageLength)));
        }
        else if (messageLength == 0) {
            // Zero length message.
            return bytesRead;
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
            bytesRead += read(buf, headerLength, messageLength, true);
            return bytesRead;
        }
    }


}
