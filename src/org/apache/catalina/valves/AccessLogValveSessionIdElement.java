package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.Session2;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveSessionIdElement implements
		AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (request == null) {
			buf.append('-');
		} else {
			Session2 session = request.getSessionInternal(false);
			if (session == null) {
				buf.append('-');
			} else {
				buf.append(session.getIdInternal());
			}
		}
	}
}