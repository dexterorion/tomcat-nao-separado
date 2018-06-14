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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.buf.B2CConverter;

/**
 * <p>
 * Implementation of the <b>Valve</b> interface that generates a web server
 * access log with the detailed line contents matching a configurable pattern.
 * The syntax of the available patterns is similar to that supported by the <a
 * href="http://httpd.apache.org/">Apache HTTP Server</a>
 * <code>mod_log_config</code> module. As an additional feature, automatic
 * rollover of log files when the date changes is also supported.
 * </p>
 *
 * <p>
 * Patterns for the logged message may include constant text or any of the
 * following replacement strings, for which the corresponding information from
 * the specified Response is substituted:
 * </p>
 * <ul>
 * <li><b>%a</b> - Remote IP address
 * <li><b>%A</b> - Local IP address
 * <li><b>%b</b> - Bytes sent, excluding HTTP headers, or '-' if no bytes were
 * sent
 * <li><b>%B</b> - Bytes sent, excluding HTTP headers
 * <li><b>%h</b> - Remote host name (or IP address if <code>enableLookups</code>
 * for the connector is false)
 * <li><b>%H</b> - Request protocol
 * <li><b>%l</b> - Remote logical username from identd (always returns '-')
 * <li><b>%m</b> - Request method
 * <li><b>%p</b> - Local port
 * <li><b>%q</b> - Query string (prepended with a '?' if it exists, otherwise an
 * empty string
 * <li><b>%r</b> - First line of the request
 * <li><b>%s</b> - HTTP status code of the response
 * <li><b>%S</b> - User session ID
 * <li><b>%t</b> - Date and time, in Common Log Format format
 * <li><b>%t{format}</b> - Date and time, in any format supported by
 * SimpleDateFormat
 * <li><b>%u</b> - Remote user that was authenticated
 * <li><b>%U</b> - Requested URL path
 * <li><b>%v</b> - Local server name
 * <li><b>%D</b> - Time taken to process the request, in millis
 * <li><b>%T</b> - Time taken to process the request, in seconds
 * <li><b>%I</b> - current Request thread name (can compare later with
 * stacktraces)
 * </ul>
 * <p>
 * In addition, the caller can specify one of the following aliases for commonly
 * utilized patterns:
 * </p>
 * <ul>
 * <li><b>common</b> - <code>%h %l %u %t "%r" %s %b</code>
 * <li><b>combined</b> -
 * <code>%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i"</code>
 * </ul>
 *
 * <p>
 * There is also support to write information from the cookie, incoming header,
 * the Session or something else in the ServletRequest.<br>
 * It is modeled after the <a href="http://httpd.apache.org/">Apache HTTP
 * Server</a> log configuration syntax:
 * </p>
 * <ul>
 * <li><code>%{xxx}i</code> for incoming headers
 * <li><code>%{xxx}o</code> for outgoing response headers
 * <li><code>%{xxx}c</code> for a specific cookie
 * <li><code>%{xxx}r</code> xxx is an attribute in the ServletRequest
 * <li><code>%{xxx}s</code> xxx is an attribute in the HttpSession
 * <li><code>%{xxx}t</code> xxx is an enhanced SimpleDateFormat pattern (see
 * Configuration Reference document for details on supported time patterns)
 * </ul>
 *
 * <p>
 * Log rotation can be on or off. This is dictated by the <code>rotatable</code>
 * property.
 * </p>
 *
 * <p>
 * For UNIX users, another field called <code>checkExists</code> is also
 * available. If set to true, the log file's existence will be checked before
 * each logging. This way an external log rotator can move the file somewhere
 * and Tomcat will start with a new file.
 * </p>
 *
 * <p>
 * For JMX junkies, a public method called <code>rotate</code> has been made
 * available to allow you to tell this instance to move the existing log file to
 * somewhere else and start writing a new log file.
 * </p>
 *
 * <p>
 * Conditional logging is also supported. This can be done with the
 * <code>conditionUnless</code> and <code>conditionIf</code> properties. If the
 * value returned from ServletRequest.getAttribute(conditionUnless) yields a
 * non-null value, the logging will be skipped. If the value returned from
 * ServletRequest.getAttribute(conditionIf) yields the null value, the logging
 * will be skipped. The <code>condition</code> attribute is synonym for
 * <code>conditionUnless</code> and is provided for backwards compatibility.
 * </p>
 *
 * <p>
 * For extended attributes coming from a getAttribute() call, it is you
 * responsibility to ensure there are no newline or control characters.
 * </p>
 *
 * @author Craig R. McClanahan
 * @author Jason Brittain
 * @author Remy Maucherat
 * @author Takayuki Kaneko
 * @author Peter Rossbach
 */
public class AccessLogValve extends ValveBase implements AccessLog {

	private static final Log log = LogFactory.getLog(AccessLogValve.class);

	// ------------------------------------------------------ Constructor
	public AccessLogValve() {
		super(true);
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * The as-of date for the currently open log file, or a zero-length string
	 * if there is no open log file.
	 */
	private volatile String dateStamp = "";

	/**
	 * The directory in which log files are created.
	 */
	private String directory = "logs";

	/**
	 * The descriptive information about this implementation.
	 */
	private static final String info = "org.apache.catalina.valves.AccessLogValve/2.2";

	/**
	 * enabled this component
	 */
	private boolean enabled = true;

	/**
	 * The pattern used to format our access log lines.
	 */
	private String pattern = null;

	/**
	 * The prefix that is added to log file filenames.
	 */
	private String prefix = "access_log.";

	/**
	 * Should we rotate our log file? Default is true (like old behavior)
	 */
	private boolean rotatable = true;

	/**
	 * Should we defer inclusion of the date stamp in the file name until rotate
	 * time? Default is false.
	 */
	private boolean renameOnRotate = false;

	/**
	 * Buffered logging.
	 */
	private boolean buffered = true;

	/**
	 * The suffix that is added to log file filenames.
	 */
	private String suffix = "";

	/**
	 * The PrintWriter to which we are currently logging, if any.
	 */
	private PrintWriter writer = null;

	/**
	 * A date formatter to format a Date using the format given by
	 * <code>fileDateFormat</code>.
	 */
	private SimpleDateFormat fileDateFormatter = null;

	/**
	 * The size of our global date format cache
	 */
	private static final int globalCacheSize = 300;

	/**
	 * The size of our thread local date format cache
	 */
	private static final int localCacheSize = 60;

	/**
	 * The current log file we are writing to. Helpful when checkExists is true.
	 */
	private File currentLogFile = null;

	/**
	 * Global date format cache.
	 */
	private static final AccessLogValveDateFormatCache globalDateCache = new AccessLogValveDateFormatCache(
			globalCacheSize, Locale.getDefault(), null);

	/**
	 * Thread local date format cache.
	 */
	private static final ThreadLocal<AccessLogValveDateFormatCache> localDateCache = new ThreadLocal1();

	/**
	 * The system time when we last updated the Date that this valve uses for
	 * log lines.
	 */
	private static final ThreadLocal<Date> localDate = new ThreadLocal<Date>() {
		@Override
		protected Date initialValue() {
			return new Date();
		}
	};

	/**
	 * Resolve hosts.
	 */
	private boolean resolveHosts = false;

	/**
	 * Instant when the log daily rotation was last checked.
	 */
	private volatile long rotationLastChecked = 0L;

	/**
	 * Do we check for log file existence? Helpful if an external agent renames
	 * the log file so we can automagically recreate it.
	 */
	private boolean checkExists = false;

	/**
	 * Are we doing conditional logging. default null. It is the value of
	 * <code>conditionUnless</code> property.
	 */
	private String condition = null;

	/**
	 * Are we doing conditional logging. default null. It is the value of
	 * <code>conditionIf</code> property.
	 */
	private String conditionIf = null;

	/**
	 * Date format to place in log file name.
	 */
	private String fileDateFormat = "yyyy-MM-dd";

	/**
	 * Name of locale used to format timestamps in log entries and in log file
	 * name suffix.
	 */
	private String localeName = Locale.getDefault().toString();

	/**
	 * Locale used to format timestamps in log entries and in log file name
	 * suffix.
	 */
	private Locale locale = Locale.getDefault();

	/**
	 * Character set used by the log file. If it is <code>null</code>, the
	 * system default character set will be used. An empty string will be
	 * treated as <code>null</code> when this property is assigned.
	 */
	private String encoding = null;

	/**
	 * Array of AccessLogElement, they will be used to make log message.
	 */
	private AccessLogValveAccessLogElement[] logElements = null;

	/**
	 * @see #setRequestAttributesEnabled(boolean)
	 */
	private boolean requestAttributesEnabled = false;

	// ------------------------------------------------------------- Properties

	/**
	 * @return Returns the enabled.
	 */
	public boolean getEnabled() {
		return enabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
		this.requestAttributesEnabled = requestAttributesEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getRequestAttributesEnabled() {
		return requestAttributesEnabled;
	}

	/**
	 * @param enabled
	 *            The enabled to set.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Return the directory in which we create log files.
	 */
	public String getDirectory() {
		return (directory);
	}

	/**
	 * Set the directory in which we create log files.
	 *
	 * @param directory
	 *            The new log file directory
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * Return descriptive information about this implementation.
	 */
	@Override
	public String getInfo() {
		return (info);
	}

	/**
	 * Return the format pattern.
	 */
	public String getPattern() {
		return (this.pattern);
	}

	/**
	 * Set the format pattern, first translating any recognized alias.
	 *
	 * @param pattern
	 *            The new pattern
	 */
	public void setPattern(String pattern) {
		if (pattern == null) {
			this.pattern = "";
		} else if (pattern.equals(ConstantsAccessLog.getCommonAlias())) {
			this.pattern = ConstantsAccessLog.getCommonPattern();
		} else if (pattern.equals(ConstantsAccessLog.getCombinedAlias())) {
			this.pattern = ConstantsAccessLog.getCombinedPattern();
		} else {
			this.pattern = pattern;
		}
		logElements = createLogElements();
	}

	/**
	 * Check for file existence before logging.
	 */
	public boolean isCheckExists() {

		return checkExists;

	}

	/**
	 * Set whether to check for log file existence before logging.
	 *
	 * @param checkExists
	 *            true meaning to check for file existence.
	 */
	public void setCheckExists(boolean checkExists) {

		this.checkExists = checkExists;

	}

	/**
	 * Return the log file prefix.
	 */
	public String getPrefix() {
		return (prefix);
	}

	/**
	 * Set the log file prefix.
	 *
	 * @param prefix
	 *            The new log file prefix
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Should we rotate the logs
	 */
	public boolean isRotatable() {
		return rotatable;
	}

	/**
	 * Set the value is we should we rotate the logs
	 *
	 * @param rotatable
	 *            true is we should rotate.
	 */
	public void setRotatable(boolean rotatable) {
		this.rotatable = rotatable;
	}

	/**
	 * Should we defer inclusion of the date stamp in the file name until rotate
	 * time
	 */
	public boolean isRenameOnRotate() {
		return renameOnRotate;
	}

	/**
	 * Set the value if we should defer inclusion of the date stamp in the file
	 * name until rotate time
	 *
	 * @param renameOnRotate
	 *            true if defer inclusion of date stamp
	 */
	public void setRenameOnRotate(boolean renameOnRotate) {
		this.renameOnRotate = renameOnRotate;
	}

	/**
	 * Is the logging buffered
	 */
	public boolean isBuffered() {
		return buffered;
	}

	/**
	 * Set the value if the logging should be buffered
	 *
	 * @param buffered
	 *            true if buffered.
	 */
	public void setBuffered(boolean buffered) {
		this.buffered = buffered;
	}

	/**
	 * Return the log file suffix.
	 */
	public String getSuffix() {
		return (suffix);
	}

	/**
	 * Set the log file suffix.
	 *
	 * @param suffix
	 *            The new log file suffix
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Set the resolve hosts flag.
	 *
	 * @param resolveHosts
	 *            The new resolve hosts value
	 * @deprecated Unused, removed in Tomcat 8. See
	 *             org.apache.catalina.connector
	 *             .Connector.setEnableLookups(boolean).
	 */
	@Deprecated
	public void setResolveHosts(boolean resolveHosts) {
		this.resolveHosts = resolveHosts;
	}

	/**
	 * Get the value of the resolve hosts flag.
	 * 
	 * @deprecated Unused, removed in Tomcat 8. See
	 *             org.apache.catalina.connector
	 *             .Connector.setEnableLookups(boolean).
	 */
	@Deprecated
	public boolean isResolveHosts() {
		return resolveHosts;
	}

	/**
	 * Return whether the attribute name to look for when performing conditional
	 * logging. If null, every request is logged.
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * Set the ServletRequest.attribute to look for to perform conditional
	 * logging. Set to null to log everything.
	 *
	 * @param condition
	 *            Set to null to log everything
	 */
	public void setCondition(String condition) {
		this.condition = condition;
	}

	/**
	 * Return whether the attribute name to look for when performing conditional
	 * logging. If null, every request is logged.
	 */
	public String getConditionUnless() {
		return getCondition();
	}

	/**
	 * Set the ServletRequest.attribute to look for to perform conditional
	 * logging. Set to null to log everything.
	 *
	 * @param condition
	 *            Set to null to log everything
	 */
	public void setConditionUnless(String condition) {
		setCondition(condition);
	}

	/**
	 * Return whether the attribute name to look for when performing conditional
	 * logging. If null, every request is logged.
	 */
	public String getConditionIf() {
		return conditionIf;
	}

	/**
	 * Set the ServletRequest.attribute to look for to perform conditional
	 * logging. Set to null to log everything.
	 *
	 * @param condition
	 *            Set to null to log everything
	 */
	public void setConditionIf(String condition) {
		this.conditionIf = condition;
	}

	/**
	 * Return the date format date based log rotation.
	 */
	public String getFileDateFormat() {
		return fileDateFormat;
	}

	/**
	 * Set the date format date based log rotation.
	 */
	public void setFileDateFormat(String fileDateFormat) {
		String newFormat;
		if (fileDateFormat == null) {
			newFormat = "";
		} else {
			newFormat = fileDateFormat;
		}
		this.fileDateFormat = newFormat;

		synchronized (this) {
			fileDateFormatter = new SimpleDateFormat(newFormat, Locale.US);
			fileDateFormatter.setTimeZone(TimeZone.getDefault());
		}
	}

	/**
	 * Return the locale used to format timestamps in log entries and in log
	 * file name suffix.
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Set the locale used to format timestamps in log entries and in log file
	 * name suffix. Changing the locale is only supported as long as the
	 * AccessLogValve has not logged anything. Changing the locale later can
	 * lead to inconsistent formatting.
	 *
	 * @param localeName
	 *            The locale to use.
	 */
	public void setLocale(String localeName) {
		this.localeName = localeName;
		locale = findLocale(localeName, locale);
	}

	/**
	 * Return the character set name that is used to write the log file.
	 *
	 * @return Character set name, or <code>null</code> if the system default
	 *         character set is used.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Set the character set that is used to write the log file.
	 *
	 * @param encoding
	 *            The name of the character set.
	 */
	public void setEncoding(String encoding) {
		if (encoding != null && encoding.length() > 0) {
			this.encoding = encoding;
		} else {
			this.encoding = null;
		}
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Execute a periodic task, such as reloading, etc. This method will be
	 * invoked inside the classloading context of this container. Unexpected
	 * throwables will be caught and logged.
	 */
	@Override
	public synchronized void backgroundProcess() {
		if (getState().isAvailable() && getEnabled() && writer != null
				&& buffered) {
			writer.flush();
		}
	}

	/**
	 * Log a message summarizing the specified request and response, according
	 * to the format specified by the <code>pattern</code> property.
	 *
	 * @param request
	 *            Request being processed
	 * @param response
	 *            Response being processed
	 *
	 * @exception IOException
	 *                if an input/output error has occurred
	 * @exception ServletException
	 *                if a servlet error has occurred
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
		getNext().invoke(request, response);
	}

	@Override
	public void log(Request request, Response response, long time) {
		if (!getState().isAvailable() || !getEnabled() || logElements == null
				|| condition != null
				&& null != request.getRequest().getAttribute(condition)
				|| conditionIf != null
				&& null == request.getRequest().getAttribute(conditionIf)) {
			return;
		}

		/**
		 * XXX This is a bit silly, but we want to have start and stop time and
		 * duration consistent. It would be better to keep start and stop simply
		 * in the request and/or response object and remove time (duration) from
		 * the interface.
		 */
		long start = request.getCoyoteRequest().getStartTime();
		Date date = getDate(start + time);

		StringBuilder result = new StringBuilder(128);

		for (int i = 0; i < logElements.length; i++) {
			logElements[i].addElement(result, date, request, response, time);
		}

		log(result.toString());
	}

	/**
	 * Rotate the log file if necessary.
	 */
	public void rotate() {
		if (rotatable) {
			// Only do a logfile switch check once a second, max.
			long systime = System.currentTimeMillis();
			if ((systime - rotationLastChecked) > 1000) {
				synchronized (this) {
					if ((systime - rotationLastChecked) > 1000) {
						rotationLastChecked = systime;

						String tsDate;
						// Check for a change of date
						tsDate = fileDateFormatter.format(new Date(systime));

						// If the date has changed, switch log files
						if (!dateStamp.equals(tsDate)) {
							close(true);
							dateStamp = tsDate;
							open();
						}
					}
				}
			}
		}
	}

	/**
	 * Rename the existing log file to something else. Then open the old log
	 * file name up once again. Intended to be called by a JMX agent.
	 *
	 *
	 * @param newFileName
	 *            The file name to move the log file entry to
	 * @return true if a file was rotated with no error
	 */
	public synchronized boolean rotate(String newFileName) {

		if (currentLogFile != null) {
			File holder = currentLogFile;
			close(false);
			try {
				holder.renameTo(new File(newFileName));
			} catch (Throwable e) {
				ExceptionUtils2.handleThrowable(e);
				log.error(getSm().getString("accessLogValve.rotateFail"), e);
			}

			/* Make sure date is correct */
			dateStamp = fileDateFormatter.format(new Date(System
					.currentTimeMillis()));

			open();
			return true;
		} else {
			return false;
		}

	}

	// -------------------------------------------------------- Private Methods

	/**
	 * Create a File object based on the current log file name. Directories are
	 * created as needed but the underlying file is not created or opened.
	 *
	 * @param useDateStamp
	 *            include the timestamp in the file name.
	 * @return the log file object
	 */
	private File getLogFile(boolean useDateStamp) {

		// Create the directory if necessary
		File dir = new File(directory);
		if (!dir.isAbsolute()) {
			dir = new File(System.getProperty(Globals.getCatalinaBaseProp()),
					directory);
		}
		if (!dir.mkdirs() && !dir.isDirectory()) {
			log.error(getSm().getString("accessLogValve.openDirFail", dir));
		}

		// Calculate the current log file name
		File pathname;
		if (useDateStamp) {
			pathname = new File(dir.getAbsoluteFile(), prefix + dateStamp
					+ suffix);
		} else {
			pathname = new File(dir.getAbsoluteFile(), prefix + suffix);
		}
		File parent = pathname.getParentFile();
		if (!parent.mkdirs() && !parent.isDirectory()) {
			log.error(getSm().getString("accessLogValve.openDirFail", parent));
		}
		return pathname;
	}

	/**
	 * Move a current but rotated log file back to the unrotated one. Needed if
	 * date stamp inclusion is deferred to rotation time.
	 */
	private void restore() {
		File newLogFile = getLogFile(false);
		File rotatedLogFile = getLogFile(true);
		if (rotatedLogFile.exists() && !newLogFile.exists()
				&& !rotatedLogFile.equals(newLogFile)) {
			try {
				if (!rotatedLogFile.renameTo(newLogFile)) {
					log.error(getSm().getString("accessLogValve.renameFail",
							rotatedLogFile, newLogFile));
				}
			} catch (Throwable e) {
				ExceptionUtils2.handleThrowable(e);
				log.error(getSm().getString("accessLogValve.renameFail",
						rotatedLogFile, newLogFile), e);
			}
		}
	}

	/**
	 * Close the currently open log file (if any)
	 *
	 * @param rename
	 *            Rename file to final name after closing
	 */
	private synchronized void close(boolean rename) {
		if (writer == null) {
			return;
		}
		writer.flush();
		writer.close();
		if (rename && renameOnRotate) {
			File newLogFile = getLogFile(true);
			if (!newLogFile.exists()) {
				try {
					if (!currentLogFile.renameTo(newLogFile)) {
						log.error(getSm().getString("accessLogValve.renameFail",
								currentLogFile, newLogFile));
					}
				} catch (Throwable e) {
					ExceptionUtils2.handleThrowable(e);
					log.error(getSm().getString("accessLogValve.renameFail",
							currentLogFile, newLogFile), e);
				}
			} else {
				log.error(getSm().getString("accessLogValve.alreadyExists",
						currentLogFile, newLogFile));
			}
		}
		writer = null;
		dateStamp = "";
		currentLogFile = null;
	}

	/**
	 * Log the specified message to the log file, switching files if the date
	 * has changed since the previous log call.
	 *
	 * @param message
	 *            Message to be logged
	 */
	public void log(String message) {

		rotate();

		/* In case something external rotated the file instead */
		if (checkExists) {
			synchronized (this) {
				if (currentLogFile != null && !currentLogFile.exists()) {
					try {
						close(false);
					} catch (Throwable e) {
						ExceptionUtils2.handleThrowable(e);
						log.info(getSm().getString("accessLogValve.closeFail"), e);
					}

					/* Make sure date is correct */
					dateStamp = fileDateFormatter.format(new Date(System
							.currentTimeMillis()));

					open();
				}
			}
		}

		// Log this message
		synchronized (this) {
			if (writer != null) {
				writer.println(message);
				if (!buffered) {
					writer.flush();
				}
			}
		}

	}

	/**
	 * Open the new log file for the date specified by <code>dateStamp</code>.
	 */
	protected synchronized void open() {
		// Open the current log file
		// If no rotate - no need for dateStamp in fileName
		File pathname = getLogFile(rotatable && !renameOnRotate);

		Charset charset = null;
		if (encoding != null) {
			try {
				charset = B2CConverter.getCharset(encoding);
			} catch (UnsupportedEncodingException ex) {
				log.error(getSm().getString("accessLogValve.unsupportedEncoding",
						encoding), ex);
			}
		}
		if (charset == null) {
			charset = Charset.defaultCharset();
		}

		try {
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(pathname, true), charset), 128000),
					false);

			currentLogFile = pathname;
		} catch (IOException e) {
			writer = null;
			currentLogFile = null;
			log.error(getSm().getString("accessLogValve.openFail", pathname), e);
		}
	}

	/**
	 * This method returns a ThreadLocal Date object that is set to the
	 * specified time. This saves creating a new Date object for every request.
	 *
	 * @return Date
	 */
	private static Date getDate(long systime) {
		Date date = localDate.get();
		date.setTime(systime);
		return date;
	}

	/**
	 * Find a locale by name
	 */
	protected static Locale findLocale(String name, Locale fallback) {
		if (name == null || name.isEmpty()) {
			return Locale.getDefault();
		} else {
			for (Locale l : Locale.getAvailableLocales()) {
				if (name.equals(l.toString())) {
					return (l);
				}
			}
		}
		log.error(getSm().getString("accessLogValve.invalidLocale", name));
		return fallback;
	}

	/**
	 * Start this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void startInternal() throws LifecycleException {

		// Initialize the Date formatters
		String format = getFileDateFormat();
		fileDateFormatter = new SimpleDateFormat(format, Locale.US);
		fileDateFormatter.setTimeZone(TimeZone.getDefault());
		dateStamp = fileDateFormatter.format(new Date(System
				.currentTimeMillis()));
		if (rotatable && renameOnRotate) {
			restore();
		}
		open();

		setState(LifecycleState.STARTING);
	}

	/**
	 * Stop this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void stopInternal() throws LifecycleException {

		setState(LifecycleState.STOPPING);
		close(false);
	}

	public String getDateStamp() {
		return dateStamp;
	}

	public void setDateStamp(String dateStamp) {
		this.dateStamp = dateStamp;
	}

	public PrintWriter getWriter() {
		return writer;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	public SimpleDateFormat getFileDateFormatter() {
		return fileDateFormatter;
	}

	public void setFileDateFormatter(SimpleDateFormat fileDateFormatter) {
		this.fileDateFormatter = fileDateFormatter;
	}

	public File getCurrentLogFile() {
		return currentLogFile;
	}

	public void setCurrentLogFile(File currentLogFile) {
		this.currentLogFile = currentLogFile;
	}

	public long getRotationLastChecked() {
		return rotationLastChecked;
	}

	public void setRotationLastChecked(long rotationLastChecked) {
		this.rotationLastChecked = rotationLastChecked;
	}

	public String getLocaleName() {
		return localeName;
	}

	public void setLocaleName(String localeName) {
		this.localeName = localeName;
	}

	public AccessLogValveAccessLogElement[] getLogElements() {
		return logElements;
	}

	public void setLogElements(AccessLogValveAccessLogElement[] logElements) {
		this.logElements = logElements;
	}

	public static Log getLog() {
		return log;
	}

	public static int getGlobalcachesize() {
		return globalCacheSize;
	}

	public static int getLocalcachesize() {
		return localCacheSize;
	}

	public static AccessLogValveDateFormatCache getGlobaldatecache() {
		return globalDateCache;
	}

	public static ThreadLocal<AccessLogValveDateFormatCache> getLocaldatecache() {
		return localDateCache;
	}

	public static ThreadLocal<Date> getLocaldate() {
		return localDate;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	protected AccessLogValveAccessLogElement[] createLogElements() {
		List<AccessLogValveAccessLogElement> list = new ArrayList<AccessLogValveAccessLogElement>();
		boolean replace = false;
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < pattern.length(); i++) {
			char ch = pattern.charAt(i);
			if (replace) {
				/*
				 * For code that processes {, the behavior will be ... if I do
				 * not encounter a closing } - then I ignore the {
				 */
				if ('{' == ch) {
					StringBuilder name = new StringBuilder();
					int j = i + 1;
					for (; j < pattern.length() && '}' != pattern.charAt(j); j++) {
						name.append(pattern.charAt(j));
					}
					if (j + 1 < pattern.length()) {
						/* the +1 was to account for } which we increment now */
						j++;
						list.add(createAccessLogElement(name.toString(),
								pattern.charAt(j)));
						i = j; /* Since we walked more than one character */
					} else {
						// D'oh - end of string - pretend we never did this
						// and do processing the "old way"
						list.add(createAccessLogElement(ch));
					}
				} else {
					list.add(createAccessLogElement(ch));
				}
				replace = false;
			} else if (ch == '%') {
				replace = true;
				list.add(new AccessLogValveStringElement(buf.toString()));
				buf = new StringBuilder();
			} else {
				buf.append(ch);
			}
		}
		if (buf.length() > 0) {
			list.add(new AccessLogValveStringElement(buf.toString()));
		}
		return list.toArray(new AccessLogValveAccessLogElement[0]);
	}

	/**
	 * create an AccessLogElement implementation which needs header string
	 */
	protected AccessLogValveAccessLogElement createAccessLogElement(
			String header, char pattern) {
		switch (pattern) {
		case 'i':
			return new AccessLogValveHeaderElement(header);
		case 'c':
			return new AccessLogValveCookieElement(header);
		case 'o':
			return new AccessLogValveResponseHeaderElement(header);
		case 'r':
			return new AccessLogValveRequestAttributeElement(header);
		case 's':
			return new AccessLogValveSessionAttributeElement(header);
		case 't':
			return new AccessLogValveDateAndTimeElement(this, header);
		default:
			return new AccessLogValveStringElement("???");
		}
	}

	/**
	 * create an AccessLogElement implementation
	 */
	protected AccessLogValveAccessLogElement createAccessLogElement(char pattern) {
		switch (pattern) {
		case 'a':
			return new AccessLogValveRemoteAddrElement(this);
		case 'A':
			return new AccessLogValveLocalAddrElement();
		case 'b':
			return new AccessLogValveByteSentElement(true);
		case 'B':
			return new AccessLogValveByteSentElement(false);
		case 'D':
			return new AccessLogValveElapsedTimeElement(true);
		case 'F':
			return new AccessLogValveFirstByteTimeElement();
		case 'h':
			return new AccessLogValveHostElement(this);
		case 'H':
			return new AccessLogValveProtocolElement(this);
		case 'l':
			return new AccessLogValveLogicalUserNameElement();
		case 'm':
			return new AccessLogValveMethodElement();
		case 'p':
			return new AccessLogValveLocalPortElement(this);
		case 'q':
			return new AccessLogValveQueryElement();
		case 'r':
			return new AccessLogValveRequestElement();
		case 's':
			return new AccessLogValveHttpStatusCodeElement();
		case 'S':
			return new AccessLogValveSessionIdElement();
		case 't':
			return new AccessLogValveDateAndTimeElement(this);
		case 'T':
			return new AccessLogValveElapsedTimeElement(false);
		case 'u':
			return new AccessLogValveUserElement();
		case 'U':
			return new AccessLogValveRequestURIElement();
		case 'v':
			return new AccessLogValveLocalServerNameElement();
		case 'I':
			return new AccessLogValveThreadNameElement();
		default:
			return new AccessLogValveStringElement("???" + pattern + "???");
		}
	}
	
	
}