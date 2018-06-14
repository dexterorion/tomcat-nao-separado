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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.coyote.ActionCode;
import org.apache.coyote.ErrorState;
import org.apache.coyote.RequestInfo;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.tomcat.util.net.AprEndpointSendfileData;

import org.apache.coyote.Constants24;

/**
 * Processes HTTP requests.
 *
 * @author Remy Maucherat
 */
public class Http11AprProcessor extends AbstractHttp11Processor<Long> {

	private static final Log log = LogFactory.getLog(Http11AprProcessor.class);

	@Override
	protected Log getLog() {
		return log;
	}

	// ----------------------------------------------------------- Constructors

	public Http11AprProcessor(int headerBufferSize, AprEndpoint endpoint,
			int maxTrailerSize, int maxExtensionSize, int maxSwallowSize) {

		super(endpoint);

		inputBuffer = new InternalAprInputBuffer(getRequest(), headerBufferSize);
		getRequest().setInputBuffer(inputBuffer);

		outputBuffer = new InternalAprOutputBuffer(getResponse(), headerBufferSize);
		getResponse().setOutputBuffer(outputBuffer);

		initializeFilters(maxTrailerSize, maxExtensionSize, maxSwallowSize);
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * Input.
	 */
	private InternalAprInputBuffer inputBuffer = null;

	/**
	 * Output.
	 */
	private InternalAprOutputBuffer outputBuffer = null;

	/**
	 * Sendfile data.
	 */
	private AprEndpointSendfileData sendfileData = null;

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
	 * Process pipelined HTTP requests using the specified input and output
	 * streams.
	 *
	 * @throws IOException
	 *             error during an I/O operation
	 */
	@Override
	public SocketState event(SocketStatus status) throws IOException {

		RequestInfo rp = getRequest().getRequestProcessor();

		try {
			rp.setStage(Constants24.getStageService());
			if (!getAdapter().event(getRequest(), getResponse(), status)) {
				setErrorState(ErrorState.CLOSE_NOW, null);
			}
		} catch (InterruptedIOException e) {
			setErrorState(ErrorState.CLOSE_NOW, e);
		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			// 500 - Internal Server Error
			getResponse().setStatus(500);
			setErrorState(ErrorState.CLOSE_NOW, t);
			getAdapter().log(getRequest(), getResponse(), 0);
			log.error(getSm().getString("http11processor.request.process"), t);
		}

		rp.setStage(Constants24.getStageEnded());

		if (getErrorState().isError() || status == SocketStatus.STOP) {
			return SocketState.CLOSED;
		} else if (!isComet()) {
			inputBuffer.nextRequest();
			outputBuffer.nextRequest();
			return SocketState.OPEN;
		} else {
			return SocketState.LONG;
		}
	}

	@Override
	protected boolean disableKeepAlive() {
		return false;
	}

	@Override
	protected void setRequestLineReadTimeout() throws IOException {
		// Timeouts while in the poller are handled entirely by the poller
		// Only need to be concerned with socket timeouts

		// APR uses simulated blocking so if some request line data is present
		// then it must all be presented (with the normal socket timeout).

		// When entering the processing loop for the first time there will
		// always be some data to read so the keep-alive timeout is not required

		// For the second and subsequent executions of the processing loop, if
		// there is no request line data present then no further data will be
		// read from the socket. If there is request line data present then it
		// must all be presented (with the normal socket timeout)

		// When the socket is created it is given the correct timeout.
		// sendfile may change the timeout but will restore it
		// This processor may change the timeout for uploads but will restore it

		// NO-OP
	}

	@Override
	protected boolean handleIncompleteRequestLineRead() {
		// This means that no data is available right now
		// (long keepalive), so that the processor should be recycled
		// and the method should return true
		setOpenSocket(true);
		return true;
	}

	@Override
	protected void setSocketTimeout(int timeout) {
		Socket.timeoutSet(getSocketWrapper().getSocket().longValue(), timeout * 1000);
	}

	@Override
	protected void setCometTimeouts(SocketWrapper<Long> socketWrapper) {
		// NO-OP for APR/native
	}

	@Override
	protected boolean breakKeepAliveLoop(SocketWrapper<Long> socketWrapper) {
		setOpenSocket(isKeepAlive());
		// Do sendfile as needed: add socket to sendfile and end
		if (sendfileData != null && !getErrorState().isError()) {
			sendfileData.setSocket(getSocketWrapper().getSocket().longValue());
			sendfileData.setKeepAlive(isKeepAlive());
			if (!((AprEndpoint) getEndpoint()).getSendfile().add(sendfileData)) {
				// Didn't send all of the data to sendfile.
				if (sendfileData.getSocket() == 0) {
					// The socket is no longer set. Something went wrong.
					// Close the connection. Too late to set status code.
					if (log.isDebugEnabled()) {
						log.debug(getSm().getString(
								"http11processor.sendfile.error"));
					}
					setErrorState(ErrorState.CLOSE_NOW, null);
				} else {
					// The sendfile Poller will add the socket to the main
					// Poller once sendfile processing is complete
					setSendfileInProgress(true);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	protected void resetTimeouts() {
		// NOOP for APR
	}

	@Override
	public void recycleInternal() {
		setSocketWrapper(null);
		sendfileData = null;
	}

	@Override
	public void setSslSupport(SSLSupport sslSupport) {
		// NOOP for APR
	}

	// ----------------------------------------------------- ActionHook Methods

	/**
	 * Send an action to the connector.
	 *
	 * @param actionCode
	 *            Type of the action
	 * @param param
	 *            Action parameter
	 */
	@Override
	@SuppressWarnings("incomplete-switch")
	// Other cases are handled by action()
	public void actionInternal(ActionCode actionCode, Object param) {

		long socketRef = getSocketWrapper().getSocket().longValue();

		switch (actionCode) {
		case REQ_HOST_ADDR_ATTRIBUTE: {
			// Get remote host address
			if (getRemoteAddr() == null && (socketRef != 0)) {
				try {
					long sa = Address.get(Socket.getAprRemote(), socketRef);
					setRemoteAddr(Address.getip(sa));
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.info"),
							e);
				}
			}
			getRequest().remoteAddr().setString(getRemoteAddr());
			break;
		}
		case REQ_LOCAL_NAME_ATTRIBUTE: {
			// Get local host name
			if (getLocalName() == null && (socketRef != 0)) {
				try {
					long sa = Address.get(Socket.getAprLocal(), socketRef);
					setLocalName(Address.getnameinfo(sa, 0));
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.info"),
							e);
				}
			}
			getRequest().localName().setString(getLocalName());
			break;
		}
		case REQ_HOST_ATTRIBUTE: {
			// Get remote host name
			if (getRemoteHost() == null && (socketRef != 0)) {
				try {
					long sa = Address.get(Socket.getAprRemote(), socketRef);
					setRemoteHost(Address.getnameinfo(sa, 0));
					if (getRemoteHost() == null) {
						setRemoteHost(Address.getip(sa));
					}
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.info"),
							e);
				}
			}
			getRequest().remoteHost().setString(getRemoteHost());
			break;
		}
		case REQ_LOCAL_ADDR_ATTRIBUTE: {
			// Get local host address
			if (getLocalAddr() == null && (socketRef != 0)) {
				try {
					long sa = Address.get(Socket.getAprLocal(), socketRef);
					setLocalAddr(Address.getip(sa));
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.info"),
							e);
				}
			}

			getRequest().localAddr().setString(getLocalAddr());
			break;
		}
		case REQ_REMOTEPORT_ATTRIBUTE: {
			// Get remote port
			if (getRemotePort() == -1 && (socketRef != 0)) {
				try {
					long sa = Address.get(Socket.getAprRemote(), socketRef);
					Sockaddr addr = Address.getInfo(sa);
					setRemotePort(addr.getPort());
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.info"),
							e);
				}
			}
			getRequest().setRemotePort(getRemotePort());
			break;
		}
		case REQ_LOCALPORT_ATTRIBUTE: {
			// Get local port
			if (getLocalPort() == -1 && (socketRef != 0)) {
				try {
					long sa = Address.get(Socket.getAprLocal(), socketRef);
					Sockaddr addr = Address.getInfo(sa);
					setLocalPort(addr.getPort());
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.info"),
							e);
				}
			}
			getRequest().setLocalPort(getLocalPort());
			break;
		}
		case REQ_SSL_ATTRIBUTE: {
			if (getEndpoint().isSSLEnabled() && (socketRef != 0)) {
				try {
					// Cipher suite
					Object sslO = SSLSocket.getInfoS(socketRef,
							SSL.getSslInfoCipher());
					if (sslO != null) {
						getRequest().setAttribute(SSLSupport.CIPHER_SUITE_KEY, sslO);
					}
					// Get client certificate and the certificate chain if
					// present
					// certLength == -1 indicates an error
					int certLength = SSLSocket.getInfoI(socketRef,
							SSL.getSslInfoClientCertChain());
					byte[] clientCert = SSLSocket.getInfoB(socketRef,
							SSL.getSslInfoClientCert());
					X509Certificate[] certs = null;
					if (clientCert != null && certLength > -1) {
						certs = new X509Certificate[certLength + 1];
						CertificateFactory cf;
						if (clientCertProvider == null) {
							cf = CertificateFactory.getInstance("X.509");
						} else {
							cf = CertificateFactory.getInstance("X.509",
									clientCertProvider);
						}
						certs[0] = (X509Certificate) cf
								.generateCertificate(new ByteArrayInputStream(
										clientCert));
						for (int i = 0; i < certLength; i++) {
							byte[] data = SSLSocket.getInfoB(socketRef,
									SSL.getSslInfoClientCertChain() + i);
							certs[i + 1] = (X509Certificate) cf
									.generateCertificate(new ByteArrayInputStream(
											data));
						}
					}
					if (certs != null) {
						getRequest().setAttribute(SSLSupport.CERTIFICATE_KEY, certs);
					}
					// User key size
					sslO = Integer.valueOf(SSLSocket.getInfoI(socketRef,
							SSL.getSslInfoCipherUsekeysize()));
					getRequest().setAttribute(SSLSupport.KEY_SIZE_KEY, sslO);

					// SSL session ID
					sslO = SSLSocket.getInfoS(socketRef,
							SSL.getSslInfoSessionId());
					if (sslO != null) {
						getRequest().setAttribute(SSLSupport.SESSION_ID_KEY, sslO);
					}
					// TODO provide a hook to enable the SSL session to be
					// invalidated. Set AprEndpoint.SESSION_MGR req attr
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.ssl"), e);
				}
			}
			break;
		}
		case REQ_SSL_CERTIFICATE: {
			if (getEndpoint().isSSLEnabled() && (socketRef != 0)) {
				// Consume and buffer the request body, so that it does not
				// interfere with the client's handshake messages
				InputFilter[] inputFilters = inputBuffer.getFilters();
				((BufferedInputFilter) inputFilters[Constants26
						.getBufferedFilter()]).setLimit(getMaxSavePostSize());
				inputBuffer.addActiveFilter(inputFilters[Constants26
						.getBufferedFilter()]);
				try {
					// Configure connection to require a certificate
					SSLSocket.setVerify(socketRef, SSL.getSslCverifyRequire(),
							((AprEndpoint) getEndpoint()).getSSLVerifyDepth());
					// Renegotiate certificates
					if (SSLSocket.renegotiate(socketRef) == 0) {
						// Don't look for certs unless we know renegotiation
						// worked.
						// Get client certificate and the certificate chain if
						// present
						// certLength == -1 indicates an error
						int certLength = SSLSocket.getInfoI(socketRef,
								SSL.getSslInfoClientCertChain());
						byte[] clientCert = SSLSocket.getInfoB(socketRef,
								SSL.getSslInfoClientCert());
						X509Certificate[] certs = null;
						if (clientCert != null && certLength > -1) {
							certs = new X509Certificate[certLength + 1];
							CertificateFactory cf = CertificateFactory
									.getInstance("X.509");
							certs[0] = (X509Certificate) cf
									.generateCertificate(new ByteArrayInputStream(
											clientCert));
							for (int i = 0; i < certLength; i++) {
								byte[] data = SSLSocket.getInfoB(socketRef,
										SSL.getSslInfoClientCertChain() + i);
								certs[i + 1] = (X509Certificate) cf
										.generateCertificate(new ByteArrayInputStream(
												data));
							}
						}
						if (certs != null) {
							getRequest().setAttribute(SSLSupport.CERTIFICATE_KEY,
									certs);
						}
					}
				} catch (Exception e) {
					log.warn(getSm().getString("http11processor.socket.ssl"), e);
				}
			}
			break;
		}
		case AVAILABLE: {
			getRequest().setAvailable(inputBuffer.available());
			break;
		}
		case COMET_BEGIN: {
			setComet(true);
			break;
		}
		case COMET_END: {
			setComet(false);
			break;
		}
		case COMET_CLOSE: {
			((AprEndpoint) getEndpoint()).processSocketAsync(this.getSocketWrapper(),
					SocketStatus.OPEN_READ);
			break;
		}
		case COMET_SETTIMEOUT: {
			// no op
			break;
		}
		case ASYNC_COMPLETE: {
			if (getAsyncStateMachine().asyncComplete()) {
				((AprEndpoint) getEndpoint()).processSocketAsync(this.getSocketWrapper(),
						SocketStatus.OPEN_READ);
			}
			break;
		}
		case ASYNC_SETTIMEOUT: {
			if (param == null) {
				return;
			}
			long timeout = ((Long) param).longValue();
			getSocketWrapper().setTimeout(timeout);
			break;
		}
		case ASYNC_DISPATCH: {
			if (getAsyncStateMachine().asyncDispatch()) {
				((AprEndpoint) getEndpoint()).processSocketAsync(this.getSocketWrapper(),
						SocketStatus.OPEN_READ);
			}
			break;
		}
		}

	}

	// ------------------------------------------------------ Protected Methods

	@Override
	protected void prepareRequestInternal() {
		sendfileData = null;
	}

	@Override
	public boolean prepareSendfile(OutputFilter[] outputFilters) {
		String fileName = (String) getRequest()
				.getAttribute(Constants24.getSendfileFilenameAttr());
		if (fileName != null) {
			// No entity body sent here
			outputBuffer.addActiveFilter(outputFilters[Constants26
					.getVoidFilter()]);
			setContentDelimitation(true);
			sendfileData = new AprEndpointSendfileData();
			sendfileData.setFileName(fileName);
			sendfileData
					.setStart(((Long) getRequest()
							.getAttribute(Constants24.getSendfileFileStartAttr()))
							.longValue());
			sendfileData
					.setEnd(((Long) getRequest()
							.getAttribute(Constants24.getSendfileFileEndAttr()))
							.longValue());
			return true;
		}
		return false;
	}

	@Override
	protected AbstractInputBuffer<Long> getInputBuffer() {
		return inputBuffer;
	}

	@Override
	protected AbstractOutputBuffer<Long> getOutputBuffer() {
		return outputBuffer;
	}
}
