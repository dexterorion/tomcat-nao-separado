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

package org.apache.coyote.ajp;

import java.util.Hashtable;

import org.apache.tomcat.util.buf.ByteChunk;

/**
 * Constants.
 *
 * @author Remy Maucherat
 */
public final class Constants25 {

	// -------------------------------------------------------------- Constants

	/**
	 * Package name.
	 */
	private static final String Package = "org.apache.coyote.ajp";

	private static final int DEFAULT_CONNECTION_LINGER = -1;
	private static final int DEFAULT_CONNECTION_TIMEOUT = -1;
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int DEFAULT_CONNECTION_UPLOAD_TIMEOUT = 300000;
	private static final boolean DEFAULT_TCP_NO_DELAY = true;
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final boolean DEFAULT_USE_SENDFILE = false;

	// Prefix codes for message types from server to container
	private static final byte JK_AJP13_FORWARD_REQUEST = 2;
	private static final byte JK_AJP13_SHUTDOWN = 7; // XXX Unused
	private static final byte JK_AJP13_PING_REQUEST = 8; // XXX Unused
	private static final byte JK_AJP13_CPING_REQUEST = 10;

	// Prefix codes for message types from container to server
	private static final byte JK_AJP13_SEND_BODY_CHUNK = 3;
	private static final byte JK_AJP13_SEND_HEADERS = 4;
	private static final byte JK_AJP13_END_RESPONSE = 5;
	private static final byte JK_AJP13_GET_BODY_CHUNK = 6;
	private static final byte JK_AJP13_CPONG_REPLY = 9;

	// Integer codes for common response header strings
	private static final int SC_RESP_CONTENT_TYPE = 0xA001;
	private static final int SC_RESP_CONTENT_LANGUAGE = 0xA002;
	private static final int SC_RESP_CONTENT_LENGTH = 0xA003;
	private static final int SC_RESP_DATE = 0xA004;
	private static final int SC_RESP_LAST_MODIFIED = 0xA005;
	private static final int SC_RESP_LOCATION = 0xA006;
	private static final int SC_RESP_SET_COOKIE = 0xA007;
	private static final int SC_RESP_SET_COOKIE2 = 0xA008;
	private static final int SC_RESP_SERVLET_ENGINE = 0xA009;
	private static final int SC_RESP_STATUS = 0xA00A;
	private static final int SC_RESP_WWW_AUTHENTICATE = 0xA00B;
	private static final int SC_RESP_AJP13_MAX = 11;

	// Integer codes for common (optional) request attribute names
	private static final byte SC_A_CONTEXT = 1; // XXX Unused
	private static final byte SC_A_SERVLET_PATH = 2; // XXX Unused
	private static final byte SC_A_REMOTE_USER = 3;
	private static final byte SC_A_AUTH_TYPE = 4;
	private static final byte SC_A_QUERY_STRING = 5;
	private static final byte SC_A_JVM_ROUTE = 6;
	private static final byte SC_A_SSL_CERT = 7;
	private static final byte SC_A_SSL_CIPHER = 8;
	private static final byte SC_A_SSL_SESSION = 9;
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte SC_A_SSL_KEYSIZE = 11;
	private static final byte SC_A_SSL_KEY_SIZE = 11;
	private static final byte SC_A_SECRET = 12;
	private static final byte SC_A_STORED_METHOD = 13;

	// Used for attributes which are not in the list above
	private static final byte SC_A_REQ_ATTRIBUTE = 10;

	/**
	 * AJP private request attributes
	 */
	private static final String SC_A_REQ_LOCAL_ADDR = "AJP_LOCAL_ADDR";
	private static final String SC_A_REQ_REMOTE_PORT = "AJP_REMOTE_PORT";

	// Terminates list of attributes
	private static final byte SC_A_ARE_DONE = (byte) 0xFF;

	// Ajp13 specific - needs refactoring for the new model

	/**
	 * Default maximum total byte size for a AJP packet
	 */
	private static final int MAX_PACKET_SIZE = 8192;
	/**
	 * Size of basic packet header
	 */
	private static final int H_SIZE = 4;

	/**
	 * Size of the header metadata
	 */
	private static final int READ_HEAD_LEN = 6;
	private static final int SEND_HEAD_LEN = 8;

	/**
	 * Default maximum size of data that can be sent in one packet
	 */
	private static final int MAX_READ_SIZE = MAX_PACKET_SIZE - READ_HEAD_LEN;
	private static final int MAX_SEND_SIZE = MAX_PACKET_SIZE - SEND_HEAD_LEN;

	// Translates integer codes to names of HTTP methods
	private static final String[] methodTransArray = { "OPTIONS", "GET",
			"HEAD", "POST", "PUT", "DELETE", "TRACE", "PROPFIND", "PROPPATCH",
			"MKCOL", "COPY", "MOVE", "LOCK", "UNLOCK", "ACL", "REPORT",
			"VERSION-CONTROL", "CHECKIN", "CHECKOUT", "UNCHECKOUT", "SEARCH",
			"MKWORKSPACE", "UPDATE", "LABEL", "MERGE", "BASELINE-CONTROL",
			"MKACTIVITY" };

	/**
	 * Converts an AJP coded HTTP method to the method name.
	 * 
	 * @param code
	 *            the coded value
	 * @return the string value of the method
	 */
	public static final String getMethodForCode(final int code) {
		return methodTransArray[code];
	}

	private static final int SC_M_JK_STORED = (byte) 0xFF;

	// id's for common request headers
	private static final int SC_REQ_ACCEPT = 1;
	private static final int SC_REQ_ACCEPT_CHARSET = 2;
	private static final int SC_REQ_ACCEPT_ENCODING = 3;
	private static final int SC_REQ_ACCEPT_LANGUAGE = 4;
	private static final int SC_REQ_AUTHORIZATION = 5;
	private static final int SC_REQ_CONNECTION = 6;
	private static final int SC_REQ_CONTENT_TYPE = 7;
	private static final int SC_REQ_CONTENT_LENGTH = 8;
	private static final int SC_REQ_COOKIE = 9;
	private static final int SC_REQ_COOKIE2 = 10;
	private static final int SC_REQ_HOST = 11;
	private static final int SC_REQ_PRAGMA = 12;
	private static final int SC_REQ_REFERER = 13;
	private static final int SC_REQ_USER_AGENT = 14;

	// Translates integer codes to request header names
	private static final String[] headerTransArray = { "accept",
			"accept-charset", "accept-encoding", "accept-language",
			"authorization", "connection", "content-type", "content-length",
			"cookie", "cookie2", "host", "pragma", "referer", "user-agent" };

	/**
	 * Converts an AJP coded HTTP request header to the header name.
	 * 
	 * @param code
	 *            the coded value
	 * @return the string value of the header name
	 */
	public static final String getHeaderForCode(final int code) {
		return headerTransArray[code];
	}

	// Translates integer codes to response header names
	private static final String[] responseTransArray = { "Content-Type",
			"Content-Language", "Content-Length", "Date", "Last-Modified",
			"Location", "Set-Cookie", "Set-Cookie2", "Servlet-Engine",
			"Status", "WWW-Authenticate" };

	/**
	 * Converts an AJP coded response header name to the HTTP response header
	 * name.
	 * 
	 * @param code
	 *            the coded value
	 * @return the string value of the header
	 */
	public static final String getResponseHeaderForCode(final int code) {
		return responseTransArray[code];
	}

	private static final Hashtable<String, Integer> responseTransHash = new Hashtable<String, Integer>(
			20);

	static {
		try {
			int i;
			for (i = 0; i < SC_RESP_AJP13_MAX; i++) {
				responseTransHash.put(getResponseHeaderForCode(i),
						Integer.valueOf(0xA001 + i));
			}
		} catch (Exception e) {
			// Do nothing
		}
	}

	public static final int getResponseAjpIndex(String header) {
		Integer i = responseTransHash.get(header);
		if (i == null)
			return 0;
		else
			return i.intValue();
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

	public static byte[] getColonBytes() {
		return COLON_BYTES;
	}

	public static byte[] getCrlfBytes() {
		return CRLF_BYTES;
	}

	public static String getPackage() {
		return Package;
	}

	public static int getDefaultConnectionLinger() {
		return DEFAULT_CONNECTION_LINGER;
	}

	public static int getDefaultConnectionTimeout() {
		return DEFAULT_CONNECTION_TIMEOUT;
	}

	public static int getDefaultConnectionUploadTimeout() {
		return DEFAULT_CONNECTION_UPLOAD_TIMEOUT;
	}

	public static boolean isDefaultTcpNoDelay() {
		return DEFAULT_TCP_NO_DELAY;
	}

	public static boolean isDefaultUseSendfile() {
		return DEFAULT_USE_SENDFILE;
	}

	public static byte getJkAjp13ForwardRequest() {
		return JK_AJP13_FORWARD_REQUEST;
	}

	public static byte getJkAjp13Shutdown() {
		return JK_AJP13_SHUTDOWN;
	}

	public static byte getJkAjp13PingRequest() {
		return JK_AJP13_PING_REQUEST;
	}

	public static byte getJkAjp13CpingRequest() {
		return JK_AJP13_CPING_REQUEST;
	}

	public static byte getJkAjp13SendBodyChunk() {
		return JK_AJP13_SEND_BODY_CHUNK;
	}

	public static byte getJkAjp13SendHeaders() {
		return JK_AJP13_SEND_HEADERS;
	}

	public static byte getJkAjp13EndResponse() {
		return JK_AJP13_END_RESPONSE;
	}

	public static byte getJkAjp13GetBodyChunk() {
		return JK_AJP13_GET_BODY_CHUNK;
	}

	public static byte getJkAjp13CpongReply() {
		return JK_AJP13_CPONG_REPLY;
	}

	public static int getScRespContentType() {
		return SC_RESP_CONTENT_TYPE;
	}

	public static int getScRespContentLanguage() {
		return SC_RESP_CONTENT_LANGUAGE;
	}

	public static int getScRespContentLength() {
		return SC_RESP_CONTENT_LENGTH;
	}

	public static int getScRespDate() {
		return SC_RESP_DATE;
	}

	public static int getScRespLastModified() {
		return SC_RESP_LAST_MODIFIED;
	}

	public static int getScRespLocation() {
		return SC_RESP_LOCATION;
	}

	public static int getScRespSetCookie() {
		return SC_RESP_SET_COOKIE;
	}

	public static int getScRespSetCookie2() {
		return SC_RESP_SET_COOKIE2;
	}

	public static int getScRespServletEngine() {
		return SC_RESP_SERVLET_ENGINE;
	}

	public static int getScRespStatus() {
		return SC_RESP_STATUS;
	}

	public static int getScRespWwwAuthenticate() {
		return SC_RESP_WWW_AUTHENTICATE;
	}

	public static byte getScAContext() {
		return SC_A_CONTEXT;
	}

	public static byte getScAServletPath() {
		return SC_A_SERVLET_PATH;
	}

	public static byte getScARemoteUser() {
		return SC_A_REMOTE_USER;
	}

	public static byte getScAAuthType() {
		return SC_A_AUTH_TYPE;
	}

	public static byte getScAQueryString() {
		return SC_A_QUERY_STRING;
	}

	public static byte getScAJvmRoute() {
		return SC_A_JVM_ROUTE;
	}

	public static byte getScASslCert() {
		return SC_A_SSL_CERT;
	}

	public static byte getScASslCipher() {
		return SC_A_SSL_CIPHER;
	}

	public static byte getScASslSession() {
		return SC_A_SSL_SESSION;
	}

	public static byte getScASslKeysize() {
		return SC_A_SSL_KEYSIZE;
	}

	public static byte getScASslKeySize() {
		return SC_A_SSL_KEY_SIZE;
	}

	public static byte getScASecret() {
		return SC_A_SECRET;
	}

	public static byte getScAStoredMethod() {
		return SC_A_STORED_METHOD;
	}

	public static byte getScAReqAttribute() {
		return SC_A_REQ_ATTRIBUTE;
	}

	public static String getScAReqLocalAddr() {
		return SC_A_REQ_LOCAL_ADDR;
	}

	public static String getScAReqRemotePort() {
		return SC_A_REQ_REMOTE_PORT;
	}

	public static byte getScAAreDone() {
		return SC_A_ARE_DONE;
	}

	public static int gethSize() {
		return H_SIZE;
	}

	public static int getMaxReadSize() {
		return MAX_READ_SIZE;
	}

	public static int getMaxSendSize() {
		return MAX_SEND_SIZE;
	}

	public static int getScMJkStored() {
		return SC_M_JK_STORED;
	}

	public static int getScReqAccept() {
		return SC_REQ_ACCEPT;
	}

	public static int getScReqAcceptCharset() {
		return SC_REQ_ACCEPT_CHARSET;
	}

	public static int getScReqAcceptLanguage() {
		return SC_REQ_ACCEPT_LANGUAGE;
	}

	public static int getScReqAcceptEncoding() {
		return SC_REQ_ACCEPT_ENCODING;
	}

	public static int getScReqAuthorization() {
		return SC_REQ_AUTHORIZATION;
	}

	public static int getScReqConnection() {
		return SC_REQ_CONNECTION;
	}

	public static int getScReqContentType() {
		return SC_REQ_CONTENT_TYPE;
	}

	public static int getScReqContentLength() {
		return SC_REQ_CONTENT_LENGTH;
	}

	public static int getScReqCookie() {
		return SC_REQ_COOKIE;
	}

	public static int getScReqCookie2() {
		return SC_REQ_COOKIE2;
	}

	public static int getScReqHost() {
		return SC_REQ_HOST;
	}

	public static int getScReqPragma() {
		return SC_REQ_PRAGMA;
	}

	public static int getScReqReferer() {
		return SC_REQ_REFERER;
	}

	public static int getScReqUserAgent() {
		return SC_REQ_USER_AGENT;
	}

	public static byte[] getServerBytes() {
		return SERVER_BYTES;
	}

	public static byte getCr() {
		return CR;
	}

	public static byte getLf() {
		return LF;
	}

	public static byte getSp() {
		return SP;
	}

	public static byte getHt() {
		return HT;
	}

	public static byte getColon() {
		return COLON;
	}

	public static byte getZ() {
		return Z;
	}

	public static byte getQuestion() {
		return QUESTION;
	}

	public static byte getLcOffset() {
		return LC_OFFSET;
	}

	public static int getDefaultHttpHeaderBufferSize() {
		return DEFAULT_HTTP_HEADER_BUFFER_SIZE;
	}

	/**
	 * CRLF.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String CRLF = "\r\n";

	/**
	 * Server string.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] SERVER_BYTES = ByteChunk
			.convertToBytes("Server: Apache-Coyote/1.1" + CRLF);

	/**
	 * CR.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte CR = (byte) '\r';

	/**
	 * LF.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte LF = (byte) '\n';

	/**
	 * SP.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte SP = (byte) ' ';

	/**
	 * HT.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte HT = (byte) '\t';

	/**
	 * COLON.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte COLON = (byte) ':';

	/**
	 * 'A'.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte A = (byte) 'A';

	/**
	 * 'a'.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte a = (byte) 'a';

	/**
	 * 'Z'.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte Z = (byte) 'Z';

	/**
	 * '?'.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte QUESTION = (byte) '?';

	/**
	 * Lower case offset.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte LC_OFFSET = A - a;

	/**
	 * Default HTTP header buffer size.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int DEFAULT_HTTP_HEADER_BUFFER_SIZE = 48 * 1024;

	/* Various constant "strings" */
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] CRLF_BYTES = ByteChunk.convertToBytes(CRLF);
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] COLON_BYTES = ByteChunk.convertToBytes(": ");
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String CONNECTION = "Connection";
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String CLOSE = "close";
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] CLOSE_BYTES = ByteChunk.convertToBytes(CLOSE);
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String KEEPALIVE = "keep-alive";
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] KEEPALIVE_BYTES = ByteChunk
			.convertToBytes(KEEPALIVE);
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String CHUNKED = "chunked";
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] ACK_BYTES = ByteChunk
			.convertToBytes("HTTP/1.1 100 Continue" + CRLF + CRLF);
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String TRANSFERENCODING = "Transfer-Encoding";
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] _200_BYTES = ByteChunk.convertToBytes("200");
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] _400_BYTES = ByteChunk.convertToBytes("400");
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] _404_BYTES = ByteChunk.convertToBytes("404");

	/**
	 * Identity filters (input and output).
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int IDENTITY_FILTER = 0;

	/**
	 * Chunked filters (input and output).
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int CHUNKED_FILTER = 1;

	/**
	 * Void filters (input and output).
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int VOID_FILTER = 2;

	/**
	 * GZIP filter (output).
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int GZIP_FILTER = 3;

	/**
	 * Buffered filter (input)
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final int BUFFERED_FILTER = 3;

	/**
	 * HTTP/1.0.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String HTTP_10 = "HTTP/1.0";

	/**
	 * HTTP/1.1.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String HTTP_11 = "HTTP/1.1";
	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final byte[] HTTP_11_BYTES = ByteChunk
			.convertToBytes(HTTP_11);

	/**
	 * GET.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String GET = "GET";

	/**
	 * HEAD.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String HEAD = "HEAD";

	/**
	 * POST.
	 * 
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	private static final String POST = "POST";

	public static int getScRespAjp13Max() {
		return SC_RESP_AJP13_MAX;
	}

	public static int getMaxPacketSize() {
		return MAX_PACKET_SIZE;
	}

	public static int getReadHeadLen() {
		return READ_HEAD_LEN;
	}

	public static int getSendHeadLen() {
		return SEND_HEAD_LEN;
	}

	public static String[] getMethodtransarray() {
		return methodTransArray;
	}

	public static String[] getHeadertransarray() {
		return headerTransArray;
	}

	public static String[] getResponsetransarray() {
		return responseTransArray;
	}

	public static Hashtable<String, Integer> getResponsetranshash() {
		return responseTransHash;
	}

	public static String getCrlf() {
		return CRLF;
	}

	public static byte getA() {
		return A;
	}

	public static byte geta() {
		return a;
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

}
