package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveRequestAttributeElement implements
		AccessLogValveAccessLogElement {
	private final String header;

	public AccessLogValveRequestAttributeElement(String header) {
		this.header = header;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		Object value = null;
		if (request != null) {
			value = request.getAttribute(header);
		} else {
			value = "??";
		}
		if (value != null) {
			if (value instanceof String) {
				buf.append((String) value);
			} else {
				buf.append(value.toString());
			}
		} else {
			buf.append('-');
		}
	}
}