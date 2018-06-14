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
package org.apache.catalina.valves;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.ServerInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;



/**
 * An implementation of the W3c Extended Log File Format. See
 * http://www.w3.org/TR/WD-logfile.html for more information about the format.
 *
 * The following fields are supported:
 * <ul>
 * <li><code>c-dns</code>:  Client hostname (or ip address if
 * <code>enableLookups</code> for the connector is false)</li>
 * <li><code>c-ip</code>:  Client ip address</li>
 * <li><code>bytes</code>:  bytes served</li>
 * <li><code>cs-method</code>:  request method</li>
 * <li><code>cs-uri</code>:  The full uri requested</li>
 * <li><code>cs-uri-query</code>:  The query string</li>
 * <li><code>cs-uri-stem</code>:  The uri without query string</li>
 * <li><code>date</code>:  The date in yyyy-mm-dd  format for GMT</li>
 * <li><code>s-dns</code>: The server dns entry </li>
 * <li><code>s-ip</code>:  The server ip address</li>
 * <li><code>cs(XXX)</code>:  The value of header XXX from client to server</li>
 * <li><code>sc(XXX)</code>: The value of header XXX from server to client </li>
 * <li><code>sc-status</code>:  The status code</li>
 * <li><code>time</code>:  Time the request was served</li>
 * <li><code>time-taken</code>:  Time (in seconds) taken to serve the request</li>
 * <li><code>x-threadname</code>: Current request thread name (can compare later with stacktraces)</li>
 * <li><code>x-A(XXX)</code>: Pull XXX attribute from the servlet context </li>
 * <li><code>x-C(XXX)</code>: Pull the first cookie of the name XXX </li>
 * <li><code>x-O(XXX)</code>: Pull the all response header values XXX </li>
 * <li><code>x-R(XXX)</code>: Pull XXX attribute from the servlet request </li>
 * <li><code>x-S(XXX)</code>: Pull XXX attribute from the session </li>
 * <li><code>x-P(...)</code>:  Call request.getParameter(...)
 *                             and URLencode it. Helpful to capture
 *                             certain POST parameters.
 * </li>
 * <li>For any of the x-H(...) the following method will be called from the
 *                HttpServletRequest object </li>
 * <li><code>x-H(authType)</code>: getAuthType </li>
 * <li><code>x-H(characterEncoding)</code>: getCharacterEncoding </li>
 * <li><code>x-H(contentLength)</code>: getContentLength </li>
 * <li><code>x-H(locale)</code>:  getLocale</li>
 * <li><code>x-H(protocol)</code>: getProtocol </li>
 * <li><code>x-H(remoteUser)</code>:  getRemoteUser</li>
 * <li><code>x-H(requestedSessionId)</code>: getRequestedSessionId</li>
 * <li><code>x-H(requestedSessionIdFromCookie)</code>:
 *                  isRequestedSessionIdFromCookie </li>
 * <li><code>x-H(requestedSessionIdValid)</code>:
 *                  isRequestedSessionIdValid</li>
 * <li><code>x-H(scheme)</code>:  getScheme</li>
 * <li><code>x-H(secure)</code>:  isSecure</li>
 * </ul>
 *
 *
 *
 * <p>
 * Log rotation can be on or off. This is dictated by the
 * <code>rotatable</code> property.
 * </p>
 *
 * <p>
 * For UNIX users, another field called <code>checkExists</code> is also
 * available. If set to true, the log file's existence will be checked before
 * each logging. This way an external log rotator can move the file
 * somewhere and Tomcat will start with a new file.
 * </p>
 *
 * <p>
 * For JMX junkies, a public method called <code>rotate</code> has
 * been made available to allow you to tell this instance to move
 * the existing log file to somewhere else and start writing a new log file.
 * </p>
 *
 * <p>
 * Conditional logging is also supported. This can be done with the
 * <code>condition</code> property.
 * If the value returned from ServletRequest.getAttribute(condition)
 * yields a non-null value, the logging will be skipped.
 * </p>
 *
 * <p>
 * For extended attributes coming from a getAttribute() call,
 * it is you responsibility to ensure there are no newline or
 * control characters.
 * </p>
 *
 *
 * @author Tim Funk
 * @author Peter Rossbach
 */
public class ExtendedAccessLogValve extends AccessLogValve {

    private static final Log log = LogFactory.getLog(ExtendedAccessLogValve.class);

    // ----------------------------------------------------- Instance Variables


    /**
     * The descriptive information about this implementation.
     */
    private static final String extendedAccessLogInfo =
        "org.apache.catalina.valves.ExtendedAccessLogValve/2.1";


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this implementation.
     */
    @Override
    public String getInfo() {
        return (extendedAccessLogInfo);
    }


    // --------------------------------------------------------- Public Methods


    // -------------------------------------------------------- Private Methods

    /**
     *  Wrap the incoming value into quotes and escape any inner
     *  quotes with double quotes.
     *
     *  @param value - The value to wrap quotes around
     *  @return '-' if empty of null. Otherwise, toString() will
     *     be called on the object and the value will be wrapped
     *     in quotes and any quotes will be escaped with 2
     *     sets of quotes.
     */
    public String wrap(Object value) {
        String svalue;
        // Does the value contain a " ? If so must encode it
        if (value == null || "-".equals(value)) {
            return "-";
        }

        try {
            svalue = value.toString();
            if ("".equals(svalue)) {
                return "-";
            }
        } catch (Throwable e) {
            ExceptionUtils2.handleThrowable(e);
            /* Log error */
            return "-";
        }

        /* Wrap all quotes in double quotes. */
        StringBuilder buffer = new StringBuilder(svalue.length() + 2);
        buffer.append('\'');
        int i = 0;
        while (i < svalue.length()) {
            int j = svalue.indexOf('\'', i);
            if (j == -1) {
                buffer.append(svalue.substring(i));
                i = svalue.length();
            } else {
                buffer.append(svalue.substring(i, j + 1));
                buffer.append('"');
                i = j + 2;
            }
        }

        buffer.append('\'');
        return buffer.toString();
    }

    /**
     * Open the new log file for the date specified by <code>dateStamp</code>.
     */
    @Override
    protected synchronized void open() {
        super.open();
        if (getCurrentLogFile().length()==0) {
            getWriter().println("#Fields: " + getPattern());
            getWriter().println("#Version: 2.0");
            getWriter().println("#Software: " + ServerInfo.getServerInfo());
        }
    }


    @Override
    protected AccessLogValveAccessLogElement[] createLogElements() {
        if (log.isDebugEnabled()) {
            log.debug("decodePattern, pattern =" + getPattern());
        }
        List<AccessLogValveAccessLogElement> list = new ArrayList<AccessLogValveAccessLogElement>();

        ExtendedAccessLogValvePatternTokenizer tokenizer = new ExtendedAccessLogValvePatternTokenizer(getPattern());
        try {

            // Ignore leading whitespace.
            tokenizer.getWhiteSpaces();

            if (tokenizer.isEnded()) {
                log.info("pattern was just empty or whitespace");
                return null;
            }

            String token = tokenizer.getToken();
            while (token != null) {
                if (log.isDebugEnabled()) {
                    log.debug("token = " + token);
                }
                AccessLogValveAccessLogElement element = getLogElement(token, tokenizer);
                if (element == null) {
                    break;
                }
                list.add(element);
                String whiteSpaces = tokenizer.getWhiteSpaces();
                if (whiteSpaces.length() > 0) {
                    list.add(new AccessLogValveStringElement(whiteSpaces));
                }
                if (tokenizer.isEnded()) {
                    break;
                }
                token = tokenizer.getToken();
            }
            if (log.isDebugEnabled()) {
                log.debug("finished decoding with element size of: " + list.size());
            }
            return list.toArray(new AccessLogValveAccessLogElement[0]);
        } catch (IOException e) {
            log.error("parse error", e);
            return null;
        }
    }

    protected AccessLogValveAccessLogElement getLogElement(String token, ExtendedAccessLogValvePatternTokenizer tokenizer) throws IOException {
        if ("date".equals(token)) {
            return new ExtendedAccessLogValveDateElement();
        } else if ("time".equals(token)) {
            if (tokenizer.hasSubToken()) {
                String nextToken = tokenizer.getToken();
                if ("taken".equals(nextToken)) {
                    return new AccessLogValveElapsedTimeElement(false);
                }
            } else {
                return new ExtendedAccessLogValveTimeElement();
            }
        } else if ("bytes".equals(token)) {
            return new AccessLogValveByteSentElement(true);
        } else if ("cached".equals(token)) {
            /* I don't know how to evaluate this! */
            return new AccessLogValveStringElement("-");
        } else if ("c".equals(token)) {
            String nextToken = tokenizer.getToken();
            if ("ip".equals(nextToken)) {
                return new AccessLogValveRemoteAddrElement(this);
            } else if ("dns".equals(nextToken)) {
                return new AccessLogValveHostElement(this);
            }
        } else if ("s".equals(token)) {
            String nextToken = tokenizer.getToken();
            if ("ip".equals(nextToken)) {
                return new AccessLogValveLocalAddrElement();
            } else if ("dns".equals(nextToken)) {
                return new AccessLogValveAccessLogElement() {
                    @Override
                    public void addElement(StringBuilder buf, Date date,
                            Request request, Response response, long time) {
                        String value;
                        try {
                            value = InetAddress.getLocalHost().getHostName();
                        } catch (Throwable e) {
                            ExceptionUtils2.handleThrowable(e);
                            value = "localhost";
                        }
                        buf.append(value);
                    }
                };
            }
        } else if ("cs".equals(token)) {
            return getClientToServerElement(tokenizer);
        } else if ("sc".equals(token)) {
            return getServerToClientElement(tokenizer);
        } else if ("sr".equals(token) || "rs".equals(token)) {
            return getProxyElement(tokenizer);
        } else if ("x".equals(token)) {
            return getXParameterElement(tokenizer);
        }
        log.error("unable to decode with rest of chars starting: " + token);
        return null;
    }

    protected AccessLogValveAccessLogElement getClientToServerElement(
    		ExtendedAccessLogValvePatternTokenizer tokenizer) throws IOException {
        if (tokenizer.hasSubToken()) {
            String token = tokenizer.getToken();
            if ("method".equals(token)) {
                return new AccessLogValveMethodElement();
            } else if ("uri".equals(token)) {
                if (tokenizer.hasSubToken()) {
                    token = tokenizer.getToken();
                    if ("stem".equals(token)) {
                        return new AccessLogValveRequestURIElement();
                    } else if ("query".equals(token)) {
                        return new AccessLogValveAccessLogElement() {
                            @Override
                            public void addElement(StringBuilder buf, Date date,
                                    Request request, Response response,
                                    long time) {
                                String query = request.getQueryString();
                                if (query != null) {
                                    buf.append(query);
                                } else {
                                    buf.append('-');
                                }
                            }
                        };
                    }
                } else {
                    return new AccessLogValveAccessLogElement() {
                        @Override
                        public void addElement(StringBuilder buf, Date date,
                                Request request, Response response, long time) {
                            String query = request.getQueryString();
                            if (query == null) {
                                buf.append(request.getRequestURI());
                            } else {
                                buf.append(request.getRequestURI());
                                buf.append('?');
                                buf.append(request.getQueryString());
                            }
                        }
                    };
                }
            }
        } else if (tokenizer.hasParameter()) {
            String parameter = tokenizer.getParameter();
            if (parameter == null) {
                log.error("No closing ) found for in decode");
                return null;
            }
            return new ExtendedAccessLogValveRequestHeaderElement(this, parameter);
        }
        log.error("The next characters couldn't be decoded: "
                + tokenizer.getRemains());
        return null;
    }

    protected AccessLogValveAccessLogElement getServerToClientElement(
    		ExtendedAccessLogValvePatternTokenizer tokenizer) throws IOException {
        if (tokenizer.hasSubToken()) {
            String token = tokenizer.getToken();
            if ("status".equals(token)) {
                return new AccessLogValveLocalPortElement(this);
            } else if ("comment".equals(token)) {
                return new AccessLogValveStringElement("?");
            }
        } else if (tokenizer.hasParameter()) {
            String parameter = tokenizer.getParameter();
            if (parameter == null) {
                log.error("No closing ) found for in decode");
                return null;
            }
            return new ExtendedAccessLogValveResponseHeaderElement(this, parameter);
        }
        log.error("The next characters couldn't be decoded: "
                + tokenizer.getRemains());
        return null;
    }

    protected AccessLogValveAccessLogElement getProxyElement(ExtendedAccessLogValvePatternTokenizer tokenizer)
        throws IOException {
        String token = null;
        if (tokenizer.hasSubToken()) {
            tokenizer.getToken();
            return new AccessLogValveStringElement("-");
        } else if (tokenizer.hasParameter()) {
            tokenizer.getParameter();
            return new AccessLogValveStringElement("-");
        }
        log.error("The next characters couldn't be decoded: " + token);
        return null;
    }

    protected AccessLogValveAccessLogElement getXParameterElement(ExtendedAccessLogValvePatternTokenizer tokenizer)
            throws IOException {
        if (!tokenizer.hasSubToken()) {
            log.error("x param in wrong format. Needs to be 'x-#(...)' read the docs!");
            return null;
        }
        String token = tokenizer.getToken();
        if ("threadname".equals(token)) {
            return new AccessLogValveThreadNameElement();
        }

        if (!tokenizer.hasParameter()) {
            log.error("x param in wrong format. Needs to be 'x-#(...)' read the docs!");
            return null;
        }
        String parameter = tokenizer.getParameter();
        if (parameter == null) {
            log.error("No closing ) found for in decode");
            return null;
        }
        if ("A".equals(token)) {
            return new ExtendedAccessLogValveServletContextElement(this, parameter);
        } else if ("C".equals(token)) {
            return new ExtendedAccessLogValveCookieElement(this, parameter);
        } else if ("R".equals(token)) {
            return new ExtendedAccessLogValveRequestAttributeElement(this, parameter);
        } else if ("S".equals(token)) {
            return new ExtendedAccessLogValveSessionAttributeElement(this, parameter);
        } else if ("H".equals(token)) {
            return getServletRequestElement(parameter);
        } else if ("P".equals(token)) {
            return new ExtendedAccessLogValveRequestParameterElement(this, parameter);
        } else if ("O".equals(token)) {
            return new ExtendedAccessLogValveResponseAllHeaderElement(this, parameter);
        }
        log.error("x param for servlet request, couldn't decode value: "
                + token);
        return null;
    }

    protected AccessLogValveAccessLogElement getServletRequestElement(String parameter) {
        if ("authType".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(request.getAuthType()));
                }
            };
        } else if ("remoteUser".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(request.getRemoteUser()));
                }
            };
        } else if ("requestedSessionId".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(request.getRequestedSessionId()));
                }
            };
        } else if ("requestedSessionIdFromCookie".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(""
                            + request.isRequestedSessionIdFromCookie()));
                }
            };
        } else if ("requestedSessionIdValid".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap("" + request.isRequestedSessionIdValid()));
                }
            };
        } else if ("contentLength".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap("" + request.getContentLength()));
                }
            };
        } else if ("characterEncoding".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(request.getCharacterEncoding()));
                }
            };
        } else if ("locale".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(request.getLocale()));
                }
            };
        } else if ("protocol".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap(request.getProtocol()));
                }
            };
        } else if ("scheme".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(request.getScheme());
                }
            };
        } else if ("secure".equals(parameter)) {
            return new AccessLogValveAccessLogElement() {
                @Override
                public void addElement(StringBuilder buf, Date date,
                        Request request, Response response, long time) {
                    buf.append(wrap("" + request.isSecure()));
                }
            };
        }
        log.error("x param for servlet request, couldn't decode value: "
                + parameter);
        return null;
    }
}
