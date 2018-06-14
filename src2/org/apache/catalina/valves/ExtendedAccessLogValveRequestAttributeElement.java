package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveRequestAttributeElement implements AccessLogValveAccessLogElement {
    /**
	 * 
	 */
	private final ExtendedAccessLogValve extendedAccessLogValve;
	private final String attribute;

    public ExtendedAccessLogValveRequestAttributeElement(ExtendedAccessLogValve extendedAccessLogValve, String attribute) {
        this.extendedAccessLogValve = extendedAccessLogValve;
		this.attribute = attribute;
    }

    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        buf.append(this.extendedAccessLogValve.wrap(request.getAttribute(attribute)));
    }
}