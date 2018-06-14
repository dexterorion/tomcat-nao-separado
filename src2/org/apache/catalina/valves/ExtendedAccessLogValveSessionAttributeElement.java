package org.apache.catalina.valves;

import java.util.Date;

import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveSessionAttributeElement implements AccessLogValveAccessLogElement {
    /**
	 * 
	 */
	private final ExtendedAccessLogValve extendedAccessLogValve;
	private final String attribute;

    public ExtendedAccessLogValveSessionAttributeElement(ExtendedAccessLogValve extendedAccessLogValve, String attribute) {
        this.extendedAccessLogValve = extendedAccessLogValve;
		this.attribute = attribute;
    }
    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        HttpSession session = null;
        if (request != null) {
            session = request.getSession(false);
            if (session != null) {
                buf.append(this.extendedAccessLogValve.wrap(session.getAttribute(attribute)));
            }
        }
    }
}