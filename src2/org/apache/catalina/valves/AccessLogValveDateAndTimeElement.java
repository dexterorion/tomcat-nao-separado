package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveDateAndTimeElement implements
		AccessLogValveAccessLogElement {

	/**
	 * 
	 */
	private final AccessLogValve accessLogValve;

	/**
	 * Format prefix specifying request start time
	 */
	private static final String requestStartPrefix = "begin";

	/**
	 * Format prefix specifying response end time
	 */
	private static final String responseEndPrefix = "end";

	/**
	 * Separator between optional prefix and rest of format
	 */
	private static final String prefixSeparator = ":";

	/**
	 * Special format for seconds since epoch
	 */
	private static final String secFormat = "sec";

	/**
	 * Special format for milliseconds since epoch
	 */
	private static final String msecFormat = "msec";

	/**
	 * Special format for millisecond part of timestamp
	 */
	private static final String msecFractionFormat = "msec_frac";

	/**
	 * The patterns we use to replace "S" and "SSS" millisecond formatting
	 * of SimpleDateFormat by our own handling
	 */
	private static final String msecPattern = "{#}";
	private static final String trippleMsecPattern = msecPattern
			+ msecPattern + msecPattern;

	/* Our format description string, null if CLF */
	private String format = null;
	/* Whether to use begin of request or end of response as the timestamp */
	private boolean usesBegin = false;
	/* The format type */
	private AccessLogValveFormatType type = AccessLogValveFormatType.CLF;
	/* Whether we need to postprocess by adding milliseconds */
	private boolean usesMsecs = false;

	public AccessLogValveDateAndTimeElement(AccessLogValve accessLogValve) {
		this(accessLogValve, null);
	}

	/**
	 * Replace the millisecond formatting character 'S' by some dummy
	 * characters in order to make the resulting formatted time stamps
	 * cacheable. We replace the dummy chars later with the actual
	 * milliseconds because that's relatively cheap.
	 */
	private String tidyFormat(String format) {
		boolean escape = false;
		StringBuilder result = new StringBuilder();
		int len = format.length();
		char x;
		for (int i = 0; i < len; i++) {
			x = format.charAt(i);
			if (escape || x != 'S') {
				result.append(x);
			} else {
				result.append(msecPattern);
				usesMsecs = true;
			}
			if (x == '\'') {
				escape = !escape;
			}
		}
		return result.toString();
	}

	public AccessLogValveDateAndTimeElement(AccessLogValve accessLogValve, String header) {
		this.accessLogValve = accessLogValve;
		format = header;
		if (format != null) {
			if (format.equals(requestStartPrefix)) {
				usesBegin = true;
				format = "";
			} else if (format.startsWith(requestStartPrefix
					+ prefixSeparator)) {
				usesBegin = true;
				format = format.substring(6);
			} else if (format.equals(responseEndPrefix)) {
				usesBegin = false;
				format = "";
			} else if (format.startsWith(responseEndPrefix
					+ prefixSeparator)) {
				usesBegin = false;
				format = format.substring(4);
			}
			if (format.length() == 0) {
				type = AccessLogValveFormatType.CLF;
			} else if (format.equals(secFormat)) {
				type = AccessLogValveFormatType.SEC;
			} else if (format.equals(msecFormat)) {
				type = AccessLogValveFormatType.MSEC;
			} else if (format.equals(msecFractionFormat)) {
				type = AccessLogValveFormatType.MSEC_FRAC;
			} else {
				type = AccessLogValveFormatType.SDF;
				format = tidyFormat(format);
			}
		}
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		long timestamp = date.getTime();
		long frac;
		if (usesBegin) {
			timestamp -= time;
		}
		switch (type) {
		case CLF:
			buf.append(AccessLogValve.getLocaldatecache().get().getFormat(timestamp));
			break;
		case SEC:
			buf.append(timestamp / 1000);
			break;
		case MSEC:
			buf.append(timestamp);
			break;
		case MSEC_FRAC:
			frac = timestamp % 1000;
			if (frac < 100) {
				if (frac < 10) {
					buf.append('0');
					buf.append('0');
				} else {
					buf.append('0');
				}
			}
			buf.append(frac);
			break;
		case SDF:
			String temp = AccessLogValve.getLocaldatecache().get().getFormat(format, this.accessLogValve.getLocale(),
					timestamp);
			if (usesMsecs) {
				frac = timestamp % 1000;
				StringBuilder trippleMsec = new StringBuilder(4);
				if (frac < 100) {
					if (frac < 10) {
						trippleMsec.append('0');
						trippleMsec.append('0');
					} else {
						trippleMsec.append('0');
					}
				}
				trippleMsec.append(frac);
				temp = temp.replace(trippleMsecPattern, trippleMsec);
				temp = temp.replace(msecPattern, Long.toString(frac));
			}
			buf.append(temp);
			break;
		}
	}
}