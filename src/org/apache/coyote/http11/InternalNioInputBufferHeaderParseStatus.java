package org.apache.coyote.http11;

public enum InternalNioInputBufferHeaderParseStatus {
	DONE, HAVE_MORE_HEADERS, NEED_MORE_DATA
}