package org.apache.coyote.http11;

public enum InternalNioInputBufferHeaderParsePosition {
	/**
	 * Start of a new header. A CRLF here means that there are no more
	 * headers. Any other character starts a header name.
	 */
	HEADER_START,
	/**
	 * Reading a header name. All characters of header are HTTP_TOKEN_CHAR.
	 * Header name is followed by ':'. No whitespace is allowed.<br />
	 * Any non-HTTP_TOKEN_CHAR (this includes any whitespace) encountered
	 * before ':' will result in the whole line being ignored.
	 */
	HEADER_NAME,
	/**
	 * Skipping whitespace before text of header value starts, either on the
	 * first line of header value (just after ':') or on subsequent lines
	 * when it is known that subsequent line starts with SP or HT.
	 */
	HEADER_VALUE_START,
	/**
	 * Reading the header value. We are inside the value. Either on the
	 * first line or on any subsequent line. We come into this state from
	 * HEADER_VALUE_START after the first non-SP/non-HT byte is encountered
	 * on the line.
	 */
	HEADER_VALUE,
	/**
	 * Before reading a new line of a header. Once the next byte is peeked,
	 * the state changes without advancing our position. The state becomes
	 * either HEADER_VALUE_START (if that first byte is SP or HT), or
	 * HEADER_START (otherwise).
	 */
	HEADER_MULTI_LINE,
	/**
	 * Reading all bytes until the next CRLF. The line is being ignored.
	 */
	HEADER_SKIPLINE
}