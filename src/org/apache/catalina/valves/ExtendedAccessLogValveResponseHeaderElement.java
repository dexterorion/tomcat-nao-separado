package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveResponseHeaderElement implements AccessLogValveAccessLogElement {
    /**
	 * 
	 */
	private final ExtendedAccessLogValve extendedAccessLogValve;
	private final String header;

    public ExtendedAccessLogValveResponseHeaderElement(ExtendedAccessLogValve extendedAccessLogValve, String header) {
        this.extendedAccessLogValve = extendedAccessLogValve;
		this.header = header;
    }

    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        buf.append(this.extendedAccessLogValve.wrap(response.getHeader(header)));
    }
}