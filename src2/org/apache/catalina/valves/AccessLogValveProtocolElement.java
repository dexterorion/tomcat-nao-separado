package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveProtocolElement implements AccessLogValveAccessLogElement {
	/**
	 * 
	 */
	private final AccessLogValve accessLogValve;

	/**
	 * @param accessLogValve
	 */
	public AccessLogValveProtocolElement(AccessLogValve accessLogValve) {
		this.accessLogValve = accessLogValve;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (this.accessLogValve.getRequestAttributesEnabled()) {
			Object proto = request.getAttribute(AccessLogValve.PROTOCOL_ATTRIBUTE);
			if (proto == null) {
				buf.append(request.getProtocol());
			} else {
				buf.append(proto);
			}
		} else {
			buf.append(request.getProtocol());
		}
	}
}