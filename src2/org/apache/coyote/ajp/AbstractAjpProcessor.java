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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletResponse;

import org.apache.coyote.AbstractProcessor;
import org.apache.coyote.ActionCode;
import org.apache.coyote.AsyncContextCallback;
import org.apache.coyote.ErrorState;
import org.apache.coyote.RequestInfo;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.res.StringManager3;
import org.apache.coyote.Constants24;

import org.apache.coyote.http11.upgrade.UpgradeInbound;

/**
 * Base class for AJP Processor implementations.
 */
public abstract class AbstractAjpProcessor<S> extends AbstractProcessor<S> {

	/**
	 * The string manager for this package.
	 */
	private static final StringManager3 sm = StringManager3
			.getManager(Constants25.getPackage());

	/**
	 * End message array.
	 */
	private static final byte[] endMessageArray;
	private static final byte[] endAndCloseMessageArray;

	/**
	 * Flush message array.
	 */
	private static final byte[] flushMessageArray;

	/**
	 * Pong message array.
	 */
	private static final byte[] pongMessageArray;

	static {
		// Allocate the end message array
		AjpMessage endMessage = new AjpMessage(16);
		endMessage.reset();
		endMessage.appendByte(Constants25.getJkAjp13EndResponse());
		endMessage.appendByte(1);
		endMessage.end();
		endMessageArray = new byte[endMessage.getLen()];
		System.arraycopy(endMessage.getBuffer(), 0, endMessageArray, 0,
				endMessage.getLen());

		// Allocate the end and close message array
		AjpMessage endAndCloseMessage = new AjpMessage(16);
		endAndCloseMessage.reset();
		endAndCloseMessage.appendByte(Constants25.getJkAjp13EndResponse());
		endAndCloseMessage.appendByte(0);
		endAndCloseMessage.end();
		endAndCloseMessageArray = new byte[endAndCloseMessage.getLen()];
		System.arraycopy(endAndCloseMessage.getBuffer(), 0,
				endAndCloseMessageArray, 0, endAndCloseMessage.getLen());

		// Allocate the flush message array
		AjpMessage flushMessage = new AjpMessage(16);
		flushMessage.reset();
		flushMessage.appendByte(Constants25.getJkAjp13SendBodyChunk());
		flushMessage.appendInt(0);
		flushMessage.appendByte(0);
		flushMessage.end();
		flushMessageArray = new byte[flushMessage.getLen()];
		System.arraycopy(flushMessage.getBuffer(), 0, flushMessageArray, 0,
				flushMessage.getLen());

		// Allocate the pong message array
		AjpMessage pongMessage = new AjpMessage(16);
		pongMessage.reset();
		pongMessage.appendByte(Constants25.getJkAjp13CpongReply());
		pongMessage.end();
		pongMessageArray = new byte[pongMessage.getLen()];
		System.arraycopy(pongMessage.getBuffer(), 0, pongMessageArray, 0,
				pongMessage.getLen());
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * GetBody message array. Not static like the other message arrays since the
	 * message varies with packetSize and that can vary per connector.
	 */
	private final byte[] getBodyMessageArray;

	/**
	 * AJP packet size.
	 */
	private int packetSize;

	/**
	 * Header message. Note that this header is merely the one used during the
	 * processing of the first message of a "request", so it might not be a
	 * request header. It will stay unchanged during the processing of the whole
	 * getRequest().
	 */
	private AjpMessage requestHeaderMessage = null;

	/**
	 * Message used for response composition.
	 */
	private AjpMessage responseMessage = null;

	/**
	 * Body message.
	 */
	private AjpMessage bodyMessage = null;

	/**
	 * Body message.
	 */
	private MessageBytes bodyBytes = MessageBytes.newInstance();

	/**
	 * Host name (used to avoid useless B2C conversion on the host name).
	 */
	private char[] hostNameC = new char[0];

	/**
	 * Temp message bytes used for processing.
	 */
	private MessageBytes tmpMB = MessageBytes.newInstance();

	/**
	 * Byte chunk for certs.
	 */
	private MessageBytes certificates = MessageBytes.newInstance();

	/**
	 * End of stream flag.
	 */
	private boolean endOfStream = false;

	/**
	 * Body empty flag.
	 */
	private boolean empty = true;

	/**
	 * First read.
	 */
	private boolean first = true;

	/**
	 * Replay read.
	 */
	private boolean replay = false;

	/**
	 * Should any response body be swallowed and not sent to the client.
	 */
	private boolean swallowResponse = false;

	/**
	 * Finished response.
	 */
	private boolean finished = false;

	/**
	 * Bytes written to client for the current getRequest().
	 */
	private long bytesWritten = 0;

	// ------------------------------------------------------------ Constructor

	public AbstractAjpProcessor(int packetSize, AbstractEndpoint<S> endpoint) {

		super(endpoint);

		this.packetSize = packetSize;

		getRequest().setInputBuffer(new AbstractAjpProcessorSocketInputBuffer<S>(this));

		requestHeaderMessage = new AjpMessage(packetSize);
		responseMessage = new AjpMessage(packetSize);
		bodyMessage = new AjpMessage(packetSize);

		// Set the getBody message buffer
		AjpMessage getBodyMessage = new AjpMessage(16);
		getBodyMessage.reset();
		getBodyMessage.appendByte(Constants25.getJkAjp13GetBodyChunk());
		// Adjust read size if packetSize != default (Constants.MAX_PACKET_SIZE)
		getBodyMessage.appendInt(Constants25.getMaxReadSize() + packetSize
				- Constants25.getMaxPacketSize());
		getBodyMessage.end();
		getBodyMessageArray = new byte[getBodyMessage.getLen()];
		System.arraycopy(getBodyMessage.getBuffer(), 0, getBodyMessageArray, 0,
				getBodyMessage.getLen());
	}

	// ------------------------------------------------------------- Properties

	/**
	 * The number of milliseconds Tomcat will wait for a subsequent request
	 * before closing the connection. The default is the same as for Apache HTTP
	 * Server (15 000 milliseconds).
	 */
	private int keepAliveTimeout = -1;

	public int getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	public void setKeepAliveTimeout(int timeout) {
		keepAliveTimeout = timeout;
	}

	/**
	 * Use Tomcat authentication ?
	 */
	private boolean tomcatAuthentication = true;

	public boolean getTomcatAuthentication() {
		return tomcatAuthentication;
	}

	public void setTomcatAuthentication(boolean tomcatAuthentication) {
		this.tomcatAuthentication = tomcatAuthentication;
	}

	/**
	 * Required secret.
	 */
	private String requiredSecret = null;

	public void setRequiredSecret(String requiredSecret) {
		this.requiredSecret = requiredSecret;
	}

	/**
	 * When client certificate information is presented in a form other than
	 * instances of {@link java.security.cert.X509Certificate} it needs to be
	 * converted before it can be used and this property controls which JSSE
	 * provider is used to perform the conversion. For example it is used with
	 * the AJP connectors, the HTTP APR connector and with the
	 * {@link org.apache.catalina.valves.SSLValve}. If not specified, the
	 * default provider will be used.
	 */
	private String clientCertProvider = null;

	public String getClientCertProvider() {
		return clientCertProvider;
	}

	public void setClientCertProvider(String s) {
		this.clientCertProvider = s;
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Send an action to the connector.
	 *
	 * @param actionCode
	 *            Type of the action
	 * @param param
	 *            Action parameter
	 */
	@Override
	public final void action(ActionCode actionCode, Object param) {

		switch (actionCode) {
		case COMMIT: {
			if (getResponse().isCommitted())
				return;

			// Validate and write response headers
			try {
				prepareResponse();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}

			try {
				flush(false);
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}
			break;
		}
		case CLIENT_FLUSH: {
			if (!getResponse().isCommitted()) {
				// Validate and write response headers
				try {
					prepareResponse();
				} catch (IOException e) {
					setErrorState(ErrorState.CLOSE_NOW, e);
					return;
				}
			}

			try {
				flush(true);
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}
			break;
		}
		case IS_ERROR: {
			((AtomicBoolean) param).set(getErrorState().isError());
			break;
		}
		case DISABLE_SWALLOW_INPUT: {
			// TODO: Do not swallow request input but
			// make sure we are closing the connection
			setErrorState(ErrorState.CLOSE_CLEAN, null);
			break;
		}
		case CLOSE: {
			// Close
			// End the processing of the current request, and stop any further
			// transactions with the client

			try {
				finish();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}
			break;
		}
		case REQ_SSL_ATTRIBUTE: {
			if (!certificates.isNull()) {
				ByteChunk certData = certificates.getByteChunk();
				X509Certificate jsseCerts[] = null;
				ByteArrayInputStream bais = new ByteArrayInputStream(
						certData.getBytes(), certData.getStart(),
						certData.getLength());
				// Fill the elements.
				try {
					CertificateFactory cf;
					if (clientCertProvider == null) {
						cf = CertificateFactory.getInstance("X.509");
					} else {
						cf = CertificateFactory.getInstance("X.509",
								clientCertProvider);
					}
					while (bais.available() > 0) {
						X509Certificate cert = (X509Certificate) cf
								.generateCertificate(bais);
						if (jsseCerts == null) {
							jsseCerts = new X509Certificate[1];
							jsseCerts[0] = cert;
						} else {
							X509Certificate[] temp = new X509Certificate[jsseCerts.length + 1];
							System.arraycopy(jsseCerts, 0, temp, 0,
									jsseCerts.length);
							temp[jsseCerts.length] = cert;
							jsseCerts = temp;
						}
					}
				} catch (java.security.cert.CertificateException e) {
					getLog().error(sm.getString("ajpprocessor.certs.fail"), e);
					return;
				} catch (NoSuchProviderException e) {
					getLog().error(sm.getString("ajpprocessor.certs.fail"), e);
					return;
				}
				getRequest()
						.setAttribute(SSLSupport.CERTIFICATE_KEY, jsseCerts);
			}
			break;
		}
		case REQ_HOST_ATTRIBUTE: {
			// Get remote host name using a DNS resolution
			if (getRequest().remoteHost().isNull()) {
				try {
					getRequest().remoteHost().setString(
							InetAddress.getByName(
									getRequest().remoteAddr().toString())
									.getHostName());
				} catch (IOException iex) {
					// Ignore
				}
			}
			break;
		}
		case REQ_LOCAL_ADDR_ATTRIBUTE: {
			// Automatically populated during prepareRequest() when using
			// modern AJP forwarder, otherwise copy from local name
			if (getRequest().localAddr().isNull()) {
				getRequest().localAddr().setString(
						getRequest().localName().toString());
			}
			break;
		}
		case REQ_SET_BODY_REPLAY: {
			// Set the given bytes as the content
			ByteChunk bc = (ByteChunk) param;
			int length = bc.getLength();
			bodyBytes.setBytes(bc.getBytes(), bc.getStart(), length);
			getRequest().setContentLength(length);
			first = false;
			empty = false;
			replay = true;
			endOfStream = false;
			break;
		}
		case ASYNC_START: {
			getAsyncStateMachine().asyncStart((AsyncContextCallback) param);
			// Async time out is based on SocketWrapper access time
			getSocketWrapper().access();
			break;
		}
		case ASYNC_DISPATCHED: {
			getAsyncStateMachine().asyncDispatched();
			break;
		}
		case ASYNC_TIMEOUT: {
			AtomicBoolean result = (AtomicBoolean) param;
			result.set(getAsyncStateMachine().asyncTimeout());
			break;
		}
		case ASYNC_RUN: {
			getAsyncStateMachine().asyncRun((Runnable) param);
			break;
		}
		case ASYNC_ERROR: {
			getAsyncStateMachine().asyncError();
			break;
		}
		case ASYNC_IS_STARTED: {
			((AtomicBoolean) param)
					.set(getAsyncStateMachine().isAsyncStarted());
			break;
		}
		case ASYNC_IS_DISPATCHING: {
			((AtomicBoolean) param).set(getAsyncStateMachine()
					.isAsyncDispatching());
			break;
		}
		case ASYNC_IS_ASYNC: {
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsync());
			break;
		}
		case ASYNC_IS_TIMINGOUT: {
			((AtomicBoolean) param).set(getAsyncStateMachine()
					.isAsyncTimingOut());
			break;
		}
		case ASYNC_IS_ERROR: {
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsyncError());
			break;
		}
		case UPGRADE_TOMCAT: {
			// HTTP connections only. Unsupported for AJP.
			// NOOP
			break;
		}
		default: {
			actionInternal(actionCode, param);
			break;
		}
		case CLOSE_NOW: {
			// Prevent further writes to the response
			swallowResponse = true;
			setErrorState(ErrorState.CLOSE_NOW, null);
			break;
		}
		}
	}

	@Override
	public SocketState asyncDispatch(SocketStatus status) {

		RequestInfo rp = getRequest().getRequestProcessor();
		try {
			rp.setStage(Constants24.getStageService());
			if (!getAdapter()
					.asyncDispatch(getRequest(), getResponse(), status)) {
				setErrorState(ErrorState.CLOSE_NOW, null);
			}
			resetTimeouts();
		} catch (InterruptedIOException e) {
			setErrorState(ErrorState.CLOSE_NOW, e);
		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			setErrorState(ErrorState.CLOSE_NOW, t);
			getLog().error(
					sm.getString("http11processor.getRequest().process"), t);
		} finally {
			if (getErrorState().isError()) {
				// 500 - Internal Server Error
				getResponse().setStatus(500);
				getAdapter().log(getRequest(), getResponse(), 0);
			}
		}

		rp.setStage(Constants24.getStageEnded());

		if (isAsync()) {
			if (getErrorState().isError()) {
				getRequest().updateCounters();
				return SocketState.CLOSED;
			} else {
				return SocketState.LONG;
			}
		} else {
			getRequest().updateCounters();
			if (getErrorState().isError()) {
				return SocketState.CLOSED;
			} else {
				return SocketState.OPEN;
			}
		}
	}

	@Override
	public void setSslSupport(SSLSupport sslSupport) {
		// Should never reach this code but in case we do...
		throw new IllegalStateException(
				sm.getString("ajpprocessor.ssl.notsupported"));
	}

	@Override
	public SocketState event(SocketStatus status) throws IOException {
		// Should never reach this code but in case we do...
		throw new IOException(sm.getString("ajpprocessor.comet.notsupported"));
	}

	@Override
	public SocketState upgradeDispatch() throws IOException {
		// Should never reach this code but in case we do...
		throw new IOException(
				sm.getString("ajpprocessor.httpupgrade.notsupported"));
	}

	/**
	 * @deprecated Will be removed in Tomcat 8.0.x.
	 */
	@Deprecated
	@Override
	public UpgradeInbound getUpgradeInbound() {
		// Can't throw exception as this is used to test if connection has been
		// upgraded using Tomcat's proprietary HTTP upgrade mechanism.
		return null;
	}

	@Override
	public SocketState upgradeDispatch(SocketStatus status) throws IOException {
		// Should never reach this code but in case we do...
		throw new IOException(
				sm.getString("ajpprocessor.httpupgrade.notsupported"));
	}

	@Override
	public HttpUpgradeHandler getHttpUpgradeHandler() {
		// Should never reach this code but in case we do...
		throw new IllegalStateException(
				sm.getString("ajpprocessor.httpupgrade.notsupported"));
	}

	/**
	 * Recycle the processor, ready for the next request which may be on the
	 * same connection or a different connection.
	 * 
	 * @param socketClosing
	 *            Indicates if the socket is about to be closed allowing the
	 *            processor to perform any additional clean-up that may be
	 *            required
	 */
	@Override
	public void recycle(boolean socketClosing) {
		getAdapter().checkRecycled(getRequest(), getResponse());

		getAsyncStateMachine().recycle();

		// Recycle Request object
		first = true;
		endOfStream = false;
		empty = true;
		replay = false;
		finished = false;
		getRequest().recycle();
		getResponse().recycle();
		certificates.recycle();
		swallowResponse = false;
		bytesWritten = 0;
		resetErrorState();
	}

	// ------------------------------------------------------ Protected Methods

	// Methods called by action()
	protected abstract void actionInternal(ActionCode actionCode, Object param);

	// Methods called by asyncDispatch
	/**
	 * Provides a mechanism for those connector implementations (currently only
	 * NIO) that need to reset timeouts from Async timeouts to standard HTTP
	 * timeouts once async processing completes.
	 */
	protected abstract void resetTimeouts();

	// Methods called by prepareResponse()
	protected abstract void output(byte[] src, int offset, int length)
			throws IOException;

	// Methods used by SocketInputBuffer
	protected abstract boolean receive() throws IOException;

	@Override
	public final boolean isComet() {
		// AJP does not support Comet
		return false;
	}

	@Override
	public final boolean isUpgrade() {
		// AJP does not support HTTP upgrade
		return false;
	}

	/**
	 * Get more request body data from the web server and store it in the
	 * internal buffer.
	 *
	 * @return true if there is more data, false if not.
	 */
	protected boolean refillReadBuffer() throws IOException {
		// If the server returns an empty packet, assume that that end of
		// the stream has been reached (yuck -- fix protocol??).
		// FORM support
		if (replay) {
			endOfStream = true; // we've read everything there is
		}
		if (endOfStream) {
			return false;
		}

		// Request more data immediately
		output(getBodyMessageArray, 0, getBodyMessageArray.length);

		boolean moreData = receive();
		if (!moreData) {
			endOfStream = true;
		}
		return moreData;
	}

	/**
	 * After reading the request headers, we have to setup the request filters.
	 */
	protected void prepareRequest() {

		// Translate the HTTP method code to a String.
		byte methodCode = requestHeaderMessage.getByte();
		if (methodCode != Constants25.getScMJkStored()) {
			String methodName = Constants25.getMethodForCode(methodCode - 1);
			getRequest().method().setString(methodName);
		}

		requestHeaderMessage.getBytes(getRequest().protocol());
		requestHeaderMessage.getBytes(getRequest().requestURI());

		requestHeaderMessage.getBytes(getRequest().remoteAddr());
		requestHeaderMessage.getBytes(getRequest().remoteHost());
		requestHeaderMessage.getBytes(getRequest().localName());
		getRequest().setLocalPort(requestHeaderMessage.getInt());

		boolean isSSL = requestHeaderMessage.getByte() != 0;
		if (isSSL) {
			getRequest().scheme().setString("https");
		}

		// Decode headers
		MimeHeaders headers = getRequest().getMimeHeaders();

		// Set this every time in case limit has been changed via JMX
		headers.setLimit(getEndpoint().getMaxHeaderCount());

		boolean contentLengthSet = false;
		int hCount = requestHeaderMessage.getInt();
		for (int i = 0; i < hCount; i++) {
			String hName = null;

			// Header names are encoded as either an integer code starting
			// with 0xA0, or as a normal string (in which case the first
			// two bytes are the length).
			int isc = requestHeaderMessage.peekInt();
			int hId = isc & 0xFF;

			MessageBytes vMB = null;
			isc &= 0xFF00;
			if (0xA000 == isc) {
				requestHeaderMessage.getInt(); // To advance the read position
				hName = Constants25.getHeaderForCode(hId - 1);
				vMB = headers.addValue(hName);
			} else {
				// reset hId -- if the header currently being read
				// happens to be 7 or 8 bytes long, the code below
				// will think it's the content-type header or the
				// content-length header - SC_REQ_CONTENT_TYPE=7,
				// SC_REQ_CONTENT_LENGTH=8 - leading to unexpected
				// behaviour. see bug 5861 for more information.
				hId = -1;
				requestHeaderMessage.getBytes(tmpMB);
				ByteChunk bc = tmpMB.getByteChunk();
				vMB = headers.addValue(bc.getBuffer(), bc.getStart(),
						bc.getLength());
			}

			requestHeaderMessage.getBytes(vMB);

			if (hId == Constants25.getScReqContentLength()
					|| (hId == -1 && tmpMB.equalsIgnoreCase("Content-Length"))) {
				long cl = vMB.getLong();
				if (contentLengthSet) {
					getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
					setErrorState(ErrorState.CLOSE_CLEAN, null);
				} else {
					contentLengthSet = true;
					// Set the content-length header for the request
					getRequest().setContentLength(cl);
				}
			} else if (hId == Constants25.getScReqContentType()
					|| (hId == -1 && tmpMB.equalsIgnoreCase("Content-Type"))) {
				// just read the content-type header, so set it
				ByteChunk bchunk = vMB.getByteChunk();
				getRequest().contentType().setBytes(bchunk.getBytes(),
						bchunk.getOffset(), bchunk.getLength());
			}
		}

		// Decode extra attributes
		boolean secret = false;
		byte attributeCode;
		while ((attributeCode = requestHeaderMessage.getByte()) != Constants25.getScAAreDone()) {

			switch (attributeCode) {

			case 10:
				requestHeaderMessage.getBytes(tmpMB);
				String n = tmpMB.toString();
				requestHeaderMessage.getBytes(tmpMB);
				String v = tmpMB.toString();
				/*
				 * AJP13 misses to forward the local IP address and the remote
				 * port. Allow the AJP connector to add this info via private
				 * request attributes. We will accept the forwarded data and
				 * remove it from the public list of request attributes.
				 */
				if (n.equals(Constants25.getScAReqLocalAddr())) {
					getRequest().localAddr().setString(v);
				} else if (n.equals(Constants25.getScAReqRemotePort())) {
					try {
						getRequest().setRemotePort(Integer.parseInt(v));
					} catch (NumberFormatException nfe) {
						// Ignore invalid value
					}
				} else {
					getRequest().setAttribute(n, v);
				}
				break;

			case 1:
				requestHeaderMessage.getBytes(tmpMB);
				// nothing
				break;

			case 2:
				requestHeaderMessage.getBytes(tmpMB);
				// nothing
				break;

			case 3:
				if (tomcatAuthentication) {
					// ignore server
					requestHeaderMessage.getBytes(tmpMB);
				} else {
					requestHeaderMessage.getBytes(getRequest().getRemoteUser());
				}
				break;

			case 4:
				if (tomcatAuthentication) {
					// ignore server
					requestHeaderMessage.getBytes(tmpMB);
				} else {
					requestHeaderMessage.getBytes(getRequest().getAuthType());
				}
				break;

			case 5:
				requestHeaderMessage.getBytes(getRequest().queryString());
				break;

			case 6:
				requestHeaderMessage.getBytes(getRequest().instanceId());
				break;

			case 7:
				getRequest().scheme().setString("https");
				// SSL certificate extraction is lazy, moved to JkCoyoteHandler
				requestHeaderMessage.getBytes(certificates);
				break;

			case 8:
				getRequest().scheme().setString("https");
				requestHeaderMessage.getBytes(tmpMB);
				getRequest().setAttribute(SSLSupport.CIPHER_SUITE_KEY,
						tmpMB.toString());
				break;

			case 9:
				getRequest().scheme().setString("https");
				requestHeaderMessage.getBytes(tmpMB);
				getRequest().setAttribute(SSLSupport.SESSION_ID_KEY,
						tmpMB.toString());
				break;

			case 11:
				getRequest().setAttribute(SSLSupport.KEY_SIZE_KEY,
						Integer.valueOf(requestHeaderMessage.getInt()));
				break;

			case 13:
				requestHeaderMessage.getBytes(getRequest().method());
				break;

			case 12:
				requestHeaderMessage.getBytes(tmpMB);
				if (requiredSecret != null) {
					secret = true;
					if (!tmpMB.equals(requiredSecret)) {
						getResponse().setStatus(403);
						setErrorState(ErrorState.CLOSE_CLEAN, null);
					}
				}
				break;

			default:
				// Ignore unknown attribute for backward compatibility
				break;

			}

		}

		// Check if secret was submitted if required
		if ((requiredSecret != null) && !secret) {
			getResponse().setStatus(403);
			setErrorState(ErrorState.CLOSE_CLEAN, null);
		}

		// Check for a full URI (including protocol://host:port/)
		ByteChunk uriBC = getRequest().requestURI().getByteChunk();
		if (uriBC.startsWithIgnoreCase("http", 0)) {

			int pos = uriBC.indexOf("://", 0, 3, 4);
			int uriBCStart = uriBC.getStart();
			int slashPos = -1;
			if (pos != -1) {
				byte[] uriB = uriBC.getBytes();
				slashPos = uriBC.indexOf('/', pos + 3);
				if (slashPos == -1) {
					slashPos = uriBC.getLength();
					// Set URI as "/"
					getRequest().requestURI().setBytes(uriB,
							uriBCStart + pos + 1, 1);
				} else {
					getRequest().requestURI()
							.setBytes(uriB, uriBCStart + slashPos,
									uriBC.getLength() - slashPos);
				}
				MessageBytes hostMB = headers.setValue("host");
				hostMB.setBytes(uriB, uriBCStart + pos + 3, slashPos - pos - 3);
			}

		}

		MessageBytes valueMB = getRequest().getMimeHeaders().getValue("host");
		parseHost(valueMB);

		if (getErrorState().isError()) {
			getAdapter().log(getRequest(), getResponse(), 0);
		}
	}

	/**
	 * Parse host.
	 */
	protected void parseHost(MessageBytes valueMB) {

		if (valueMB == null || valueMB.isNull()) {
			// HTTP/1.0
			getRequest().setServerPort(getRequest().getLocalPort());
			try {
				getRequest().serverName().duplicate(getRequest().localName());
			} catch (IOException e) {
				getResponse().setStatus(400);
				setErrorState(ErrorState.CLOSE_CLEAN, e);
			}
			return;
		}

		ByteChunk valueBC = valueMB.getByteChunk();
		byte[] valueB = valueBC.getBytes();
		int valueL = valueBC.getLength();
		int valueS = valueBC.getStart();
		int colonPos = -1;
		if (hostNameC.length < valueL) {
			hostNameC = new char[valueL];
		}

		boolean ipv6 = (valueB[valueS] == '[');
		boolean bracketClosed = false;
		for (int i = 0; i < valueL; i++) {
			char b = (char) valueB[i + valueS];
			hostNameC[i] = b;
			if (b == ']') {
				bracketClosed = true;
			} else if (b == ':') {
				if (!ipv6 || bracketClosed) {
					colonPos = i;
					break;
				}
			}
		}

		if (colonPos < 0) {
			if (getRequest().scheme().equalsIgnoreCase("https")) {
				// 443 - Default HTTPS port
				getRequest().setServerPort(443);
			} else {
				// 80 - Default HTTTP port
				getRequest().setServerPort(80);
			}
			getRequest().serverName().setChars(hostNameC, 0, valueL);
		} else {

			getRequest().serverName().setChars(hostNameC, 0, colonPos);

			int port = 0;
			int mult = 1;
			for (int i = valueL - 1; i > colonPos; i--) {
				int charValue = HexUtils.getDec(valueB[i + valueS]);
				if (charValue == -1) {
					// Invalid character
					// 400 - Bad request
					getResponse().setStatus(400);
					setErrorState(ErrorState.CLOSE_CLEAN, null);
					break;
				}
				port = port + (charValue * mult);
				mult = 10 * mult;
			}
			getRequest().setServerPort(port);
		}
	}

	/**
	 * When committing the response, we have to validate the set of headers, as
	 * well as setup the response filters.
	 */
	protected void prepareResponse() throws IOException {

		getResponse().setCommitted(true);

		responseMessage.reset();
		responseMessage.appendByte(Constants25.getJkAjp13SendHeaders());

		// Responses with certain status codes are not permitted to include a
		// response body.
		int statusCode = getResponse().getStatus();
		if (statusCode < 200 || statusCode == 204 || statusCode == 205
				|| statusCode == 304) {
			// No entity body
			swallowResponse = true;
		}

		// Responses to HEAD requests are not permitted to incude a response
		// body.
		MessageBytes methodMB = getRequest().method();
		if (methodMB.equals("HEAD")) {
			// No entity body
			swallowResponse = true;
		}

		// HTTP header contents
		responseMessage.appendInt(statusCode);
		String message = null;
		if (Constants24.isUseCustomStatusMsgInHeader()
				&& HttpMessages.isSafeInHttpHeader(getResponse().getMessage())) {
			message = getResponse().getMessage();
		}
		if (message == null) {
			message = HttpMessages.getInstance(getResponse().getLocale())
					.getMessage(getResponse().getStatus());
		}
		if (message == null) {
			// mod_jk + httpd 2.x fails with a null status message - bug 45026
			message = Integer.toString(getResponse().getStatus());
		}
		tmpMB.setString(message);
		responseMessage.appendBytes(tmpMB);

		// Special headers
		MimeHeaders headers = getResponse().getMimeHeaders();
		String contentType = getResponse().getContentType();
		if (contentType != null) {
			headers.setValue("Content-Type").setString(contentType);
		}
		String contentLanguage = getResponse().getContentLanguage();
		if (contentLanguage != null) {
			headers.setValue("Content-Language").setString(contentLanguage);
		}
		long contentLength = getResponse().getContentLengthLong();
		if (contentLength >= 0) {
			headers.setValue("Content-Length").setLong(contentLength);
		}

		// Other headers
		int numHeaders = headers.size();
		responseMessage.appendInt(numHeaders);
		for (int i = 0; i < numHeaders; i++) {
			MessageBytes hN = headers.getName(i);
			int hC = Constants25.getResponseAjpIndex(hN.toString());
			if (hC > 0) {
				responseMessage.appendInt(hC);
			} else {
				responseMessage.appendBytes(hN);
			}
			MessageBytes hV = headers.getValue(i);
			responseMessage.appendBytes(hV);
		}

		// Write to buffer
		responseMessage.end();
		output(responseMessage.getBuffer(), 0, responseMessage.getLen());
	}

	/**
	 * Callback to write data from the buffer.
	 */
	protected void flush(boolean explicit) throws IOException {
		if (explicit && !finished) {
			// Send the flush message
			output(flushMessageArray, 0, flushMessageArray.length);
		}
	}

	/**
	 * Finish AJP response.
	 */
	protected void finish() throws IOException {

		if (!getResponse().isCommitted()) {
			// Validate and write response headers
			try {
				prepareResponse();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
				return;
			}
		}

		if (finished)
			return;

		finished = true;

		// Swallow the unread body packet if present
		if (first && getRequest().getContentLengthLong() > 0) {
			receive();
		}

		// Add the end message
		if (getErrorState().isError()) {
			output(endAndCloseMessageArray, 0, endAndCloseMessageArray.length);
		} else {
			output(endMessageArray, 0, endMessageArray.length);
		}
	}

	public int getPacketSize() {
		return packetSize;
	}

	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}

	public AjpMessage getRequestHeaderMessage() {
		return requestHeaderMessage;
	}

	public void setRequestHeaderMessage(AjpMessage requestHeaderMessage) {
		this.requestHeaderMessage = requestHeaderMessage;
	}

	public AjpMessage getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(AjpMessage responseMessage) {
		this.responseMessage = responseMessage;
	}

	public AjpMessage getBodyMessage() {
		return bodyMessage;
	}

	public void setBodyMessage(AjpMessage bodyMessage) {
		this.bodyMessage = bodyMessage;
	}

	public MessageBytes getBodyBytes() {
		return bodyBytes;
	}

	public void setBodyBytes(MessageBytes bodyBytes) {
		this.bodyBytes = bodyBytes;
	}

	public char[] getHostNameC() {
		return hostNameC;
	}

	public void setHostNameC(char[] hostNameC) {
		this.hostNameC = hostNameC;
	}

	public MessageBytes getTmpMB() {
		return tmpMB;
	}

	public void setTmpMB(MessageBytes tmpMB) {
		this.tmpMB = tmpMB;
	}

	public MessageBytes getCertificates() {
		return certificates;
	}

	public void setCertificates(MessageBytes certificates) {
		this.certificates = certificates;
	}

	public boolean isEndOfStream() {
		return endOfStream;
	}

	public void setEndOfStream(boolean endOfStream) {
		this.endOfStream = endOfStream;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public boolean isReplay() {
		return replay;
	}

	public void setReplay(boolean replay) {
		this.replay = replay;
	}

	public boolean isSwallowResponse() {
		return swallowResponse;
	}

	public void setSwallowResponse(boolean swallowResponse) {
		this.swallowResponse = swallowResponse;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public long getBytesWritten() {
		return bytesWritten;
	}

	public void setBytesWritten(long bytesWritten) {
		this.bytesWritten = bytesWritten;
	}

	public static StringManager3 getSm() {
		return sm;
	}

	public static byte[] getEndmessagearray() {
		return endMessageArray;
	}

	public static byte[] getEndandclosemessagearray() {
		return endAndCloseMessageArray;
	}

	public static byte[] getFlushmessagearray() {
		return flushMessageArray;
	}

	public static byte[] getPongmessagearray() {
		return pongMessageArray;
	}

	public byte[] getGetBodyMessageArray() {
		return getBodyMessageArray;
	}

	public String getRequiredSecret() {
		return requiredSecret;
	}

}