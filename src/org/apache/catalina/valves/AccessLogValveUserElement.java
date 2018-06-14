package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveUserElement implements AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (request != null) {
			String value = request.getRemoteUser();
			if (value != null) {
				buf.append(value);
			} else {
				buf.append('-');
			}
		} else {
			buf.append('-');
		}
	}
}