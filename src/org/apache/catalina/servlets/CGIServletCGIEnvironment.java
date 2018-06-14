package org.apache.catalina.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.util.IOTools;

/**
 * Encapsulates the CGI environment and rules to derive
 * that environment from the servlet container and request information.
 *
 * <p>
 * </p>
 *
 * @since    Tomcat 4.0
 */
public class CGIServletCGIEnvironment {


    /**
	 * 
	 */
	private final CGIServlet cgiServlet;

	/** context of the enclosing servlet */
    private ServletContext context = null;

    /** context path of enclosing servlet */
    private String contextPath = null;

    /** servlet URI of the enclosing servlet */
    private String servletPath = null;

    /** pathInfo for the current request */
    private String pathInfo = null;

    /** real file system directory of the enclosing servlet's web app */
    private String webAppRootDir = null;

    /** tempdir for context - used to expand scripts in unexpanded wars */
    private File tmpDir = null;

    /** derived cgi environment */
    private Hashtable<String, String> env = null;

    /** cgi command to be invoked */
    private String command = null;

    /** cgi command's desired working directory */
    private File workingDirectory = null;

    /** cgi command's command line parameters */
    private ArrayList<String> cmdLineParameters = new ArrayList<String>();

    /** whether or not this object is valid or not */
    private boolean valid = false;


    /**
     * Creates a CGIEnvironment and derives the necessary environment,
     * query parameters, working directory, cgi command, etc.
     *
     * @param  req       HttpServletRequest for information provided by
     *                   the Servlet API
     * @param  context   ServletContext for information provided by the
     *                   Servlet API
     * @param cgiServlet TODO
     *
     */
    public CGIServletCGIEnvironment(CGIServlet cgiServlet, HttpServletRequest req,
                             ServletContext context) throws IOException {
        this.cgiServlet = cgiServlet;
		setupFromContext(context);
        setupFromRequest(req);

        this.valid = setCGIEnvironment(req);

        if (this.valid) {
            workingDirectory = new File(command.substring(0,
                  command.lastIndexOf(File.separator)));
        }

    }


    /**
     * Uses the ServletContext to set some CGI variables
     *
     * @param  context   ServletContext for information provided by the
     *                   Servlet API
     */
    protected void setupFromContext(ServletContext context) {
        this.context = context;
        this.webAppRootDir = context.getRealPath("/");
        this.tmpDir = (File) context.getAttribute(ServletContext.TEMPDIR);
    }


    /**
     * Uses the HttpServletRequest to set most CGI variables
     *
     * @param  req   HttpServletRequest for information provided by
     *               the Servlet API
     * @throws UnsupportedEncodingException
     */
    protected void setupFromRequest(HttpServletRequest req)
            throws UnsupportedEncodingException {

        boolean isIncluded = false;

        // Look to see if this request is an include
        if (req.getAttribute(
                RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
            isIncluded = true;
        }
        if (isIncluded) {
            this.contextPath = (String) req.getAttribute(
                    RequestDispatcher.INCLUDE_CONTEXT_PATH);
            this.servletPath = (String) req.getAttribute(
                    RequestDispatcher.INCLUDE_SERVLET_PATH);
            this.pathInfo = (String) req.getAttribute(
                    RequestDispatcher.INCLUDE_PATH_INFO);
        } else {
            this.contextPath = req.getContextPath();
            this.servletPath = req.getServletPath();
            this.pathInfo = req.getPathInfo();
        }
        // If getPathInfo() returns null, must be using extension mapping
        // In this case, pathInfo should be same as servletPath
        if (this.pathInfo == null) {
            this.pathInfo = this.servletPath;
        }

        // If the request method is GET, POST or HEAD and the query string
        // does not contain an unencoded "=" this is an indexed query.
        // The parsed query string becomes the command line parameters
        // for the cgi command.
        if (req.getMethod().equals("GET")
            || req.getMethod().equals("POST")
            || req.getMethod().equals("HEAD")) {
            String qs;
            if (isIncluded) {
                qs = (String) req.getAttribute(
                        RequestDispatcher.INCLUDE_QUERY_STRING);
            } else {
                qs = req.getQueryString();
            }
            if (qs != null && qs.indexOf("=") == -1) {
                StringTokenizer qsTokens = new StringTokenizer(qs, "+");
                while ( qsTokens.hasMoreTokens() ) {
                    cmdLineParameters.add(URLDecoder.decode(qsTokens.nextToken(),
                                          this.cgiServlet.getParameterEncoding()));
                }
            }
        }
    }


    /**
     * Resolves core information about the cgi script.
     *
     * <p>
     * Example URI:
     * <PRE> /servlet/cgigateway/dir1/realCGIscript/pathinfo1 </PRE>
     * <ul>
     * <LI><b>path</b> = $CATALINA_HOME/mywebapp/dir1/realCGIscript
     * <LI><b>scriptName</b> = /servlet/cgigateway/dir1/realCGIscript
     * <LI><b>cgiName</b> = /dir1/realCGIscript
     * <LI><b>name</b> = realCGIscript
     * </ul>
     * </p>
     * <p>
     * CGI search algorithm: search the real path below
     *    &lt;my-webapp-root&gt; and find the first non-directory in
     *    the getPathTranslated("/"), reading/searching from left-to-right.
     *</p>
     *<p>
     *   The CGI search path will start at
     *   webAppRootDir + File.separator + cgiPathPrefix
     *   (or webAppRootDir alone if cgiPathPrefix is
     *   null).
     *</p>
     *<p>
     *   cgiPathPrefix is defined by setting
     *   this servlet's cgiPathPrefix init parameter
     *
     *</p>
     *
     * @param pathInfo       String from HttpServletRequest.getPathInfo()
     * @param webAppRootDir  String from context.getRealPath("/")
     * @param contextPath    String as from
     *                       HttpServletRequest.getContextPath()
     * @param servletPath    String as from
     *                       HttpServletRequest.getServletPath()
     * @param cgiPathPrefix  subdirectory of webAppRootDir below which
     *                       the web app's CGIs may be stored; can be null.
     *                       The CGI search path will start at
     *                       webAppRootDir + File.separator + cgiPathPrefix
     *                       (or webAppRootDir alone if cgiPathPrefix is
     *                       null).  cgiPathPrefix is defined by setting
     *                       the servlet's cgiPathPrefix init parameter.
     *
     *
     * @return
     * <ul>
     * <li>
     * <code>path</code> -    full file-system path to valid cgi script,
     *                        or null if no cgi was found
     * <li>
     * <code>scriptName</code> -
     *                        CGI variable SCRIPT_NAME; the full URL path
     *                        to valid cgi script or null if no cgi was
     *                        found
     * <li>
     * <code>cgiName</code> - servlet pathInfo fragment corresponding to
     *                        the cgi script itself, or null if not found
     * <li>
     * <code>name</code> -    simple name (no directories) of the
     *                        cgi script, or null if no cgi was found
     * </ul>
     *
     * @since Tomcat 4.0
     */
    protected String[] findCGI(String pathInfo, String webAppRootDir,
                               String contextPath, String servletPath,
                               String cgiPathPrefix) {
        String path = null;
        String name = null;
        String scriptname = null;
        String cginame = "";

        if ((webAppRootDir != null)
            && (webAppRootDir.lastIndexOf(File.separator) ==
                (webAppRootDir.length() - 1))) {
                //strip the trailing "/" from the webAppRootDir
                webAppRootDir =
                webAppRootDir.substring(0, (webAppRootDir.length() - 1));
        }

        if (cgiPathPrefix != null) {
            webAppRootDir = webAppRootDir + File.separator
                + cgiPathPrefix;
        }

        if (this.cgiServlet.getDebug() >= 2) {
            this.cgiServlet.log("findCGI: path=" + pathInfo + ", " + webAppRootDir);
        }

        File currentLocation = new File(webAppRootDir);
        StringTokenizer dirWalker =
        new StringTokenizer(pathInfo, "/");
        if (this.cgiServlet.getDebug() >= 3) {
            this.cgiServlet.log("findCGI: currentLoc=" + currentLocation);
        }
        while (!currentLocation.isFile() && dirWalker.hasMoreElements()) {
            if (this.cgiServlet.getDebug() >= 3) {
                this.cgiServlet.log("findCGI: currentLoc=" + currentLocation);
            }
            String nextElement = (String) dirWalker.nextElement();
            currentLocation = new File(currentLocation, nextElement);
            cginame = cginame + "/" + nextElement;
        }
        if (!currentLocation.isFile()) {
            return new String[] { null, null, null, null };
        }

        if (this.cgiServlet.getDebug() >= 2) {
            this.cgiServlet.log("findCGI: FOUND cgi at " + currentLocation);
        }
        path = currentLocation.getAbsolutePath();
        name = currentLocation.getName();

        if (".".equals(contextPath)) {
            scriptname = servletPath;
        } else {
            scriptname = contextPath + servletPath;
        }
        if (!servletPath.equals(cginame)) {
            scriptname = scriptname + cginame;
        }

        if (this.cgiServlet.getDebug() >= 1) {
            this.cgiServlet.log("findCGI calc: name=" + name + ", path=" + path
                + ", scriptname=" + scriptname + ", cginame=" + cginame);
        }
        return new String[] { path, scriptname, cginame, name };
    }

    /**
     * Constructs the CGI environment to be supplied to the invoked CGI
     * script; relies heavily on Servlet API methods and findCGI
     *
     * @param    req request associated with the CGI
     *           Invocation
     *
     * @return   true if environment was set OK, false if there
     *           was a problem and no environment was set
     */
    protected boolean setCGIEnvironment(HttpServletRequest req) throws IOException {

        /*
         * This method is slightly ugly; c'est la vie.
         * "You cannot stop [ugliness], you can only hope to contain [it]"
         * (apologies to Marv Albert regarding MJ)
         */

        Hashtable<String,String> envp = new Hashtable<String,String>();

        // Add the shell environment variables (if any)
        envp.putAll(this.cgiServlet.getShellEnv());

        // Add the CGI environment variables
        String sPathInfoOrig = null;
        String sPathInfoCGI = null;
        String sPathTranslatedCGI = null;
        String sCGIFullPath = null;
        String sCGIScriptName = null;
        String sCGIFullName = null;
        String sCGIName = null;
        String[] sCGINames;


        sPathInfoOrig = this.pathInfo;
        sPathInfoOrig = sPathInfoOrig == null ? "" : sPathInfoOrig;

        if (webAppRootDir == null ) {
            // The app has not been deployed in exploded form
            webAppRootDir = tmpDir.toString();
            expandCGIScript();
        }

        sCGINames = findCGI(sPathInfoOrig,
                            webAppRootDir,
                            contextPath,
                            servletPath,
                            this.cgiServlet.getCgiPathPrefix());

        sCGIFullPath = sCGINames[0];
        sCGIScriptName = sCGINames[1];
        sCGIFullName = sCGINames[2];
        sCGIName = sCGINames[3];

        if (sCGIFullPath == null
            || sCGIScriptName == null
            || sCGIFullName == null
            || sCGIName == null) {
            return false;
        }

        envp.put("SERVER_SOFTWARE", "TOMCAT");

        envp.put("SERVER_NAME", nullsToBlanks(req.getServerName()));

        envp.put("GATEWAY_INTERFACE", "CGI/1.1");

        envp.put("SERVER_PROTOCOL", nullsToBlanks(req.getProtocol()));

        int port = req.getServerPort();
        Integer iPort =
            (port == 0 ? Integer.valueOf(-1) : Integer.valueOf(port));
        envp.put("SERVER_PORT", iPort.toString());

        envp.put("REQUEST_METHOD", nullsToBlanks(req.getMethod()));

        envp.put("REQUEST_URI", nullsToBlanks(req.getRequestURI()));


        /*-
         * PATH_INFO should be determined by using sCGIFullName:
         * 1) Let sCGIFullName not end in a "/" (see method findCGI)
         * 2) Let sCGIFullName equal the pathInfo fragment which
         *    corresponds to the actual cgi script.
         * 3) Thus, PATH_INFO = request.getPathInfo().substring(
         *                      sCGIFullName.length())
         *
         * (see method findCGI, where the real work is done)
         *
         */
        if (pathInfo == null
            || (pathInfo.substring(sCGIFullName.length()).length() <= 0)) {
            sPathInfoCGI = "";
        } else {
            sPathInfoCGI = pathInfo.substring(sCGIFullName.length());
        }
        envp.put("PATH_INFO", sPathInfoCGI);


        /*-
         * PATH_TRANSLATED must be determined after PATH_INFO (and the
         * implied real cgi-script) has been taken into account.
         *
         * The following example demonstrates:
         *
         * servlet info   = /servlet/cgigw/dir1/dir2/cgi1/trans1/trans2
         * cgifullpath    = /servlet/cgigw/dir1/dir2/cgi1
         * path_info      = /trans1/trans2
         * webAppRootDir  = servletContext.getRealPath("/")
         *
         * path_translated = servletContext.getRealPath("/trans1/trans2")
         *
         * That is, PATH_TRANSLATED = webAppRootDir + sPathInfoCGI
         * (unless sPathInfoCGI is null or blank, then the CGI
         * specification dictates that the PATH_TRANSLATED metavariable
         * SHOULD NOT be defined.
         *
         */
        if (sPathInfoCGI != null && !("".equals(sPathInfoCGI))) {
            sPathTranslatedCGI = context.getRealPath(sPathInfoCGI);
        }
        if (sPathTranslatedCGI == null || "".equals(sPathTranslatedCGI)) {
            //NOOP
        } else {
            envp.put("PATH_TRANSLATED", nullsToBlanks(sPathTranslatedCGI));
        }


        envp.put("SCRIPT_NAME", nullsToBlanks(sCGIScriptName));

        envp.put("QUERY_STRING", nullsToBlanks(req.getQueryString()));

        envp.put("REMOTE_HOST", nullsToBlanks(req.getRemoteHost()));

        envp.put("REMOTE_ADDR", nullsToBlanks(req.getRemoteAddr()));

        envp.put("AUTH_TYPE", nullsToBlanks(req.getAuthType()));

        envp.put("REMOTE_USER", nullsToBlanks(req.getRemoteUser()));

        envp.put("REMOTE_IDENT", ""); //not necessary for full compliance

        envp.put("CONTENT_TYPE", nullsToBlanks(req.getContentType()));


        /* Note CGI spec says CONTENT_LENGTH must be NULL ("") or undefined
         * if there is no content, so we cannot put 0 or -1 in as per the
         * Servlet API spec.
         */
        int contentLength = req.getContentLength();
        String sContentLength = (contentLength <= 0 ? "" :
            (Integer.valueOf(contentLength)).toString());
        envp.put("CONTENT_LENGTH", sContentLength);


        Enumeration<String> headers = req.getHeaderNames();
        String header = null;
        while (headers.hasMoreElements()) {
            header = null;
            header = headers.nextElement().toUpperCase(Locale.ENGLISH);
            //REMIND: rewrite multiple headers as if received as single
            //REMIND: change character set
            //REMIND: I forgot what the previous REMIND means
            if ("AUTHORIZATION".equalsIgnoreCase(header) ||
                "PROXY_AUTHORIZATION".equalsIgnoreCase(header)) {
                //NOOP per CGI specification section 11.2
            } else {
                envp.put("HTTP_" + header.replace('-', '_'),
                         req.getHeader(header));
            }
        }

        File fCGIFullPath = new File(sCGIFullPath);
        command = fCGIFullPath.getCanonicalPath();

        envp.put("X_TOMCAT_SCRIPT_PATH", command);  //for kicks

        envp.put("SCRIPT_FILENAME", command);  //for PHP

        this.env = envp;

        return true;

    }

    /**
     * Extracts requested resource from web app archive to context work
     * directory to enable CGI script to be executed.
     */
    protected void expandCGIScript() {
        StringBuilder srcPath = new StringBuilder();
        StringBuilder destPath = new StringBuilder();
        InputStream is = null;

        // paths depend on mapping
        if (this.cgiServlet.getCgiPathPrefix() == null ) {
            srcPath.append(pathInfo);
            is = context.getResourceAsStream(srcPath.toString());
            destPath.append(tmpDir);
            destPath.append(pathInfo);
        } else {
            // essentially same search algorithm as findCGI()
            srcPath.append(this.cgiServlet.getCgiPathPrefix());
            StringTokenizer pathWalker =
                    new StringTokenizer (pathInfo, "/");
            // start with first element
            while (pathWalker.hasMoreElements() && (is == null)) {
                srcPath.append("/");
                srcPath.append(pathWalker.nextElement());
                is = context.getResourceAsStream(srcPath.toString());
            }
            destPath.append(tmpDir);
            destPath.append("/");
            destPath.append(srcPath);
        }

        if (is == null) {
            // didn't find anything, give up now
            if (this.cgiServlet.getDebug() >= 2) {
                this.cgiServlet.log("expandCGIScript: source '" + srcPath + "' not found");
            }
             return;
        }

        File f = new File(destPath.toString());
        if (f.exists()) {
            // Don't need to expand if it already exists
            return;
        }

        // create directories
        String dirPath = destPath.toString().substring(
                0,destPath.toString().lastIndexOf("/"));
        File dir = new File(dirPath);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            if (this.cgiServlet.getDebug() >= 2) {
                this.cgiServlet.log("expandCGIScript: failed to create directories for '" +
                        dir.getAbsolutePath() + "'");
            }
            return;
        }

        try {
            synchronized (CGIServlet.getExpandFileLock()) {
                // make sure file doesn't exist
                if (f.exists()) {
                    return;
                }

                // create file
                if (!f.createNewFile()) {
                    return;
                }
                FileOutputStream fos = new FileOutputStream(f);

                // copy data
                IOTools.flow(is, fos);
                is.close();
                fos.close();
                if (this.cgiServlet.getDebug() >= 2) {
                    this.cgiServlet.log("expandCGIScript: expanded '" + srcPath + "' to '" + destPath + "'");
                }
            }
        } catch (IOException ioe) {
            // delete in case file is corrupted
            if (f.exists()) {
                if (!f.delete() && this.cgiServlet.getDebug() >= 2) {
                    this.cgiServlet.log("expandCGIScript: failed to delete '" +
                            f.getAbsolutePath() + "'");
                }
            }
        }
    }


    /**
     * Print important CGI environment information in a easy-to-read HTML
     * table
     *
     * @return  HTML string containing CGI environment info
     *
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("<TABLE border=2>");

        sb.append("<tr><th colspan=2 bgcolor=grey>");
        sb.append("CGIEnvironment Info</th></tr>");

        sb.append("<tr><td>Debug Level</td><td>");
        sb.append(this.cgiServlet.getDebug());
        sb.append("</td></tr>");

        sb.append("<tr><td>Validity:</td><td>");
        sb.append(isValid());
        sb.append("</td></tr>");

        if (isValid()) {
            Enumeration<String> envk = env.keys();
            while (envk.hasMoreElements()) {
                String s = envk.nextElement();
                sb.append("<tr><td>");
                sb.append(s);
                sb.append("</td><td>");
                sb.append(blanksToString(env.get(s),
                                         "[will be set to blank]"));
                sb.append("</td></tr>");
            }
        }

        sb.append("<tr><td colspan=2><HR></td></tr>");

        sb.append("<tr><td>Derived Command</td><td>");
        sb.append(nullsToBlanks(command));
        sb.append("</td></tr>");

        sb.append("<tr><td>Working Directory</td><td>");
        if (workingDirectory != null) {
            sb.append(workingDirectory.toString());
        }
        sb.append("</td></tr>");

        sb.append("<tr><td>Command Line Params</td><td>");
        for (int i=0; i < cmdLineParameters.size(); i++) {
            String param = cmdLineParameters.get(i);
            sb.append("<p>");
            sb.append(param);
            sb.append("</p>");
        }
        sb.append("</td></tr>");

        sb.append("</TABLE><p>end.");

        return sb.toString();
    }


    /**
     * Gets derived command string
     *
     * @return  command string
     *
     */
    protected String getCommand() {
        return command;
    }


    /**
     * Gets derived CGI working directory
     *
     * @return  working directory
     *
     */
    protected File getWorkingDirectory() {
        return workingDirectory;
    }


    /**
     * Gets derived CGI environment
     *
     * @return   CGI environment
     *
     */
    protected Hashtable<String,String> getEnvironment() {
        return env;
    }


    /**
     * Gets derived CGI query parameters
     *
     * @return   CGI query parameters
     *
     */
    protected ArrayList<String> getParameters() {
        return cmdLineParameters;
    }


    /**
     * Gets validity status
     *
     * @return   true if this environment is valid, false
     *           otherwise
     *
     */
    protected boolean isValid() {
        return valid;
    }


    /**
     * Converts null strings to blank strings ("")
     *
     * @param    s string to be converted if necessary
     * @return   a non-null string, either the original or the empty string
     *           ("") if the original was <code>null</code>
     */
    protected String nullsToBlanks(String s) {
        return nullsToString(s, "");
    }


    /**
     * Converts null strings to another string
     *
     * @param    couldBeNull string to be converted if necessary
     * @param    subForNulls string to return instead of a null string
     * @return   a non-null string, either the original or the substitute
     *           string if the original was <code>null</code>
     */
    protected String nullsToString(String couldBeNull,
                                   String subForNulls) {
        return (couldBeNull == null ? subForNulls : couldBeNull);
    }


    /**
     * Converts blank strings to another string
     *
     * @param    couldBeBlank string to be converted if necessary
     * @param    subForBlanks string to return instead of a blank string
     * @return   a non-null string, either the original or the substitute
     *           string if the original was <code>null</code> or empty ("")
     */
    protected String blanksToString(String couldBeBlank,
                                  String subForBlanks) {
        return (("".equals(couldBeBlank) || couldBeBlank == null)
                ? subForBlanks
                : couldBeBlank);
    }


}