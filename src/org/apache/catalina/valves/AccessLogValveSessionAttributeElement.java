package org.apache.catalina.valves;

import java.util.Date;

import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveSessionAttributeElement implements
		AccessLogValveAccessLogElement {
	private final String header;

	public AccessLogValveSessionAttributeElement(String header) {
		this.header = header;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		Object value = null;
		if (null != request) {
			HttpSession sess = request.getSession(false);
			if (null != sess) {
				value = sess.getAttribute(header);
			}
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