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

import org.apache.coyote.ActionCode;
import org.apache.coyote.Response3;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketWrapper;

/**
 * Output buffer.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
public class InternalAprOutputBuffer extends AbstractOutputBuffer<Long> {

	// ----------------------------------------------------------- Constructors

	/**
	 * Default constructor.
	 */
	public InternalAprOutputBuffer(Response3 response, int headerBufferSize) {

		this.setResponse(response);

		setBuf(new byte[headerBufferSize]);
		if (headerBufferSize < (8 * 1024)) {
			bbuf = ByteBuffer.allocateDirect(6 * 1500);
		} else {
			bbuf = ByteBuffer
					.allocateDirect((headerBufferSize / 1500 + 1) * 1500);
		}

		setOutputStreamOutputBuffer(new InternalAprOutputBufferSocketOutputBuffer(
				this));

		setFilterLibrary(new OutputFilter[0]);
		setActiveFilters(new OutputFilter[0]);
		setLastActiveFilter(-1);

		setCommitted(false);
		setFinished(false);

		// Cause loading of HttpMessages
		HttpMessages.getInstance(response.getLocale()).getMessage(200);

	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * Underlying socket.
	 */
	private long socket;

	/**
	 * Direct byte buffer used for writing.
	 */
	private ByteBuffer bbuf = null;

	// --------------------------------------------------------- Public Methods

	@Override
	public void init(SocketWrapper<Long> socketWrapper,
			AbstractEndpoint<Long> endpoint) throws IOException {

		socket = socketWrapper.getSocket().longValue();
		Socket.setsbb(this.socket, bbuf);
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
		flushBuffer();
	}

	/**
	 * Recycle the output buffer. This should be called when closing the
	 * connection.
	 */
	@Override
	public void recycle() {

		super.recycle();

		bbuf.clear();
	}

	/**
	 * End request.
	 * 
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	@Override
	public void endRequest() throws IOException {

		if (!isCommitted()) {

			// Send the connector a request for commit. The connector should
			// then validate the headers, send them (using sendHeader) and
			// set the filters accordingly.
			getResponse().action(ActionCode.COMMIT, null);

		}

		if (isFinished())
			return;

		if (getLastActiveFilter() != -1)
			getActiveFilters()[getLastActiveFilter()].end();

		flushBuffer();

		setFinished(true);

	}

	// ------------------------------------------------ HTTP/1.1 Output Methods

	/**
	 * Send an acknowledgment.
	 */
	@Override
	public void sendAck() throws IOException {

		if (!isCommitted()) {
			if (Socket.send(socket, Constants26.getAckBytes(), 0,
					Constants26.getAckBytes().length) < 0)
				throw new IOException(getSm().getString("iib.failedwrite"));
		}

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
			bbuf.put(getBuf(), 0, getPos());
		}

	}

	/**
	 * Callback to write data from the buffer.
	 */
	public void flushBuffer() throws IOException {
		if (bbuf.position() > 0) {
			if (Socket.sendbb(socket, 0, bbuf.position()) < 0) {
				throw new IOException();
			}
			bbuf.clear();
		}
	}

	public long getSocket() {
		return socket;
	}

	public void setSocket(long socket) {
		this.socket = socket;
	}

	public ByteBuffer getBbuf() {
		return bbuf;
	}

	public void setBbuf(ByteBuffer bbuf) {
		this.bbuf = bbuf;
	}

}
