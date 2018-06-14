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
import java.io.InterruptedIOException;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.coyote.AbstractProcessor;
import org.apache.coyote.ActionCode;
import org.apache.coyote.AsyncContextCallback;
import org.apache.coyote.ErrorState;
import org.apache.coyote.RequestInfo;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import org.apache.coyote.http11.filters.ChunkedInputFilter;
import org.apache.coyote.http11.filters.ChunkedOutputFilter;
import org.apache.coyote.http11.filters.GzipOutputFilter;
import org.apache.coyote.http11.filters.IdentityInputFilter;
import org.apache.coyote.http11.filters.IdentityOutputFilter;
import org.apache.coyote.http11.filters.SavedRequestInputFilter;
import org.apache.coyote.http11.filters.VoidInputFilter;
import org.apache.coyote.http11.filters.VoidOutputFilter;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.buf.Ascii;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.log.Mode;
import org.apache.tomcat.util.log.UserDataHelper;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.tomcat.util.res.StringManager3;

import org.apache.coyote.http11.upgrade.UpgradeInbound;
import org.apache.coyote.Constants24;

public abstract class AbstractHttp11Processor<S> extends AbstractProcessor<S> {

	private final UserDataHelper userDataHelper;

	/**
	 * The string manager for this package.
	 */
	private static final StringManager3 sm = StringManager3
			.getManager(Constants26.getPackage());

	/*
	 * Tracks how many internal filters are in the filter library so they are
	 * skipped when looking for pluggable filters.
	 */
	private int pluggableFilterIndex = Integer.MAX_VALUE;

	/**
	 * Keep-alive.
	 */
	private boolean keepAlive = true;

	/**
	 * Flag used to indicate that the socket should be kept open (e.g. for keep
	 * alive or send file.
	 */
	private boolean openSocket = false;

	/**
	 * Flag used to indicate that the socket should treat the next request
	 * processed like a keep-alive connection - i.e. one where there may not be
	 * any data to process. The initial value of this flag on entering the
	 * process method is different for connectors that use polling (NIO / APR -
	 * data is always expected) compared to those that use blocking (BIO - data
	 * is only expected if the connection isn't in the keep-alive state).
	 */
	private boolean keptAlive;

	/**
	 * Flag that indicates that send file processing is in progress and that the
	 * socket should not be returned to the poller (where a poller is used).
	 */
	private boolean sendfileInProgress = false;

	/**
	 * Flag that indicates if the request headers have been completely read.
	 */
	private boolean readComplete = true;

	/**
	 * HTTP/1.1 flag.
	 */
	private boolean http11 = true;

	/**
	 * HTTP/0.9 flag.
	 */
	private boolean http09 = false;

	/**
	 * Content delimiter for the request (if false, the connection will be
	 * closed at the end of the request).
	 */
	private boolean contentDelimitation = true;

	/**
	 * Is there an expectation ?
	 */
	private boolean expectation = false;

	/**
	 * Comet used.
	 */
	private boolean comet = false;

	/**
	 * Regular expression that defines the restricted user agents.
	 */
	private Pattern restrictedUserAgents = null;

	/**
	 * Maximum number of Keep-Alive requests to honor.
	 */
	private int maxKeepAliveRequests = -1;

	/**
	 * The number of seconds Tomcat will wait for a subsequent request before
	 * closing the connection.
	 */
	private int keepAliveTimeout = -1;

	/**
	 * Remote Address associated with the current connection.
	 */
	private String remoteAddr = null;

	/**
	 * Remote Host associated with the current connection.
	 */
	private String remoteHost = null;

	/**
	 * Local Host associated with the current connection.
	 */
	private String localName = null;

	/**
	 * Local port to which the socket is connected
	 */
	private int localPort = -1;

	/**
	 * Remote port to which the socket is connected
	 */
	private int remotePort = -1;

	/**
	 * The local Host address.
	 */
	private String localAddr = null;

	/**
	 * Maximum timeout on uploads. 5 minutes as in Apache HTTPD server.
	 */
	private int connectionUploadTimeout = 300000;

	/**
	 * Flag to disable setting a different time-out on uploads.
	 */
	private boolean disableUploadTimeout = false;

	/**
	 * Allowed compression level.
	 */
	private int compressionLevel = 0;

	/**
	 * Minimum content size to make compression.
	 */
	private int compressionMinSize = 2048;

	/**
	 * Socket buffering.
	 */
	private int socketBuffer = -1;

	/**
	 * Max saved post size.
	 */
	private int maxSavePostSize = 4 * 1024;

	/**
	 * Regular expression that defines the user agents to not use gzip with
	 */
	private Pattern noCompressionUserAgents = null;

	/**
	 * List of MIMES which could be gzipped
	 */
	private String[] compressableMimeTypes = { "text/html", "text/xml",
			"text/plain" };

	/**
	 * Host name (used to avoid useless B2C conversion on the host name).
	 */
	private char[] hostNameC = new char[0];

	/**
	 * Allow a customized the server header for the tin-foil hat folks.
	 */
	private String server = null;

	/**
	 * Listener to which data available events are passed once the associated
	 * connection has completed the proprietary Tomcat HTTP upgrade process.
	 * 
	 * @deprecated Will be removed in Tomcat 8.0.x.
	 */
	@Deprecated
	private UpgradeInbound upgradeInbound = null;

	/**
	 * Instance of the new protocol to use after the HTTP connection has been
	 * upgraded using the Servlet 3.1 based upgrade process.
	 */
	private HttpUpgradeHandler httpUpgradeHandler = null;

	public AbstractHttp11Processor(AbstractEndpoint<S> endpoint) {
		super(endpoint);
		userDataHelper = new UserDataHelper(getLog());
	}

	/**
	 * Set compression level.
	 */
	public void setCompression(String compression) {
		if (compression.equals("on")) {
			this.compressionLevel = 1;
		} else if (compression.equals("force")) {
			this.compressionLevel = 2;
		} else if (compression.equals("off")) {
			this.compressionLevel = 0;
		} else {
			try {
				// Try to parse compression as an int, which would give the
				// minimum compression size
				compressionMinSize = Integer.parseInt(compression);
				this.compressionLevel = 1;
			} catch (Exception e) {
				this.compressionLevel = 0;
			}
		}
	}

	/**
	 * Set Minimum size to trigger compression.
	 */
	public void setCompressionMinSize(int compressionMinSize) {
		this.compressionMinSize = compressionMinSize;
	}

	/**
	 * Set no compression user agent pattern. Regular expression as supported by
	 * {@link Pattern}.
	 *
	 * ie: "gorilla|desesplorer|tigrus"
	 */
	public void setNoCompressionUserAgents(String noCompressionUserAgents) {
		if (noCompressionUserAgents == null
				|| noCompressionUserAgents.length() == 0) {
			this.noCompressionUserAgents = null;
		} else {
			this.noCompressionUserAgents = Pattern
					.compile(noCompressionUserAgents);
		}
	}

	/**
	 * Add a mime-type which will be compressible The mime-type String will be
	 * exactly matched in the response mime-type header .
	 *
	 * @param mimeType
	 *            mime-type string
	 */
	public void addCompressableMimeType(String mimeType) {
		compressableMimeTypes = addStringArray(compressableMimeTypes, mimeType);
	}

	/**
	 * Set compressible mime-type list (this method is best when used with a
	 * large number of connectors, where it would be better to have all of them
	 * referenced a single array).
	 */
	public void setCompressableMimeTypes(String[] compressableMimeTypes) {
		this.compressableMimeTypes = compressableMimeTypes;
	}

	/**
	 * Set compressable mime-type list List contains users agents separated by
	 * ',' :
	 *
	 * ie: "text/html,text/xml,text/plain"
	 */
	public void setCompressableMimeTypes(String compressableMimeTypes) {
		if (compressableMimeTypes != null) {
			this.compressableMimeTypes = null;
			StringTokenizer st = new StringTokenizer(compressableMimeTypes, ",");

			while (st.hasMoreTokens()) {
				addCompressableMimeType(st.nextToken().trim());
			}
		}
	}

	/**
	 * Return compression level.
	 */
	public String getCompression() {
		switch (compressionLevel) {
		case 0:
			return "off";
		case 1:
			return "on";
		case 2:
			return "force";
		}
		return "off";
	}

	/**
	 * General use method
	 *
	 * @param sArray
	 *            the StringArray
	 * @param value
	 *            string
	 */
	private String[] addStringArray(String sArray[], String value) {
		String[] result = null;
		if (sArray == null) {
			result = new String[1];
			result[0] = value;
		} else {
			result = new String[sArray.length + 1];
			for (int i = 0; i < sArray.length; i++) {
				result[i] = sArray[i];
			}
			result[sArray.length] = value;
		}
		return result;
	}

	/**
	 * Checks if any entry in the string array starts with the specified value
	 *
	 * @param sArray
	 *            the StringArray
	 * @param value
	 *            string
	 */
	private boolean startsWithStringArray(String sArray[], String value) {
		if (value == null) {
			return false;
		}
		for (int i = 0; i < sArray.length; i++) {
			if (value.startsWith(sArray[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set restricted user agent list (which will downgrade the connector to
	 * HTTP/1.0 mode). Regular expression as supported by {@link Pattern}.
	 *
	 * ie: "gorilla|desesplorer|tigrus"
	 */
	public void setRestrictedUserAgents(String restrictedUserAgents) {
		if (restrictedUserAgents == null || restrictedUserAgents.length() == 0) {
			this.restrictedUserAgents = null;
		} else {
			this.restrictedUserAgents = Pattern.compile(restrictedUserAgents);
		}
	}

	/**
	 * Set the maximum number of Keep-Alive requests to honor. This is to
	 * safeguard from DoS attacks. Setting to a negative value disables the
	 * check.
	 */
	public void setMaxKeepAliveRequests(int mkar) {
		maxKeepAliveRequests = mkar;
	}

	/**
	 * Return the number of Keep-Alive requests that we will honor.
	 */
	public int getMaxKeepAliveRequests() {
		return maxKeepAliveRequests;
	}

	/**
	 * Set the Keep-Alive timeout.
	 */
	public void setKeepAliveTimeout(int timeout) {
		keepAliveTimeout = timeout;
	}

	/**
	 * Return the number Keep-Alive timeout.
	 */
	public int getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	/**
	 * Set the maximum size of a POST which will be buffered in SSL mode.
	 */
	public void setMaxSavePostSize(int msps) {
		maxSavePostSize = msps;
	}

	/**
	 * Return the maximum size of a POST which will be buffered in SSL mode.
	 */
	public int getMaxSavePostSize() {
		return maxSavePostSize;
	}

	/**
	 * Set the flag to control upload time-outs.
	 */
	public void setDisableUploadTimeout(boolean isDisabled) {
		disableUploadTimeout = isDisabled;
	}

	/**
	 * Get the flag that controls upload time-outs.
	 */
	public boolean getDisableUploadTimeout() {
		return disableUploadTimeout;
	}

	/**
	 * Set the socket buffer flag.
	 */
	public void setSocketBuffer(int socketBuffer) {
		this.socketBuffer = socketBuffer;
	}

	/**
	 * Get the socket buffer flag.
	 */
	public int getSocketBuffer() {
		return socketBuffer;
	}

	/**
	 * Set the upload timeout.
	 */
	public void setConnectionUploadTimeout(int timeout) {
		connectionUploadTimeout = timeout;
	}

	/**
	 * Get the upload timeout.
	 */
	public int getConnectionUploadTimeout() {
		return connectionUploadTimeout;
	}

	/**
	 * Set the server header name.
	 */
	public void setServer(String server) {
		if (server == null || server.equals("")) {
			this.server = null;
		} else {
			this.server = server;
		}
	}

	/**
	 * Get the server header name.
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Check if the resource could be compressed, if the client supports it.
	 */
	private boolean isCompressable() {

		// Check if content is not already gzipped
		MessageBytes contentEncodingMB = getResponse().getMimeHeaders().getValue(
				"Content-Encoding");

		if ((contentEncodingMB != null)
				&& (contentEncodingMB.indexOf("gzip") != -1)) {
			return false;
		}

		// If force mode, always compress (test purposes only)
		if (compressionLevel == 2) {
			return true;
		}

		// Check if sufficient length to trigger the compression
		long contentLength = getResponse().getContentLengthLong();
		if ((contentLength == -1) || (contentLength > compressionMinSize)) {
			// Check for compatible MIME-TYPE
			if (compressableMimeTypes != null) {
				return (startsWithStringArray(compressableMimeTypes,
						getResponse().getContentType()));
			}
		}

		return false;
	}

	/**
	 * Check if compression should be used for this resource. Already checked
	 * that the resource could be compressed if the client supports it.
	 */
	private boolean useCompression() {

		// Check if browser support gzip encoding
		MessageBytes acceptEncodingMB = getRequest().getMimeHeaders().getValue(
				"accept-encoding");

		if ((acceptEncodingMB == null)
				|| (acceptEncodingMB.indexOf("gzip") == -1)) {
			return false;
		}

		// If force mode, always compress (test purposes only)
		if (compressionLevel == 2) {
			return true;
		}

		// Check for incompatible Browser
		if (noCompressionUserAgents != null) {
			MessageBytes userAgentValueMB = getRequest().getMimeHeaders().getValue(
					"user-agent");
			if (userAgentValueMB != null) {
				String userAgentValue = userAgentValueMB.toString();

				if (noCompressionUserAgents != null
						&& noCompressionUserAgents.matcher(userAgentValue)
								.matches()) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Specialized utility method: find a sequence of lower case bytes inside a
	 * ByteChunk.
	 */
	protected int findBytes(ByteChunk bc, byte[] b) {

		byte first = b[0];
		byte[] buff = bc.getBuffer();
		int start = bc.getStart();
		int end = bc.getEnd();

		// Look for first char
		int srcEnd = b.length;

		for (int i = start; i <= (end - srcEnd); i++) {
			if (Ascii.toLower(buff[i]) != first) {
				continue;
			}
			// found first char, now look for a match
			int myPos = i + 1;
			for (int srcPos = 1; srcPos < srcEnd;) {
				if (Ascii.toLower(buff[myPos++]) != b[srcPos++]) {
					break;
				}
				if (srcPos == srcEnd) {
					return i - start; // found it
				}
			}
		}
		return -1;
	}

	/**
	 * Determine if we must drop the connection because of the HTTP status code.
	 * Use the same list of codes as Apache/httpd.
	 */
	protected boolean statusDropsConnection(int status) {
		return status == 400 /* SC_BAD_REQUEST */|| status == 408 /* SC_REQUEST_TIMEOUT */
				|| status == 411 /* SC_LENGTH_REQUIRED */|| status == 413 /* SC_REQUEST_ENTITY_TOO_LARGE */
				|| status == 414 /* SC_REQUEST_URI_TOO_LONG */|| status == 500 /* SC_INTERNAL_SERVER_ERROR */
				|| status == 503 /* SC_SERVICE_UNAVAILABLE */|| status == 501 /* SC_NOT_IMPLEMENTED */;
	}

	/**
	 * Exposes input buffer to super class to allow better code re-use.
	 * 
	 * @return The input buffer used by the processor.
	 */
	protected abstract AbstractInputBuffer<S> getInputBuffer();

	/**
	 * Exposes output buffer to super class to allow better code re-use.
	 * 
	 * @return The output buffer used by the processor.
	 */
	protected abstract AbstractOutputBuffer<S> getOutputBuffer();

	/**
	 * Initialize standard input and output filters.
	 */
	protected void initializeFilters(int maxTrailerSize, int maxExtensionSize,
			int maxSwallowSize) {
		// Create and add the identity filters.
		getInputBuffer().addFilter(new IdentityInputFilter(maxSwallowSize));
		getOutputBuffer().addFilter(new IdentityOutputFilter());

		// Create and add the chunked filters.
		getInputBuffer().addFilter(
				new ChunkedInputFilter(maxTrailerSize, maxExtensionSize,
						maxSwallowSize));
		getOutputBuffer().addFilter(new ChunkedOutputFilter());

		// Create and add the void filters.
		getInputBuffer().addFilter(new VoidInputFilter());
		getOutputBuffer().addFilter(new VoidOutputFilter());

		// Create and add buffered input filter
		getInputBuffer().addFilter(new BufferedInputFilter());

		// Create and add the chunked filters.
		// getInputBuffer().addFilter(new GzipInputFilter());
		getOutputBuffer().addFilter(new GzipOutputFilter());

		pluggableFilterIndex = getInputBuffer().getFilters().length;
	}

	/**
	 * Add an input filter to the current request. If the encoding is not
	 * supported, a 501 response will be returned to the client.
	 */
	private void addInputFilter(InputFilter[] inputFilters, String encodingName) {

		// Trim provided encoding name and convert to lower case since transfer
		// encoding names are case insensitive. (RFC2616, section 3.6)
		encodingName = encodingName.trim().toLowerCase(Locale.ENGLISH);

		if (encodingName.equals("identity")) {
			// Skip
		} else if (encodingName.equals("chunked")) {
			getInputBuffer().addActiveFilter(
					inputFilters[Constants26.getChunkedFilter()]);
			contentDelimitation = true;
		} else {
			for (int i = pluggableFilterIndex; i < inputFilters.length; i++) {
				if (inputFilters[i].getEncodingName().toString()
						.equals(encodingName)) {
					getInputBuffer().addActiveFilter(inputFilters[i]);
					return;
				}
			}
			// Unsupported transfer encoding
			// 501 - Unimplemented
			getResponse().setStatus(501);
			setErrorState(ErrorState.CLOSE_CLEAN, null);
			if (getLog().isDebugEnabled()) {
				getLog().debug(
						sm.getString("http11processor.request.prepare")
								+ " Unsupported transfer encoding ["
								+ encodingName + "]");
			}
		}
	}

	/**
	 * Send an action to the connector.
	 *
	 * @param actionCode
	 *            Type of the action
	 * @param param
	 *            Action parameter
	 */
	@Override
	@SuppressWarnings("deprecation")
	// Inbound/Outbound based upgrade mechanism
	public final void action(ActionCode actionCode, Object param) {

		switch (actionCode) {
		case CLOSE: {
			// End the processing of the current request
			try {
				getOutputBuffer().endRequest();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}
			break;
		}
		case COMMIT: {
			// Commit current response
			if (getResponse().isCommitted()) {
				return;
			}

			// Validate and write response headers
			try {
				prepareResponse();
				getOutputBuffer().commit();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}
			break;
		}
		case ACK: {
			// Acknowledge request
			// Send a 100 status back if it makes sense (response not committed
			// yet, and client specified an expectation for 100-continue)
			if ((getResponse().isCommitted()) || !expectation) {
				return;
			}

			getInputBuffer().setSwallowInput(true);
			try {
				getOutputBuffer().sendAck();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			}
			break;
		}
		case CLIENT_FLUSH: {
			try {
				getOutputBuffer().flush();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
				getResponse().setErrorException(e);
			}
			break;
		}
		case IS_ERROR: {
			((AtomicBoolean) param).set(getErrorState().isError());
			break;
		}
		case DISABLE_SWALLOW_INPUT: {
			// Do not swallow request input and make sure we are closing the
			// connection
			setErrorState(ErrorState.CLOSE_CLEAN, null);
			getInputBuffer().setSwallowInput(false);
			break;
		}
		case RESET: {
			// Note: This must be called before the response is committed
			getOutputBuffer().reset();
			break;
		}
		case CUSTOM: {
			// Do nothing
			// TODO Remove this action
			break;
		}
		case REQ_SET_BODY_REPLAY: {
			ByteChunk body = (ByteChunk) param;

			InputFilter savedBody = new SavedRequestInputFilter(body);
			savedBody.setRequest(getRequest());

			@SuppressWarnings("unchecked")
			AbstractInputBuffer<S> internalBuffer = (AbstractInputBuffer<S>) getRequest()
					.getInputBuffer();
			internalBuffer.addActiveFilter(savedBody);
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
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsyncStarted());
			break;
		}
		case ASYNC_IS_DISPATCHING: {
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsyncDispatching());
			break;
		}
		case ASYNC_IS_ASYNC: {
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsync());
			break;
		}
		case ASYNC_IS_TIMINGOUT: {
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsyncTimingOut());
			break;
		}
		case ASYNC_IS_ERROR: {
			((AtomicBoolean) param).set(getAsyncStateMachine().isAsyncError());
			break;
		}
		case UPGRADE_TOMCAT: {
			upgradeInbound = (UpgradeInbound) param;
			// Stop further HTTP output
			getOutputBuffer().setFinished(true);
			break;
		}
		case UPGRADE: {
			httpUpgradeHandler = (HttpUpgradeHandler) param;
			// Stop further HTTP output
			getOutputBuffer().setFinished(true);
			break;
		}
		case CLOSE_NOW: {
			// Block further output
			getOutputBuffer().setFinished(true);
			setErrorState(ErrorState.CLOSE_NOW, null);
			break;
		}
		default: {
			actionInternal(actionCode, param);
			break;
		}
		}
	}

	public abstract void actionInternal(ActionCode actionCode, Object param);

	/**
	 * Processors (currently only HTTP BIO) may elect to disable HTTP keep-alive
	 * in some circumstances. This method allows the processor implementation to
	 * determine if keep-alive should be disabled or not.
	 */
	protected abstract boolean disableKeepAlive();

	/**
	 * Configures the timeout to be used for reading the request line.
	 */
	protected abstract void setRequestLineReadTimeout() throws IOException;

	/**
	 * Defines how a connector handles an incomplete request line read.
	 *
	 * @return <code>true</code> if the processor should break out of the
	 *         processing loop, otherwise <code>false</code>.
	 */
	protected abstract boolean handleIncompleteRequestLineRead();

	/**
	 * Set the socket timeout.
	 */
	protected abstract void setSocketTimeout(int timeout) throws IOException;

	/**
	 * Process pipelined HTTP requests using the specified input and output
	 * streams.
	 *
	 * @param socketWrapper
	 *            Socket from which the HTTP requests will be read and the HTTP
	 *            responses will be written.
	 *
	 * @throws IOException
	 *             error during an I/O operation
	 */
	@Override
	public SocketState process(SocketWrapper<S> socketWrapper)
			throws IOException {
		RequestInfo rp = getRequest().getRequestProcessor();
		rp.setStage(Constants24.getStageParse());

		// Setting up the I/O
		setSocketWrapper(socketWrapper);
		getInputBuffer().init(socketWrapper, getEndpoint());
		getOutputBuffer().init(socketWrapper, getEndpoint());

		// Flags
		keepAlive = true;
		comet = false;
		openSocket = false;
		sendfileInProgress = false;
		readComplete = true;
		if (getEndpoint().getUsePolling()) {
			keptAlive = false;
		} else {
			keptAlive = socketWrapper.isKeptAlive();
		}

		if (disableKeepAlive()) {
			socketWrapper.setKeepAliveLeft(0);
		}

		while (!getErrorState().isError() && keepAlive && !comet && !isAsync()
				&& upgradeInbound == null && httpUpgradeHandler == null
				&& !getEndpoint().isPaused()) {

			// Parsing the request header
			try {
				setRequestLineReadTimeout();

				if (!getInputBuffer().parseRequestLine(keptAlive)) {
					if (handleIncompleteRequestLineRead()) {
						break;
					}
				}

				if (getEndpoint().isPaused()) {
					// 503 - Service unavailable
					getResponse().setStatus(503);
					setErrorState(ErrorState.CLOSE_CLEAN, null);
				} else {
					keptAlive = true;
					// Set this every time in case limit has been changed via
					// JMX
					getRequest().getMimeHeaders().setLimit(
							getEndpoint().getMaxHeaderCount());
					// Currently only NIO will ever return false here
					if (!getInputBuffer().parseHeaders()) {
						// We've read part of the request, don't recycle it
						// instead associate it with the socket
						openSocket = true;
						readComplete = false;
						break;
					}
					if (!disableUploadTimeout) {
						setSocketTimeout(connectionUploadTimeout);
					}
				}
			} catch (IOException e) {
				if (getLog().isDebugEnabled()) {
					getLog().debug(
							sm.getString("http11processor.header.parse"), e);
				}
				setErrorState(ErrorState.CLOSE_NOW, e);
				break;
			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
				Mode logMode = userDataHelper.getNextMode();
				if (logMode != null) {
					String message = sm
							.getString("http11processor.header.parse");
					switch (logMode) {
					case INFO_THEN_DEBUG:
						message += sm.getString("http11processor.fallToDebug");
						//$FALL-THROUGH$
					case INFO:
						getLog().info(message);
						break;
					case DEBUG:
						getLog().debug(message);
					}
				}
				// 400 - Bad Request
				getResponse().setStatus(400);
				setErrorState(ErrorState.CLOSE_CLEAN, t);
				getAdapter().log(getRequest(), getResponse(), 0);
			}

			if (!getErrorState().isError()) {
				// Setting up filters, and parse some request headers
				rp.setStage(Constants24.getStagePrepare());
				try {
					prepareRequest();
				} catch (Throwable t) {
					ExceptionUtils2.handleThrowable(t);
					if (getLog().isDebugEnabled()) {
						getLog().debug(
								sm.getString("http11processor.request.prepare"),
								t);
					}
					// 500 - Internal Server Error
					getResponse().setStatus(500);
					setErrorState(ErrorState.CLOSE_CLEAN, t);
					getAdapter().log(getRequest(), getResponse(), 0);
				}
			}

			if (maxKeepAliveRequests == 1) {
				keepAlive = false;
			} else if (maxKeepAliveRequests > 0
					&& socketWrapper.decrementKeepAlive() <= 0) {
				keepAlive = false;
			}

			// Process the request in the adapter
			if (!getErrorState().isError()) {
				try {
					rp.setStage(Constants24.getStageService());
					getAdapter().service(getRequest(), getResponse());
					// Handle when the response was committed before a serious
					// error occurred. Throwing a ServletException should both
					// set the status to 500 and set the errorException.
					// If we fail here, then the response is likely already
					// committed, so we can't try and set headers.
					if (keepAlive
							&& !getErrorState().isError()
							&& (getResponse().getErrorException() != null || (!isAsync() && statusDropsConnection(getResponse()
									.getStatus())))) {
						setErrorState(ErrorState.CLOSE_CLEAN, null);
					}
					setCometTimeouts(socketWrapper);
				} catch (InterruptedIOException e) {
					setErrorState(ErrorState.CLOSE_NOW, e);
				} catch (HeadersTooLargeException e) {
					// The response should not have been committed but check it
					// anyway to be safe
					if (getResponse().isCommitted()) {
						setErrorState(ErrorState.CLOSE_NOW, e);
					} else {
						getResponse().reset();
						getResponse().setStatus(500);
						setErrorState(ErrorState.CLOSE_CLEAN, e);
						getResponse().setHeader("Connection", "close"); // TODO:
																	// Remove
					}
				} catch (Throwable t) {
					ExceptionUtils2.handleThrowable(t);
					getLog().error(
							sm.getString("http11processor.request.process"), t);
					// 500 - Internal Server Error
					getResponse().setStatus(500);
					setErrorState(ErrorState.CLOSE_CLEAN, t);
					getAdapter().log(getRequest(), getResponse(), 0);
				}
			}

			// Finish the handling of the request
			rp.setStage(Constants24.getStageEndinput());

			if (!isAsync() && !comet) {
				if (getErrorState().isError()) {
					// If we know we are closing the connection, don't drain
					// input. This way uploading a 100GB file doesn't tie up the
					// thread if the servlet has rejected it.
					getInputBuffer().setSwallowInput(false);
				} else if (expectation
						&& (getResponse().getStatus() < 200 || getResponse().getStatus() > 299)) {
					// Client sent Expect: 100-continue but received a
					// non-2xx final response. Disable keep-alive (if enabled)
					// to ensure that the connection is closed. Some clients may
					// still send the body, some may send the next request.
					// No way to differentiate, so close the connection to
					// force the client to send the next request.
					getInputBuffer().setSwallowInput(false);
					keepAlive = false;
				}
				endRequest();
			}

			rp.setStage(Constants24.getStageEndoutput());

			// If there was an error, make sure the request is counted as
			// and error, and update the statistics counter
			if (getErrorState().isError()) {
				getResponse().setStatus(500);
			}
			getRequest().updateCounters();

			if (!isAsync() && !comet || getErrorState().isError()) {
				if (getErrorState().isIoAllowed()) {
					getInputBuffer().nextRequest();
					getOutputBuffer().nextRequest();
				}
			}

			if (!disableUploadTimeout) {
				if (getEndpoint().getSoTimeout() > 0) {
					setSocketTimeout(getEndpoint().getSoTimeout());
				} else {
					setSocketTimeout(0);
				}
			}

			rp.setStage(Constants24.getStageKeepalive());

			if (breakKeepAliveLoop(socketWrapper)) {
				break;
			}
		}

		rp.setStage(Constants24.getStageEnded());

		if (getErrorState().isError() || getEndpoint().isPaused()) {
			return SocketState.CLOSED;
		} else if (isAsync() || comet) {
			return SocketState.LONG;
		} else if (isUpgrade()) {
			return SocketState.UPGRADING;
		} else if (getUpgradeInbound() != null) {
			return SocketState.UPGRADING_TOMCAT;
		} else {
			if (sendfileInProgress) {
				return SocketState.SENDFILE;
			} else {
				if (openSocket) {
					if (readComplete) {
						return SocketState.OPEN;
					} else {
						return SocketState.LONG;
					}
				} else {
					return SocketState.CLOSED;
				}
			}
		}
	}

	/**
	 * After reading the request headers, we have to setup the request filters.
	 */
	protected void prepareRequest() {

		http11 = true;
		http09 = false;
		contentDelimitation = false;
		expectation = false;

		prepareRequestInternal();

		if (getEndpoint().isSSLEnabled()) {
			getRequest().scheme().setString("https");
		}
		MessageBytes protocolMB = getRequest().protocol();
		if (protocolMB.equals(Constants26.getHttp11())) {
			http11 = true;
			protocolMB.setString(Constants26.getHttp11());
		} else if (protocolMB.equals(Constants26.getHttp10())) {
			http11 = false;
			keepAlive = false;
			protocolMB.setString(Constants26.getHttp10());
		} else if (protocolMB.equals("")) {
			// HTTP/0.9
			http09 = true;
			http11 = false;
			keepAlive = false;
		} else {
			// Unsupported protocol
			http11 = false;
			// Send 505; Unsupported HTTP version
			getResponse().setStatus(505);
			setErrorState(ErrorState.CLOSE_CLEAN, null);
			if (getLog().isDebugEnabled()) {
				getLog().debug(
						sm.getString("http11processor.request.prepare")
								+ " Unsupported HTTP version \"" + protocolMB
								+ "\"");
			}
		}

		MessageBytes methodMB = getRequest().method();
		if (methodMB.equals(Constants26.getGet())) {
			methodMB.setString(Constants26.getGet());
		} else if (methodMB.equals(Constants26.getPost())) {
			methodMB.setString(Constants26.getPost());
		}

		MimeHeaders headers = getRequest().getMimeHeaders();

		// Check connection header
		MessageBytes connectionValueMB = headers.getValue(Constants26.getConnection());
		if (connectionValueMB != null) {
			ByteChunk connectionValueBC = connectionValueMB.getByteChunk();
			if (findBytes(connectionValueBC, Constants26.getCloseBytes()) != -1) {
				keepAlive = false;
			} else if (findBytes(connectionValueBC, Constants26.getKeepaliveBytes()) != -1) {
				keepAlive = true;
			}
		}

		MessageBytes expectMB = null;
		if (http11) {
			expectMB = headers.getValue("expect");
		}
		if (expectMB != null) {
			if (expectMB.indexOfIgnoreCase("100-continue", 0) != -1) {
				getInputBuffer().setSwallowInput(false);
				expectation = true;
			} else {
				getResponse().setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
				setErrorState(ErrorState.CLOSE_CLEAN, null);
			}
		}

		// Check user-agent header
		if ((restrictedUserAgents != null) && ((http11) || (keepAlive))) {
			MessageBytes userAgentValueMB = headers.getValue("user-agent");
			// Check in the restricted list, and adjust the http11
			// and keepAlive flags accordingly
			if (userAgentValueMB != null) {
				String userAgentValue = userAgentValueMB.toString();
				if (restrictedUserAgents != null
						&& restrictedUserAgents.matcher(userAgentValue)
								.matches()) {
					http11 = false;
					keepAlive = false;
				}
			}
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
					getRequest().requestURI()
							.setBytes(uriB, uriBCStart + pos + 1, 1);
				} else {
					getRequest().requestURI().setBytes(uriB, uriBCStart + slashPos,
							uriBC.getLength() - slashPos);
				}
				MessageBytes hostMB = headers.setValue("host");
				hostMB.setBytes(uriB, uriBCStart + pos + 3, slashPos - pos - 3);
			}

		}

		// Input filter setup
		InputFilter[] inputFilters = getInputBuffer().getFilters();

		// Parse transfer-encoding header
		MessageBytes transferEncodingValueMB = null;
		if (http11) {
			transferEncodingValueMB = headers.getValue("transfer-encoding");
		}
		if (transferEncodingValueMB != null) {
			String transferEncodingValue = transferEncodingValueMB.toString();
			// Parse the comma separated list. "identity" codings are ignored
			int startPos = 0;
			int commaPos = transferEncodingValue.indexOf(',');
			String encodingName = null;
			while (commaPos != -1) {
				encodingName = transferEncodingValue.substring(startPos,
						commaPos);
				addInputFilter(inputFilters, encodingName);
				startPos = commaPos + 1;
				commaPos = transferEncodingValue.indexOf(',', startPos);
			}
			encodingName = transferEncodingValue.substring(startPos);
			addInputFilter(inputFilters, encodingName);
		}

		// Parse content-length header
		long contentLength = getRequest().getContentLengthLong();
		if (contentLength >= 0) {
			if (contentDelimitation) {
				// contentDelimitation being true at this point indicates that
				// chunked encoding is being used but chunked encoding should
				// not be used with a content length. RFC 2616, section 4.4,
				// bullet 3 states Content-Length must be ignored in this case -
				// so remove it.
				headers.removeHeader("content-length");
				getRequest().setContentLength(-1);
			} else {
				getInputBuffer().addActiveFilter(
						inputFilters[Constants26.getIdentityFilter()]);
				contentDelimitation = true;
			}
		}

		MessageBytes valueMB = headers.getValue("host");

		// Check host header
		if (http11 && (valueMB == null)) {
			// 400 - Bad request
			getResponse().setStatus(400);
			setErrorState(ErrorState.CLOSE_CLEAN, null);
			if (getLog().isDebugEnabled()) {
				getLog().debug(
						sm.getString("http11processor.request.prepare")
								+ " host header missing");
			}
		}

		parseHost(valueMB);

		if (!contentDelimitation) {
			// If there's no content length
			// (broken HTTP/1.0 or HTTP/1.1), assume
			// the client is not broken and didn't send a body
			getInputBuffer().addActiveFilter(
					inputFilters[Constants26.getVoidFilter()]);
			contentDelimitation = true;
		}

		// Advertise sendfile support through a request attribute
		if (getEndpoint().getUseSendfile()) {
			getRequest().setAttribute(
					Constants24.getSendfileSupportedAttr(),
					Boolean.TRUE);
		}

		// Advertise comet support through a request attribute
		if (getEndpoint().getUseComet()) {
			getRequest().setAttribute(
					Constants24.getCometSupportedAttr(),
					Boolean.TRUE);
		}
		// Advertise comet timeout support
		if (getEndpoint().getUseCometTimeout()) {
			getRequest().setAttribute(
					Constants24.getCometTimeoutSupportedAttr(),
					Boolean.TRUE);
		}
		if (getErrorState().isError()) {
			getAdapter().log(getRequest(), getResponse(), 0);
		}
	}

	/**
	 * Connector implementation specific request preparation. Ideally, this will
	 * go away in the future.
	 */
	protected abstract void prepareRequestInternal();

	/**
	 * When committing the response, we have to validate the set of headers, as
	 * well as setup the response filters.
	 */
	private void prepareResponse() {

		boolean entityBody = true;
		contentDelimitation = false;

		OutputFilter[] outputFilters = getOutputBuffer().getFilters();

		if (http09 == true) {
			// HTTP/0.9
			getOutputBuffer().addActiveFilter(
					outputFilters[Constants26.getIdentityFilter()]);
			return;
		}

		int statusCode = getResponse().getStatus();
		if (statusCode < 200 || statusCode == 204 || statusCode == 205
				|| statusCode == 304) {
			// No entity body
			getOutputBuffer().addActiveFilter(
					outputFilters[Constants26.getVoidFilter()]);
			entityBody = false;
			contentDelimitation = true;
		}

		MessageBytes methodMB = getRequest().method();
		if (methodMB.equals("HEAD")) {
			// No entity body
			getOutputBuffer().addActiveFilter(
					outputFilters[Constants26.getVoidFilter()]);
			contentDelimitation = true;
		}

		// Sendfile support
		boolean sendingWithSendfile = false;
		if (getEndpoint().getUseSendfile()) {
			sendingWithSendfile = prepareSendfile(outputFilters);
		}

		// Check for compression
		boolean isCompressable = false;
		boolean useCompression = false;
		if (entityBody && (compressionLevel > 0) && !sendingWithSendfile) {
			isCompressable = isCompressable();
			if (isCompressable) {
				useCompression = useCompression();
			}
			// Change content-length to -1 to force chunking
			if (useCompression) {
				getResponse().setContentLength(-1);
			}
		}

		MimeHeaders headers = getResponse().getMimeHeaders();
		if (!entityBody) {
			getResponse().setContentLength(-1);
		}
		// A SC_NO_CONTENT response may include entity headers
		if (entityBody || statusCode == 204) {
			String contentType = getResponse().getContentType();
			if (contentType != null) {
				headers.setValue("Content-Type").setString(contentType);
			}
			String contentLanguage = getResponse().getContentLanguage();
			if (contentLanguage != null) {
				headers.setValue("Content-Language").setString(contentLanguage);
			}
		}

		long contentLength = getResponse().getContentLengthLong();
		boolean connectionClosePresent = false;
		if (contentLength != -1) {
			headers.setValue("Content-Length").setLong(contentLength);
			getOutputBuffer().addActiveFilter(
					outputFilters[Constants26.getIdentityFilter()]);
			contentDelimitation = true;
		} else {
			// If the response code supports an entity body and we're on
			// HTTP 1.1 then we chunk unless we have a Connection: close header
			connectionClosePresent = isConnectionClose(headers);
			if (entityBody && http11 && !connectionClosePresent) {
				getOutputBuffer().addActiveFilter(
						outputFilters[Constants26.getChunkedFilter()]);
				contentDelimitation = true;
				headers.addValue(Constants26.getTransferencoding()).setString(
						Constants26.getChunked());
			} else {
				getOutputBuffer().addActiveFilter(
						outputFilters[Constants26.getIdentityFilter()]);
			}
		}

		if (useCompression) {
			getOutputBuffer().addActiveFilter(
					outputFilters[Constants26.getGzipFilter()]);
			headers.setValue("Content-Encoding").setString("gzip");
		}
		// If it might be compressed, set the Vary header
		if (isCompressable) {
			// Make Proxies happy via Vary (from mod_deflate)
			MessageBytes vary = headers.getValue("Vary");
			if (vary == null) {
				// Add a new Vary header
				headers.setValue("Vary").setString("Accept-Encoding");
			} else if (vary.equals("*")) {
				// No action required
			} else {
				// Merge into current header
				headers.setValue("Vary").setString(
						vary.getString() + ",Accept-Encoding");
			}
		}

		// Add date header unless application has already set one (e.g. in a
		// Caching Filter)
		if (headers.getValue("Date") == null) {
			headers.setValue("Date").setString(
					FastHttpDateFormat.getCurrentDate());
		}

		// FIXME: Add transfer encoding header

		if ((entityBody) && (!contentDelimitation)) {
			// Mark as close the connection after the request, and add the
			// connection: close header
			keepAlive = false;
		}

		// If we know that the request is bad this early, add the
		// Connection: close header.
		keepAlive = keepAlive && !statusDropsConnection(statusCode);
		if (!keepAlive) {
			// Avoid adding the close header twice
			if (!connectionClosePresent) {
				headers.addValue(Constants26.getConnection()).setString(
						Constants26.getClose());
			}
		} else if (!http11 && !getErrorState().isError()) {
			headers.addValue(Constants26.getConnection()).setString(
					Constants26.getKeepalive());
		}

		// Build the response header
		getOutputBuffer().sendStatus();

		// Add server header
		if (server != null) {
			// Always overrides anything the app might set
			headers.setValue("Server").setString(server);
		} else if (headers.getValue("Server") == null) {
			// If app didn't set the header, use the default
			getOutputBuffer().write(Constants26.getServerBytes());
		}

		int size = headers.size();
		for (int i = 0; i < size; i++) {
			getOutputBuffer().sendHeader(headers.getName(i),
					headers.getValue(i));
		}
		getOutputBuffer().endHeaders();

	}

	private boolean isConnectionClose(MimeHeaders headers) {
		MessageBytes connection = headers.getValue(Constants26.getConnection());
		if (connection == null) {
			return false;
		}
		return connection.equals(Constants26.getClose());
	}

	public abstract boolean prepareSendfile(OutputFilter[] outputFilters);

	/**
	 * Parse host.
	 */
	protected void parseHost(MessageBytes valueMB) {

		if (valueMB == null || valueMB.isNull()) {
			// HTTP/1.0
			// If no host header, use the port info from the endpoint
			// The host will be obtained lazily from the socket if required
			// using ActionCode#REQ_LOCAL_NAME_ATTRIBUTE
			getRequest().setServerPort(getEndpoint().getPort());
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
			if (!getEndpoint().isSSLEnabled()) {
				// 80 - Default HTTP port
				getRequest().setServerPort(80);
			} else {
				// 443 - Default HTTPS port
				getRequest().setServerPort(443);
			}
			getRequest().serverName().setChars(hostNameC, 0, valueL);
		} else {
			getRequest().serverName().setChars(hostNameC, 0, colonPos);

			int port = 0;
			int mult = 1;
			for (int i = valueL - 1; i > colonPos; i--) {
				int charValue = HexUtils.getDec(valueB[i + valueS]);
				if (charValue == -1 || charValue > 9) {
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

	@Override
	public SocketState asyncDispatch(SocketStatus status) {

		RequestInfo rp = getRequest().getRequestProcessor();
		try {
			rp.setStage(Constants24.getStageService());
			if (!getAdapter().asyncDispatch(getRequest(), getResponse(), status)) {
				setErrorState(ErrorState.CLOSE_NOW, null);
			}
			resetTimeouts();
		} catch (InterruptedIOException e) {
			setErrorState(ErrorState.CLOSE_NOW, e);
		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			setErrorState(ErrorState.CLOSE_NOW, t);
			getLog().error(sm.getString("http11processor.request.process"), t);
		} finally {
			if (getErrorState().isError()) {
				// 500 - Internal Server Error
				getResponse().setStatus(500);
				getAdapter().log(getRequest(), getResponse(), 0);
			}
		}

		rp.setStage(Constants24.getStageEnded());

		if (getErrorState().isError()) {
			return SocketState.CLOSED;
		} else if (isAsync()) {
			return SocketState.LONG;
		} else {
			if (!keepAlive) {
				return SocketState.CLOSED;
			} else {
				getInputBuffer().nextRequest();
				getOutputBuffer().nextRequest();
				return SocketState.OPEN;
			}
		}
	}

	@Override
	public boolean isComet() {
		return comet;
	}

	@Override
	public SocketState upgradeDispatch() throws IOException {
		// Should never reach this code but in case we do...
		// TODO
		throw new IOException(sm.getString("TODO"));
	}

	/**
	 * @deprecated Will be removed in Tomcat 8.0.x.
	 */
	@Deprecated
	@Override
	public UpgradeInbound getUpgradeInbound() {
		return upgradeInbound;
	}

	@Override
	public boolean isUpgrade() {
		return httpUpgradeHandler != null;
	}

	@Override
	public SocketState upgradeDispatch(SocketStatus status) throws IOException {
		// Should never reach this code but in case we do...
		throw new IOException(
				sm.getString("ajpprocessor.httpupgrade.notsupported"));
	}

	@Override
	public HttpUpgradeHandler getHttpUpgradeHandler() {
		return httpUpgradeHandler;
	}

	/**
	 * Provides a mechanism for those connector implementations (currently only
	 * NIO) that need to reset timeouts from Async timeouts to standard HTTP
	 * timeouts once async processing completes.
	 */
	protected abstract void resetTimeouts();

	/**
	 * Provides a mechanism for those connectors (currently only NIO) that need
	 * that need to set comet timeouts.
	 */
	protected abstract void setCometTimeouts(SocketWrapper<S> socketWrapper);

	public void endRequest() {

		// Finish the handling of the request
		if (getErrorState().isIoAllowed()) {
			try {
				getInputBuffer().endRequest();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
				// 500 - Internal Server Error
				// Can't add a 500 to the access log since that has already been
				// written in the Adapter.service method.
				getResponse().setStatus(500);
				setErrorState(ErrorState.CLOSE_NOW, t);
				getLog().error(sm.getString("http11processor.request.finish"),
						t);
			}
		}
		if (getErrorState().isIoAllowed()) {
			try {
				getOutputBuffer().endRequest();
			} catch (IOException e) {
				setErrorState(ErrorState.CLOSE_NOW, e);
			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
				setErrorState(ErrorState.CLOSE_NOW, t);
				getLog().error(sm.getString("http11processor.response.finish"),
						t);
			}
		}
	}

	/**
	 * Checks to see if the keep-alive loop should be broken, performing any
	 * processing (e.g. sendfile handling) that may have an impact on whether or
	 * not the keep-alive loop should be broken.
	 * 
	 * @return true if the keep-alive loop should be broken
	 */
	protected abstract boolean breakKeepAliveLoop(SocketWrapper<S> socketWrapper);

	@Override
	public final void recycle(boolean isSocketClosing) {
		getAdapter().checkRecycled(getRequest(), getResponse());

		if (getInputBuffer() != null) {
			getInputBuffer().recycle();
		}
		if (getOutputBuffer() != null) {
			getOutputBuffer().recycle();
		}
		if (getAsyncStateMachine() != null) {
			getAsyncStateMachine().recycle();
		}
		upgradeInbound = null;
		httpUpgradeHandler = null;
		setRemoteAddr(null);
		setRemoteHost(null);
		setLocalAddr(null);
		setLocalName(null);
		setRemotePort(-1);
		setLocalPort(-1);
		comet = false;
		resetErrorState();
		recycleInternal();
	}

	protected abstract void recycleInternal();

	public String getLocalAddr() {
		return localAddr;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public String getLocalName() {
		return localName;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public int getPluggableFilterIndex() {
		return pluggableFilterIndex;
	}

	public void setPluggableFilterIndex(int pluggableFilterIndex) {
		this.pluggableFilterIndex = pluggableFilterIndex;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public boolean isOpenSocket() {
		return openSocket;
	}

	public void setOpenSocket(boolean openSocket) {
		this.openSocket = openSocket;
	}

	public boolean isKeptAlive() {
		return keptAlive;
	}

	public void setKeptAlive(boolean keptAlive) {
		this.keptAlive = keptAlive;
	}

	public boolean isSendfileInProgress() {
		return sendfileInProgress;
	}

	public void setSendfileInProgress(boolean sendfileInProgress) {
		this.sendfileInProgress = sendfileInProgress;
	}

	public boolean isReadComplete() {
		return readComplete;
	}

	public void setReadComplete(boolean readComplete) {
		this.readComplete = readComplete;
	}

	public boolean isHttp11() {
		return http11;
	}

	public void setHttp11(boolean http11) {
		this.http11 = http11;
	}

	public boolean isHttp09() {
		return http09;
	}

	public void setHttp09(boolean http09) {
		this.http09 = http09;
	}

	public boolean isContentDelimitation() {
		return contentDelimitation;
	}

	public void setContentDelimitation(boolean contentDelimitation) {
		this.contentDelimitation = contentDelimitation;
	}

	public boolean isExpectation() {
		return expectation;
	}

	public void setExpectation(boolean expectation) {
		this.expectation = expectation;
	}

	public Pattern getRestrictedUserAgents() {
		return restrictedUserAgents;
	}

	public void setRestrictedUserAgents(Pattern restrictedUserAgents) {
		this.restrictedUserAgents = restrictedUserAgents;
	}

	public int getCompressionLevel() {
		return compressionLevel;
	}

	public void setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	public Pattern getNoCompressionUserAgents() {
		return noCompressionUserAgents;
	}

	public void setNoCompressionUserAgents(Pattern noCompressionUserAgents) {
		this.noCompressionUserAgents = noCompressionUserAgents;
	}

	public char[] getHostNameC() {
		return hostNameC;
	}

	public void setHostNameC(char[] hostNameC) {
		this.hostNameC = hostNameC;
	}

	public UserDataHelper getUserDataHelper() {
		return userDataHelper;
	}

	public static StringManager3 getSm() {
		return sm;
	}

	public int getCompressionMinSize() {
		return compressionMinSize;
	}

	public String[] getCompressableMimeTypes() {
		return compressableMimeTypes;
	}

	public void setComet(boolean comet) {
		this.comet = comet;
	}

	public void setUpgradeInbound(
			UpgradeInbound upgradeInbound) {
		this.upgradeInbound = upgradeInbound;
	}

	public void setHttpUpgradeHandler(HttpUpgradeHandler httpUpgradeHandler) {
		this.httpUpgradeHandler = httpUpgradeHandler;
	}

}
