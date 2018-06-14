package org.apache.catalina.servlets;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.IOTools;

public class CGIServletCGIRunner {

    /**
	 * 
	 */
	private final CGIServlet cgiServlet;

	/** script/command to be executed */
    private String command = null;

    /** environment used when invoking the cgi script */
    private Hashtable<String,String> env = null;

    /** working directory used when invoking the cgi script */
    private File wd = null;

    /** command line parameters to be passed to the invoked script */
    private ArrayList<String> params = null;

    /** stdin to be passed to cgi script */
    private InputStream stdin = null;

    /** response object used to set headers & get output stream */
    private HttpServletResponse response = null;

    /** boolean tracking whether this object has enough info to run() */
    private boolean readyToRun = false;


    /**
     *  Creates a CGIRunner and initializes its environment, working
     *  directory, and query parameters.
     *  <BR>
     *  Input/output streams (optional) are set using the
     *  <code>setInput</code> and <code>setResponse</code> methods,
     *  respectively.
     *
     * @param  command  string full path to command to be executed
     * @param  env      Hashtable with the desired script environment
     * @param  wd       File with the script's desired working directory
     * @param  params   ArrayList with the script's query command line
     *                  parameters as strings
     * @param cgiServlet TODO
     */
    public CGIServletCGIRunner(CGIServlet cgiServlet, String command, Hashtable<String,String> env,
                        File wd, ArrayList<String> params) {
        this.cgiServlet = cgiServlet;
		this.command = command;
        this.env = env;
        this.wd = wd;
        this.params = params;
        updateReadyStatus();
    }


    /**
     * Checks & sets ready status
     */
    protected void updateReadyStatus() {
        if (command != null
            && env != null
            && wd != null
            && params != null
            && response != null) {
            readyToRun = true;
        } else {
            readyToRun = false;
        }
    }


    /**
     * Gets ready status
     *
     * @return   false if not ready (<code>run</code> will throw
     *           an exception), true if ready
     */
    protected boolean isReady() {
        return readyToRun;
    }


    /**
     * Sets HttpServletResponse object used to set headers and send
     * output to
     *
     * @param  response   HttpServletResponse to be used
     *
     */
    protected void setResponse(HttpServletResponse response) {
        this.response = response;
        updateReadyStatus();
    }


    /**
     * Sets standard input to be passed on to the invoked cgi script
     *
     * @param  stdin   InputStream to be used
     *
     */
    protected void setInput(InputStream stdin) {
        this.stdin = stdin;
        updateReadyStatus();
    }


    /**
     * Converts a Hashtable to a String array by converting each
     * key/value pair in the Hashtable to a String in the form
     * "key=value" (hashkey + "=" + hash.get(hashkey).toString())
     *
     * @param  h   Hashtable to convert
     *
     * @return     converted string array
     *
     * @exception  NullPointerException   if a hash key has a null value
     *
     */
    protected String[] hashToStringArray(Hashtable<String,?> h)
        throws NullPointerException {
        Vector<String> v = new Vector<String>();
        Enumeration<String> e = h.keys();
        while (e.hasMoreElements()) {
            String k = e.nextElement();
            v.add(k + "=" + h.get(k).toString());
        }
        String[] strArr = new String[v.size()];
        v.copyInto(strArr);
        return strArr;
    }


    /**
     * Executes a CGI script with the desired environment, current working
     * directory, and input/output streams
     *
     * <p>
     * This implements the following CGI specification recommedations:
     * <UL>
     * <LI> Servers SHOULD provide the "<code>query</code>" component of
     *      the script-URI as command-line arguments to scripts if it
     *      does not contain any unencoded "=" characters and the
     *      command-line arguments can be generated in an unambiguous
     *      manner.
     * <LI> Servers SHOULD set the AUTH_TYPE metavariable to the value
     *      of the "<code>auth-scheme</code>" token of the
     *      "<code>Authorization</code>" if it was supplied as part of the
     *      request header.  See <code>getCGIEnvironment</code> method.
     * <LI> Where applicable, servers SHOULD set the current working
     *      directory to the directory in which the script is located
     *      before invoking it.
     * <LI> Server implementations SHOULD define their behavior for the
     *      following cases:
     *     <ul>
     *     <LI> <u>Allowed characters in pathInfo</u>:  This implementation
     *             does not allow ASCII NUL nor any character which cannot
     *             be URL-encoded according to internet standards;
     *     <LI> <u>Allowed characters in path segments</u>: This
     *             implementation does not allow non-terminal NULL
     *             segments in the the path -- IOExceptions may be thrown;
     *     <LI> <u>"<code>.</code>" and "<code>..</code>" path
     *             segments</u>:
     *             This implementation does not allow "<code>.</code>" and
     *             "<code>..</code>" in the the path, and such characters
     *             will result in an IOException being thrown (this should
     *             never happen since Tomcat normalises the requestURI
     *             before determining the contextPath, servletPath and
     *             pathInfo);
     *     <LI> <u>Implementation limitations</u>: This implementation
     *             does not impose any limitations except as documented
     *             above.  This implementation may be limited by the
     *             servlet container used to house this implementation.
     *             In particular, all the primary CGI variable values
     *             are derived either directly or indirectly from the
     *             container's implementation of the Servlet API methods.
     *     </ul>
     * </UL>
     * </p>
     *
     * @exception IOException if problems during reading/writing occur
     *
     * @see    java.lang.Runtime#exec(String command, String[] envp,
     *                                File dir)
     */
    protected void run() throws IOException {

        /*
         * REMIND:  this method feels too big; should it be re-written?
         */

        if (!isReady()) {
            throw new IOException(this.getClass().getName()
                                  + ": not ready to run.");
        }

        if (this.cgiServlet.getDebug() >= 1 ) {
            this.cgiServlet.log("runCGI(envp=[" + env + "], command=" + command + ")");
        }

        if ((command.indexOf(File.separator + "." + File.separator) >= 0)
            || (command.indexOf(File.separator + "..") >= 0)
            || (command.indexOf(".." + File.separator) >= 0)) {
            throw new IOException(this.getClass().getName()
                                  + "Illegal Character in CGI command "
                                  + "path ('.' or '..') detected.  Not "
                                  + "running CGI [" + command + "].");
        }

        /* original content/structure of this section taken from
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4216884
         * with major modifications by Martin Dengler
         */
        Runtime rt = null;
        BufferedReader cgiHeaderReader = null;
        InputStream cgiOutput = null;
        BufferedReader commandsStdErr = null;
        Thread errReaderThread = null;
        BufferedOutputStream commandsStdIn = null;
        Process proc = null;
        int bufRead = -1;

        List<String> cmdAndArgs = new ArrayList<String>();
        if (this.cgiServlet.getCgiExecutable().length() != 0) {
            cmdAndArgs.add(this.cgiServlet.getCgiExecutable());
        }
        if (this.cgiServlet.getCgiExecutableArgs() != null) {
            cmdAndArgs.addAll(this.cgiServlet.getCgiExecutableArgs());
        }
        cmdAndArgs.add(command);
        cmdAndArgs.addAll(params);

        try {
            rt = Runtime.getRuntime();
            proc = rt.exec(
                    cmdAndArgs.toArray(new String[cmdAndArgs.size()]),
                    hashToStringArray(env), wd);

            String sContentLength = env.get("CONTENT_LENGTH");

            if(!"".equals(sContentLength)) {
                commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
                IOTools.flow(stdin, commandsStdIn);
                commandsStdIn.flush();
                commandsStdIn.close();
            }

            /* we want to wait for the process to exit,  Process.waitFor()
             * is useless in our situation; see
             * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4223650
             */

            boolean isRunning = true;
            commandsStdErr = new BufferedReader
                (new InputStreamReader(proc.getErrorStream()));
            final BufferedReader stdErrRdr = commandsStdErr ;

            errReaderThread = new Thread() {
                @Override
                public void run () {
                    sendToLog(stdErrRdr) ;
                }
            };
            errReaderThread.start();

            InputStream cgiHeaderStream =
                new CGIServletHTTPHeaderInputStream(proc.getInputStream());
            cgiHeaderReader =
                new BufferedReader(new InputStreamReader(cgiHeaderStream));

            while (isRunning) {
                try {
                    //set headers
                    String line = null;
                    while (((line = cgiHeaderReader.readLine()) != null)
                           && !("".equals(line))) {
                        if (this.cgiServlet.getDebug() >= 2) {
                            this.cgiServlet.log("runCGI: addHeader(\"" + line + "\")");
                        }
                        if (line.startsWith("HTTP")) {
                            response.setStatus(getSCFromHttpStatusLine(line));
                        } else if (line.indexOf(":") >= 0) {
                            String header =
                                line.substring(0, line.indexOf(":")).trim();
                            String value =
                                line.substring(line.indexOf(":") + 1).trim();
                            if (header.equalsIgnoreCase("status")) {
                                response.setStatus(getSCFromCGIStatusHeader(value));
                            } else {
                                response.addHeader(header , value);
                            }
                        } else {
                            this.cgiServlet.log("runCGI: bad header line \"" + line + "\"");
                        }
                    }

                    //write output
                    byte[] bBuf = new byte[2048];

                    OutputStream out = response.getOutputStream();
                    cgiOutput = proc.getInputStream();

                    try {
                        while ((bufRead = cgiOutput.read(bBuf)) != -1) {
                            if (this.cgiServlet.getDebug() >= 4) {
                                this.cgiServlet.log("runCGI: output " + bufRead +
                                    " bytes of data");
                            }
                            out.write(bBuf, 0, bufRead);
                        }
                    } finally {
                        // Attempt to consume any leftover byte if something bad happens,
                        // such as a socket disconnect on the servlet side; otherwise, the
                        // external process could hang
                        if (bufRead != -1) {
                            while ((bufRead = cgiOutput.read(bBuf)) != -1) {
                                // NOOP - just read the data
                            }
                        }
                    }

                    proc.exitValue(); // Throws exception if alive

                    isRunning = false;

                } catch (IllegalThreadStateException e) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                        // Ignore
                    }
                }
            }

        }
        catch (IOException e){
            this.cgiServlet.log ("Caught exception " + e);
            throw e;
        }
        finally{
            // Close the header reader
            if (cgiHeaderReader != null) {
                try {
                    cgiHeaderReader.close();
                } catch (IOException ioe) {
                    this.cgiServlet.log ("Exception closing header reader " + ioe);
                }
            }
            // Close the output stream if used
            if (cgiOutput != null) {
                try {
                    cgiOutput.close();
                } catch (IOException ioe) {
                    this.cgiServlet.log ("Exception closing output stream " + ioe);
                }
            }
            // Make sure the error stream reader has finished
            if (errReaderThread != null) {
                try {
                    errReaderThread.join(this.cgiServlet.getStderrTimeout());
                } catch (InterruptedException e) {
                    this.cgiServlet.log ("Interupted waiting for stderr reader thread");
                }
            }
            if (this.cgiServlet.getDebug() > 4) {
                this.cgiServlet.log ("Running finally block");
            }
            if (proc != null){
                proc.destroy();
                proc = null;
            }
        }
    }

    /**
     * Parses the Status-Line and extracts the status code.
     *
     * @param line The HTTP Status-Line (RFC2616, section 6.1)
     * @return The extracted status code or the code representing an
     * internal error if a valid status code cannot be extracted.
     */
    private int getSCFromHttpStatusLine(String line) {
        int statusStart = line.indexOf(' ') + 1;

        if (statusStart < 1 || line.length() < statusStart + 3) {
            // Not a valid HTTP Status-Line
            this.cgiServlet.log ("runCGI: invalid HTTP Status-Line:" + line);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        String status = line.substring(statusStart, statusStart + 3);

        int statusCode;
        try {
            statusCode = Integer.parseInt(status);
        } catch (NumberFormatException nfe) {
            // Not a valid status code
            this.cgiServlet.log ("runCGI: invalid status code:" + status);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        return statusCode;
    }

    /**
     * Parses the CGI Status Header value and extracts the status code.
     *
     * @param value The CGI Status value of the form <code>
     *             digit digit digit SP reason-phrase</code>
     * @return The extracted status code or the code representing an
     * internal error if a valid status code cannot be extracted.
     */
    private int getSCFromCGIStatusHeader(String value) {
        if (value.length() < 3) {
            // Not a valid status value
            this.cgiServlet.log ("runCGI: invalid status value:" + value);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        String status = value.substring(0, 3);

        int statusCode;
        try {
            statusCode = Integer.parseInt(status);
        } catch (NumberFormatException nfe) {
            // Not a valid status code
            this.cgiServlet.log ("runCGI: invalid status code:" + status);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        return statusCode;
    }

    private void sendToLog(BufferedReader rdr) {
        String line = null;
        int lineCount = 0 ;
        try {
            while ((line = rdr.readLine()) != null) {
                this.cgiServlet.log("runCGI (stderr):" +  line) ;
                lineCount++ ;
            }
        } catch (IOException e) {
            this.cgiServlet.log("sendToLog error", e) ;
        } finally {
            try {
                rdr.close() ;
            } catch (IOException ce) {
                this.cgiServlet.log("sendToLog error", ce) ;
            }
        }
        if ( lineCount > 0 && this.cgiServlet.getDebug() > 2) {
            this.cgiServlet.log("runCGI: " + lineCount + " lines received on stderr") ;
        }
    }
}