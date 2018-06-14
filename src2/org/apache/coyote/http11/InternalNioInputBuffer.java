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
import java.nio.channels.Selector;
import java.nio.charset.Charset;

import org.apache.coyote.Request2;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioEndpointKeyAttachment;
import org.apache.tomcat.util.net.NioSelectorPool;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Implementation of InputBuffer which provides HTTP request header parsing as
 * well as transfer decoding.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Filip Hanik
 */
public class InternalNioInputBuffer extends AbstractInputBuffer<NioChannel> {

	private static final Log log = LogFactory.getLog(InternalNioInputBuffer.class);

	private static final Charset DEFAULT_CHARSET = Charset
			.forName("ISO-8859-1");

	/**
	 * Alternate constructor.
	 */
	public InternalNioInputBuffer(Request2 request, int headerBufferSize) {

		this.setRequest(request);
		setHeaders(request.getMimeHeaders());

		this.headerBufferSize = headerBufferSize;

		setInputStreamInputBuffer(new InternalNioInputBufferSocketInputBuffer(this));

		setFilterLibrary(new InputFilter[0]);
		setActiveFilters(new InputFilter[0]);
		setLastActiveFilter(-1);

		setParsingHeader(true);
		parsingRequestLine = true;
		parsingRequestLinePhase = 0;
		parsingRequestLineEol = false;
		parsingRequestLineStart = 0;
		parsingRequestLineQPos = -1;
		headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_START;
		headerData.recycle();
		setSwallowInput(true);

	}

	/**
	 * Parsing state - used for non blocking parsing so that when more data
	 * arrives, we can pick up where we left off.
	 */
	private boolean parsingRequestLine;
	private int parsingRequestLinePhase = 0;
	private boolean parsingRequestLineEol = false;
	private int parsingRequestLineStart = 0;
	private int parsingRequestLineQPos = -1;
	private InternalNioInputBufferHeaderParsePosition headerParsePos;

	/**
	 * Underlying socket.
	 */
	private NioChannel socket;

	/**
	 * Selector pool, for blocking reads and blocking writes
	 */
	private NioSelectorPool pool;

	/**
	 * Maximum allowed size of the HTTP request line plus headers plus any
	 * leading blank lines.
	 */
	private final int headerBufferSize;

	/**
	 * Known size of the NioChannel read buffer.
	 */
	private int socketReadBufferSize;

	// --------------------------------------------------------- Public Methods

	/**
	 * Recycle the input buffer. This should be called when closing the
	 * connection.
	 */
	@Override
	public void recycle() {
		super.recycle();
		socket = null;
		headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_START;
		parsingRequestLine = true;
		parsingRequestLinePhase = 0;
		parsingRequestLineEol = false;
		parsingRequestLineStart = 0;
		parsingRequestLineQPos = -1;
		headerData.recycle();
	}

	/**
	 * End processing of current HTTP request. Note: All bytes of the current
	 * request should have been already consumed. This method only resets all
	 * the pointers so that we are ready to parse the next HTTP request.
	 */
	@Override
	public void nextRequest() {
		super.nextRequest();
		headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_START;
		parsingRequestLine = true;
		parsingRequestLinePhase = 0;
		parsingRequestLineEol = false;
		parsingRequestLineStart = 0;
		parsingRequestLineQPos = -1;
		headerData.recycle();
	}

	/**
	 * Read the request line. This function is meant to be used during the HTTP
	 * request header parsing. Do NOT attempt to read the request body using it.
	 *
	 * @throws IOException
	 *             If an exception occurs during the underlying socket read
	 *             operations, or if the given buffer is not big enough to
	 *             accommodate the whole line.
	 * @return true if data is properly fed; false if no data is available
	 *         immediately and thread should be freed
	 */
	@Override
	public boolean parseRequestLine(boolean useAvailableDataOnly)
			throws IOException {

		// check state
		if (!parsingRequestLine)
			return true;
		//
		// Skipping blank lines
		//
		if (parsingRequestLinePhase == 0) {
			byte chr = 0;
			do {

				// Read new bytes if needed
				if (getPos() >= getLastValid()) {
					if (useAvailableDataOnly) {
						return false;
					}
					// Do a simple read with a short timeout
					if (!fill(true, false)) {
						return false;
					}
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

			parsingRequestLineStart = getPos();
			parsingRequestLinePhase = 2;
			if (log.isDebugEnabled()) {
				log.debug("Received ["
						+ new String(getBuf(), getPos(), getLastValid()
								- getPos(), DEFAULT_CHARSET) + "]");
			}
		}
		if (parsingRequestLinePhase == 2) {
			//
			// Reading the method name
			// Method name is always US-ASCII
			//
			boolean space = false;
			while (!space) {
				// Read new bytes if needed
				if (getPos() >= getLastValid()) {
					if (!fill(true, false)) // request line parsing
						return false;
				}
				// Spec says no CR or LF in method name
				if (getBuf()[getPos()] == Constants26.getCr()
						|| getBuf()[getPos()] == Constants26.getLf()) {
					throw new IllegalArgumentException(getSm().getString(
							"iib.invalidmethod"));
				}
				if (getBuf()[getPos()] == Constants26.getSp()
						|| getBuf()[getPos()] == Constants26.getHt()) {
					space = true;
					getRequest().method().setBytes(getBuf(),
							parsingRequestLineStart,
							getPos() - parsingRequestLineStart);
				}
				setPos(getPos() + 1);
			}
			parsingRequestLinePhase = 3;
		}
		if (parsingRequestLinePhase == 3) {
			// Spec says single SP but also be tolerant of multiple and/or HT
			boolean space = true;
			while (space) {
				// Read new bytes if needed
				if (getPos() >= getLastValid()) {
					if (!fill(true, false)) // request line parsing
						return false;
				}
				if (getBuf()[getPos()] == Constants26.getSp()
						|| getBuf()[getPos()] == Constants26.getHt()) {
					setPos(getPos() + 1);
				} else {
					space = false;
				}
			}
			parsingRequestLineStart = getPos();
			parsingRequestLinePhase = 4;
		}
		if (parsingRequestLinePhase == 4) {
			// Mark the current buffer position

			int end = 0;
			//
			// Reading the URI
			//
			boolean space = false;
			while (!space) {
				// Read new bytes if needed
				if (getPos() >= getLastValid()) {
					if (!fill(true, false)) // request line parsing
						return false;
				}
				if (getBuf()[getPos()] == Constants26.getSp()
						|| getBuf()[getPos()] == Constants26.getHt()) {
					space = true;
					end = getPos();
				} else if ((getBuf()[getPos()] == Constants26.getCr())
						|| (getBuf()[getPos()] == Constants26.getLf())) {
					// HTTP/0.9 style request
					parsingRequestLineEol = true;
					space = true;
					end = getPos();
				} else if ((getBuf()[getPos()] == Constants26.getQuestion())
						&& (parsingRequestLineQPos == -1)) {
					parsingRequestLineQPos = getPos();
				}
				setPos(getPos() + 1);
			}
			getRequest().unparsedURI().setBytes(getBuf(),
					parsingRequestLineStart, end - parsingRequestLineStart);
			if (parsingRequestLineQPos >= 0) {
				getRequest().queryString().setBytes(getBuf(),
						parsingRequestLineQPos + 1,
						end - parsingRequestLineQPos - 1);
				getRequest().requestURI().setBytes(getBuf(),
						parsingRequestLineStart,
						parsingRequestLineQPos - parsingRequestLineStart);
			} else {
				getRequest().requestURI().setBytes(getBuf(),
						parsingRequestLineStart, end - parsingRequestLineStart);
			}
			parsingRequestLinePhase = 5;
		}
		if (parsingRequestLinePhase == 5) {
			// Spec says single SP but also be tolerant of multiple and/or HT
			boolean space = true;
			while (space) {
				// Read new bytes if needed
				if (getPos() >= getLastValid()) {
					if (!fill(true, false)) // request line parsing
						return false;
				}
				if (getBuf()[getPos()] == Constants26.getSp()
						|| getBuf()[getPos()] == Constants26.getHt()) {
					setPos(getPos() + 1);
				} else {
					space = false;
				}
			}
			parsingRequestLineStart = getPos();
			parsingRequestLinePhase = 6;

			// Mark the current buffer position
			setEnd(0);
		}
		if (parsingRequestLinePhase == 6) {
			//
			// Reading the protocol
			// Protocol is always US-ASCII
			//
			while (!parsingRequestLineEol) {
				// Read new bytes if needed
				if (getPos() >= getLastValid()) {
					if (!fill(true, false)) // request line parsing
						return false;
				}

				if (getBuf()[getPos()] == Constants26.getCr()) {
					setEnd(getPos());
				} else if (getBuf()[getPos()] == Constants26.getLf()) {
					if (getEnd() == 0)
						setEnd(getPos());
					parsingRequestLineEol = true;
				}
				setPos(getPos() + 1);
			}

			if ((getEnd() - parsingRequestLineStart) > 0) {
				getRequest().protocol().setBytes(getBuf(),
						parsingRequestLineStart,
						getEnd() - parsingRequestLineStart);
			} else {
				getRequest().protocol().setString("");
			}
			parsingRequestLine = false;
			parsingRequestLinePhase = 0;
			parsingRequestLineEol = false;
			parsingRequestLineStart = 0;
			return true;
		}
		throw new IllegalStateException("Invalid request line parse phase:"
				+ parsingRequestLinePhase);
	}

	private void expand(int newsize) {
		if (newsize > getBuf().length) {
			if (isParsingHeader()) {
				throw new IllegalArgumentException(getSm().getString(
						"iib.requestheadertoolarge.error"));
			}
			// Should not happen
			log.warn("Expanding buffer size. Old size: " + getBuf().length
					+ ", new size: " + newsize, new Exception());
			byte[] tmp = new byte[newsize];
			System.arraycopy(getBuf(), 0, tmp, 0, getBuf().length);
			setBuf(tmp);
		}
	}

	/**
	 * Perform blocking read with a timeout if desired
	 * 
	 * @param timeout
	 *            boolean - if we want to use the timeout data
	 * @param block
	 *            - true if the system should perform a blocking read, false
	 *            otherwise
	 * @return boolean - true if data was read, false is no data read,
	 *         EOFException if EOF is reached
	 * @throws IOException
	 *             if a socket exception occurs
	 * @throws EOFException
	 *             if end of stream is reached
	 */

	private int readSocket(boolean timeout, boolean block) throws IOException {
		int nRead = 0;
		socket.getBufHandler().getReadBuffer().clear();
		if (block) {
			Selector selector = null;
			try {
				selector = pool.get();
			} catch (IOException x) {
				// Ignore
			}
			try {
				NioEndpointKeyAttachment att = (NioEndpointKeyAttachment) socket
						.getAttachment(false);
				if (att == null) {
					throw new IOException("Key must be cancelled.");
				}
				nRead = pool.read(socket.getBufHandler().getReadBuffer(),
						socket, selector, socket.getIOChannel().socket()
								.getSoTimeout());
			} catch (EOFException eof) {
				nRead = -1;
			} finally {
				if (selector != null)
					pool.put(selector);
			}
		} else {
			nRead = socket.read(socket.getBufHandler().getReadBuffer());
		}
		if (nRead > 0) {
			socket.getBufHandler().getReadBuffer().flip();
			socket.getBufHandler().getReadBuffer().limit(nRead);
			expand(nRead + getPos());
			socket.getBufHandler().getReadBuffer()
					.get(getBuf(), getPos(), nRead);
			setLastValid(getPos() + nRead);
			return nRead;
		} else if (nRead == -1) {
			// return false;
			throw new EOFException(getSm().getString("iib.eof.error"));
		} else {
			return 0;
		}
	}

	/**
	 * Parse the HTTP headers.
	 */
	@Override
	public boolean parseHeaders() throws IOException {
		if (!isParsingHeader()) {
			throw new IllegalStateException(getSm().getString(
					"iib.parseheaders.ise.error"));
		}

		InternalNioInputBufferHeaderParseStatus status = InternalNioInputBufferHeaderParseStatus.HAVE_MORE_HEADERS;

		do {
			status = parseHeader();
			// Checking that
			// (1) Headers plus request line size does not exceed its limit
			// (2) There are enough bytes to avoid expanding the buffer when
			// reading body
			// Technically, (2) is technical limitation, (1) is logical
			// limitation to enforce the meaning of headerBufferSize
			// From the way how buf is allocated and how blank lines are being
			// read, it should be enough to check (1) only.
			if (getPos() > headerBufferSize
					|| getBuf().length - getPos() < socketReadBufferSize) {
				throw new IllegalArgumentException(getSm().getString(
						"iib.requestheadertoolarge.error"));
			}
		} while (status == InternalNioInputBufferHeaderParseStatus.HAVE_MORE_HEADERS);
		if (status == InternalNioInputBufferHeaderParseStatus.DONE) {
			setParsingHeader(false);
			setEnd(getPos());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Parse an HTTP header.
	 * 
	 * @return false after reading a blank line (which indicates that the HTTP
	 *         header parsing is done
	 */
	private InternalNioInputBufferHeaderParseStatus parseHeader() throws IOException {

		//
		// Check for blank line
		//

		byte chr = 0;
		while (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_START) {

			// Read new bytes if needed
			if (getPos() >= getLastValid()) {
				if (!fill(true, false)) {// parse header
					headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_START;
					return InternalNioInputBufferHeaderParseStatus.NEED_MORE_DATA;
				}
			}

			chr = getBuf()[getPos()];

			if (chr == Constants26.getCr()) {
				// Skip
			} else if (chr == Constants26.getLf()) {
				setPos(getPos() + 1);
				return InternalNioInputBufferHeaderParseStatus.DONE;
			} else {
				break;
			}

			setPos(getPos() + 1);

		}

		if (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_START) {
			// Mark the current buffer position
			headerData.setStart(getPos());
			headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_NAME;
		}

		//
		// Reading the header name
		// Header name is always US-ASCII
		//

		while (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_NAME) {

			// Read new bytes if needed
			if (getPos() >= getLastValid()) {
				if (!fill(true, false)) { // parse header
					return InternalNioInputBufferHeaderParseStatus.NEED_MORE_DATA;
				}
			}

			chr = getBuf()[getPos()];
			if (chr == Constants26.getColon()) {
				headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_VALUE_START;
				headerData.setHeaderValue(getHeaders().addValue(getBuf(),
						headerData.getStart(), getPos() - headerData.getStart()));
				setPos(getPos() + 1);
				// Mark the current buffer position
				headerData.setStart(getPos());
				headerData.setRealPos(getPos());
				headerData.setLastSignificantChar(getPos());
				break;
			} else if (!getHttpTokenChar()[chr]) {
				// If a non-token header is detected, skip the line and
				// ignore the header
				headerData.setLastSignificantChar(getPos());
				return skipLine();
			}

			// chr is next byte of header name. Convert to lowercase.
			if ((chr >= Constants26.getA()) && (chr <= Constants26.getZ())) {
				getBuf()[getPos()] = (byte) (chr - Constants26.getLcOffset());
			}
			setPos(getPos() + 1);
		}

		// Skip the line and ignore the header
		if (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_SKIPLINE) {
			return skipLine();
		}

		//
		// Reading the header value (which can be spanned over multiple lines)
		//

		while (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_VALUE_START
				|| headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_VALUE
				|| headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_MULTI_LINE) {

			if (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_VALUE_START) {
				// Skipping spaces
				while (true) {
					// Read new bytes if needed
					if (getPos() >= getLastValid()) {
						if (!fill(true, false)) {// parse header
							// HEADER_VALUE_START
							return InternalNioInputBufferHeaderParseStatus.NEED_MORE_DATA;
						}
					}

					chr = getBuf()[getPos()];
					if (chr == Constants26.getSp() || chr == Constants26.getHt()) {
						setPos(getPos() + 1);
					} else {
						headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_VALUE;
						break;
					}
				}
			}
			if (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_VALUE) {

				// Reading bytes until the end of the line
				boolean eol = false;
				while (!eol) {

					// Read new bytes if needed
					if (getPos() >= getLastValid()) {
						if (!fill(true, false)) {// parse header
							// HEADER_VALUE
							return InternalNioInputBufferHeaderParseStatus.NEED_MORE_DATA;
						}
					}

					chr = getBuf()[getPos()];
					if (chr == Constants26.getCr()) {
						// Skip
					} else if (chr == Constants26.getLf()) {
						eol = true;
					} else if (chr == Constants26.getSp()
							|| chr == Constants26.getHt()) {
						getBuf()[headerData.getRealPos()] = chr;
						headerData.setRealPos(headerData.getRealPos() + 1);
					} else {
						getBuf()[headerData.getRealPos()] = chr;
						headerData.setRealPos(headerData.getRealPos() + 1);
						headerData.setLastSignificantChar(headerData.getRealPos());
					}

					setPos(getPos() + 1);
				}

				// Ignore whitespaces at the end of the line
				headerData.setRealPos(headerData.getLastSignificantChar());

				// Checking the first character of the new line. If the
				// character
				// is a LWS, then it's a multiline header
				headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_MULTI_LINE;
			}
			// Read new bytes if needed
			if (getPos() >= getLastValid()) {
				if (!fill(true, false)) {// parse header

					// HEADER_MULTI_LINE
					return InternalNioInputBufferHeaderParseStatus.NEED_MORE_DATA;
				}
			}

			chr = getBuf()[getPos()];
			if (headerParsePos == InternalNioInputBufferHeaderParsePosition.HEADER_MULTI_LINE) {
				if ((chr != Constants26.getSp()) && (chr != Constants26.getHt())) {
					headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_START;
					break;
				} else {
					// Copying one extra space in the buffer (since there must
					// be at least one space inserted between the lines)
					getBuf()[headerData.getRealPos()] = chr;
					headerData.setRealPos(headerData.getRealPos() + 1);
					headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_VALUE_START;
				}
			}
		}
		// Set the header value
		headerData.getHeaderValue().setBytes(getBuf(), headerData.getStart(),
				headerData.getLastSignificantChar() - headerData.getStart());
		headerData.recycle();
		return InternalNioInputBufferHeaderParseStatus.HAVE_MORE_HEADERS;
	}

	public int getParsingRequestLinePhase() {
		return parsingRequestLinePhase;
	}

	private InternalNioInputBufferHeaderParseStatus skipLine() throws IOException {
		headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_SKIPLINE;
		boolean eol = false;

		// Reading bytes until the end of the line
		while (!eol) {

			// Read new bytes if needed
			if (getPos() >= getLastValid()) {
				if (!fill(true, false)) {
					return InternalNioInputBufferHeaderParseStatus.NEED_MORE_DATA;
				}
			}

			if (getBuf()[getPos()] == Constants26.getCr()) {
				// Skip
			} else if (getBuf()[getPos()] == Constants26.getLf()) {
				eol = true;
			} else {
				headerData.setLastSignificantChar(getPos());
			}

			setPos(getPos() + 1);
		}
		if (log.isDebugEnabled()) {
			log.debug(getSm().getString(
					"iib.invalidheader",
					new String(getBuf(), headerData.getStart(),
							headerData.getLastSignificantChar() - headerData.getStart()
									+ 1, DEFAULT_CHARSET)));
		}

		headerParsePos = InternalNioInputBufferHeaderParsePosition.HEADER_START;
		return InternalNioInputBufferHeaderParseStatus.HAVE_MORE_HEADERS;
	}

	private InternalNioInputBufferHeaderParseData headerData = new InternalNioInputBufferHeaderParseData();

	@Override
	protected void init(SocketWrapper<NioChannel> socketWrapper,
			AbstractEndpoint<NioChannel> endpoint) throws IOException {

		socket = socketWrapper.getSocket();
		socketReadBufferSize = socket.getBufHandler().getReadBuffer()
				.capacity();

		int bufLength = headerBufferSize + socketReadBufferSize;
		if (getBuf() == null || getBuf().length < bufLength) {
			setBuf(new byte[bufLength]);
		}

		pool = ((NioEndpoint) endpoint).getSelectorPool();
	}

	/**
	 * Fill the internal buffer using data from the underlying input stream.
	 * 
	 * @return false if at end of stream
	 */
	@Override
	protected boolean fill(boolean block) throws IOException, EOFException {
		return fill(true, block);
	}

	protected boolean fill(boolean timeout, boolean block) throws IOException,
			EOFException {

		boolean read = false;

		if (isParsingHeader()) {

			if (getLastValid() > headerBufferSize) {
				throw new IllegalArgumentException(getSm().getString(
						"iib.requestheadertoolarge.error"));
			}

			// Do a simple read with a short timeout
			read = readSocket(timeout, block) > 0;
		} else {
			setLastValid(getEnd());
			setPos(getEnd());
			// Do a simple read with a short timeout
			read = readSocket(timeout, block) > 0;
		}
		return read;
	}
}
