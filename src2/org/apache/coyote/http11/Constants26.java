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

import org.apache.tomcat.util.buf.ByteChunk;

/**
 * Constants.
 *
 * @author Remy Maucherat
 */
public final class Constants26 {

	// -------------------------------------------------------------- Constants

	/**
	 * Package name.
	 */
	private static final String Package = "org.apache.coyote.http11";

	private static final int DEFAULT_CONNECTION_LINGER = -1;
	private static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
	private static final boolean DEFAULT_TCP_NO_DELAY = true;

	/**
	 * CRLF.
	 */
	private static final String CRLF = "\r\n";

	/**
	 * Server string.
	 */
	private static final byte[] SERVER_BYTES = ByteChunk
			.convertToBytes("Server: Apache-Coyote/1.1" + CRLF);

	/**
	 * CR.
	 */
	private static final byte CR = (byte) '\r';

	/**
	 * LF.
	 */
	private static final byte LF = (byte) '\n';

	/**
	 * SP.
	 */
	private static final byte SP = (byte) ' ';

	/**
	 * HT.
	 */
	private static final byte HT = (byte) '\t';

	/**
	 * COLON.
	 */
	private static final byte COLON = (byte) ':';

	/**
	 * SEMI_COLON.
	 */
	private static final byte SEMI_COLON = (byte) ';';

	/**
	 * 'A'.
	 */
	private static final byte A = (byte) 'A';

	/**
	 * 'a'.
	 */
	private static final byte a = (byte) 'a';

	/**
	 * 'Z'.
	 */
	private static final byte Z = (byte) 'Z';

	/**
	 * '?'.
	 */
	private static final byte QUESTION = (byte) '?';

	/**
	 * Lower case offset.
	 */
	private static final byte LC_OFFSET = A - a;

	/* Various constant "strings" */
	private static final String CONNECTION = "Connection";
	private static final String CLOSE = "close";
	private static final byte[] CLOSE_BYTES = ByteChunk.convertToBytes(CLOSE);
	private static final String KEEPALIVE = "keep-alive";
	private static final byte[] KEEPALIVE_BYTES = ByteChunk
			.convertToBytes(KEEPALIVE);
	private static final String CHUNKED = "chunked";
	private static final byte[] ACK_BYTES = ByteChunk
			.convertToBytes("HTTP/1.1 100 Continue" + CRLF + CRLF);
	private static final String TRANSFERENCODING = "Transfer-Encoding";
	private static final byte[] _200_BYTES = ByteChunk.convertToBytes("200");
	private static final byte[] _400_BYTES = ByteChunk.convertToBytes("400");
	private static final byte[] _404_BYTES = ByteChunk.convertToBytes("404");

	/**
	 * Identity filters (input and output).
	 */
	private static final int IDENTITY_FILTER = 0;

	/**
	 * Chunked filters (input and output).
	 */
	private static final int CHUNKED_FILTER = 1;

	/**
	 * Void filters (input and output).
	 */
	private static final int VOID_FILTER = 2;

	/**
	 * GZIP filter (output).
	 */
	private static final int GZIP_FILTER = 3;

	/**
	 * Buffered filter (input)
	 */
	private static final int BUFFERED_FILTER = 3;

	/**
	 * HTTP/1.0.
	 */
	private static final String HTTP_10 = "HTTP/1.0";

	/**
	 * HTTP/1.1.
	 */
	private static final String HTTP_11 = "HTTP/1.1";
	private static final byte[] HTTP_11_BYTES = ByteChunk
			.convertToBytes(HTTP_11);

	/**
	 * GET.
	 */
	private static final String GET = "GET";

	/**
	 * HEAD.
	 */
	private static final String HEAD = "HEAD";

	/**
	 * POST.
	 */
	private static final String POST = "POST";

	/**
	 * Has security been turned on?
	 *
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final boolean IS_SECURITY_ENABLED = (System
			.getSecurityManager() != null);

	public static boolean isSecurityEnabled() {
		return IS_SECURITY_ENABLED;
	}

	public static String getPost() {
		return POST;
	}

	public static String getHead() {
		return HEAD;
	}

	public static String getGet() {
		return GET;
	}

	public static byte[] getHttp11Bytes() {
		return HTTP_11_BYTES;
	}

	public static String getHttp10() {
		return HTTP_10;
	}

	public static int getBufferedFilter() {
		return BUFFERED_FILTER;
	}

	public static int getGzipFilter() {
		return GZIP_FILTER;
	}

	public static int getVoidFilter() {
		return VOID_FILTER;
	}

	public static int getChunkedFilter() {
		return CHUNKED_FILTER;
	}

	public static int getIdentityFilter() {
		return IDENTITY_FILTER;
	}

	public static byte[] get404Bytes() {
		return _404_BYTES;
	}

	public static byte[] get400Bytes() {
		return _400_BYTES;
	}

	public static byte[] get200Bytes() {
		return _200_BYTES;
	}

	public static String getTransferencoding() {
		return TRANSFERENCODING;
	}

	public static byte[] getAckBytes() {
		return ACK_BYTES;
	}

	public static String getChunked() {
		return CHUNKED;
	}

	public static byte[] getKeepaliveBytes() {
		return KEEPALIVE_BYTES;
	}

	public static byte[] getCloseBytes() {
		return CLOSE_BYTES;
	}

	public static String getConnection() {
		return CONNECTION;
	}

	public static byte getLcOffset() {
		return LC_OFFSET;
	}

	public static byte getQuestion() {
		return QUESTION;
	}

	public static byte getZ() {
		return Z;
	}

	public static byte getSemiColon() {
		return SEMI_COLON;
	}

	public static byte getColon() {
		return COLON;
	}

	public static byte getHt() {
		return HT;
	}

	public static byte getSp() {
		return SP;
	}

	public static byte getLf() {
		return LF;
	}

	public static byte getCr() {
		return CR;
	}

	public static byte[] getServerBytes() {
		return SERVER_BYTES;
	}

	public static boolean isDefaultTcpNoDelay() {
		return DEFAULT_TCP_NO_DELAY;
	}

	public static int getDefaultConnectionTimeout() {
		return DEFAULT_CONNECTION_TIMEOUT;
	}

	public static int getDefaultConnectionLinger() {
		return DEFAULT_CONNECTION_LINGER;
	}

	public static String getPackage() {
		return Package;
	}

	public static String getCrlf() {
		return CRLF;
	}

	public static byte getA() {
		return A;
	}

	public static String getClose() {
		return CLOSE;
	}

	public static String getKeepalive() {
		return KEEPALIVE;
	}

	public static String getHttp11() {
		return HTTP_11;
	}

	public static char geta(){
		return a;
	}
}
