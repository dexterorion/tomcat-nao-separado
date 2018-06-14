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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Vector;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.catalina.util.DOMWriter;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.XMLWriter;
import org.apache.naming.resources.CacheEntry;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.tomcat.util.http.RequestUtil2;
import org.apache.tomcat.util.security.MD5Encoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Servlet which adds support for WebDAV level 2. All the basic HTTP requests
 * are handled by the DefaultServlet. The WebDAVServlet must not be used as the
 * default servlet (ie mapped to '/') as it will not work in this configuration.
 * <p/>
 * Mapping a subpath (e.g. <code>/webdav/*</code> to this servlet has the effect
 * of re-mounting the entire web application under that sub-path, with WebDAV
 * access to all the resources. This <code>WEB-INF</code> and
 * <code>META-INF</code> directories are protected in this re-mounted resource
 * tree.
 * <p/>
 * To enable WebDAV for a context add the following to web.xml:
 * 
 * <pre>
 * &lt;servlet&gt;
 *  &lt;servlet-name&gt;webdav&lt;/servlet-name&gt;
 *  &lt;servlet-class&gt;org.apache.catalina.servlets.WebdavServlet&lt;/servlet-class&gt;
 *    &lt;init-param&gt;
 *      &lt;param-name&gt;debug&lt;/param-name&gt;
 *      &lt;param-value&gt;0&lt;/param-value&gt;
 *    &lt;/init-param&gt;
 *    &lt;init-param&gt;
 *      &lt;param-name&gt;listings&lt;/param-name&gt;
 *      &lt;param-value&gt;false&lt;/param-value&gt;
 *    &lt;/init-param&gt;
 *  &lt;/servlet&gt;
 *  &lt;servlet-mapping&gt;
 *    &lt;servlet-name&gt;webdav&lt;/servlet-name&gt;
 *    &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * This will enable read only access. To enable read-write access add:
 * 
 * <pre>
 *  &lt;init-param&gt;
 *    &lt;param-name&gt;readonly&lt;/param-name&gt;
 *    &lt;param-value&gt;false&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre>
 * 
 * To make the content editable via a different URL, use the following mapping:
 * 
 * <pre>
 *  &lt;servlet-mapping&gt;
 *    &lt;servlet-name&gt;webdav&lt;/servlet-name&gt;
 *    &lt;url-pattern&gt;/webdavedit/*&lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * By default access to /WEB-INF and META-INF are not available via WebDAV. To
 * enable access to these URLs, use add:
 * 
 * <pre>
 *  &lt;init-param&gt;
 *    &lt;param-name&gt;allowSpecialPaths&lt;/param-name&gt;
 *    &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre>
 * 
 * Don't forget to secure access appropriately to the editing URLs, especially
 * if allowSpecialPaths is used. With the mapping configuration above, the
 * context will be accessible to normal users as before. Those users with the
 * necessary access will be able to edit content available via
 * http://host:port/context/content using
 * http://host:port/context/webdavedit/content
 *
 * @author Remy Maucherat
 */
public class WebdavServlet extends DefaultServlet {

	private static final long serialVersionUID = 1L;

	// -------------------------------------------------------------- Constants

	private static final String METHOD_PROPFIND = "PROPFIND";
	private static final String METHOD_PROPPATCH = "PROPPATCH";
	private static final String METHOD_MKCOL = "MKCOL";
	private static final String METHOD_COPY = "COPY";
	private static final String METHOD_MOVE = "MOVE";
	private static final String METHOD_LOCK = "LOCK";
	private static final String METHOD_UNLOCK = "UNLOCK";

	/**
	 * PROPFIND - Specify a property mask.
	 */
	private static final int FIND_BY_PROPERTY = 0;

	/**
	 * PROPFIND - Display all properties.
	 */
	private static final int FIND_ALL_PROP = 1;

	/**
	 * PROPFIND - Return property names.
	 */
	private static final int FIND_PROPERTY_NAMES = 2;

	/**
	 * Create a new lock.
	 */
	private static final int LOCK_CREATION = 0;

	/**
	 * Refresh lock.
	 */
	private static final int LOCK_REFRESH = 1;

	/**
	 * Default lock timeout value.
	 */
	private static final int DEFAULT_TIMEOUT = 3600;

	/**
	 * Maximum lock timeout.
	 */
	private static final int MAX_TIMEOUT = 604800;

	/**
	 * Default namespace.
	 */
	private static final String DEFAULT_NAMESPACE = "DAV:";

	/**
	 * Simple date format for the creation date ISO representation (partial).
	 */
	private static final SimpleDateFormat creationDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

	/**
	 * MD5 message digest provider.
	 */
	private static MessageDigest md5Helper;

	/**
	 * The MD5 helper object for this class.
	 *
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	private static final MD5Encoder md5Encoder = new MD5Encoder();

	static {
		creationDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * Repository of the locks put on single resources.
	 * <p>
	 * Key : path <br>
	 * Value : LockInfo
	 */
	private Hashtable<String, WebdavServletLockInfo> resourceLocks = new Hashtable<String, WebdavServletLockInfo>();

	/**
	 * Repository of the lock-null resources.
	 * <p>
	 * Key : path of the collection containing the lock-null resource<br>
	 * Value : Vector of lock-null resource which are members of the collection.
	 * Each element of the Vector is the path associated with the lock-null
	 * resource.
	 */
	private Hashtable<String, Vector<String>> lockNullResources = new Hashtable<String, Vector<String>>();

	/**
	 * Vector of the heritable locks.
	 * <p>
	 * Key : path <br>
	 * Value : LockInfo
	 */
	private Vector<WebdavServletLockInfo> collectionLocks = new Vector<WebdavServletLockInfo>();

	/**
	 * Secret information used to generate reasonably secure lock ids.
	 */
	private String secret = "catalina";

	/**
	 * Default depth in spec is infinite. Limit depth to 3 by default as
	 * infinite depth makes operations very expensive.
	 */
	private int maxDepth = 3;

	/**
	 * Is access allowed via WebDAV to the special paths (/WEB-INF and
	 * /META-INF)?
	 */
	private boolean allowSpecialPaths = false;

	// --------------------------------------------------------- Public Methods

	/**
	 * Initialize this servlet.
	 */
	@Override
	public void init() throws ServletException {

		super.init();

		if (getServletConfig().getInitParameter("secret") != null)
			secret = getServletConfig().getInitParameter("secret");

		if (getServletConfig().getInitParameter("maxDepth") != null)
			maxDepth = Integer.parseInt(getServletConfig().getInitParameter(
					"maxDepth"));

		if (getServletConfig().getInitParameter("allowSpecialPaths") != null)
			allowSpecialPaths = Boolean.parseBoolean(getServletConfig()
					.getInitParameter("allowSpecialPaths"));

		// Load the MD5 helper used to calculate signatures.
		try {
			md5Helper = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new UnavailableException("No MD5");
		}

	}

	// ------------------------------------------------------ Protected Methods

	/**
	 * Return JAXP document builder instance.
	 */
	protected DocumentBuilder getDocumentBuilder() throws ServletException {
		DocumentBuilder documentBuilder = null;
		DocumentBuilderFactory documentBuilderFactory = null;
		try {
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			documentBuilderFactory.setExpandEntityReferences(false);
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new WebdavServletWebdavResolver(
					this.getServletContext()));
		} catch (ParserConfigurationException e) {
			throw new ServletException(getSm().getString(
					"webdavservlet.jaxpfailed"));
		}
		return documentBuilder;
	}

	/**
	 * Handles the special WebDAV methods.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		final String path = getRelativePath(req);

		// Block access to special subdirectories.
		// DefaultServlet assumes it services resources from the root of the web
		// app
		// and doesn't add any special path protection
		// WebdavServlet remounts the webapp under a new path, so this check is
		// necessary on all methods (including GET).
		if (isSpecialPath(path)) {
			resp.sendError(WebdavServletWebdavStatus.getScNotFound());
			return;
		}

		final String method = req.getMethod();

		if (getDebug() > 0) {
			log("[" + method + "] " + path);
		}

		if (method.equals(METHOD_PROPFIND)) {
			doPropfind(req, resp);
		} else if (method.equals(METHOD_PROPPATCH)) {
			doProppatch(req, resp);
		} else if (method.equals(METHOD_MKCOL)) {
			doMkcol(req, resp);
		} else if (method.equals(METHOD_COPY)) {
			doCopy(req, resp);
		} else if (method.equals(METHOD_MOVE)) {
			doMove(req, resp);
		} else if (method.equals(METHOD_LOCK)) {
			doLock(req, resp);
		} else if (method.equals(METHOD_UNLOCK)) {
			doUnlock(req, resp);
		} else {
			// DefaultServlet processing
			super.service(req, resp);
		}

	}

	/**
	 * Checks whether a given path refers to a resource under
	 * <code>WEB-INF</code> or <code>META-INF</code>.
	 * 
	 * @param path
	 *            the full path of the resource being accessed
	 * @return <code>true</code> if the resource specified is under a special
	 *         path
	 */
	private final boolean isSpecialPath(final String path) {
		return !allowSpecialPaths
				&& (path.toUpperCase(Locale.ENGLISH).startsWith("/WEB-INF") || path
						.toUpperCase(Locale.ENGLISH).startsWith("/META-INF"));
	}

	/**
	 * Check if the conditions specified in the optional If headers are
	 * satisfied.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            The resource information
	 * @return boolean true if the resource meets all the specified conditions,
	 *         and false if any of the conditions is not satisfied, in which
	 *         case request processing is stopped
	 */
	@Override
	protected boolean checkIfHeaders(HttpServletRequest request,
			HttpServletResponse response, ResourceAttributes resourceAttributes)
			throws IOException {

		if (!super.checkIfHeaders(request, response, resourceAttributes))
			return false;

		// TODO : Checking the WebDAV If header
		return true;

	}

	/**
	 * Override the DefaultServlet implementation and only use the PathInfo. If
	 * the ServletPath is non-null, it will be because the WebDAV servlet has
	 * been mapped to a url other than /* to configure editing at different url
	 * than normal viewing.
	 *
	 * @param request
	 *            The servlet request we are processing
	 */
	@Override
	protected String getRelativePath(HttpServletRequest request) {
		// Are we being processed by a RequestDispatcher.include()?
		if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
			String result = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if ((result == null) || (result.equals("")))
				result = "/";
			return (result);
		}

		// No, extract the desired path directly from the request
		String result = request.getPathInfo();
		if ((result == null) || (result.equals(""))) {
			result = "/";
		}
		return (result);

	}

	/**
	 * Determines the prefix for standard directory GET listings.
	 */
	@Override
	protected String getPathPrefix(final HttpServletRequest request) {
		// Repeat the servlet path (e.g. /webdav/) in the listing path
		String contextPath = request.getContextPath();
		if (request.getServletPath() != null) {
			contextPath = contextPath + request.getServletPath();
		}
		return contextPath;
	}

	/**
	 * OPTIONS Method.
	 *
	 * @param req
	 *            The request
	 * @param resp
	 *            The response
	 * @throws ServletException
	 *             If an error occurs
	 * @throws IOException
	 *             If an IO error occurs
	 */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.addHeader("DAV", "1,2");

		StringBuilder methodsAllowed = determineMethodsAllowed(getResources(),
				req);

		resp.addHeader("Allow", methodsAllowed.toString());
		resp.addHeader("MS-Author-Via", "DAV");

	}

	/**
	 * PROPFIND Method.
	 */
	protected void doPropfind(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (!isListings()) {
			// Get allowed methods
			StringBuilder methodsAllowed = determineMethodsAllowed(
					getResources(), req);

			resp.addHeader("Allow", methodsAllowed.toString());
			resp.sendError(WebdavServletWebdavStatus.getScMethodNotAllowed());
			return;
		}

		String path = getRelativePath(req);
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);

		// Properties which are to be displayed.
		Vector<String> properties = null;
		// Propfind depth
		int depth = maxDepth;
		// Propfind type
		int type = FIND_ALL_PROP;

		String depthStr = req.getHeader("Depth");

		if (depthStr == null) {
			depth = maxDepth;
		} else {
			if (depthStr.equals("0")) {
				depth = 0;
			} else if (depthStr.equals("1")) {
				depth = 1;
			} else if (depthStr.equals("infinity")) {
				depth = maxDepth;
			}
		}

		Node propNode = null;

		if (req.getContentLength() > 0) {
			DocumentBuilder documentBuilder = getDocumentBuilder();

			try {
				Document document = documentBuilder.parse(new InputSource(req
						.getInputStream()));

				// Get the root element of the document
				Element rootElement = document.getDocumentElement();
				NodeList childList = rootElement.getChildNodes();

				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						if (currentNode.getNodeName().endsWith("prop")) {
							type = FIND_BY_PROPERTY;
							propNode = currentNode;
						}
						if (currentNode.getNodeName().endsWith("propname")) {
							type = FIND_PROPERTY_NAMES;
						}
						if (currentNode.getNodeName().endsWith("allprop")) {
							type = FIND_ALL_PROP;
						}
						break;
					}
				}
			} catch (SAXException e) {
				// Something went wrong - bad request
				resp.sendError(WebdavServletWebdavStatus.getScBadRequest());
				return;
			} catch (IOException e) {
				// Something went wrong - bad request
				resp.sendError(WebdavServletWebdavStatus.getScBadRequest());
				return;
			}
		}

		if (type == FIND_BY_PROPERTY) {
			properties = new Vector<String>();
			// propNode must be non-null if type == FIND_BY_PROPERTY
			@SuppressWarnings("null")
			NodeList childList = propNode.getChildNodes();

			for (int i = 0; i < childList.getLength(); i++) {
				Node currentNode = childList.item(i);
				switch (currentNode.getNodeType()) {
				case Node.TEXT_NODE:
					break;
				case Node.ELEMENT_NODE:
					String nodeName = currentNode.getNodeName();
					String propertyName = null;
					if (nodeName.indexOf(':') != -1) {
						propertyName = nodeName
								.substring(nodeName.indexOf(':') + 1);
					} else {
						propertyName = nodeName;
					}
					// href is a live property which is handled differently
					properties.addElement(propertyName);
					break;
				}
			}

		}

		boolean exists = true;
		Object object = null;
		try {
			object = getResources().lookup(path);
		} catch (NamingException e) {
			exists = false;
			int slash = path.lastIndexOf('/');
			if (slash != -1) {
				String parentPath = path.substring(0, slash);
				Vector<String> currentLockNullResources = lockNullResources
						.get(parentPath);
				if (currentLockNullResources != null) {
					Enumeration<String> lockNullResourcesList = currentLockNullResources
							.elements();
					while (lockNullResourcesList.hasMoreElements()) {
						String lockNullPath = lockNullResourcesList
								.nextElement();
						if (lockNullPath.equals(path)) {
							resp.setStatus(WebdavServletWebdavStatus
									.getScMultiStatus());
							resp.setContentType("text/xml; charset=UTF-8");
							// Create multistatus object
							XMLWriter generatedXML = new XMLWriter(
									resp.getWriter());
							generatedXML.writeXMLHeader();
							generatedXML.writeElement("D", DEFAULT_NAMESPACE,
									"multistatus", XMLWriter.getOpening());
							parseLockNullProperties(req, generatedXML,
									lockNullPath, type, properties);
							generatedXML.writeElement("D", "multistatus",
									XMLWriter.getClosing());
							generatedXML.sendData();
							return;
						}
					}
				}
			}
		}

		if (!exists) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, path);
			return;
		}

		resp.setStatus(WebdavServletWebdavStatus.getScMultiStatus());

		resp.setContentType("text/xml; charset=UTF-8");

		// Create multistatus object
		XMLWriter generatedXML = new XMLWriter(resp.getWriter());
		generatedXML.writeXMLHeader();

		generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus",
				XMLWriter.getOpening());

		if (depth == 0) {
			parseProperties(req, generatedXML, path, type, properties);
		} else {
			// The stack always contains the object of the current level
			Stack<String> stack = new Stack<String>();
			stack.push(path);

			// Stack of the objects one level below
			Stack<String> stackBelow = new Stack<String>();

			while ((!stack.isEmpty()) && (depth >= 0)) {

				String currentPath = stack.pop();
				parseProperties(req, generatedXML, currentPath, type,
						properties);

				try {
					object = getResources().lookup(currentPath);
				} catch (NamingException e) {
					continue;
				}

				if ((object instanceof DirContext) && (depth > 0)) {

					try {
						NamingEnumeration<NameClassPair> enumeration = getResources()
								.list(currentPath);
						while (enumeration.hasMoreElements()) {
							NameClassPair ncPair = enumeration.nextElement();
							String newPath = currentPath;
							if (!(newPath.endsWith("/")))
								newPath += "/";
							newPath += ncPair.getName();
							stackBelow.push(newPath);
						}
					} catch (NamingException e) {
						resp.sendError(
								HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
								path);
						return;
					}

					// Displaying the lock-null resources present in that
					// collection
					String lockPath = currentPath;
					if (lockPath.endsWith("/"))
						lockPath = lockPath.substring(0, lockPath.length() - 1);
					Vector<String> currentLockNullResources = lockNullResources
							.get(lockPath);
					if (currentLockNullResources != null) {
						Enumeration<String> lockNullResourcesList = currentLockNullResources
								.elements();
						while (lockNullResourcesList.hasMoreElements()) {
							String lockNullPath = lockNullResourcesList
									.nextElement();
							parseLockNullProperties(req, generatedXML,
									lockNullPath, type, properties);
						}
					}

				}

				if (stack.isEmpty()) {
					depth--;
					stack = stackBelow;
					stackBelow = new Stack<String>();
				}

				generatedXML.sendData();

			}
		}

		generatedXML.writeElement("D", "multistatus", XMLWriter.getClosing());

		generatedXML.sendData();

	}

	/**
	 * PROPPATCH Method.
	 */
	protected void doProppatch(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);

	}

	/**
	 * MKCOL Method.
	 */
	protected void doMkcol(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		String path = getRelativePath(req);

		boolean exists = true;
		try {
			getResources().lookup(path);
		} catch (NamingException e) {
			exists = false;
		}

		// Can't create a collection if a resource already exists at the given
		// path
		if (exists) {
			// Get allowed methods
			StringBuilder methodsAllowed = determineMethodsAllowed(
					getResources(), req);

			resp.addHeader("Allow", methodsAllowed.toString());

			resp.sendError(WebdavServletWebdavStatus.getScMethodNotAllowed());
			return;
		}

		if (req.getContentLength() > 0) {
			DocumentBuilder documentBuilder = getDocumentBuilder();
			try {
				// Document document =
				documentBuilder.parse(new InputSource(req.getInputStream()));
				// TODO : Process this request body
				resp.sendError(WebdavServletWebdavStatus.getScNotImplemented());
				return;

			} catch (SAXException saxe) {
				// Parse error - assume invalid content
				resp.sendError(WebdavServletWebdavStatus
						.getScUnsupportedMediaType());
				return;
			}
		}

		boolean result = true;
		try {
			getResources().createSubcontext(path);
		} catch (NamingException e) {
			result = false;
		}

		if (!result) {
			resp.sendError(WebdavServletWebdavStatus.getScConflict(),
					WebdavServletWebdavStatus
							.getStatusText(WebdavServletWebdavStatus
									.getScConflict()));
		} else {
			resp.setStatus(WebdavServletWebdavStatus.getScCreated());
			// Removing any lock-null resource which would be present
			lockNullResources.remove(path);
		}

	}

	/**
	 * DELETE Method.
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		deleteResource(req, resp);

	}

	/**
	 * Process a PUT request for the specified resource.
	 *
	 * @param req
	 *            The servlet request we are processing
	 * @param resp
	 *            The servlet response we are creating
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		super.doPut(req, resp);

		String path = getRelativePath(req);

		// Removing any lock-null resource which would be present
		lockNullResources.remove(path);

	}

	/**
	 * COPY Method.
	 */
	protected void doCopy(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		copyResource(req, resp);

	}

	/**
	 * MOVE Method.
	 */
	protected void doMove(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		String path = getRelativePath(req);

		if (copyResource(req, resp)) {
			deleteResource(path, req, resp, false);
		}

	}

	/**
	 * LOCK Method.
	 */
	protected void doLock(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		WebdavServletLockInfo lock = new WebdavServletLockInfo(this);

		// Parsing lock request

		// Parsing depth header

		String depthStr = req.getHeader("Depth");

		if (depthStr == null) {
			lock.setDepth(maxDepth);
		} else {
			if (depthStr.equals("0")) {
				lock.setDepth(0);
			} else {
				lock.setDepth(maxDepth);
			}
		}

		// Parsing timeout header

		int lockDuration = DEFAULT_TIMEOUT;
		String lockDurationStr = req.getHeader("Timeout");
		if (lockDurationStr == null) {
			lockDuration = DEFAULT_TIMEOUT;
		} else {
			int commaPos = lockDurationStr.indexOf(",");
			// If multiple timeouts, just use the first
			if (commaPos != -1) {
				lockDurationStr = lockDurationStr.substring(0, commaPos);
			}
			if (lockDurationStr.startsWith("Second-")) {
				lockDuration = (new Integer(lockDurationStr.substring(7)))
						.intValue();
			} else {
				if (lockDurationStr.equalsIgnoreCase("infinity")) {
					lockDuration = MAX_TIMEOUT;
				} else {
					try {
						lockDuration = (new Integer(lockDurationStr))
								.intValue();
					} catch (NumberFormatException e) {
						lockDuration = MAX_TIMEOUT;
					}
				}
			}
			if (lockDuration == 0) {
				lockDuration = DEFAULT_TIMEOUT;
			}
			if (lockDuration > MAX_TIMEOUT) {
				lockDuration = MAX_TIMEOUT;
			}
		}
		lock.setExpiresAt(System.currentTimeMillis() + (lockDuration * 1000));

		int lockRequestType = LOCK_CREATION;

		Node lockInfoNode = null;

		DocumentBuilder documentBuilder = getDocumentBuilder();

		try {
			Document document = documentBuilder.parse(new InputSource(req
					.getInputStream()));

			// Get the root element of the document
			Element rootElement = document.getDocumentElement();
			lockInfoNode = rootElement;
		} catch (IOException e) {
			lockRequestType = LOCK_REFRESH;
		} catch (SAXException e) {
			lockRequestType = LOCK_REFRESH;
		}

		if (lockInfoNode != null) {

			// Reading lock information

			NodeList childList = lockInfoNode.getChildNodes();
			StringWriter strWriter = null;
			DOMWriter domWriter = null;

			Node lockScopeNode = null;
			Node lockTypeNode = null;
			Node lockOwnerNode = null;

			for (int i = 0; i < childList.getLength(); i++) {
				Node currentNode = childList.item(i);
				switch (currentNode.getNodeType()) {
				case Node.TEXT_NODE:
					break;
				case Node.ELEMENT_NODE:
					String nodeName = currentNode.getNodeName();
					if (nodeName.endsWith("lockscope")) {
						lockScopeNode = currentNode;
					}
					if (nodeName.endsWith("locktype")) {
						lockTypeNode = currentNode;
					}
					if (nodeName.endsWith("owner")) {
						lockOwnerNode = currentNode;
					}
					break;
				}
			}

			if (lockScopeNode != null) {

				childList = lockScopeNode.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						String tempScope = currentNode.getNodeName();
						if (tempScope.indexOf(':') != -1) {
							lock.setScope(tempScope.substring(tempScope
									.indexOf(':') + 1));
						} else {
							lock.setScope(tempScope);
						}
						break;
					}
				}

				if (lock.getScope() == null) {
					// Bad request
					resp.setStatus(WebdavServletWebdavStatus.getScBadRequest());
				}

			} else {
				// Bad request
				resp.setStatus(WebdavServletWebdavStatus.getScBadRequest());
			}

			if (lockTypeNode != null) {

				childList = lockTypeNode.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						String tempType = currentNode.getNodeName();
						if (tempType.indexOf(':') != -1) {
							lock.setType(tempType.substring(tempType
									.indexOf(':') + 1));
						} else {
							lock.setType(tempType);
						}
						break;
					}
				}

				if (lock.getType() == null) {
					// Bad request
					resp.setStatus(WebdavServletWebdavStatus.getScBadRequest());
				}

			} else {
				// Bad request
				resp.setStatus(WebdavServletWebdavStatus.getScBadRequest());
			}

			if (lockOwnerNode != null) {

				childList = lockOwnerNode.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						lock.setOwner(lock.getOwner() + currentNode.getNodeValue());
						break;
					case Node.ELEMENT_NODE:
						strWriter = new StringWriter();
						domWriter = new DOMWriter(strWriter, true);
						domWriter.setQualifiedNames(false);
						domWriter.print(currentNode);
						lock.setOwner(lock.getOwner() + strWriter.toString());
						break;
					}
				}

				if (lock.getOwner() == null) {
					// Bad request
					resp.setStatus(WebdavServletWebdavStatus.getScBadRequest());
				}

			} else {
				lock.setOwner("");
			}

		}

		String path = getRelativePath(req);

		lock.setPath(path);

		boolean exists = true;
		Object object = null;
		try {
			object = getResources().lookup(path);
		} catch (NamingException e) {
			exists = false;
		}

		Enumeration<WebdavServletLockInfo> locksList = null;

		if (lockRequestType == LOCK_CREATION) {

			// Generating lock id
			String lockTokenStr = req.getServletPath() + "-" + lock.getType() + "-"
					+ lock.getScope() + "-" + req.getUserPrincipal() + "-"
					+ lock.getDepth() + "-" + lock.getOwner() + "-" + lock.getTokens() + "-"
					+ lock.getExpiresAt() + "-" + System.currentTimeMillis() + "-"
					+ secret;
			String lockToken = MD5Encoder.encode(md5Helper.digest(lockTokenStr
					.getBytes(Charset.defaultCharset())));

			if ((exists) && (object instanceof DirContext)
					&& (lock.getDepth() == maxDepth)) {

				// Locking a collection (and all its member resources)

				// Checking if a child resource of this collection is
				// already locked
				Vector<String> lockPaths = new Vector<String>();
				locksList = collectionLocks.elements();
				while (locksList.hasMoreElements()) {
					WebdavServletLockInfo currentLock = locksList.nextElement();
					if (currentLock.hasExpired()) {
						resourceLocks.remove(currentLock.getPath());
						continue;
					}
					if ((currentLock.getPath().startsWith(lock.getPath()))
							&& ((currentLock.isExclusive()) || (lock
									.isExclusive()))) {
						// A child collection of this collection is locked
						lockPaths.addElement(currentLock.getPath());
					}
				}
				locksList = resourceLocks.elements();
				while (locksList.hasMoreElements()) {
					WebdavServletLockInfo currentLock = locksList.nextElement();
					if (currentLock.hasExpired()) {
						resourceLocks.remove(currentLock.getPath());
						continue;
					}
					if ((currentLock.getPath().startsWith(lock.getPath()))
							&& ((currentLock.isExclusive()) || (lock
									.isExclusive()))) {
						// A child resource of this collection is locked
						lockPaths.addElement(currentLock.getPath());
					}
				}

				if (!lockPaths.isEmpty()) {

					// One of the child paths was locked
					// We generate a multistatus error report

					Enumeration<String> lockPathsList = lockPaths.elements();

					resp.setStatus(WebdavServletWebdavStatus.getScConflict());

					XMLWriter generatedXML = new XMLWriter();
					generatedXML.writeXMLHeader();

					generatedXML.writeElement("D", DEFAULT_NAMESPACE,
							"multistatus", XMLWriter.getOpening());

					while (lockPathsList.hasMoreElements()) {
						generatedXML.writeElement("D", "response",
								XMLWriter.getOpening());
						generatedXML.writeElement("D", "href",
								XMLWriter.getOpening());
						generatedXML.writeText(lockPathsList.nextElement());
						generatedXML.writeElement("D", "href",
								XMLWriter.getClosing());
						generatedXML.writeElement("D", "status",
								XMLWriter.getOpening());
						generatedXML
								.writeText("HTTP/1.1 "
										+ WebdavServletWebdavStatus
												.getScLocked()
										+ " "
										+ WebdavServletWebdavStatus
												.getStatusText(WebdavServletWebdavStatus
														.getScLocked()));
						generatedXML.writeElement("D", "status",
								XMLWriter.getClosing());

						generatedXML.writeElement("D", "response",
								XMLWriter.getClosing());
					}

					generatedXML.writeElement("D", "multistatus",
							XMLWriter.getClosing());

					Writer writer = resp.getWriter();
					writer.write(generatedXML.toString());
					writer.close();

					return;

				}

				boolean addLock = true;

				// Checking if there is already a shared lock on this path
				locksList = collectionLocks.elements();
				while (locksList.hasMoreElements()) {

					WebdavServletLockInfo currentLock = locksList.nextElement();
					if (currentLock.getPath().equals(lock.getPath())) {

						if (currentLock.isExclusive()) {
							resp.sendError(WebdavServletWebdavStatus
									.getScLocked());
							return;
						} else {
							if (lock.isExclusive()) {
								resp.sendError(WebdavServletWebdavStatus
										.getScLocked());
								return;
							}
						}

						currentLock.getTokens().addElement(lockToken);
						lock = currentLock;
						addLock = false;

					}

				}

				if (addLock) {
					lock.getTokens().addElement(lockToken);
					collectionLocks.addElement(lock);
				}

			} else {

				// Locking a single resource

				// Retrieving an already existing lock on that resource
				WebdavServletLockInfo presentLock = resourceLocks
						.get(lock.getPath());
				if (presentLock != null) {

					if ((presentLock.isExclusive()) || (lock.isExclusive())) {
						// If either lock is exclusive, the lock can't be
						// granted
						resp.sendError(WebdavServletWebdavStatus
								.getScPreconditionFailed());
						return;
					} else {
						presentLock.getTokens().addElement(lockToken);
						lock = presentLock;
					}

				} else {

					lock.getTokens().addElement(lockToken);
					resourceLocks.put(lock.getPath(), lock);

					// Checking if a resource exists at this path
					exists = true;
					try {
						object = getResources().lookup(path);
					} catch (NamingException e) {
						exists = false;
					}
					if (!exists) {

						// "Creating" a lock-null resource
						int slash = lock.getPath().lastIndexOf('/');
						String parentPath = lock.getPath().substring(0, slash);

						Vector<String> lockNulls = lockNullResources
								.get(parentPath);
						if (lockNulls == null) {
							lockNulls = new Vector<String>();
							lockNullResources.put(parentPath, lockNulls);
						}

						lockNulls.addElement(lock.getPath());

					}
					// Add the Lock-Token header as by RFC 2518 8.10.1
					// - only do this for newly created locks
					resp.addHeader("Lock-Token", "<opaquelocktoken:"
							+ lockToken + ">");
				}

			}

		}

		if (lockRequestType == LOCK_REFRESH) {

			String ifHeader = req.getHeader("If");
			if (ifHeader == null)
				ifHeader = "";

			// Checking resource locks

			WebdavServletLockInfo toRenew = resourceLocks.get(path);
			Enumeration<String> tokenList = null;

			if (toRenew != null) {
				// At least one of the tokens of the locks must have been given
				tokenList = toRenew.getTokens().elements();
				while (tokenList.hasMoreElements()) {
					String token = tokenList.nextElement();
					if (ifHeader.indexOf(token) != -1) {
						toRenew.setExpiresAt(lock.getExpiresAt());
						lock = toRenew;
					}
				}
			}

			// Checking inheritable collection locks

			Enumeration<WebdavServletLockInfo> collectionLocksList = collectionLocks
					.elements();
			while (collectionLocksList.hasMoreElements()) {
				toRenew = collectionLocksList.nextElement();
				if (path.equals(toRenew.getPath())) {

					tokenList = toRenew.getTokens().elements();
					while (tokenList.hasMoreElements()) {
						String token = tokenList.nextElement();
						if (ifHeader.indexOf(token) != -1) {
							toRenew.setExpiresAt(lock.getExpiresAt());
							lock = toRenew;
						}
					}

				}
			}

		}

		// Set the status, then generate the XML response containing
		// the lock information
		XMLWriter generatedXML = new XMLWriter();
		generatedXML.writeXMLHeader();
		generatedXML.writeElement("D", DEFAULT_NAMESPACE, "prop",
				XMLWriter.getOpening());

		generatedXML.writeElement("D", "lockdiscovery", XMLWriter.getOpening());

		lock.toXML(generatedXML);

		generatedXML.writeElement("D", "lockdiscovery", XMLWriter.getClosing());

		generatedXML.writeElement("D", "prop", XMLWriter.getClosing());

		resp.setStatus(WebdavServletWebdavStatus.getScOk());
		resp.setContentType("text/xml; charset=UTF-8");
		Writer writer = resp.getWriter();
		writer.write(generatedXML.toString());
		writer.close();

	}

	/**
	 * UNLOCK Method.
	 */
	protected void doUnlock(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		if (isReadOnly()) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return;
		}

		if (isLocked(req)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return;
		}

		String path = getRelativePath(req);

		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null)
			lockTokenHeader = "";

		// Checking resource locks

		WebdavServletLockInfo lock = resourceLocks.get(path);
		Enumeration<String> tokenList = null;
		if (lock != null) {

			// At least one of the tokens of the locks must have been given

			tokenList = lock.getTokens().elements();
			while (tokenList.hasMoreElements()) {
				String token = tokenList.nextElement();
				if (lockTokenHeader.indexOf(token) != -1) {
					lock.getTokens().removeElement(token);
				}
			}

			if (lock.getTokens().isEmpty()) {
				resourceLocks.remove(path);
				// Removing any lock-null resource which would be present
				lockNullResources.remove(path);
			}

		}

		// Checking inheritable collection locks

		Enumeration<WebdavServletLockInfo> collectionLocksList = collectionLocks
				.elements();
		while (collectionLocksList.hasMoreElements()) {
			lock = collectionLocksList.nextElement();
			if (path.equals(lock.getPath())) {

				tokenList = lock.getTokens().elements();
				while (tokenList.hasMoreElements()) {
					String token = tokenList.nextElement();
					if (lockTokenHeader.indexOf(token) != -1) {
						lock.getTokens().removeElement(token);
						break;
					}
				}

				if (lock.getTokens().isEmpty()) {
					collectionLocks.removeElement(lock);
					// Removing any lock-null resource which would be present
					lockNullResources.remove(path);
				}

			}
		}

		resp.setStatus(WebdavServletWebdavStatus.getScNoContent());

	}

	// -------------------------------------------------------- Private Methods

	/**
	 * Check to see if a resource is currently write locked. The method will
	 * look at the "If" header to make sure the client has give the appropriate
	 * lock tokens.
	 *
	 * @param req
	 *            Servlet request
	 * @return boolean true if the resource is locked (and no appropriate lock
	 *         token has been found for at least one of the non-shared locks
	 *         which are present on the resource).
	 */
	private boolean isLocked(HttpServletRequest req) {

		String path = getRelativePath(req);

		String ifHeader = req.getHeader("If");
		if (ifHeader == null)
			ifHeader = "";

		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null)
			lockTokenHeader = "";

		return isLocked(path, ifHeader + lockTokenHeader);

	}

	/**
	 * Check to see if a resource is currently write locked.
	 *
	 * @param path
	 *            Path of the resource
	 * @param ifHeader
	 *            "If" HTTP header which was included in the request
	 * @return boolean true if the resource is locked (and no appropriate lock
	 *         token has been found for at least one of the non-shared locks
	 *         which are present on the resource).
	 */
	private boolean isLocked(String path, String ifHeader) {

		// Checking resource locks

		WebdavServletLockInfo lock = resourceLocks.get(path);
		Enumeration<String> tokenList = null;
		if ((lock != null) && (lock.hasExpired())) {
			resourceLocks.remove(path);
		} else if (lock != null) {

			// At least one of the tokens of the locks must have been given

			tokenList = lock.getTokens().elements();
			boolean tokenMatch = false;
			while (tokenList.hasMoreElements()) {
				String token = tokenList.nextElement();
				if (ifHeader.indexOf(token) != -1) {
					tokenMatch = true;
					break;
				}
			}
			if (!tokenMatch)
				return true;

		}

		// Checking inheritable collection locks

		Enumeration<WebdavServletLockInfo> collectionLocksList = collectionLocks
				.elements();
		while (collectionLocksList.hasMoreElements()) {
			lock = collectionLocksList.nextElement();
			if (lock.hasExpired()) {
				collectionLocks.removeElement(lock);
			} else if (path.startsWith(lock.getPath())) {

				tokenList = lock.getTokens().elements();
				boolean tokenMatch = false;
				while (tokenList.hasMoreElements()) {
					String token = tokenList.nextElement();
					if (ifHeader.indexOf(token) != -1) {
						tokenMatch = true;
						break;
					}
				}
				if (!tokenMatch)
					return true;

			}
		}

		return false;

	}

	/**
	 * Copy a resource.
	 *
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @return boolean true if the copy is successful
	 */
	private boolean copyResource(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {

		// Parsing destination header

		String destinationPath = req.getHeader("Destination");

		if (destinationPath == null) {
			resp.sendError(WebdavServletWebdavStatus.getScBadRequest());
			return false;
		}

		// Remove url encoding from destination
		destinationPath = RequestUtil.URLDecode(
				destinationPath, "UTF8");

		int protocolIndex = destinationPath.indexOf("://");
		if (protocolIndex >= 0) {
			// if the Destination URL contains the protocol, we can safely
			// trim everything upto the first "/" character after "://"
			int firstSeparator = destinationPath
					.indexOf("/", protocolIndex + 4);
			if (firstSeparator < 0) {
				destinationPath = "/";
			} else {
				destinationPath = destinationPath.substring(firstSeparator);
			}
		} else {
			String hostName = req.getServerName();
			if ((hostName != null) && (destinationPath.startsWith(hostName))) {
				destinationPath = destinationPath.substring(hostName.length());
			}

			int portIndex = destinationPath.indexOf(":");
			if (portIndex >= 0) {
				destinationPath = destinationPath.substring(portIndex);
			}

			if (destinationPath.startsWith(":")) {
				int firstSeparator = destinationPath.indexOf("/");
				if (firstSeparator < 0) {
					destinationPath = "/";
				} else {
					destinationPath = destinationPath.substring(firstSeparator);
				}
			}
		}

		// Normalise destination path (remove '.' and '..')
		destinationPath = RequestUtil2.normalize(destinationPath);

		String contextPath = req.getContextPath();
		if ((contextPath != null) && (destinationPath.startsWith(contextPath))) {
			destinationPath = destinationPath.substring(contextPath.length());
		}

		String pathInfo = req.getPathInfo();
		if (pathInfo != null) {
			String servletPath = req.getServletPath();
			if ((servletPath != null)
					&& (destinationPath.startsWith(servletPath))) {
				destinationPath = destinationPath.substring(servletPath
						.length());
			}
		}

		if (getDebug() > 0)
			log("Dest path :" + destinationPath);

		// Check destination path to protect special subdirectories
		if (isSpecialPath(destinationPath)) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return false;
		}

		String path = getRelativePath(req);

		if (destinationPath.equals(path)) {
			resp.sendError(WebdavServletWebdavStatus.getScForbidden());
			return false;
		}

		// Parsing overwrite header

		boolean overwrite = true;
		String overwriteHeader = req.getHeader("Overwrite");

		if (overwriteHeader != null) {
			if (overwriteHeader.equalsIgnoreCase("T")) {
				overwrite = true;
			} else {
				overwrite = false;
			}
		}

		// Overwriting the destination

		boolean exists = true;
		try {
			getResources().lookup(destinationPath);
		} catch (NamingException e) {
			exists = false;
		}

		if (overwrite) {

			// Delete destination resource, if it exists
			if (exists) {
				if (!deleteResource(destinationPath, req, resp, true)) {
					return false;
				}
			} else {
				resp.setStatus(WebdavServletWebdavStatus.getScCreated());
			}

		} else {

			// If the destination exists, then it's a conflict
			if (exists) {
				resp.sendError(WebdavServletWebdavStatus
						.getScPreconditionFailed());
				return false;
			}

		}

		// Copying source to destination

		Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

		boolean result = copyResource(getResources(), errorList, path,
				destinationPath);

		if ((!result) || (!errorList.isEmpty())) {
			if (errorList.size() == 1) {
				resp.sendError(errorList.elements().nextElement().intValue());
			} else {
				sendReport(req, resp, errorList);
			}
			return false;
		}

		// Copy was successful
		if (exists) {
			resp.setStatus(WebdavServletWebdavStatus.getScNoContent());
		} else {
			resp.setStatus(WebdavServletWebdavStatus.getScCreated());
		}

		// Removing any lock-null resource which would be present at
		// the destination path
		lockNullResources.remove(destinationPath);

		return true;

	}

	/**
	 * Copy a collection.
	 *
	 * @param dirContext
	 *            Resources implementation to be used
	 * @param errorList
	 *            Hashtable containing the list of errors which occurred during
	 *            the copy operation
	 * @param source
	 *            Path of the resource to be copied
	 * @param dest
	 *            Destination path
	 */
	private boolean copyResource(DirContext dirContext,
			Hashtable<String, Integer> errorList, String source, String dest) {

		if (getDebug() > 1)
			log("Copy: " + source + " To: " + dest);

		Object object = null;
		try {
			object = dirContext.lookup(source);
		} catch (NamingException e) {
			// Ignore
		}

		if (object instanceof DirContext) {

			try {
				dirContext.createSubcontext(dest);
			} catch (NamingException e) {
				errorList.put(dest,
						new Integer(WebdavServletWebdavStatus.getScConflict()));
				return false;
			}

			try {
				NamingEnumeration<NameClassPair> enumeration = dirContext
						.list(source);
				while (enumeration.hasMoreElements()) {
					NameClassPair ncPair = enumeration.nextElement();
					String childDest = dest;
					if (!childDest.equals("/"))
						childDest += "/";
					childDest += ncPair.getName();
					String childSrc = source;
					if (!childSrc.equals("/"))
						childSrc += "/";
					childSrc += ncPair.getName();
					copyResource(dirContext, errorList, childSrc, childDest);
				}
			} catch (NamingException e) {
				errorList.put(
						dest,
						new Integer(WebdavServletWebdavStatus
								.getScInternalServerError()));
				return false;
			}

		} else {

			if (object instanceof Resource) {
				try {
					dirContext.bind(dest, object);
				} catch (NamingException e) {
					if (e.getCause() instanceof FileNotFoundException) {
						// We know the source exists so it must be the
						// destination dir that can't be found
						errorList.put(source, new Integer(
								WebdavServletWebdavStatus.getScConflict()));
					} else {
						errorList.put(
								source,
								new Integer(WebdavServletWebdavStatus
										.getScInternalServerError()));
					}
					return false;
				}
			} else {
				errorList.put(
						source,
						new Integer(WebdavServletWebdavStatus
								.getScInternalServerError()));
				return false;
			}

		}

		return true;

	}

	/**
	 * Delete a resource.
	 *
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @return boolean true if the copy is successful
	 */
	private boolean deleteResource(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {

		String path = getRelativePath(req);

		return deleteResource(path, req, resp, true);

	}

	/**
	 * Delete a resource.
	 *
	 * @param path
	 *            Path of the resource which is to be deleted
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @param setStatus
	 *            Should the response status be set on successful completion
	 */
	private boolean deleteResource(String path, HttpServletRequest req,
			HttpServletResponse resp, boolean setStatus) throws IOException {

		String ifHeader = req.getHeader("If");
		if (ifHeader == null)
			ifHeader = "";

		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null)
			lockTokenHeader = "";

		if (isLocked(path, ifHeader + lockTokenHeader)) {
			resp.sendError(WebdavServletWebdavStatus.getScLocked());
			return false;
		}

		boolean exists = true;
		Object object = null;
		try {
			object = getResources().lookup(path);
		} catch (NamingException e) {
			exists = false;
		}

		if (!exists) {
			resp.sendError(WebdavServletWebdavStatus.getScNotFound());
			return false;
		}

		boolean collection = (object instanceof DirContext);

		if (!collection) {
			try {
				getResources().unbind(path);
			} catch (NamingException e) {
				resp.sendError(WebdavServletWebdavStatus
						.getScInternalServerError());
				return false;
			}
		} else {

			Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

			deleteCollection(req, getResources(), path, errorList);
			try {
				getResources().unbind(path);
			} catch (NamingException e) {
				errorList.put(
						path,
						new Integer(WebdavServletWebdavStatus
								.getScInternalServerError()));
			}

			if (!errorList.isEmpty()) {

				sendReport(req, resp, errorList);
				return false;

			}

		}
		if (setStatus) {
			resp.setStatus(WebdavServletWebdavStatus.getScNoContent());
		}
		return true;

	}

	/**
	 * Deletes a collection.
	 *
	 * @param dirContext
	 *            Resources implementation associated with the context
	 * @param path
	 *            Path to the collection to be deleted
	 * @param errorList
	 *            Contains the list of the errors which occurred
	 */
	private void deleteCollection(HttpServletRequest req,
			DirContext dirContext, String path,
			Hashtable<String, Integer> errorList) {

		if (getDebug() > 1)
			log("Delete:" + path);

		// Prevent deletion of special subdirectories
		if (isSpecialPath(path)) {
			errorList.put(path,
					new Integer(WebdavServletWebdavStatus.getScForbidden()));
			return;
		}

		String ifHeader = req.getHeader("If");
		if (ifHeader == null)
			ifHeader = "";

		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null)
			lockTokenHeader = "";

		Enumeration<NameClassPair> enumeration = null;
		try {
			enumeration = dirContext.list(path);
		} catch (NamingException e) {
			errorList.put(
					path,
					new Integer(WebdavServletWebdavStatus
							.getScInternalServerError()));
			return;
		}

		while (enumeration.hasMoreElements()) {
			NameClassPair ncPair = enumeration.nextElement();
			String childName = path;
			if (!childName.equals("/"))
				childName += "/";
			childName += ncPair.getName();

			if (isLocked(childName, ifHeader + lockTokenHeader)) {

				errorList.put(childName,
						new Integer(WebdavServletWebdavStatus.getScLocked()));

			} else {

				try {
					Object object = dirContext.lookup(childName);
					if (object instanceof DirContext) {
						deleteCollection(req, dirContext, childName, errorList);
					}

					try {
						dirContext.unbind(childName);
					} catch (NamingException e) {
						if (!(object instanceof DirContext)) {
							// If it's not a collection, then it's an unknown
							// error
							errorList.put(
									childName,
									new Integer(WebdavServletWebdavStatus
											.getScInternalServerError()));
						}
					}
				} catch (NamingException e) {
					errorList.put(
							childName,
							new Integer(WebdavServletWebdavStatus
									.getScInternalServerError()));
				}
			}

		}

	}

	/**
	 * Send a multistatus element containing a complete error report to the
	 * client.
	 *
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @param errorList
	 *            List of error to be displayed
	 */
	private void sendReport(HttpServletRequest req, HttpServletResponse resp,
			Hashtable<String, Integer> errorList) throws IOException {

		resp.setStatus(WebdavServletWebdavStatus.getScMultiStatus());

		String absoluteUri = req.getRequestURI();
		String relativePath = getRelativePath(req);

		XMLWriter generatedXML = new XMLWriter();
		generatedXML.writeXMLHeader();

		generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus",
				XMLWriter.getOpening());

		Enumeration<String> pathList = errorList.keys();
		while (pathList.hasMoreElements()) {

			String errorPath = pathList.nextElement();
			int errorCode = errorList.get(errorPath).intValue();

			generatedXML.writeElement("D", "response", XMLWriter.getOpening());

			generatedXML.writeElement("D", "href", XMLWriter.getOpening());
			String toAppend = errorPath.substring(relativePath.length());
			if (!toAppend.startsWith("/"))
				toAppend = "/" + toAppend;
			generatedXML.writeText(absoluteUri + toAppend);
			generatedXML.writeElement("D", "href", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText("HTTP/1.1 " + errorCode + " "
					+ WebdavServletWebdavStatus.getStatusText(errorCode));
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());

			generatedXML.writeElement("D", "response", XMLWriter.getClosing());

		}

		generatedXML.writeElement("D", "multistatus", XMLWriter.getClosing());

		Writer writer = resp.getWriter();
		writer.write(generatedXML.toString());
		writer.close();

	}

	/**
	 * Propfind helper method.
	 *
	 * @param req
	 *            The servlet request
	 * @param resources
	 *            Resources object associated with this context
	 * @param generatedXML
	 *            XML response to the Propfind request
	 * @param path
	 *            Path of the current resource
	 * @param type
	 *            Propfind type
	 * @param propertiesVector
	 *            If the propfind type is find properties by name, then this
	 *            Vector contains those properties
	 */
	private void parseProperties(HttpServletRequest req,
			XMLWriter generatedXML, String path, int type,
			Vector<String> propertiesVector) {

		// Exclude any resource in the /WEB-INF and /META-INF subdirectories
		if (isSpecialPath(path))
			return;

		CacheEntry cacheEntry = getResources().lookupCache(path);
		if (!cacheEntry.isExists()) {
			// File is in directory listing but doesn't appear to exist
			// Broken symlink or odd permission settings?
			return;
		}

		generatedXML.writeElement("D", "response", XMLWriter.getOpening());
		String status = "HTTP/1.1 "
				+ WebdavServletWebdavStatus.getScOk()
				+ " "
				+ WebdavServletWebdavStatus
						.getStatusText(WebdavServletWebdavStatus.getScOk());

		// Generating href element
		generatedXML.writeElement("D", "href", XMLWriter.getOpening());

		String href = req.getContextPath() + req.getServletPath();
		if ((href.endsWith("/")) && (path.startsWith("/")))
			href += path.substring(1);
		else
			href += path;
		if ((cacheEntry.getContext() != null) && (!href.endsWith("/")))
			href += "/";

		generatedXML.writeText(rewriteUrl(href));

		generatedXML.writeElement("D", "href", XMLWriter.getClosing());

		String resourceName = path;
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash != -1)
			resourceName = resourceName.substring(lastSlash + 1);

		switch (type) {

		case FIND_ALL_PROP:

			generatedXML.writeElement("D", "propstat", XMLWriter.getOpening());
			generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

			generatedXML
					.writeProperty("D", "creationdate",
							getISOCreationDate(cacheEntry.getAttributes()
									.getCreation()));
			generatedXML.writeElement("D", "displayname",
					XMLWriter.getOpening());
			generatedXML.writeData(resourceName);
			generatedXML.writeElement("D", "displayname",
					XMLWriter.getClosing());
			if (cacheEntry.getResource() != null) {
				generatedXML.writeProperty("D", "getlastmodified",
						FastHttpDateFormat.formatDate(cacheEntry
								.getAttributes().getLastModified(), null));
				generatedXML
						.writeProperty("D", "getcontentlength", String
								.valueOf(cacheEntry.getAttributes()
										.getContentLength()));
				String contentType = getServletContext().getMimeType(
						cacheEntry.getName());
				if (contentType != null) {
					generatedXML.writeProperty("D", "getcontenttype",
							contentType);
				}
				generatedXML.writeProperty("D", "getetag", cacheEntry
						.getAttributes().getETag());
				generatedXML.writeElement("D", "resourcetype",
						XMLWriter.getNoContent());
			} else {
				generatedXML.writeElement("D", "resourcetype",
						XMLWriter.getOpening());
				generatedXML.writeElement("D", "collection",
						XMLWriter.getNoContent());
				generatedXML.writeElement("D", "resourcetype",
						XMLWriter.getClosing());
			}

			generatedXML.writeProperty("D", "source", "");

			String supportedLocks = "<D:lockentry>"
					+ "<D:lockscope><D:exclusive/></D:lockscope>"
					+ "<D:locktype><D:write/></D:locktype>" + "</D:lockentry>"
					+ "<D:lockentry>"
					+ "<D:lockscope><D:shared/></D:lockscope>"
					+ "<D:locktype><D:write/></D:locktype>" + "</D:lockentry>";
			generatedXML.writeElement("D", "supportedlock",
					XMLWriter.getOpening());
			generatedXML.writeText(supportedLocks);
			generatedXML.writeElement("D", "supportedlock",
					XMLWriter.getClosing());

			generateLockDiscovery(path, generatedXML);

			generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());
			generatedXML.writeElement("D", "propstat", XMLWriter.getClosing());

			break;

		case FIND_PROPERTY_NAMES:

			generatedXML.writeElement("D", "propstat", XMLWriter.getOpening());
			generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

			generatedXML.writeElement("D", "creationdate",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "displayname",
					XMLWriter.getNoContent());
			if (cacheEntry.getResource() != null) {
				generatedXML.writeElement("D", "getcontentlanguage",
						XMLWriter.getNoContent());
				generatedXML.writeElement("D", "getcontentlength",
						XMLWriter.getNoContent());
				generatedXML.writeElement("D", "getcontenttype",
						XMLWriter.getNoContent());
				generatedXML.writeElement("D", "getetag",
						XMLWriter.getNoContent());
				generatedXML.writeElement("D", "getlastmodified",
						XMLWriter.getNoContent());
			}
			generatedXML.writeElement("D", "resourcetype",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "source", XMLWriter.getNoContent());
			generatedXML.writeElement("D", "lockdiscovery",
					XMLWriter.getNoContent());

			generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());
			generatedXML.writeElement("D", "propstat", XMLWriter.getClosing());

			break;

		case FIND_BY_PROPERTY:

			Vector<String> propertiesNotFound = new Vector<String>();

			// Parse the list of properties

			generatedXML.writeElement("D", "propstat", XMLWriter.getOpening());
			generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

			Enumeration<String> properties = propertiesVector.elements();

			while (properties.hasMoreElements()) {

				String property = properties.nextElement();

				if (property.equals("creationdate")) {
					generatedXML.writeProperty("D", "creationdate",
							getISOCreationDate(cacheEntry.getAttributes()
									.getCreation()));
				} else if (property.equals("displayname")) {
					generatedXML.writeElement("D", "displayname",
							XMLWriter.getOpening());
					generatedXML.writeData(resourceName);
					generatedXML.writeElement("D", "displayname",
							XMLWriter.getClosing());
				} else if (property.equals("getcontentlanguage")) {
					if (cacheEntry.getContext() != null) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeElement("D", "getcontentlanguage",
								XMLWriter.getNoContent());
					}
				} else if (property.equals("getcontentlength")) {
					if (cacheEntry.getContext() != null) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty("D", "getcontentlength",
								(String.valueOf(cacheEntry.getAttributes()
										.getContentLength())));
					}
				} else if (property.equals("getcontenttype")) {
					if (cacheEntry.getContext() != null) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty(
								"D",
								"getcontenttype",
								getServletContext().getMimeType(
										cacheEntry.getName()));
					}
				} else if (property.equals("getetag")) {
					if (cacheEntry.getContext() != null) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty("D", "getetag", cacheEntry
								.getAttributes().getETag());
					}
				} else if (property.equals("getlastmodified")) {
					if (cacheEntry.getContext() != null) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty("D", "getlastmodified",
								FastHttpDateFormat.formatDate(cacheEntry
										.getAttributes().getLastModified(),
										null));
					}
				} else if (property.equals("resourcetype")) {
					if (cacheEntry.getContext() != null) {
						generatedXML.writeElement("D", "resourcetype",
								XMLWriter.getOpening());
						generatedXML.writeElement("D", "collection",
								XMLWriter.getNoContent());
						generatedXML.writeElement("D", "resourcetype",
								XMLWriter.getClosing());
					} else {
						generatedXML.writeElement("D", "resourcetype",
								XMLWriter.getNoContent());
					}
				} else if (property.equals("source")) {
					generatedXML.writeProperty("D", "source", "");
				} else if (property.equals("supportedlock")) {
					supportedLocks = "<D:lockentry>"
							+ "<D:lockscope><D:exclusive/></D:lockscope>"
							+ "<D:locktype><D:write/></D:locktype>"
							+ "</D:lockentry>" + "<D:lockentry>"
							+ "<D:lockscope><D:shared/></D:lockscope>"
							+ "<D:locktype><D:write/></D:locktype>"
							+ "</D:lockentry>";
					generatedXML.writeElement("D", "supportedlock",
							XMLWriter.getOpening());
					generatedXML.writeText(supportedLocks);
					generatedXML.writeElement("D", "supportedlock",
							XMLWriter.getClosing());
				} else if (property.equals("lockdiscovery")) {
					if (!generateLockDiscovery(path, generatedXML))
						propertiesNotFound.addElement(property);
				} else {
					propertiesNotFound.addElement(property);
				}

			}

			generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());
			generatedXML.writeElement("D", "propstat", XMLWriter.getClosing());

			Enumeration<String> propertiesNotFoundList = propertiesNotFound
					.elements();

			if (propertiesNotFoundList.hasMoreElements()) {

				status = "HTTP/1.1 "
						+ WebdavServletWebdavStatus.getScNotFound()
						+ " "
						+ WebdavServletWebdavStatus
								.getStatusText(WebdavServletWebdavStatus
										.getScNotFound());

				generatedXML.writeElement("D", "propstat",
						XMLWriter.getOpening());
				generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

				while (propertiesNotFoundList.hasMoreElements()) {
					generatedXML.writeElement("D",
							propertiesNotFoundList.nextElement(),
							XMLWriter.getNoContent());
				}

				generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
				generatedXML
						.writeElement("D", "status", XMLWriter.getOpening());
				generatedXML.writeText(status);
				generatedXML
						.writeElement("D", "status", XMLWriter.getClosing());
				generatedXML.writeElement("D", "propstat",
						XMLWriter.getClosing());

			}

			break;

		}

		generatedXML.writeElement("D", "response", XMLWriter.getClosing());

	}

	/**
	 * Propfind helper method. Displays the properties of a lock-null resource.
	 *
	 * @param resources
	 *            Resources object associated with this context
	 * @param generatedXML
	 *            XML response to the Propfind request
	 * @param path
	 *            Path of the current resource
	 * @param type
	 *            Propfind type
	 * @param propertiesVector
	 *            If the propfind type is find properties by name, then this
	 *            Vector contains those properties
	 */
	private void parseLockNullProperties(HttpServletRequest req,
			XMLWriter generatedXML, String path, int type,
			Vector<String> propertiesVector) {

		// Exclude any resource in the /WEB-INF and /META-INF subdirectories
		if (isSpecialPath(path))
			return;

		// Retrieving the lock associated with the lock-null resource
		WebdavServletLockInfo lock = resourceLocks.get(path);

		if (lock == null)
			return;

		generatedXML.writeElement("D", "response", XMLWriter.getOpening());
		String status = "HTTP/1.1 "
				+ WebdavServletWebdavStatus.getScOk()
				+ " "
				+ WebdavServletWebdavStatus
						.getStatusText(WebdavServletWebdavStatus.getScOk());

		// Generating href element
		generatedXML.writeElement("D", "href", XMLWriter.getOpening());

		String absoluteUri = req.getRequestURI();
		String relativePath = getRelativePath(req);
		String toAppend = path.substring(relativePath.length());
		if (!toAppend.startsWith("/"))
			toAppend = "/" + toAppend;

		generatedXML.writeText(rewriteUrl(RequestUtil2.normalize(absoluteUri
				+ toAppend)));

		generatedXML.writeElement("D", "href", XMLWriter.getClosing());

		String resourceName = path;
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash != -1)
			resourceName = resourceName.substring(lastSlash + 1);

		switch (type) {

		case FIND_ALL_PROP:

			generatedXML.writeElement("D", "propstat", XMLWriter.getOpening());
			generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

			generatedXML.writeProperty("D", "creationdate",
					getISOCreationDate(lock.getCreationDate().getTime()));
			generatedXML.writeElement("D", "displayname",
					XMLWriter.getOpening());
			generatedXML.writeData(resourceName);
			generatedXML.writeElement("D", "displayname",
					XMLWriter.getClosing());
			generatedXML.writeProperty("D", "getlastmodified",
					FastHttpDateFormat.formatDate(lock.getCreationDate().getTime(),
							null));
			generatedXML.writeProperty("D", "getcontentlength",
					String.valueOf(0));
			generatedXML.writeProperty("D", "getcontenttype", "");
			generatedXML.writeProperty("D", "getetag", "");
			generatedXML.writeElement("D", "resourcetype",
					XMLWriter.getOpening());
			generatedXML.writeElement("D", "lock-null",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "resourcetype",
					XMLWriter.getClosing());

			generatedXML.writeProperty("D", "source", "");

			String supportedLocks = "<D:lockentry>"
					+ "<D:lockscope><D:exclusive/></D:lockscope>"
					+ "<D:locktype><D:write/></D:locktype>" + "</D:lockentry>"
					+ "<D:lockentry>"
					+ "<D:lockscope><D:shared/></D:lockscope>"
					+ "<D:locktype><D:write/></D:locktype>" + "</D:lockentry>";
			generatedXML.writeElement("D", "supportedlock",
					XMLWriter.getOpening());
			generatedXML.writeText(supportedLocks);
			generatedXML.writeElement("D", "supportedlock",
					XMLWriter.getClosing());

			generateLockDiscovery(path, generatedXML);

			generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());
			generatedXML.writeElement("D", "propstat", XMLWriter.getClosing());

			break;

		case FIND_PROPERTY_NAMES:

			generatedXML.writeElement("D", "propstat", XMLWriter.getOpening());
			generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

			generatedXML.writeElement("D", "creationdate",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "displayname",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "getcontentlanguage",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "getcontentlength",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "getcontenttype",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "getetag", XMLWriter.getNoContent());
			generatedXML.writeElement("D", "getlastmodified",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "resourcetype",
					XMLWriter.getNoContent());
			generatedXML.writeElement("D", "source", XMLWriter.getNoContent());
			generatedXML.writeElement("D", "lockdiscovery",
					XMLWriter.getNoContent());

			generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());
			generatedXML.writeElement("D", "propstat", XMLWriter.getClosing());

			break;

		case FIND_BY_PROPERTY:

			Vector<String> propertiesNotFound = new Vector<String>();

			// Parse the list of properties

			generatedXML.writeElement("D", "propstat", XMLWriter.getOpening());
			generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

			Enumeration<String> properties = propertiesVector.elements();

			while (properties.hasMoreElements()) {

				String property = properties.nextElement();

				if (property.equals("creationdate")) {
					generatedXML.writeProperty("D", "creationdate",
							getISOCreationDate(lock.getCreationDate().getTime()));
				} else if (property.equals("displayname")) {
					generatedXML.writeElement("D", "displayname",
							XMLWriter.getOpening());
					generatedXML.writeData(resourceName);
					generatedXML.writeElement("D", "displayname",
							XMLWriter.getClosing());
				} else if (property.equals("getcontentlanguage")) {
					generatedXML.writeElement("D", "getcontentlanguage",
							XMLWriter.getNoContent());
				} else if (property.equals("getcontentlength")) {
					generatedXML.writeProperty("D", "getcontentlength",
							(String.valueOf(0)));
				} else if (property.equals("getcontenttype")) {
					generatedXML.writeProperty("D", "getcontenttype", "");
				} else if (property.equals("getetag")) {
					generatedXML.writeProperty("D", "getetag", "");
				} else if (property.equals("getlastmodified")) {
					generatedXML.writeProperty(
							"D",
							"getlastmodified",
							FastHttpDateFormat.formatDate(
									lock.getCreationDate().getTime(), null));
				} else if (property.equals("resourcetype")) {
					generatedXML.writeElement("D", "resourcetype",
							XMLWriter.getOpening());
					generatedXML.writeElement("D", "lock-null",
							XMLWriter.getNoContent());
					generatedXML.writeElement("D", "resourcetype",
							XMLWriter.getClosing());
				} else if (property.equals("source")) {
					generatedXML.writeProperty("D", "source", "");
				} else if (property.equals("supportedlock")) {
					supportedLocks = "<D:lockentry>"
							+ "<D:lockscope><D:exclusive/></D:lockscope>"
							+ "<D:locktype><D:write/></D:locktype>"
							+ "</D:lockentry>" + "<D:lockentry>"
							+ "<D:lockscope><D:shared/></D:lockscope>"
							+ "<D:locktype><D:write/></D:locktype>"
							+ "</D:lockentry>";
					generatedXML.writeElement("D", "supportedlock",
							XMLWriter.getOpening());
					generatedXML.writeText(supportedLocks);
					generatedXML.writeElement("D", "supportedlock",
							XMLWriter.getClosing());
				} else if (property.equals("lockdiscovery")) {
					if (!generateLockDiscovery(path, generatedXML))
						propertiesNotFound.addElement(property);
				} else {
					propertiesNotFound.addElement(property);
				}

			}

			generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
			generatedXML.writeElement("D", "status", XMLWriter.getOpening());
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.getClosing());
			generatedXML.writeElement("D", "propstat", XMLWriter.getClosing());

			Enumeration<String> propertiesNotFoundList = propertiesNotFound
					.elements();

			if (propertiesNotFoundList.hasMoreElements()) {

				status = "HTTP/1.1 "
						+ WebdavServletWebdavStatus.getScNotFound()
						+ " "
						+ WebdavServletWebdavStatus
								.getStatusText(WebdavServletWebdavStatus
										.getScNotFound());

				generatedXML.writeElement("D", "propstat",
						XMLWriter.getOpening());
				generatedXML.writeElement("D", "prop", XMLWriter.getOpening());

				while (propertiesNotFoundList.hasMoreElements()) {
					generatedXML.writeElement("D",
							propertiesNotFoundList.nextElement(),
							XMLWriter.getNoContent());
				}

				generatedXML.writeElement("D", "prop", XMLWriter.getClosing());
				generatedXML
						.writeElement("D", "status", XMLWriter.getOpening());
				generatedXML.writeText(status);
				generatedXML
						.writeElement("D", "status", XMLWriter.getClosing());
				generatedXML.writeElement("D", "propstat",
						XMLWriter.getClosing());

			}

			break;

		}

		generatedXML.writeElement("D", "response", XMLWriter.getClosing());

	}

	/**
	 * Print the lock discovery information associated with a path.
	 *
	 * @param path
	 *            Path
	 * @param generatedXML
	 *            XML data to which the locks info will be appended
	 * @return true if at least one lock was displayed
	 */
	private boolean generateLockDiscovery(String path, XMLWriter generatedXML) {

		WebdavServletLockInfo resourceLock = resourceLocks.get(path);
		Enumeration<WebdavServletLockInfo> collectionLocksList = collectionLocks
				.elements();

		boolean wroteStart = false;

		if (resourceLock != null) {
			wroteStart = true;
			generatedXML.writeElement("D", "lockdiscovery",
					XMLWriter.getOpening());
			resourceLock.toXML(generatedXML);
		}

		while (collectionLocksList.hasMoreElements()) {
			WebdavServletLockInfo currentLock = collectionLocksList
					.nextElement();
			if (path.startsWith(currentLock.getPath())) {
				if (!wroteStart) {
					wroteStart = true;
					generatedXML.writeElement("D", "lockdiscovery",
							XMLWriter.getOpening());
				}
				currentLock.toXML(generatedXML);
			}
		}

		if (wroteStart) {
			generatedXML.writeElement("D", "lockdiscovery",
					XMLWriter.getClosing());
		} else {
			return false;
		}

		return true;

	}

	/**
	 * Get creation date in ISO format.
	 */
	private String getISOCreationDate(long creationDate) {
		StringBuilder creationDateValue = new StringBuilder(
				creationDateFormat.format(new Date(creationDate)));
		/*
		 * int offset = Calendar.getInstance().getTimeZone().getRawOffset() /
		 * 3600000; // FIXME ? if (offset < 0) { creationDateValue.append("-");
		 * offset = -offset; } else if (offset > 0) {
		 * creationDateValue.append("+"); } if (offset != 0) { if (offset < 10)
		 * creationDateValue.append("0"); creationDateValue.append(offset +
		 * ":00"); } else { creationDateValue.append("Z"); }
		 */
		return creationDateValue.toString();
	}

	/**
	 * Determines the methods normally allowed for the resource.
	 *
	 */
	private StringBuilder determineMethodsAllowed(DirContext dirContext,
			HttpServletRequest req) {

		StringBuilder methodsAllowed = new StringBuilder();
		boolean exists = true;
		Object object = null;
		try {
			String path = getRelativePath(req);

			object = dirContext.lookup(path);
		} catch (NamingException e) {
			exists = false;
		}

		if (!exists) {
			methodsAllowed.append("OPTIONS, MKCOL, PUT, LOCK");
			return methodsAllowed;
		}

		methodsAllowed.append("OPTIONS, GET, HEAD, POST, DELETE, TRACE");
		methodsAllowed.append(", PROPPATCH, COPY, MOVE, LOCK, UNLOCK");

		if (isListings()) {
			methodsAllowed.append(", PROPFIND");
		}

		if (!(object instanceof DirContext)) {
			methodsAllowed.append(", PUT");
		}

		return methodsAllowed;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

}