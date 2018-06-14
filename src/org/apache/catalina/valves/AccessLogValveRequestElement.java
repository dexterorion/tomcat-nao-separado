package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveRequestElement implements
		AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (request != null) {
			String method = request.getMethod();
			if (method == null) {
				// No method means no request line
				buf.append('-');
			} else {
				buf.append(request.getMethod());
				buf.append(' ');
				buf.append(request.getRequestURI());
				if (request.getQueryString() != null) {
					buf.append('?');
					buf.append(request.getQueryString());
				}
				buf.append(' ');
				buf.append(request.getProtocol());
			}
		} else {
			buf.append('-');
		}
	}
}