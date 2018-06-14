package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveQueryElement implements
		AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		String query = null;
		if (request != null) {
			query = request.getQueryString();
		}
		if (query != null) {
			buf.append('?');
			buf.append(query);
		}
	}
}