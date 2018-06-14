package org.apache.catalina.valves;

import java.util.Date;
import java.util.Iterator;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveResponseHeaderElement implements
		AccessLogValveAccessLogElement {
	private final String header;

	public AccessLogValveResponseHeaderElement(String header) {
		this.header = header;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (null != response) {
			Iterator<String> iter = response.getHeaders(header).iterator();
			if (iter.hasNext()) {
				buf.append(iter.next());
				while (iter.hasNext()) {
					buf.append(',').append(iter.next());
				}
				return;
			}
		}
		buf.append('-');
	}
}