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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.apache.coyote.Response3;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioEndpointKeyAttachment;
import org.apache.tomcat.util.net.NioSelectorPool;
import org.apache.tomcat.util.net.SocketWrapper;

/**
 * Output buffer.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Filip Hanik
 */
public class InternalNioOutputBuffer extends AbstractOutputBuffer<NioChannel> {

    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     */
    public InternalNioOutputBuffer(Response3 response, int headerBufferSize) {

        this.setResponse(response);

        setBuf(new byte[headerBufferSize]);
        
        setOutputStreamOutputBuffer(new InternalNioOutputBufferSocketOutputBuffer(this));

        setFilterLibrary(new OutputFilter[0]);
        setActiveFilters(new OutputFilter[0]);
        setLastActiveFilter(-1);

        setCommitted(false);
        setFinished(false);
        
        // Cause loading of HttpMessages
        HttpMessages.getInstance(response.getLocale()).getMessage(200);

    }


    /**
     * Underlying socket.
     */
    private NioChannel socket;
    
    /**
     * Selector pool, for blocking reads and blocking writes
     */
    private NioSelectorPool pool;


    // --------------------------------------------------------- Public Methods


    /**
     * Flush the response.
     * 
     * @throws IOException an underlying I/O error occurred
     * 
     */
    @Override
    public void flush() throws IOException {

        super.flush();
        // Flush the current buffer
        flushBuffer();

    }


    /**
     * Recycle the output buffer. This should be called when closing the 
     * connection.
     */
    @Override
    public void recycle() {
        super.recycle();
        if (socket != null) {
            socket.getBufHandler().getWriteBuffer().clear();
            socket = null;
        }
    }


    /**
     * End request.
     * 
     * @throws IOException an underlying I/O error occurred
     */
    @Override
    public void endRequest() throws IOException {
        super.endRequest();
        flushBuffer();
    }

    // ------------------------------------------------ HTTP/1.1 Output Methods


    /** 
     * Send an acknowledgment.
     */
    @Override
    public void sendAck() throws IOException {

        if (!isCommitted()) {
            //Socket.send(socket, Constants.ACK_BYTES, 0, Constants.ACK_BYTES.length) < 0
            socket.getBufHandler() .getWriteBuffer().put(Constants26.getAckBytes(),0,Constants26.getAckBytes().length);    
            writeToSocket(socket.getBufHandler() .getWriteBuffer(),true,true);
        }

    }

    /**
     * 
     * @param bytebuffer ByteBuffer
     * @param flip boolean
     * @return int
     * @throws IOException
     * TODO Fix non blocking write properly
     */
    private synchronized int writeToSocket(ByteBuffer bytebuffer, boolean block, boolean flip) throws IOException {
        if ( flip ) bytebuffer.flip();

        int written = 0;
        NioEndpointKeyAttachment att = (NioEndpointKeyAttachment)socket.getAttachment(false);
        if ( att == null ) throw new IOException("Key must be cancelled");
        long writeTimeout = att.getWriteTimeout();
        Selector selector = null;
        try {
            selector = pool.get();
        } catch ( IOException x ) {
            //ignore
        }
        try {
            written = pool.write(bytebuffer, socket, selector, writeTimeout, block);
            //make sure we are flushed 
            do {
                if (socket.flush(true,selector,writeTimeout)) break;
            }while ( true );
        }finally { 
            if ( selector != null ) pool.put(selector);
        }
        if ( block ) bytebuffer.clear(); //only clear
        return written;
    } 


    // ------------------------------------------------------ Protected Methods

    @Override
    public void init(SocketWrapper<NioChannel> socketWrapper,
            AbstractEndpoint<NioChannel> endpoint) throws IOException {

        socket = socketWrapper.getSocket();
        pool = ((NioEndpoint)endpoint).getSelectorPool();
    }


    /**
     * Commit the response.
     * 
     * @throws IOException an underlying I/O error occurred
     */
    @Override
    protected void commit()
        throws IOException {

        // The response is now committed
        setCommitted(true);
        getResponse().setCommitted(true);

        if (getPos() > 0) {
            // Sending the response header buffer
            addToBB(getBuf(), 0, getPos());
        }

    }

    public synchronized void addToBB(byte[] buf, int offset, int length) throws IOException {
        while (length > 0) {
            int thisTime = length;
            if (socket.getBufHandler().getWriteBuffer().position() ==
                    socket.getBufHandler().getWriteBuffer().capacity()
                    || socket.getBufHandler().getWriteBuffer().remaining()==0) {
                flushBuffer();
            }
            if (thisTime > socket.getBufHandler().getWriteBuffer().remaining()) {
                thisTime = socket.getBufHandler().getWriteBuffer().remaining();
            }
            socket.getBufHandler().getWriteBuffer().put(buf, offset, thisTime);
            length = length - thisTime;
            offset = offset + thisTime;
        }
        NioEndpointKeyAttachment ka = (NioEndpointKeyAttachment)socket.getAttachment(false);
        if ( ka!= null ) ka.access();//prevent timeouts for just doing client writes
    }


    /**
     * Callback to write data from the buffer.
     */
    private void flushBuffer() throws IOException {

        //prevent timeout for async,
        SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
        if (key != null) {
            NioEndpointKeyAttachment attach = (NioEndpointKeyAttachment) key.attachment();
            attach.access();
        }

        //write to the socket, if there is anything to write
        if (socket.getBufHandler().getWriteBuffer().position() > 0) {
            socket.getBufHandler().getWriteBuffer().flip();
            writeToSocket(socket.getBufHandler().getWriteBuffer(),true, false);
        }
    }
}
