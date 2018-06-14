package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveLocalPortElement implements AccessLogValveAccessLogElement {
	/**
	 * 
	 */
	private final AccessLogValve accessLogValve;

	/**
	 * @param accessLogValve
	 */
	public AccessLogValveLocalPortElement(AccessLogValve accessLogValve) {
		this.accessLogValve = accessLogValve;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (this.accessLogValve.getRequestAttributesEnabled()) {
			Object port = request.getAttribute(AccessLogValve.SERVER_PORT_ATTRIBUTE);
			if (port == null) {
				buf.append(request.getServerPort());
			} else {
				buf.append(port);
			}
		} else {
			buf.append(request.getServerPort());
		}
	}
}