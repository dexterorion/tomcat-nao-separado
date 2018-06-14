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
package org.apache.catalina.filters;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Provides basic CSRF protection for a web application. The filter assumes
 * that:
 * <ul>
 * <li>The filter is mapped to /*</li>
 * <li>{@link HttpServletResponse#encodeRedirectURL(String)} and
 * {@link HttpServletResponse#encodeURL(String)} are used to encode all URLs
 * returned to the client
 * </ul>
 */
public class CsrfPreventionFilter extends FilterBase {

    private static final Log log =
        LogFactory.getLog(CsrfPreventionFilter.class);

    private String randomClass = SecureRandom.class.getName();

    private Random randomSource;

    private int denyStatus = HttpServletResponse.SC_FORBIDDEN;

    private final Set<String> entryPoints = new HashSet<String>();

    private int nonceCacheSize = 5;

    @Override
    protected Log getLogger() {
        return log;
    }

    /**
     * Return response status code that is used to reject denied request.
     */
    public int getDenyStatus() {
        return denyStatus;
    }

    /**
     * Set response status code that is used to reject denied request. If none
     * set, the default value of 403 will be used.
     *
     * @param denyStatus
     *            HTTP status code
     */
    public void setDenyStatus(int denyStatus) {
        this.denyStatus = denyStatus;
    }

    /**
     * Entry points are URLs that will not be tested for the presence of a valid
     * nonce. They are used to provide a way to navigate back to a protected
     * application after navigating away from it. Entry points will be limited
     * to HTTP GET requests and should not trigger any security sensitive
     * actions.
     *
     * @param entryPoints   Comma separated list of URLs to be configured as
     *                      entry points.
     */
    public void setEntryPoints(String entryPoints) {
        String values[] = entryPoints.split(",");
        for (String value : values) {
            this.entryPoints.add(value.trim());
        }
    }

    /**
     * Sets the number of previously issued nonces that will be cached on a LRU
     * basis to support parallel requests, limited use of the refresh and back
     * in the browser and similar behaviors that may result in the submission
     * of a previous nonce rather than the current one. If not set, the default
     * value of 5 will be used.
     *
     * @param nonceCacheSize    The number of nonces to cache
     */
    public void setNonceCacheSize(int nonceCacheSize) {
        this.nonceCacheSize = nonceCacheSize;
    }

    /**
     * Specify the class to use to generate the nonces. Must be in instance of
     * {@link Random}.
     *
     * @param randomClass   The name of the class to use
     */
    public void setRandomClass(String randomClass) {
        this.randomClass = randomClass;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Set the parameters
        super.init(filterConfig);

        try {
            Class<?> clazz = Class.forName(randomClass);
            randomSource = (Random) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            ServletException se = new ServletException(getSm().getString(
                    "csrfPrevention.invalidRandomClass", randomClass), e);
            throw se;
        } catch (InstantiationException e) {
            ServletException se = new ServletException(getSm().getString(
                    "csrfPrevention.invalidRandomClass", randomClass), e);
            throw se;
        } catch (IllegalAccessException e) {
            ServletException se = new ServletException(getSm().getString(
                    "csrfPrevention.invalidRandomClass", randomClass), e);
            throw se;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        ServletResponse wResponse = null;

        if (request instanceof HttpServletRequest &&
                response instanceof HttpServletResponse) {

            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            boolean skipNonceCheck = false;

            if (Constants5.getMethodGet().equals(req.getMethod())) {
                String path = req.getServletPath();
                if (req.getPathInfo() != null) {
                    path = path + req.getPathInfo();
                }

                if (entryPoints.contains(path)) {
                    skipNonceCheck = true;
                }
            }

            HttpSession session = req.getSession(false);

            @SuppressWarnings("unchecked")
            CsrfPreventionFilterLruCache<String> nonceCache = (session == null) ? null
                    : (CsrfPreventionFilterLruCache<String>) session.getAttribute(
                            Constants5.getCsrfNonceSessionAttrName());

            if (!skipNonceCheck) {
                String previousNonce =
                    req.getParameter(Constants5.getCsrfNonceRequestParam());

                if (nonceCache == null || previousNonce == null ||
                        !nonceCache.contains(previousNonce)) {
                    res.sendError(denyStatus);
                    return;
                }
            }

            if (nonceCache == null) {
                nonceCache = new CsrfPreventionFilterLruCache<String>(nonceCacheSize);
                if (session == null) {
                    session = req.getSession(true);
                }
                session.setAttribute(
                        Constants5.getCsrfNonceSessionAttrName(), nonceCache);
            }

            String newNonce = generateNonce();

            nonceCache.add(newNonce);

            wResponse = new CsrfPreventionFilterCsrfResponseWrapper(res, newNonce);
        } else {
            wResponse = response;
        }

        chain.doFilter(request, wResponse);
    }


    @Override
    protected boolean isConfigProblemFatal() {
        return true;
    }


    /**
     * Generate a once time token (nonce) for authenticating subsequent
     * requests. This will also add the token to the session. The nonce
     * generation is a simplified version of ManagerBase.generateSessionId().
     *
     */
    protected String generateNonce() {
        byte random[] = new byte[16];

        // Render the result as a String of hexadecimal digits
        StringBuilder buffer = new StringBuilder();

        randomSource.nextBytes(random);

        for (int j = 0; j < random.length; j++) {
            byte b1 = (byte) ((random[j] & 0xf0) >> 4);
            byte b2 = (byte) (random[j] & 0x0f);
            if (b1 < 10) {
                buffer.append((char) ('0' + b1));
            } else {
                buffer.append((char) ('A' + (b1 - 10)));
            }
            if (b2 < 10) {
                buffer.append((char) ('0' + b2));
            } else {
                buffer.append((char) ('A' + (b2 - 10)));
            }
        }

        return buffer.toString();
    }
}
