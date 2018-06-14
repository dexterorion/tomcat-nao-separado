package org.apache.coyote.http11;

import org.apache.tomcat.util.buf.MessageBytes;

public class InternalNioInputBufferHeaderParseData {
	/**
	 * When parsing header name: first character of the header.<br />
	 * When skipping broken header line: first character of the header.<br />
	 * When parsing header value: first character after ':'.
	 */
	private int start = 0;
	/**
	 * When parsing header name: not used (stays as 0).<br />
	 * When skipping broken header line: not used (stays as 0).<br />
	 * When parsing header value: starts as the first character after ':'.
	 * Then is increased as far as more bytes of the header are harvested.
	 * Bytes from buf[pos] are copied to buf[realPos]. Thus the string from
	 * [start] to [realPos-1] is the prepared value of the header, with
	 * whitespaces removed as needed.<br />
	 */
	private int realPos = 0;
	/**
	 * When parsing header name: not used (stays as 0).<br />
	 * When skipping broken header line: last non-CR/non-LF character.<br />
	 * When parsing header value: position after the last not-LWS character.<br />
	 */
	private int lastSignificantChar = 0;
	/**
	 * MB that will store the value of the header. It is null while parsing
	 * header name and is created after the name has been parsed.
	 */
	private MessageBytes headerValue = null;

	public void recycle() {
		setStart(0);
		setRealPos(0);
		setLastSignificantChar(0);
		setHeaderValue(null);
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getRealPos() {
		return realPos;
	}

	public void setRealPos(int realPos) {
		this.realPos = realPos;
	}

	public int getLastSignificantChar() {
		return lastSignificantChar;
	}

	public void setLastSignificantChar(int lastSignificantChar) {
		this.lastSignificantChar = lastSignificantChar;
	}

	public MessageBytes getHeaderValue() {
		return headerValue;
	}

	public void setHeaderValue(MessageBytes headerValue) {
		this.headerValue = headerValue;
	}
}