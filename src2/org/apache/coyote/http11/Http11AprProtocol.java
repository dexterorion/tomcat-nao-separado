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

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.AbstractEndpointHandler;
import org.apache.tomcat.util.net.AprEndpoint;

/**
 * Abstract the protocol implementation, including threading, etc. Processor is
 * single threaded and specific to stream-based protocols, will not fit Jk
 * protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class Http11AprProtocol extends AbstractHttp11Protocol<Long> {

	private static final Log log = LogFactory.getLog(Http11AprProtocol.class);

	@Override
	protected Log getLog() {
		return log;
	}
	
	public static Log getLogVariable(){
		return log;
	}

	@Override
	protected AbstractEndpointHandler getHandler() {
		return cHandler;
	}

	@Override
	public boolean isAprRequired() {
		// Override since this protocol implementation requires the APR/native
		// library
		return true;
	}

	public Http11AprProtocol() {
		setEndpoint(new AprEndpoint());
		cHandler = new Http11AprProtocolHttp11ConnectionHandler(this);
		((AprEndpoint) getEndpoint()).setHandler(cHandler);
		setSoLinger(Constants26.getDefaultConnectionLinger());
		setSoTimeout(Constants26.getDefaultConnectionTimeout());
		setTcpNoDelay(Constants26.isDefaultTcpNoDelay());
	}

	private final Http11AprProtocolHttp11ConnectionHandler cHandler;

	public boolean getUseSendfile() {
		return ((AprEndpoint) getEndpoint()).getUseSendfile();
	}

	public void setUseSendfile(boolean useSendfile) {
		((AprEndpoint) getEndpoint()).setUseSendfile(useSendfile);
	}

	public int getPollTime() {
		return ((AprEndpoint) getEndpoint()).getPollTime();
	}

	public void setPollTime(int pollTime) {
		((AprEndpoint) getEndpoint()).setPollTime(pollTime);
	}

	public void setPollerSize(int pollerSize) {
		getEndpoint().setMaxConnections(pollerSize);
	}

	public int getPollerSize() {
		return getEndpoint().getMaxConnections();
	}

	public int getSendfileSize() {
		return ((AprEndpoint) getEndpoint()).getSendfileSize();
	}

	public void setSendfileSize(int sendfileSize) {
		((AprEndpoint) getEndpoint()).setSendfileSize(sendfileSize);
	}

	public void setSendfileThreadCount(int sendfileThreadCount) {
		((AprEndpoint) getEndpoint()).setSendfileThreadCount(sendfileThreadCount);
	}

	public int getSendfileThreadCount() {
		return ((AprEndpoint) getEndpoint()).getSendfileThreadCount();
	}

	public boolean getDeferAccept() {
		return ((AprEndpoint) getEndpoint()).getDeferAccept();
	}

	public void setDeferAccept(boolean deferAccept) {
		((AprEndpoint) getEndpoint()).setDeferAccept(deferAccept);
	}

	// -------------------- SSL related properties --------------------

	/**
	 * SSL protocol.
	 */
	public String getSSLProtocol() {
		return ((AprEndpoint) getEndpoint()).getSSLProtocol();
	}

	public void setSSLProtocol(String SSLProtocol) {
		((AprEndpoint) getEndpoint()).setSSLProtocol(SSLProtocol);
	}

	/**
	 * SSL password (if a cert is encrypted, and no password has been provided,
	 * a callback will ask for a password).
	 */
	public String getSSLPassword() {
		return ((AprEndpoint) getEndpoint()).getSSLPassword();
	}

	public void setSSLPassword(String SSLPassword) {
		((AprEndpoint) getEndpoint()).setSSLPassword(SSLPassword);
	}

	/**
	 * SSL cipher suite.
	 */
	public String getSSLCipherSuite() {
		return ((AprEndpoint) getEndpoint()).getSSLCipherSuite();
	}

	public void setSSLCipherSuite(String SSLCipherSuite) {
		((AprEndpoint) getEndpoint()).setSSLCipherSuite(SSLCipherSuite);
	}

	/**
	 * SSL honor cipher order.
	 *
	 * Set to <code>true</code> to enforce the <i>server's</i> cipher order
	 * instead of the default which is to allow the client to choose a preferred
	 * cipher.
	 */
	public boolean getSSLHonorCipherOrder() {
		return ((AprEndpoint) getEndpoint()).getSSLHonorCipherOrder();
	}

	public void setSSLHonorCipherOrder(boolean SSLHonorCipherOrder) {
		((AprEndpoint) getEndpoint()).setSSLHonorCipherOrder(SSLHonorCipherOrder);
	}

	/**
	 * SSL certificate file.
	 */
	public String getSSLCertificateFile() {
		return ((AprEndpoint) getEndpoint()).getSSLCertificateFile();
	}

	public void setSSLCertificateFile(String SSLCertificateFile) {
		((AprEndpoint) getEndpoint()).setSSLCertificateFile(SSLCertificateFile);
	}

	/**
	 * SSL certificate key file.
	 */
	public String getSSLCertificateKeyFile() {
		return ((AprEndpoint) getEndpoint()).getSSLCertificateKeyFile();
	}

	public void setSSLCertificateKeyFile(String SSLCertificateKeyFile) {
		((AprEndpoint) getEndpoint())
				.setSSLCertificateKeyFile(SSLCertificateKeyFile);
	}

	/**
	 * SSL certificate chain file.
	 */
	public String getSSLCertificateChainFile() {
		return ((AprEndpoint) getEndpoint()).getSSLCertificateChainFile();
	}

	public void setSSLCertificateChainFile(String SSLCertificateChainFile) {
		((AprEndpoint) getEndpoint())
				.setSSLCertificateChainFile(SSLCertificateChainFile);
	}

	/**
	 * SSL CA certificate path.
	 */
	public String getSSLCACertificatePath() {
		return ((AprEndpoint) getEndpoint()).getSSLCACertificatePath();
	}

	public void setSSLCACertificatePath(String SSLCACertificatePath) {
		((AprEndpoint) getEndpoint()).setSSLCACertificatePath(SSLCACertificatePath);
	}

	/**
	 * SSL CA certificate file.
	 */
	public String getSSLCACertificateFile() {
		return ((AprEndpoint) getEndpoint()).getSSLCACertificateFile();
	}

	public void setSSLCACertificateFile(String SSLCACertificateFile) {
		((AprEndpoint) getEndpoint()).setSSLCACertificateFile(SSLCACertificateFile);
	}

	/**
	 * SSL CA revocation path.
	 */
	public String getSSLCARevocationPath() {
		return ((AprEndpoint) getEndpoint()).getSSLCARevocationPath();
	}

	public void setSSLCARevocationPath(String SSLCARevocationPath) {
		((AprEndpoint) getEndpoint()).setSSLCARevocationPath(SSLCARevocationPath);
	}

	/**
	 * SSL CA revocation file.
	 */
	public String getSSLCARevocationFile() {
		return ((AprEndpoint) getEndpoint()).getSSLCARevocationFile();
	}

	public void setSSLCARevocationFile(String SSLCARevocationFile) {
		((AprEndpoint) getEndpoint()).setSSLCARevocationFile(SSLCARevocationFile);
	}

	/**
	 * SSL verify client.
	 */
	public String getSSLVerifyClient() {
		return ((AprEndpoint) getEndpoint()).getSSLVerifyClient();
	}

	public void setSSLVerifyClient(String SSLVerifyClient) {
		((AprEndpoint) getEndpoint()).setSSLVerifyClient(SSLVerifyClient);
	}

	/**
	 * SSL verify depth.
	 */
	public int getSSLVerifyDepth() {
		return ((AprEndpoint) getEndpoint()).getSSLVerifyDepth();
	}

	public void setSSLVerifyDepth(int SSLVerifyDepth) {
		((AprEndpoint) getEndpoint()).setSSLVerifyDepth(SSLVerifyDepth);
	}

	/**
	 * Disable SSL compression.
	 */
	public boolean getSSLDisableCompression() {
		return ((AprEndpoint) getEndpoint()).getSSLDisableCompression();
	}

	public void setSSLDisableCompression(boolean disable) {
		((AprEndpoint) getEndpoint()).setSSLDisableCompression(disable);
	}

	// ----------------------------------------------------- JMX related methods

	@Override
	protected String getNamePrefix() {
		return ("http-apr");
	}

	public Http11AprProtocolHttp11ConnectionHandler getcHandler() {
		return cHandler;
	}

}
