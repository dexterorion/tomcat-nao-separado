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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.apache.tomcat.util.security.MD5Encoder;

/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of HTTP DIGEST
 * Authentication (see RFC 2069).
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */

public class DigestAuthenticator extends AuthenticatorBase {

	private static final Log log = LogFactory.getLog(DigestAuthenticator.class);

	// -------------------------------------------------------------- Constants

	/**
	 * The MD5 helper object for this class.
	 *
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	private static final MD5Encoder md5Encoder = new MD5Encoder();

	/**
	 * Descriptive information about this implementation.
	 */
	private static final String info = "org.apache.catalina.authenticator.DigestAuthenticator/1.0";

	/**
	 * Tomcat's DIGEST implementation only supports auth quality of protection.
	 */
	private static final String QOP = "auth";

	// ----------------------------------------------------------- Constructors

	public DigestAuthenticator() {
		super();
		setCache(false);
		try {
			if (md5Helper == null)
				md5Helper = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * MD5 message digest provider.
	 * 
	 * @deprecated Unused - will be removed in Tomcat 8.0.x onwards
	 */
	@Deprecated
	private static volatile MessageDigest md5Helper;

	/**
	 * List of server nonce values currently being tracked
	 */
	private Map<String, DigestAuthenticatorNonceInfo> nonces;

	/**
	 * The last timestamp used to generate a nonce. Each nonce should get a
	 * unique timestamp.
	 */
	private long lastTimestamp = 0;
	private final Object lastTimestampLock = new Object();

	/**
	 * Maximum number of server nonces to keep in the cache. If not specified,
	 * the default value of 1000 is used.
	 */
	private int nonceCacheSize = 1000;

	/**
	 * The window size to use to track seen nonce count values for a given
	 * nonce. If not specified, the default of 100 is used.
	 */
	private int nonceCountWindowSize = 100;

	/**
	 * Private key.
	 */
	private String key = null;

	/**
	 * How long server nonces are valid for in milliseconds. Defaults to 5
	 * minutes.
	 */
	private long nonceValidity = 5 * 60 * 1000;

	/**
	 * Opaque string.
	 */
	private String opaque;

	/**
	 * Should the URI be validated as required by RFC2617? Can be disabled in
	 * reverse proxies where the proxy has modified the URI.
	 */
	private boolean validateUri = true;

	// ------------------------------------------------------------- Properties

	/**
	 * Return descriptive information about this Valve implementation.
	 */
	@Override
	public String getInfo() {

		return (info);

	}

	public int getNonceCountWindowSize() {
		return nonceCountWindowSize;
	}

	public void setNonceCountWindowSize(int nonceCountWindowSize) {
		this.nonceCountWindowSize = nonceCountWindowSize;
	}

	public int getNonceCacheSize() {
		return nonceCacheSize;
	}

	public void setNonceCacheSize(int nonceCacheSize) {
		this.nonceCacheSize = nonceCacheSize;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getNonceValidity() {
		return nonceValidity;
	}

	public void setNonceValidity(long nonceValidity) {
		this.nonceValidity = nonceValidity;
	}

	public String getOpaque() {
		return opaque;
	}

	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}

	public boolean isValidateUri() {
		return validateUri;
	}

	public void setValidateUri(boolean validateUri) {
		this.validateUri = validateUri;
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Authenticate the user making this request, based on the specified login
	 * configuration. Return <code>true</code> if any specified constraint has
	 * been satisfied, or <code>false</code> if we have created a response
	 * challenge already.
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
			if (log.isDebugEnabled())
				log.debug("Already authenticated '" + principal.getName() + "'");
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
		// BASIC or FORM, which are less secure than the DIGEST auth-type
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

		// Validate any credentials already included with this request
		String authorization = request.getHeader("authorization");
		DigestAuthenticatorDigestInfo digestInfo = new DigestAuthenticatorDigestInfo(
				getOpaque(), getNonceValidity(), getKey(), nonces,
				isValidateUri());
		if (authorization != null) {
			if (digestInfo.parse(request, authorization)) {
				if (digestInfo.validate(request, config)) {
					principal = digestInfo
							.authenticate(getContext().getRealm());
				}

				if (principal != null && !digestInfo.isNonceStale()) {
					register(request, response, principal,
							HttpServletRequest.DIGEST_AUTH,
							digestInfo.getUsername(), null);
					return true;
				}
			}
		}

		// Send an "unauthorized" response and an appropriate challenge

		// Next, generate a nonce token (that is a token which is supposed
		// to be unique).
		String nonce = generateNonce(request);

		setAuthenticateHeader(request, response, config, nonce,
				principal != null && digestInfo.isNonceStale());
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		return false;
	}

	@Override
	protected String getAuthMethod() {
		return HttpServletRequest.DIGEST_AUTH;
	}

	// ------------------------------------------------------ Protected Methods

	/**
	 * Parse the username from the specified authorization string. If none can
	 * be identified, return <code>null</code>
	 *
	 * @param authorization
	 *            Authorization string to be parsed
	 *
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	protected String parseUsername(String authorization) {

		// Validate the authorization credentials format
		if (authorization == null)
			return (null);
		if (!authorization.startsWith("Digest "))
			return (null);
		authorization = authorization.substring(7).trim();

		StringTokenizer commaTokenizer = new StringTokenizer(authorization, ",");

		while (commaTokenizer.hasMoreTokens()) {
			String currentToken = commaTokenizer.nextToken();
			int equalSign = currentToken.indexOf('=');
			if (equalSign < 0)
				return null;
			String currentTokenName = currentToken.substring(0, equalSign)
					.trim();
			String currentTokenValue = currentToken.substring(equalSign + 1)
					.trim();
			if ("username".equals(currentTokenName))
				return (removeQuotes(currentTokenValue));
		}

		return (null);

	}

	/**
	 * Removes the quotes on a string. RFC2617 states quotes are optional for
	 * all parameters except realm.
	 */
	protected static String removeQuotes(String quotedString,
			boolean quotesRequired) {
		// support both quoted and non-quoted
		if (quotedString.length() > 0 && quotedString.charAt(0) != '"'
				&& !quotesRequired) {
			return quotedString;
		} else if (quotedString.length() > 2) {
			return quotedString.substring(1, quotedString.length() - 1);
		} else {
			return "";
		}
	}

	/**
	 * Removes the quotes on a string.
	 */
	protected static String removeQuotes(String quotedString) {
		return removeQuotes(quotedString, false);
	}

	/**
	 * Generate a unique token. The token is generated according to the
	 * following pattern. NOnceToken = Base64 ( MD5 ( client-IP ":" time-stamp
	 * ":" private-key ) ).
	 *
	 * @param request
	 *            HTTP Servlet request
	 */
	protected String generateNonce(Request request) {

		long currentTime = System.currentTimeMillis();

		synchronized (lastTimestampLock) {
			if (currentTime > lastTimestamp) {
				lastTimestamp = currentTime;
			} else {
				currentTime = ++lastTimestamp;
			}
		}

		String ipTimeKey = request.getRemoteAddr() + ":" + currentTime + ":"
				+ getKey();

		byte[] buffer = ConcurrentMessageDigest.digestMD5(ipTimeKey
				.getBytes(B2CConverter.getIso88591()));
		String nonce = currentTime + ":" + MD5Encoder.encode(buffer);

		DigestAuthenticatorNonceInfo info = new DigestAuthenticatorNonceInfo(
				currentTime, getNonceCountWindowSize());
		synchronized (nonces) {
			nonces.put(nonce, info);
		}

		return nonce;
	}

/**
     * Generates the WWW-Authenticate header.
     * <p>
     * The header MUST follow this template :
     * <pre>
     *      WWW-Authenticate    = "WWW-Authenticate" ":" "Digest"
     *                            digest-challenge
     *
     *      digest-challenge    = 1#( realm | [ domain ] | nonce |
     *                  [ digest-opaque ] |[ stale ] | [ algorithm ] )
     *
     *      realm               = "realm" "=" realm-value
     *      realm-value         = quoted-string
     *      domain              = "domain" "=" <"> 1#URI <">
     *      nonce               = "nonce" "=" nonce-value
     *      nonce-value         = quoted-string
     *      opaque              = "opaque" "=" quoted-string
     *      stale               = "stale" "=" ( "true" | "false" )
     *      algorithm           = "algorithm" "=" ( "MD5" | token )
     * </pre>
     *
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response
     * @param config    Login configuration describing how authentication
     *              should be performed
     * @param nonce nonce token
     */
	protected void setAuthenticateHeader(HttpServletRequest request,
			HttpServletResponse response, LoginConfig config, String nonce,
			boolean isNonceStale) {

		// Get the realm name
		String realmName = config.getRealmName();
		if (realmName == null)
			realmName = getRealmName();

		String authenticateHeader;
		if (isNonceStale) {
			authenticateHeader = "Digest realm=\"" + realmName + "\", "
					+ "qop=\"" + QOP + "\", nonce=\"" + nonce + "\", "
					+ "opaque=\"" + getOpaque() + "\", stale=true";
		} else {
			authenticateHeader = "Digest realm=\"" + realmName + "\", "
					+ "qop=\"" + QOP + "\", nonce=\"" + nonce + "\", "
					+ "opaque=\"" + getOpaque() + "\"";
		}

		response.setHeader(getAuthHeaderName(), authenticateHeader);

	}

	// ------------------------------------------------------- Lifecycle Methods

	@Override
	protected synchronized void startInternal() throws LifecycleException {
		super.startInternal();

		// Generate a random secret key
		if (getKey() == null) {
			setKey(getSessionIdGenerator().generateSessionId());
		}

		// Generate the opaque string the same way
		if (getOpaque() == null) {
			setOpaque(getSessionIdGenerator().generateSessionId());
		}

		nonces = new LinkedHashMap<String, DigestAuthenticatorNonceInfo>() {

			private static final long serialVersionUID = 1L;
			private static final long LOG_SUPPRESS_TIME = 5 * 60 * 1000;

			private long lastLog = 0;

			@Override
			protected boolean removeEldestEntry(
					Map.Entry<String, DigestAuthenticatorNonceInfo> eldest) {
				// This is called from a sync so keep it simple
				long currentTime = System.currentTimeMillis();
				if (size() > getNonceCacheSize()) {
					if (lastLog < currentTime
							&& currentTime - eldest.getValue().getTimestamp() < getNonceValidity()) {
						// Replay attack is possible
						log.warn(getSm().getString(
								"digestAuthenticator.cacheRemove"));
						lastLog = currentTime + LOG_SUPPRESS_TIME;
					}
					return true;
				}
				return false;
			}
		};
	}

	public static String getQop() {
		return QOP;
	}

}
