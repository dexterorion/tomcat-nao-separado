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
import java.io.OutputStream;
import java.net.Socket;

import org.apache.coyote.Response3;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.ByteChunkByteOutputChannel;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketWrapper;

/**
 * Output buffer.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
public class InternalOutputBuffer extends AbstractOutputBuffer<Socket>
		implements ByteChunkByteOutputChannel {

	// ----------------------------------------------------------- Constructors

	/**
	 * Default constructor.
	 */
	public InternalOutputBuffer(Response3 response, int headerBufferSize) {

		this.setResponse(response);

		setBuf(new byte[headerBufferSize]);

		setOutputStreamOutputBuffer(new InternalOutputBufferOutputStreamOutputBuffer(
				this));

		setFilterLibrary(new OutputFilter[0]);
		setActiveFilters(new OutputFilter[0]);
		setLastActiveFilter(-1);

		socketBuffer = new ByteChunk();
		socketBuffer.setByteOutputChannel(this);

		setCommitted(false);
		setFinished(false);

	}

	/**
	 * Underlying output stream. Note: protected to assist with unit testing
	 */
	private OutputStream outputStream;

	/**
	 * Socket buffer.
	 */
	private ByteChunk socketBuffer;

	/**
	 * Socket buffer (extra buffering to reduce number of packets sent).
	 */
	private boolean useSocketBuffer = false;

	/**
	 * Set the socket buffer size.
	 */
	public void setSocketBuffer(int socketBufferSize) {

		if (socketBufferSize > 500) {
			useSocketBuffer = true;
			socketBuffer.allocate(socketBufferSize, socketBufferSize);
		} else {
			useSocketBuffer = false;
		}

	}

	// --------------------------------------------------------- Public Methods

	@Override
	public void init(SocketWrapper<Socket> socketWrapper,
			AbstractEndpoint<Socket> endpoint) throws IOException {

		outputStream = socketWrapper.getSocket().getOutputStream();
	}

	/**
	 * Flush the response.
	 * 
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	@Override
	public void flush() throws IOException {

		super.flush();

		// Flush the current buffer
		if (useSocketBuffer) {
			socketBuffer.flushBuffer();
		}

	}

	/**
	 * Recycle the output buffer. This should be called when closing the
	 * connection.
	 */
	@Override
	public void recycle() {
		super.recycle();
		outputStream = null;
	}

	/**
	 * End processing of current HTTP request. Note: All bytes of the current
	 * request should have been already consumed. This method only resets all
	 * the pointers so that we are ready to parse the next HTTP request.
	 */
	@Override
	public void nextRequest() {
		super.nextRequest();
		socketBuffer.recycle();
	}

	/**
	 * End request.
	 * 
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	@Override
	public void endRequest() throws IOException {
		super.endRequest();
		if (useSocketBuffer) {
			socketBuffer.flushBuffer();
		}
	}

	// ------------------------------------------------ HTTP/1.1 Output Methods

	/**
	 * Send an acknowledgment.
	 */
	@Override
	public void sendAck() throws IOException {

		if (!isCommitted())
			outputStream.write(Constants26.getAckBytes());

	}

	// ------------------------------------------------------ Protected Methods

	/**
	 * Commit the response.
	 * 
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	@Override
	protected void commit() throws IOException {

		// The response is now committed
		setCommitted(true);
		getResponse().setCommitted(true);

		if (getPos() > 0) {
			// Sending the response header buffer
			if (useSocketBuffer) {
				socketBuffer.append(getBuf(), 0, getPos());
			} else {
				outputStream.write(getBuf(), 0, getPos());
			}
		}

	}

	/**
	 * Callback to write data from the buffer.
	 */
	@Override
	public void realWriteBytes(byte cbuf[], int off, int len)
			throws IOException {
		if (len > 0) {
			outputStream.write(cbuf, off, len);
		}
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public ByteChunk getSocketBuffer() {
		return socketBuffer;
	}

	public void setSocketBuffer(ByteChunk socketBuffer) {
		this.socketBuffer = socketBuffer;
	}

	public boolean isUseSocketBuffer() {
		return useSocketBuffer;
	}

	public void setUseSocketBuffer(boolean useSocketBuffer) {
		this.useSocketBuffer = useSocketBuffer;
	}

}
