/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;

/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of authentication
 * that utilizes SSL certificates to identify client users.
 *
 * @author Craig R. McClanahan
 */

public class SSLAuthenticator extends AuthenticatorBase {

	// ------------------------------------------------------------- Properties

	/**
	 * Descriptive information about this implementation.
	 */
	private static final String info = "org.apache.catalina.authenticator.SSLAuthenticator/1.0";

	/**
	 * Return descriptive information about this Valve implementation.
	 */
	@Override
	public String getInfo() {

		return (info);

	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Authenticate the user by checking for the existence of a certificate
	 * chain, validating it against the trust manager for the connector and then
	 * validating the user's identity against the configured Realm.
	 *
	 * @param request
	 *            Request we are processing
	 * @param response
	 *            Response we are creating
	 * @param config
	 *            Login configuration describing how authentication should be
	 *            performed
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	@Override
	public boolean authenticate(Request request, HttpServletResponse response,
			LoginConfig config) throws IOException {

		// Have we already authenticated someone?
		Principal principal = request.getUserPrincipal();
		// String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
		if (principal != null) {
			if (getContainerLog().isDebugEnabled())
				getContainerLog().debug("Already authenticated '"
						+ principal.getName() + "'");
			// Associate the session with any existing SSO session in order
			// to get coordinated session invalidation at logout
			String ssoId = (String) request
					.getNote(Constants.getReqSsoidNote());
			if (ssoId != null)
				associate(ssoId, request.getSessionInternal(true));
			return (true);
		}

		// NOTE: We don't try to reauthenticate using any existing SSO session,
		// because that will only work if the original authentication was
		// BASIC or FORM, which are less secure than the CLIENT_CERT auth-type
		// specified for this webapp
		//
		// Uncomment below to allow previous FORM or BASIC authentications
		// to authenticate users for this webapp
		// TODO make this a configurable attribute (in SingleSignOn??)
		/*
		 * // Is there an SSO session against which we can try to
		 * reauthenticate? if (ssoId != null) { if (log.isDebugEnabled())
		 * log.debug("SSO Id " + ssoId + " set; attempting " +
		 * "reauthentication"); // Try to reauthenticate using data cached by
		 * SSO. If this fails, // either the original SSO logon was of DIGEST or
		 * SSL (which // we can't reauthenticate ourselves because there is no
		 * // cached username and password), or the realm denied // the user's
		 * reauthentication for some reason. // In either case we have to prompt
		 * the user for a logon if (reauthenticateFromSSO(ssoId, request))
		 * return true; }
		 */

		// Retrieve the certificate chain for this client
		if (getContainerLog().isDebugEnabled())
			getContainerLog().debug(" Looking up certificates");

		X509Certificate certs[] = getRequestCertificates(request);

		if ((certs == null) || (certs.length < 1)) {
			if (getContainerLog().isDebugEnabled())
				getContainerLog()
						.debug("  No certificates included with this request");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, getSm()
					.getString("authenticator.certificates"));
			return false;
		}

		// Authenticate the specified certificate chain
		principal = getContext().getRealm().authenticate(certs);
		if (principal == null) {
			if (getContainerLog().isDebugEnabled())
				getContainerLog().debug("  Realm.authenticate() returned false");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, getSm()
					.getString("authenticator.unauthorized"));
			return (false);
		}

		// Cache the principal (if requested) and record this authentication
		register(request, response, principal,
				HttpServletRequest.CLIENT_CERT_AUTH, null, null);
		return (true);

	}

	@Override
	protected String getAuthMethod() {
		return HttpServletRequest.CLIENT_CERT_AUTH;
	}
}
