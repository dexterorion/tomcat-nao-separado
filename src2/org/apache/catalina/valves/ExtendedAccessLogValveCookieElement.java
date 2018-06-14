package org.apache.catalina.valves;

import java.util.Date;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveCookieElement implements AccessLogValveAccessLogElement {
    /**
	 * 
	 */
	private final ExtendedAccessLogValve extendedAccessLogValve;
	private final String name;

    public ExtendedAccessLogValveCookieElement(ExtendedAccessLogValve extendedAccessLogValve, String name) {
        this.extendedAccessLogValve = extendedAccessLogValve;
		this.name = name;
    }
    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        Cookie[] c = request.getCookies();
        for (int i = 0; c != null && i < c.length; i++) {
            if (name.equals(c[i].getName())) {
                buf.append(this.extendedAccessLogValve.wrap(c[i].getValue()));
            }
        }
    }
}