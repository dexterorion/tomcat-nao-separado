package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveRemoteAddrElement implements AccessLogValveAccessLogElement {
	/**
	 * 
	 */
	private final AccessLogValve accessLogValve;

	/**
	 * @param accessLogValve
	 */
	public AccessLogValveRemoteAddrElement(AccessLogValve accessLogValve) {
		this.accessLogValve = accessLogValve;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (this.accessLogValve.getRequestAttributesEnabled()) {
			Object addr = request.getAttribute(AccessLogValve.REMOTE_ADDR_ATTRIBUTE);
			if (addr == null) {
				buf.append(request.getRemoteAddr());
			} else {
				buf.append(addr);
			}
		} else {
			buf.append(request.getRemoteAddr());
		}
	}
}