package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveHostElement implements AccessLogValveAccessLogElement {
	/**
	 * 
	 */
	private final AccessLogValve accessLogValve;

	/**
	 * @param accessLogValve
	 */
	public AccessLogValveHostElement(AccessLogValve accessLogValve) {
		this.accessLogValve = accessLogValve;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		String value = null;
		if (this.accessLogValve.getRequestAttributesEnabled()) {
			Object host = request.getAttribute(AccessLogValve.REMOTE_HOST_ATTRIBUTE);
			if (host != null) {
				value = host.toString();
			}
		}
		if (value == null || value.length() == 0) {
			value = request.getRemoteHost();
		}
		if (value == null || value.length() == 0) {
			value = "-";
		}
		buf.append(value);
	}
}