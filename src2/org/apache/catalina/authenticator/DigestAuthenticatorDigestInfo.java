package org.apache.catalina.authenticator;

import java.io.IOException;
import java.io.StringReader;
import java.security.Principal;
import java.util.Map;

import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.apache.tomcat.util.security.MD5Encoder;

public class DigestAuthenticatorDigestInfo {

    private final String opaque;
    private final long nonceValidity;
    private final String key;
    private final Map<String,DigestAuthenticatorNonceInfo> nonces;
    private boolean validateUri = true;

    private String userName = null;
    private String method = null;
    private String uri = null;
    private String response = null;
    private String nonce = null;
    private String nc = null;
    private String cnonce = null;
    private String realmName = null;
    private String qop = null;
    private String opaqueReceived = null;

    private boolean nonceStale = false;


    public DigestAuthenticatorDigestInfo(String opaque, long nonceValidity, String key,
            Map<String,DigestAuthenticatorNonceInfo> nonces, boolean validateUri) {
        this.opaque = opaque;
        this.nonceValidity = nonceValidity;
        this.key = key;
        this.nonces = nonces;
        this.validateUri = validateUri;
    }


    public String getUsername() {
        return userName;
    }


    public boolean parse(Request request, String authorization) {
        // Validate the authorization credentials format
        if (authorization == null) {
            return false;
        }

        Map<String,String> directives;
        try {
            directives = HttpParser.parseAuthorizationDigest(
                    new StringReader(authorization));
        } catch (IOException e) {
            return false;
        }

        if (directives == null) {
            return false;
        }

        method = request.getMethod();
        userName = directives.get("username");
        realmName = directives.get("realm");
        nonce = directives.get("nonce");
        nc = directives.get("nc");
        cnonce = directives.get("cnonce");
        qop = directives.get("qop");
        uri = directives.get("uri");
        response = directives.get("response");
        opaqueReceived = directives.get("opaque");

        return true;
    }

    public boolean validate(Request request, LoginConfig config) {
        if ( (userName == null) || (realmName == null) || (nonce == null)
             || (uri == null) || (response == null) ) {
            return false;
        }

        // Validate the URI - should match the request line sent by client
        if (validateUri) {
            String uriQuery;
            String query = request.getQueryString();
            if (query == null) {
                uriQuery = request.getRequestURI();
            } else {
                uriQuery = request.getRequestURI() + "?" + query;
            }
            if (!uri.equals(uriQuery)) {
                // Some clients (older Android) use an absolute URI for
                // DIGEST but a relative URI in the request line.
                // request. 2.3.5 < fixed Android version <= 4.0.3
                String host = request.getHeader("host");
                String scheme = request.getScheme();
                if (host != null && !uriQuery.startsWith(scheme)) {
                    StringBuilder absolute = new StringBuilder();
                    absolute.append(scheme);
                    absolute.append("://");
                    absolute.append(host);
                    absolute.append(uriQuery);
                    if (!uri.equals(absolute.toString())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        // Validate the Realm name
        String lcRealm = config.getRealmName();
        if (lcRealm == null) {
            lcRealm = DigestAuthenticator.getRealmName();
        }
        if (!lcRealm.equals(realmName)) {
            return false;
        }

        // Validate the opaque string
        if (!opaque.equals(opaqueReceived)) {
            return false;
        }

        // Validate nonce
        int i = nonce.indexOf(":");
        if (i < 0 || (i + 1) == nonce.length()) {
            return false;
        }
        long nonceTime;
        try {
            nonceTime = Long.parseLong(nonce.substring(0, i));
        } catch (NumberFormatException nfe) {
            return false;
        }
        String md5clientIpTimeKey = nonce.substring(i + 1);
        long currentTime = System.currentTimeMillis();
        if ((currentTime - nonceTime) > nonceValidity) {
            nonceStale = true;
            synchronized (nonces) {
                nonces.remove(nonce);
            }
        }
        String serverIpTimeKey =
            request.getRemoteAddr() + ":" + nonceTime + ":" + key;
        byte[] buffer = ConcurrentMessageDigest.digestMD5(
                serverIpTimeKey.getBytes(B2CConverter.getIso88591()));
        String md5ServerIpTimeKey = MD5Encoder.encode(buffer);
        if (!md5ServerIpTimeKey.equals(md5clientIpTimeKey)) {
            return false;
        }

        // Validate qop
        if (qop != null && !DigestAuthenticator.getQop().equals(qop)) {
            return false;
        }

        // Validate cnonce and nc
        // Check if presence of nc and Cnonce is consistent with presence of qop
        if (qop == null) {
            if (cnonce != null || nc != null) {
                return false;
            }
        } else {
            if (cnonce == null || nc == null) {
                return false;
            }
            // RFC 2617 says nc must be 8 digits long. Older Android clients
            // use 6. 2.3.5 < fixed Android version <= 4.0.3
            if (nc.length() < 6 || nc.length() > 8) {
                return false;
            }
            long count;
            try {
                count = Long.parseLong(nc, 16);
            } catch (NumberFormatException nfe) {
                return false;
            }
            DigestAuthenticatorNonceInfo info;
            synchronized (nonces) {
                info = nonces.get(nonce);
            }
            if (info == null) {
                // Nonce is valid but not in cache. It must have dropped out
                // of the cache - force a re-authentication
                nonceStale = true;
            } else {
                if (!info.nonceCountValid(count)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isNonceStale() {
        return nonceStale;
    }

    public Principal authenticate(Realm realm) {
        // Second MD5 digest used to calculate the digest :
        // MD5(Method + ":" + uri)
        String a2 = method + ":" + uri;

        byte[] buffer = ConcurrentMessageDigest.digestMD5(
                a2.getBytes(B2CConverter.getIso88591()));
        String md5a2 = MD5Encoder.encode(buffer);

        return realm.authenticate(userName, response, nonce, nc, cnonce,
                qop, realmName, md5a2);
    }

}