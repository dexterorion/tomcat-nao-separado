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
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.coyote.ActionCode;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response3;
import org.apache.coyote.http11.filters.GzipOutputFilter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.tomcat.util.res.StringManager3;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.coyote.Constants24;

public abstract class AbstractOutputBuffer<S> implements OutputBuffer {

	// ----------------------------------------------------- Instance Variables

	/**
	 * Associated Coyote response.
	 */
	private Response3 response;

	/**
	 * Committed flag.
	 */
	private boolean committed;

	/**
	 * Finished flag.
	 */
	private boolean finished;

	/**
	 * The buffer used for header composition.
	 */
	private byte[] buf;

	/**
	 * Position in the buffer.
	 */
	private int pos;

	/**
	 * Filter library. Note: Filter[0] is always the "chunked" filter.
	 */
	private OutputFilter[] filterLibrary;

	/**
	 * Active filter (which is actually the top of the pipeline).
	 */
	private OutputFilter[] activeFilters;

	/**
	 * Index of the last active filter.
	 */
	private int lastActiveFilter;

	/**
	 * Underlying output buffer.
	 */
	private OutputBuffer outputStreamOutputBuffer;

	/**
	 * Bytes written to client for the current request
	 */
	private long byteCount = 0;

	// -------------------------------------------------------------- Variables

	/**
	 * The string manager for this package.
	 */
	private static final StringManager3 sm = StringManager3
			.getManager(Constants26.getPackage());

	/**
	 * Logger.
	 */
	private static final Log log = LogFactory
			.getLog(AbstractOutputBuffer.class);

	// ------------------------------------------------------------- Properties

	/**
	 * Add an output filter to the filter library.
	 */
	public void addFilter(OutputFilter filter) {
		OutputFilter[] newFilterLibrary = new OutputFilter[filterLibrary.length + 1];
		for (int i = 0; i < filterLibrary.length; i++) {
			newFilterLibrary[i] = filterLibrary[i];
		}
		newFilterLibrary[filterLibrary.length] = filter;
		filterLibrary = newFilterLibrary;

		activeFilters = new OutputFilter[filterLibrary.length];

	}

	/**
	 * Get filters.
	 */
	public OutputFilter[] getFilters() {

		return filterLibrary;

	}

	/**
	 * Add an output filter to the filter library.
	 */
	public void addActiveFilter(OutputFilter filter) {

		if (lastActiveFilter == -1) {
			filter.setBuffer(outputStreamOutputBuffer);
		} else {
			for (int i = 0; i <= lastActiveFilter; i++) {
				if (activeFilters[i] == filter)
					return;
			}
			filter.setBuffer(activeFilters[lastActiveFilter]);
		}

		activeFilters[++lastActiveFilter] = filter;

		filter.setResponse(response);

	}

	// --------------------------------------------------- OutputBuffer Methods

	/**
	 * Write the contents of a byte chunk.
	 * 
	 * @param chunk
	 *            byte chunk
	 * @return number of bytes written
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	@Override
	public int doWrite(ByteChunk chunk, Response3 res) throws IOException {

		if (!committed) {

			// Send the connector a request for commit. The connector should
			// then validate the headers, send them (using sendHeaders) and
			// set the filters accordingly.
			response.action(ActionCode.COMMIT, null);

		}

		if (lastActiveFilter == -1)
			return outputStreamOutputBuffer.doWrite(chunk, res);
		else
			return activeFilters[lastActiveFilter].doWrite(chunk, res);

	}

	@Override
	public long getBytesWritten() {
		if (lastActiveFilter == -1) {
			return outputStreamOutputBuffer.getBytesWritten();
		} else {
			return activeFilters[lastActiveFilter].getBytesWritten();
		}
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Flush the response.
	 * 
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	public void flush() throws IOException {

		if (!committed) {

			// Send the connector a request for commit. The connector should
			// then validate the headers, send them (using sendHeader) and
			// set the filters accordingly.
			response.action(ActionCode.COMMIT, null);

		}

		// go through the filters and if there is gzip filter
		// invoke it to flush
		for (int i = 0; i <= lastActiveFilter; i++) {
			if (activeFilters[i] instanceof GzipOutputFilter) {
				if (log.isDebugEnabled()) {
					log.debug("Flushing the gzip filter at position " + i
							+ " of the filter chain...");
				}
				((GzipOutputFilter) activeFilters[i]).flush();
				break;
			}
		}
	}

	/**
	 * Reset current response.
	 * 
	 * @throws IllegalStateException
	 *             if the response has already been committed
	 */
	public void reset() {

		if (committed)
			throw new IllegalStateException(/* FIXME:Put an error message */);

		// These will need to be reset if the reset was triggered by the error
		// handling if the headers were too large
		pos = 0;
		setByteCount(0);
	}

	/**
	 * Recycle the output buffer. This should be called when closing the
	 * connection.
	 */
	public void recycle() {
		// Sub-classes may wish to do more than this.
		nextRequest();
	}

	/**
	 * End processing of current HTTP request. Note: All bytes of the current
	 * request should have been already consumed. This method only resets all
	 * the pointers so that we are ready to parse the next HTTP request.
	 */
	public void nextRequest() {
		// Recycle filters
		for (int i = 0; i <= lastActiveFilter; i++) {
			activeFilters[i].recycle();
		}
		// Recycle response object
		response.recycle();
		// Reset pointers
		pos = 0;
		lastActiveFilter = -1;
		committed = false;
		finished = false;
		setByteCount(0);
	}

	/**
	 * End request.
	 * 
	 * @throws IOException
	 *             an underlying I/O error occurred
	 */
	public void endRequest() throws IOException {

		if (!committed) {

			// Send the connector a request for commit. The connector should
			// then validate the headers, send them (using sendHeader) and
			// set the filters accordingly.
			response.action(ActionCode.COMMIT, null);

		}

		if (finished)
			return;

		if (lastActiveFilter != -1)
			activeFilters[lastActiveFilter].end();
		finished = true;
	}

	public abstract void init(SocketWrapper<S> socketWrapper,
			AbstractEndpoint<S> endpoint) throws IOException;

	public abstract void sendAck() throws IOException;

	protected abstract void commit() throws IOException;

	/**
	 * Send the response status line.
	 */
	public void sendStatus() {

		// Write protocol name
		write(Constants26.getHttp11Bytes());
		buf[pos++] = Constants26.getSp();

		// Write status code
		int status = response.getStatus();
		switch (status) {
		case 200:
			write(Constants26.get200Bytes());
			break;
		case 400:
			write(Constants26.get400Bytes());
			break;
		case 404:
			write(Constants26.get404Bytes());
			break;
		default:
			write(status);
		}

		buf[pos++] = Constants26.getSp();

		// Write message
		String message = null;
		if (Constants24.isUseCustomStatusMsgInHeader()
				&& HttpMessages.isSafeInHttpHeader(response.getMessage())) {
			message = response.getMessage();
		}
		if (message == null) {
			write(HttpMessages.getInstance(response.getLocale()).getMessage(
					status));
		} else {
			write(message);
		}

		// End the response status line
		if (Constants24.isSecurityEnabled()) {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					buf[pos++] = Constants26.getCr();
					buf[pos++] = Constants26.getLf();
					return null;
				}
			});
		} else {
			buf[pos++] = Constants26.getCr();
			buf[pos++] = Constants26.getLf();
		}

	}

	/**
	 * Send a header.
	 * 
	 * @param name
	 *            Header name
	 * @param value
	 *            Header value
	 */
	public void sendHeader(MessageBytes name, MessageBytes value) {

		write(name);
		buf[pos++] = Constants26.getColon();
		buf[pos++] = Constants26.getSp();
		write(value);
		buf[pos++] = Constants26.getCr();
		buf[pos++] = Constants26.getLf();

	}

	/**
	 * End the header block.
	 */
	public void endHeaders() {

		buf[pos++] = Constants26.getCr();
		buf[pos++] = Constants26.getLf();

	}

	/**
	 * This method will write the contents of the specified message bytes buffer
	 * to the output stream, without filtering. This method is meant to be used
	 * to write the response header.
	 * 
	 * @param mb
	 *            data to be written
	 */
	protected void write(MessageBytes mb) {

		if (mb.getType() == MessageBytes.gettBytes()) {
			ByteChunk bc = mb.getByteChunk();
			write(bc);
		} else if (mb.getType() == MessageBytes.gettChars()) {
			CharChunk cc = mb.getCharChunk();
			write(cc);
		} else {
			write(mb.toString());
		}

	}

	/**
	 * This method will write the contents of the specified message bytes buffer
	 * to the output stream, without filtering. This method is meant to be used
	 * to write the response header.
	 * 
	 * @param bc
	 *            data to be written
	 */
	protected void write(ByteChunk bc) {

		// Writing the byte chunk to the output buffer
		int length = bc.getLength();
		checkLengthBeforeWrite(length);
		System.arraycopy(bc.getBytes(), bc.getStart(), buf, pos, length);
		pos = pos + length;

	}

	/**
	 * This method will write the contents of the specified char buffer to the
	 * output stream, without filtering. This method is meant to be used to
	 * write the response header.
	 * 
	 * @param cc
	 *            data to be written
	 */
	protected void write(CharChunk cc) {

		int start = cc.getStart();
		int end = cc.getEnd();
		checkLengthBeforeWrite(end - start);
		char[] cbuf = cc.getBuffer();
		for (int i = start; i < end; i++) {
			char c = cbuf[i];
			// Note: This is clearly incorrect for many strings,
			// but is the only consistent approach within the current
			// servlet framework. It must suffice until servlet output
			// streams properly encode their output.
			if (((c <= 31) && (c != 9)) || c == 127 || c > 255) {
				c = ' ';
			}
			buf[pos++] = (byte) c;
		}

	}

	/**
	 * This method will write the contents of the specified byte buffer to the
	 * output stream, without filtering. This method is meant to be used to
	 * write the response header.
	 * 
	 * @param b
	 *            data to be written
	 */
	public void write(byte[] b) {
		checkLengthBeforeWrite(b.length);

		// Writing the byte chunk to the output buffer
		System.arraycopy(b, 0, buf, pos, b.length);
		pos = pos + b.length;

	}

	/**
	 * This method will write the contents of the specified String to the output
	 * stream, without filtering. This method is meant to be used to write the
	 * response header.
	 * 
	 * @param s
	 *            data to be written
	 */
	protected void write(String s) {

		if (s == null)
			return;

		// From the Tomcat 3.3 HTTP/1.0 connector
		int len = s.length();
		checkLengthBeforeWrite(len);
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			// Note: This is clearly incorrect for many strings,
			// but is the only consistent approach within the current
			// servlet framework. It must suffice until servlet output
			// streams properly encode their output.
			if (((c <= 31) && (c != 9)) || c == 127 || c > 255) {
				c = ' ';
			}
			buf[pos++] = (byte) c;
		}

	}

	/**
	 * This method will print the specified integer to the output stream,
	 * without filtering. This method is meant to be used to write the response
	 * header.
	 * 
	 * @param i
	 *            data to be written
	 */
	protected void write(int i) {

		write(String.valueOf(i));

	}

	/**
	 * Checks to see if there is enough space in the buffer to write the
	 * requested number of bytes.
	 */
	private void checkLengthBeforeWrite(int length) {
		if (pos + length > buf.length) {
			throw new HeadersTooLargeException(
					sm.getString("iob.responseheadertoolarge.error"));
		}
	}

	public long getByteCount() {
		return byteCount;
	}

	public void setByteCount(long byteCount) {
		this.byteCount = byteCount;
	}

	public Response3 getResponse() {
		return response;
	}

	public void setResponse(Response3 response) {
		this.response = response;
	}

	public boolean isCommitted() {
		return committed;
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public byte[] getBuf() {
		return buf;
	}

	public void setBuf(byte[] buf) {
		this.buf = buf;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public OutputFilter[] getFilterLibrary() {
		return filterLibrary;
	}

	public void setFilterLibrary(OutputFilter[] filterLibrary) {
		this.filterLibrary = filterLibrary;
	}

	public OutputFilter[] getActiveFilters() {
		return activeFilters;
	}

	public void setActiveFilters(OutputFilter[] activeFilters) {
		this.activeFilters = activeFilters;
	}

	public int getLastActiveFilter() {
		return lastActiveFilter;
	}

	public void setLastActiveFilter(int lastActiveFilter) {
		this.lastActiveFilter = lastActiveFilter;
	}

	public OutputBuffer getOutputStreamOutputBuffer() {
		return outputStreamOutputBuffer;
	}

	public void setOutputStreamOutputBuffer(
			OutputBuffer outputStreamOutputBuffer) {
		this.outputStreamOutputBuffer = outputStreamOutputBuffer;
	}

	public static StringManager3 getSm() {
		return sm;
	}

	public static Log getLog() {
		return log;
	}

}
