package org.apache.catalina.valves;

import java.util.Date;
import java.util.Enumeration;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveHeaderElement implements
		AccessLogValveAccessLogElement {
	private final String header;

	public AccessLogValveHeaderElement(String header) {
		this.header = header;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		Enumeration<String> iter = request.getHeaders(header);
		if (iter.hasMoreElements()) {
			buf.append(iter.nextElement());
			while (iter.hasMoreElements()) {
				buf.append(',').append(iter.nextElement());
			}
			return;
		}
		buf.append('-');
	}
}