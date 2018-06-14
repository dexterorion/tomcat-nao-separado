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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.coyote.Request2;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketWrapper;

/**
 * Implementation of InputBuffer which provides HTTP request header parsing as
 * well as transfer decoding.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
public class InternalAprInputBuffer extends AbstractInputBuffer<Long> {

    private static final Log log =
        LogFactory.getLog(InternalAprInputBuffer.class);


	private static final boolean[] HTTP_TOKEN_CHAR = null;

    // ----------------------------------------------------------- Constructors


    /**
     * Alternate constructor.
     */
    public InternalAprInputBuffer(Request2 request, int headerBufferSize) {

        this.setRequest(request);
        setHeaders(request.getMimeHeaders());

        setBuf(new byte[headerBufferSize]);
        if (headerBufferSize < (8 * 1024)) {
            bbuf = ByteBuffer.allocateDirect(6 * 1500);
        } else {
            bbuf = ByteBuffer.allocateDirect((headerBufferSize / 1500 + 1) * 1500);
        }

        setInputStreamInputBuffer(new InternalAprInputBufferSocketInputBuffer(this));

        setFilterLibrary(new InputFilter[0]);
        setActiveFilters(new InputFilter[0]);
        setLastActiveFilter(-1);

        setParsingHeader(true);
        setSwallowInput(true);
        
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Direct byte buffer used to perform actual reading.
     */
    private ByteBuffer bbuf;


    /**
     * Underlying socket.
     */
    private long socket;


    // --------------------------------------------------------- Public Methods

    /**
     * Recycle the input buffer. This should be called when closing the 
     * connection.
     */
    @Override
    public void recycle() {
        socket = 0;
        super.recycle();
    }


    /**
     * Read the request line. This function is meant to be used during the 
     * HTTP request header parsing. Do NOT attempt to read the request body 
     * using it.
     *
     * @throws IOException If an exception occurs during the underlying socket
     * read operations, or if the given buffer is not big enough to accommodate
     * the whole line.
     * @return true if data is properly fed; false if no data is available 
     * immediately and thread should be freed
     */
    @Override
    public boolean parseRequestLine(boolean useAvailableData)
        throws IOException {

        int start = 0;

        //
        // Skipping blank lines
        //

        byte chr = 0;
        do {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (useAvailableData) {
                    return false;
                }
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }
            // Set the start time once we start reading data (even if it is
            // just skipping blank lines)
            if (getRequest().getStartTime() < 0) {
                getRequest().setStartTime(System.currentTimeMillis());
            }
            chr = getBuf()[getPos()];
            setPos(getPos() + 1);
        } while ((chr == Constants26.getCr()) || (chr == Constants26.getLf()));

        setPos(getPos() - 1);

        // Mark the current buffer position
        start = getPos();

        if (getPos() >= getLastValid()) {
            if (useAvailableData) {
                return false;
            }
            if (!fill())
                throw new EOFException(getSm().getString("iib.eof.error"));
        }

        //
        // Reading the method name
        // Method name is always US-ASCII
        //

        boolean space = false;

        while (!space) {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            // Spec says no CR or LF in method name
            if (getBuf()[getPos()] == Constants26.getCr() || getBuf()[getPos()] == Constants26.getLf()) {
                throw new IllegalArgumentException(
                        getSm().getString("iib.invalidmethod"));
            }
            // Spec says single SP but it also says be tolerant of HT
            if (getBuf()[getPos()] == Constants26.getSp() || getBuf()[getPos()] == Constants26.getHt()) {
                space = true;
                getRequest().method().setBytes(getBuf(), start, getPos() - start);
            }

            setPos(getPos() + 1);

        }

        // Spec says single SP but also says be tolerant of multiple and/or HT
        while (space) {
            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }
            if (getBuf()[getPos()] == Constants26.getSp() || getBuf()[getPos()] == Constants26.getHt()) {
                setPos(getPos() + 1);
            } else {
                space = false;
            }
        }

        // Mark the current buffer position
        start = getPos();
        int end = 0;
        int questionPos = -1;

        //
        // Reading the URI
        //

        boolean eol = false;

        while (!space) {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            // Spec says single SP but it also says be tolerant of HT
            if (getBuf()[getPos()] == Constants26.getSp() || getBuf()[getPos()] == Constants26.getHt()) {
                space = true;
                end = getPos();
            } else if ((getBuf()[getPos()] == Constants26.getCr()) 
                       || (getBuf()[getPos()] == Constants26.getLf())) {
                // HTTP/0.9 style request
                eol = true;
                space = true;
                end = getPos();
            } else if ((getBuf()[getPos()] == Constants26.getQuestion()) 
                       && (questionPos == -1)) {
                questionPos = getPos();
            }

            setPos(getPos() + 1);

        }

        getRequest().unparsedURI().setBytes(getBuf(), start, end - start);
        if (questionPos >= 0) {
            getRequest().queryString().setBytes(getBuf(), questionPos + 1, 
                                           end - questionPos - 1);
            getRequest().requestURI().setBytes(getBuf(), start, questionPos - start);
        } else {
            getRequest().requestURI().setBytes(getBuf(), start, end - start);
        }

        // Spec says single SP but also says be tolerant of multiple and/or HT
        while (space) {
            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }
            if (getBuf()[getPos()] == Constants26.getSp() || getBuf()[getPos()] == Constants26.getHt()) {
                setPos(getPos() + 1);
            } else {
                space = false;
            }
        }


        // Mark the current buffer position
        start = getPos();
        end = 0;

        //
        // Reading the protocol
        // Protocol is always US-ASCII
        //

        while (!eol) {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            if (getBuf()[getPos()] == Constants26.getCr()) {
                end = getPos();
            } else if (getBuf()[getPos()] == Constants26.getLf()) {
                if (end == 0)
                    end = getPos();
                eol = true;
            }

            setPos(getPos() + 1);

        }

        if ((end - start) > 0) {
            getRequest().protocol().setBytes(getBuf(), start, end - start);
        } else {
            getRequest().protocol().setString("");
        }
        
        return true;

    }


    /**
     * Parse the HTTP headers.
     */
    @Override
    public boolean parseHeaders()
        throws IOException {
        if (!isParsingHeader()) {
            throw new IllegalStateException(
                    getSm().getString("iib.parseheaders.ise.error"));
        }

        while (parseHeader()) {
            // Loop until there are no more headers
        }

        setParsingHeader(false);
        setEnd(getPos());
        return true;
    }


    /**
     * Parse an HTTP header.
     * 
     * @return false after reading a blank line (which indicates that the
     * HTTP header parsing is done
     */
    @SuppressWarnings("null") // headerValue cannot be null
    private boolean parseHeader()
        throws IOException {

        //
        // Check for blank line
        //

        byte chr = 0;
        while (true) {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            chr = getBuf()[getPos()];

            if (chr == Constants26.getCr()) {
                // Skip
            } else if (chr == Constants26.getLf()) {
                setPos(getPos() + 1);
                return false;
            } else {
                break;
            }

            setPos(getPos() + 1);

        }

        // Mark the current buffer position
        int start = getPos();

        //
        // Reading the header name
        // Header name is always US-ASCII
        //

        boolean colon = false;
        MessageBytes headerValue = null;

        while (!colon) {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            if (getBuf()[getPos()] == Constants26.getColon()) {
                colon = true;
                headerValue = getHeaders().addValue(getBuf(), start, getPos() - start);
            } else if (!HTTP_TOKEN_CHAR[getBuf()[getPos()]]) {
                // If a non-token header is detected, skip the line and
                // ignore the header
                skipLine(start);
                return true;
            }
            chr = getBuf()[getPos()];
            if ((chr >= Constants26.getA()) && (chr <= Constants26.getZ())) {
                getBuf()[getPos()] = (byte) (chr - Constants26.getLcOffset());
            }

            setPos(getPos() + 1);

        }

        // Mark the current buffer position
        start = getPos();
        int realPos = getPos();

        //
        // Reading the header value (which can be spanned over multiple lines)
        //

        boolean eol = false;
        boolean validLine = true;

        while (validLine) {

            boolean space = true;

            // Skipping spaces
            while (space) {

                // Read new bytes if needed
                if (getPos() >= getLastValid()) {
                    if (!fill())
                        throw new EOFException(getSm().getString("iib.eof.error"));
                }

                if ((getBuf()[getPos()] == Constants26.getSp()) || (getBuf()[getPos()] == Constants26.getHt())) {
                    setPos(getPos() + 1);
                } else {
                    space = false;
                }

            }

            int lastSignificantChar = realPos;

            // Reading bytes until the end of the line
            while (!eol) {

                // Read new bytes if needed
                if (getPos() >= getLastValid()) {
                    if (!fill())
                        throw new EOFException(getSm().getString("iib.eof.error"));
                }

                if (getBuf()[getPos()] == Constants26.getCr()) {
                    // Skip
                } else if (getBuf()[getPos()] == Constants26.getLf()) {
                    eol = true;
                } else if (getBuf()[getPos()] == Constants26.getSp()) {
                    getBuf()[realPos] = getBuf()[getPos()];
                    realPos++;
                } else {
                    getBuf()[realPos] = getBuf()[getPos()];
                    realPos++;
                    lastSignificantChar = realPos;
                }

                setPos(getPos() + 1);

            }

            realPos = lastSignificantChar;

            // Checking the first character of the new line. If the character
            // is a LWS, then it's a multiline header

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            chr = getBuf()[getPos()];
            if ((chr != Constants26.getSp()) && (chr != Constants26.getHt())) {
                validLine = false;
            } else {
                eol = false;
                // Copying one extra space in the buffer (since there must
                // be at least one space inserted between the lines)
                getBuf()[realPos] = chr;
                realPos++;
            }

        }

        // Set the header value
        headerValue.setBytes(getBuf(), start, realPos - start);

        return true;

    }

    
    private void skipLine(int start) throws IOException {
        boolean eol = false;
        int lastRealByte = start;
        if (getPos() - 1 > start) {
            lastRealByte = getPos() - 1;
        }
        
        while (!eol) {

            // Read new bytes if needed
            if (getPos() >= getLastValid()) {
                if (!fill())
                    throw new EOFException(getSm().getString("iib.eof.error"));
            }

            if (getBuf()[getPos()] == Constants26.getCr()) {
                // Skip
            } else if (getBuf()[getPos()] == Constants26.getLf()) {
                eol = true;
            } else {
                lastRealByte = getPos();
            }
            setPos(getPos() + 1);
        }

        if (log.isDebugEnabled()) {
            log.debug(getSm().getString("iib.invalidheader", new String(getBuf(), start,
                    lastRealByte - start + 1, Charset.forName("ISO-8859-1"))));
        }
    }
    
    
    // ---------------------------------------------------- InputBuffer Methods


    /**
     * Read some bytes.
     */
    @Override
    public int doRead(ByteChunk chunk, Request2 req) 
        throws IOException {

        if (getLastActiveFilter() == -1)
            return getInputStreamInputBuffer().doRead(chunk, req);
        else
            return getActiveFilters()[getLastActiveFilter()].doRead(chunk,req);

    }


    // ------------------------------------------------------ Protected Methods

    @Override
    protected void init(SocketWrapper<Long> socketWrapper,
            AbstractEndpoint<Long> endpoint) throws IOException {

        socket = socketWrapper.getSocket().longValue();
        Socket.setrbb(this.socket, bbuf);
    }


    @Override
    protected boolean fill(boolean block) throws IOException {
        // Ignore the block parameter and just call fill
        return fill();
    }
    
    
    /**
     * Fill the internal buffer using data from the underlying input stream.
     * 
     * @return false if at end of stream
     */
    protected boolean fill()
        throws IOException {

        int nRead = 0;

        if (isParsingHeader()) {

            if (getLastValid() == getBuf().length) {
                throw new IllegalArgumentException
                    (getSm().getString("iib.requestheadertoolarge.error"));
            }

            bbuf.clear();
            nRead = Socket.recvbb(socket, 0, getBuf().length - getLastValid());
            if (nRead > 0) {
                bbuf.limit(nRead);
                bbuf.get(getBuf(), getPos(), nRead);
                setLastValid(getPos() + nRead);
            } else {
                if ((-nRead) == Status.getEagain()) {
                    return false;
                } else {
                    throw new IOException(getSm().getString("iib.failedread"));
                }
            }

        } else {

            if (getBuf().length - getEnd() < 4500) {
                // In this case, the request header was really large, so we allocate a 
                // brand new one; the old one will get GCed when subsequent requests
                // clear all references
                setBuf(new byte[getBuf().length]);
                setEnd(0);
            }
            setPos(getEnd());
            setLastValid(getPos());
            bbuf.clear();
            nRead = Socket.recvbb(socket, 0, getBuf().length - getLastValid());
            if (nRead > 0) {
                bbuf.limit(nRead);
                bbuf.get(getBuf(), getPos(), nRead);
                setLastValid(getPos() + nRead);
            } else {
                if ((-nRead) == Status.getEtimedout() || (-nRead) == Status.getTimeup()) {
                    throw new SocketTimeoutException(getSm().getString("iib.failedread"));
                } else if (nRead == 0) {
                    // APR_STATUS_IS_EOF, since native 1.1.22
                    return false;
                } else {
                    throw new IOException(getSm().getString("iib.failedread"));
                }
            }

        }

        return (nRead > 0);

    }
}
