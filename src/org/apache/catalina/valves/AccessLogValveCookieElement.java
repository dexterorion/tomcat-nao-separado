package org.apache.catalina.valves;

import java.util.Date;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveCookieElement implements
		AccessLogValveAccessLogElement {
	private final String header;

	public AccessLogValveCookieElement(String header) {
		this.header = header;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		String value = "-";
		Cookie[] c = request.getCookies();
		if (c != null) {
			for (int i = 0; i < c.length; i++) {
				if (header.equals(c[i].getName())) {
					value = c[i].getValue();
					break;
				}
			}
		}
		buf.append(value);
	}
}