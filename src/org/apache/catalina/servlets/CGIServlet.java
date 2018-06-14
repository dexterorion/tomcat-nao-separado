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

package org.apache.catalina.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * CGI-invoking servlet for web applications, used to execute scripts which
 * comply to the Common Gateway Interface (CGI) specification and are named in
 * the path-info used to invoke this servlet.
 *
 * <p>
 * <i>Note: This code compiles and even works for simple CGI cases. Exhaustive
 * testing has not been done. Please consider it beta quality. Feedback is
 * appreciated to the author (see below).</i>
 * </p>
 * <p>
 *
 * <b>Example</b>:<br>
 * If an instance of this servlet was mapped (using
 * <code>&lt;web-app&gt;/WEB-INF/web.xml</code>) to:
 * </p>
 * <p>
 * <code>
 * &lt;web-app&gt;/cgi-bin/*
 * </code>
 * </p>
 * <p>
 * then the following request:
 * </p>
 * <p>
 * <code>
 * http://localhost:8080/&lt;web-app&gt;/cgi-bin/dir1/script/pathinfo1
 * </code>
 * </p>
 * <p>
 * would result in the execution of the script
 * </p>
 * <p>
 * <code>
 * &lt;web-app-root&gt;/WEB-INF/cgi/dir1/script
 * </code>
 * </p>
 * <p>
 * with the script's <code>PATH_INFO</code> set to <code>/pathinfo1</code>.
 * </p>
 * <p>
 * Recommendation: House all your CGI scripts under
 * <code>&lt;webapp&gt;/WEB-INF/cgi</code>. This will ensure that you do not
 * accidentally expose your cgi scripts' code to the outside world and that your
 * cgis will be cleanly ensconced underneath the WEB-INF (i.e., non-content)
 * area.
 * </p>
 * <p>
 * The default CGI location is mentioned above. You have the flexibility to put
 * CGIs wherever you want, however:
 * </p>
 * <p>
 * The CGI search path will start at webAppRootDir + File.separator +
 * cgiPathPrefix (or webAppRootDir alone if cgiPathPrefix is null).
 * </p>
 * <p>
 * cgiPathPrefix is defined by setting this servlet's cgiPathPrefix init
 * parameter
 * </p>
 *
 * <p>
 *
 * <B>CGI Specification</B>:<br>
 * derived from <a
 * href="http://cgi-spec.golux.com">http://cgi-spec.golux.com</a>. A
 * work-in-progress & expired Internet Draft. Note no actual RFC describing the
 * CGI specification exists. Where the behavior of this servlet differs from the
 * specification cited above, it is either documented here, a bug, or an
 * instance where the specification cited differs from Best Community Practice
 * (BCP). Such instances should be well-documented here. Please email the <a
 * href="mailto:dev@tomcat.apache.org">Tomcat group [dev@tomcat.apache.org]</a>
 * with amendments.
 *
 * </p>
 * <p>
 *
 * <b>Canonical metavariables</b>:<br>
 * The CGI specification defines the following canonical metavariables: <br>
 * [excerpt from CGI specification]
 * 
 * <PRE>
 *  AUTH_TYPE
 *  CONTENT_LENGTH
 *  CONTENT_TYPE
 *  GATEWAY_INTERFACE
 *  PATH_INFO
 *  PATH_TRANSLATED
 *  QUERY_STRING
 *  REMOTE_ADDR
 *  REMOTE_HOST
 *  REMOTE_IDENT
 *  REMOTE_USER
 *  REQUEST_METHOD
 *  SCRIPT_NAME
 *  SERVER_NAME
 *  SERVER_PORT
 *  SERVER_PROTOCOL
 *  SERVER_SOFTWARE
 * </PRE>
 * <p>
 * Metavariables with names beginning with the protocol name (<EM>e.g.</EM>,
 * "HTTP_ACCEPT") are also canonical in their description of request header
 * fields. The number and meaning of these fields may change independently of
 * this specification. (See also section 6.1.5 [of the CGI specification].)
 * </p>
 * [end excerpt]
 *
 * </p> <h2>Implementation notes</h2>
 * <p>
 *
 * <b>standard input handling</b>: If your script accepts standard input, then
 * the client must start sending input within a certain timeout period,
 * otherwise the servlet will assume no input is coming and carry on running the
 * script. The script's the standard input will be closed and handling of any
 * further input from the client is undefined. Most likely it will be ignored.
 * If this behavior becomes undesirable, then this servlet needs to be enhanced
 * to handle threading of the spawned process' stdin, stdout, and stderr (which
 * should not be too hard). <br>
 * If you find your cgi scripts are timing out receiving input, you can set the
 * init parameter <code></code> of your webapps' cgi-handling servlet to be
 * </p>
 * <p>
 *
 * <b>Metavariable Values</b>: According to the CGI specification,
 * implementations may choose to represent both null or missing values in an
 * implementation-specific manner, but must define that manner. This
 * implementation chooses to always define all required metavariables, but set
 * the value to "" for all metavariables whose value is either null or
 * undefined. PATH_TRANSLATED is the sole exception to this rule, as per the CGI
 * Specification.
 *
 * </p>
 * <p>
 *
 * <b>NPH -- Non-parsed-header implementation</b>: This implementation does not
 * support the CGI NPH concept, whereby server ensures that the data supplied to
 * the script are precisely as supplied by the client and unaltered by the
 * server.
 * </p>
 * <p>
 * The function of a servlet container (including Tomcat) is specifically
 * designed to parse and possible alter CGI-specific variables, and as such
 * makes NPH functionality difficult to support.
 * </p>
 * <p>
 * The CGI specification states that compliant servers MAY support NPH output.
 * It does not state servers MUST support NPH output to be unconditionally
 * compliant. Thus, this implementation maintains unconditional compliance with
 * the specification though NPH support is not present.
 * </p>
 * <p>
 *
 * The CGI specification is located at <a
 * href="http://cgi-spec.golux.com">http://cgi-spec.golux.com</a>.
 *
 * </p>
 * <p>
 * <h3>TODO:</h3>
 * <ul>
 * <li>Support for setting headers (for example, Location headers don't work)
 * <li>Support for collapsing multiple header lines (per RFC 2616)
 * <li>Ensure handling of POST method does not interfere with 2.3 Filters
 * <li>Refactor some debug code out of core
 * <li>Ensure header handling preserves encoding
 * <li>Possibly rewrite CGIRunner.run()?
 * <li>Possibly refactor CGIRunner and CGIEnvironment as non-inner classes?
 * <li>Document handling of cgi stdin when there is no stdin
 * <li>Revisit IOException handling in CGIRunner.run()
 * <li>Better documentation
 * <li>Confirm use of ServletInputStream.available() in CGIRunner.run() is not
 * needed
 * <li>[add more to this TODO list]
 * </ul>
 * </p>
 *
 * @author Martin T Dengler [root@martindengler.com]
 * @author Amy Roh
 * @since Tomcat 4.0
 */
public final class CGIServlet extends HttpServlet {

	/* some vars below copied from Craig R. McClanahan's InvokerServlet */

	private static final long serialVersionUID = 1L;

	/** the debugging detail level for this servlet. */
	private int debug = 0;

	/**
	 * The CGI search path will start at webAppRootDir + File.separator +
	 * cgiPathPrefix (or webAppRootDir alone if cgiPathPrefix is null)
	 */
	private String cgiPathPrefix = null;

	/** the executable to use with the script */
	private String cgiExecutable = "perl";

	/** additional arguments for the executable */
	private List<String> cgiExecutableArgs = null;

	/** the encoding to use for parameters */
	private String parameterEncoding = System.getProperty("file.encoding",
			"UTF-8");

	/**
	 * The time (in milliseconds) to wait for the reading of stderr to complete
	 * before terminating the CGI process.
	 */
	private long stderrTimeout = 2000;

	/** object used to ensure multiple threads don't try to expand same file */
	private static Object expandFileLock = new Object();

	/** the shell environment variables to be passed to the CGI script */
	private Hashtable<String, String> shellEnv = new Hashtable<String, String>();

	/**
	 * Sets instance variables.
	 * <P>
	 * Modified from Craig R. McClanahan's InvokerServlet
	 * </P>
	 *
	 * @param config
	 *            a <code>ServletConfig</code> object containing the servlet's
	 *            configuration and initialization parameters
	 *
	 * @exception ServletException
	 *                if an exception has occurred that interferes with the
	 *                servlet's normal operation
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);

		// Set our properties from the initialization parameters
		if (getServletConfig().getInitParameter("debug") != null)
			debug = Integer.parseInt(getServletConfig().getInitParameter(
					"debug"));
		setCgiPathPrefix(getServletConfig().getInitParameter("cgiPathPrefix"));
		boolean passShellEnvironment = Boolean.valueOf(
				getServletConfig().getInitParameter("passShellEnvironment"))
				.booleanValue();

		if (passShellEnvironment) {
			shellEnv.putAll(System.getenv());
		}

		if (getServletConfig().getInitParameter("executable") != null) {
			setCgiExecutable(getServletConfig().getInitParameter("executable"));
		}

		if (getServletConfig().getInitParameter("executable-arg-1") != null) {
			List<String> args = new ArrayList<String>();
			for (int i = 1;; i++) {
				String arg = getServletConfig().getInitParameter(
						"executable-arg-" + i);
				if (arg == null) {
					break;
				}
				args.add(arg);
			}
			setCgiExecutableArgs(args);
		}

		if (getServletConfig().getInitParameter("parameterEncoding") != null) {
			setParameterEncoding(getServletConfig().getInitParameter(
					"parameterEncoding"));
		}

		if (getServletConfig().getInitParameter("stderrTimeout") != null) {
			setStderrTimeout(Long.parseLong(getServletConfig()
					.getInitParameter("stderrTimeout")));
		}

	}

	/**
	 * Prints out important Servlet API and container information
	 *
	 * <p>
	 * Copied from SnoopAllServlet by Craig R. McClanahan
	 * </p>
	 *
	 * @param out
	 *            ServletOutputStream as target of the information
	 * @param req
	 *            HttpServletRequest object used as source of information
	 * @param res
	 *            HttpServletResponse object currently not used but could
	 *            provide future information
	 *
	 * @exception IOException
	 *                if a write operation exception occurs
	 *
	 */
	protected void printServletEnvironment(ServletOutputStream out,
			HttpServletRequest req, HttpServletResponse res) throws IOException {

		// Document the properties from ServletRequest
		out.println("<h1>ServletRequest Properties</h1>");
		out.println("<ul>");
		Enumeration<String> attrs = req.getAttributeNames();
		while (attrs.hasMoreElements()) {
			String attr = attrs.nextElement();
			out.println("<li><b>attribute</b> " + attr + " = "
					+ req.getAttribute(attr));
		}
		out.println("<li><b>characterEncoding</b> = "
				+ req.getCharacterEncoding());
		out.println("<li><b>contentLength</b> = " + req.getContentLength());
		out.println("<li><b>contentType</b> = " + req.getContentType());
		Enumeration<Locale> locales = req.getLocales();
		while (locales.hasMoreElements()) {
			Locale locale = locales.nextElement();
			out.println("<li><b>locale</b> = " + locale);
		}
		Enumeration<String> params = req.getParameterNames();
		while (params.hasMoreElements()) {
			String param = params.nextElement();
			String values[] = req.getParameterValues(param);
			for (int i = 0; i < values.length; i++)
				out.println("<li><b>parameter</b> " + param + " = " + values[i]);
		}
		out.println("<li><b>protocol</b> = " + req.getProtocol());
		out.println("<li><b>remoteAddr</b> = " + req.getRemoteAddr());
		out.println("<li><b>remoteHost</b> = " + req.getRemoteHost());
		out.println("<li><b>scheme</b> = " + req.getScheme());
		out.println("<li><b>secure</b> = " + req.isSecure());
		out.println("<li><b>serverName</b> = " + req.getServerName());
		out.println("<li><b>serverPort</b> = " + req.getServerPort());
		out.println("</ul>");
		out.println("<hr>");

		// Document the properties from HttpServletRequest
		out.println("<h1>HttpServletRequest Properties</h1>");
		out.println("<ul>");
		out.println("<li><b>authType</b> = " + req.getAuthType());
		out.println("<li><b>contextPath</b> = " + req.getContextPath());
		Cookie cookies[] = req.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++)
				out.println("<li><b>cookie</b> " + cookies[i].getName() + " = "
						+ cookies[i].getValue());
		}
		Enumeration<String> headers = req.getHeaderNames();
		while (headers.hasMoreElements()) {
			String header = headers.nextElement();
			out.println("<li><b>header</b> " + header + " = "
					+ req.getHeader(header));
		}
		out.println("<li><b>method</b> = " + req.getMethod());
		out.println("<li><a name=\"pathInfo\"><b>pathInfo</b></a> = "
				+ req.getPathInfo());
		out.println("<li><b>pathTranslated</b> = " + req.getPathTranslated());
		out.println("<li><b>queryString</b> = " + req.getQueryString());
		out.println("<li><b>remoteUser</b> = " + req.getRemoteUser());
		out.println("<li><b>requestedSessionId</b> = "
				+ req.getRequestedSessionId());
		out.println("<li><b>requestedSessionIdFromCookie</b> = "
				+ req.isRequestedSessionIdFromCookie());
		out.println("<li><b>requestedSessionIdFromURL</b> = "
				+ req.isRequestedSessionIdFromURL());
		out.println("<li><b>requestedSessionIdValid</b> = "
				+ req.isRequestedSessionIdValid());
		out.println("<li><b>requestURI</b> = " + req.getRequestURI());
		out.println("<li><b>servletPath</b> = " + req.getServletPath());
		out.println("<li><b>userPrincipal</b> = " + req.getUserPrincipal());
		out.println("</ul>");
		out.println("<hr>");

		// Document the servlet request attributes
		out.println("<h1>ServletRequest Attributes</h1>");
		out.println("<ul>");
		attrs = req.getAttributeNames();
		while (attrs.hasMoreElements()) {
			String attr = attrs.nextElement();
			out.println("<li><b>" + attr + "</b> = " + req.getAttribute(attr));
		}
		out.println("</ul>");
		out.println("<hr>");

		// Process the current session (if there is one)
		HttpSession session = req.getSession(false);
		if (session != null) {

			// Document the session properties
			out.println("<h1>HttpSession Properties</h1>");
			out.println("<ul>");
			out.println("<li><b>id</b> = " + session.getId());
			out.println("<li><b>creationTime</b> = "
					+ new Date(session.getCreationTime()));
			out.println("<li><b>lastAccessedTime</b> = "
					+ new Date(session.getLastAccessedTime()));
			out.println("<li><b>maxInactiveInterval</b> = "
					+ session.getMaxInactiveInterval());
			out.println("</ul>");
			out.println("<hr>");

			// Document the session attributes
			out.println("<h1>HttpSession Attributes</h1>");
			out.println("<ul>");
			attrs = session.getAttributeNames();
			while (attrs.hasMoreElements()) {
				String attr = attrs.nextElement();
				out.println("<li><b>" + attr + "</b> = "
						+ session.getAttribute(attr));
			}
			out.println("</ul>");
			out.println("<hr>");

		}

		// Document the servlet configuration properties
		out.println("<h1>ServletConfig Properties</h1>");
		out.println("<ul>");
		out.println("<li><b>servletName</b> = "
				+ getServletConfig().getServletName());
		out.println("</ul>");
		out.println("<hr>");

		// Document the servlet configuration initialization parameters
		out.println("<h1>ServletConfig Initialization Parameters</h1>");
		out.println("<ul>");
		params = getServletConfig().getInitParameterNames();
		while (params.hasMoreElements()) {
			String param = params.nextElement();
			String value = getServletConfig().getInitParameter(param);
			out.println("<li><b>" + param + "</b> = " + value);
		}
		out.println("</ul>");
		out.println("<hr>");

		// Document the servlet context properties
		out.println("<h1>ServletContext Properties</h1>");
		out.println("<ul>");
		out.println("<li><b>majorVersion</b> = "
				+ getServletContext().getMajorVersion());
		out.println("<li><b>minorVersion</b> = "
				+ getServletContext().getMinorVersion());
		out.println("<li><b>realPath('/')</b> = "
				+ getServletContext().getRealPath("/"));
		out.println("<li><b>serverInfo</b> = "
				+ getServletContext().getServerInfo());
		out.println("</ul>");
		out.println("<hr>");

		// Document the servlet context initialization parameters
		out.println("<h1>ServletContext Initialization Parameters</h1>");
		out.println("<ul>");
		params = getServletContext().getInitParameterNames();
		while (params.hasMoreElements()) {
			String param = params.nextElement();
			String value = getServletContext().getInitParameter(param);
			out.println("<li><b>" + param + "</b> = " + value);
		}
		out.println("</ul>");
		out.println("<hr>");

		// Document the servlet context attributes
		out.println("<h1>ServletContext Attributes</h1>");
		out.println("<ul>");
		attrs = getServletContext().getAttributeNames();
		while (attrs.hasMoreElements()) {
			String attr = attrs.nextElement();
			out.println("<li><b>" + attr + "</b> = "
					+ getServletContext().getAttribute(attr));
		}
		out.println("</ul>");
		out.println("<hr>");

	}

	/**
	 * Provides CGI Gateway service -- delegates to <code>doGet</code>
	 *
	 * @param req
	 *            HttpServletRequest passed in by servlet container
	 * @param res
	 *            HttpServletResponse passed in by servlet container
	 *
	 * @exception ServletException
	 *                if a servlet-specific exception occurs
	 * @exception IOException
	 *                if a read/write exception occurs
	 *
	 * @see javax.servlet.http.HttpServlet
	 *
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
		doGet(req, res);
	}

	/**
	 * Provides CGI Gateway service
	 *
	 * @param req
	 *            HttpServletRequest passed in by servlet container
	 * @param res
	 *            HttpServletResponse passed in by servlet container
	 *
	 * @exception ServletException
	 *                if a servlet-specific exception occurs
	 * @exception IOException
	 *                if a read/write exception occurs
	 *
	 * @see javax.servlet.http.HttpServlet
	 *
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		CGIServletCGIEnvironment cgiEnv = new CGIServletCGIEnvironment(this,
				req, getServletContext());

		if (cgiEnv.isValid()) {
			CGIServletCGIRunner cgi = new CGIServletCGIRunner(this,
					cgiEnv.getCommand(), cgiEnv.getEnvironment(),
					cgiEnv.getWorkingDirectory(), cgiEnv.getParameters());
			// if POST, we need to cgi.setInput
			// REMIND: how does this interact with Servlet API 2.3's Filters?!
			if ("POST".equals(req.getMethod())) {
				cgi.setInput(req.getInputStream());
			}
			cgi.setResponse(res);
			cgi.run();
		}

		if (!cgiEnv.isValid()) {
			res.setStatus(404);
		}

		if (debug >= 10) {

			ServletOutputStream out = res.getOutputStream();
			out.println("<HTML><HEAD><TITLE>$Name$</TITLE></HEAD>");
			out.println("<BODY>$Header$<p>");

			if (cgiEnv.isValid()) {
				out.println(cgiEnv.toString());
			} else {
				out.println("<H3>");
				out.println("CGI script not found or not specified.");
				out.println("</H3>");
				out.println("<H4>");
				out.println("Check the <b>HttpServletRequest ");
				out.println("<a href=\"#pathInfo\">pathInfo</a></b> ");
				out.println("property to see if it is what you meant ");
				out.println("it to be.  You must specify an existant ");
				out.println("and executable file as part of the ");
				out.println("path-info.");
				out.println("</H4>");
				out.println("<H4>");
				out.println("For a good discussion of how CGI scripts ");
				out.println("work and what their environment variables ");
				out.println("mean, please visit the <a ");
				out.println("href=\"http://cgi-spec.golux.com\">CGI ");
				out.println("Specification page</a>.");
				out.println("</H4>");

			}

			printServletEnvironment(out, req, res);

			out.println("</BODY></HTML>");

		}

	}

	public static Object getExpandFileLock() {
		return expandFileLock;
	}

	public static void setExpandFileLock(Object expandFileLock) {
		CGIServlet.expandFileLock = expandFileLock;
	}

	public long getStderrTimeout() {
		return stderrTimeout;
	}

	public void setStderrTimeout(long stderrTimeout) {
		this.stderrTimeout = stderrTimeout;
	}

	public String getParameterEncoding() {
		return parameterEncoding;
	}

	public void setParameterEncoding(String parameterEncoding) {
		this.parameterEncoding = parameterEncoding;
	}

	public List<String> getCgiExecutableArgs() {
		return cgiExecutableArgs;
	}

	public void setCgiExecutableArgs(List<String> cgiExecutableArgs) {
		this.cgiExecutableArgs = cgiExecutableArgs;
	}

	public String getCgiExecutable() {
		return cgiExecutable;
	}

	public void setCgiExecutable(String cgiExecutable) {
		this.cgiExecutable = cgiExecutable;
	}

	public String getCgiPathPrefix() {
		return cgiPathPrefix;
	}

	public void setCgiPathPrefix(String cgiPathPrefix) {
		this.cgiPathPrefix = cgiPathPrefix;
	}

	public int getDebug() {
		return debug;
	}

	public void setDebug(int debug) {
		this.debug = debug;
	}

	public Hashtable<String, String> getShellEnv() {
		return shellEnv;
	}

	public void setShellEnv(Hashtable<String, String> shellEnv) {
		this.shellEnv = shellEnv;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
