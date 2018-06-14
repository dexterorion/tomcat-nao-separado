package org.apache.catalina.valves;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveRequestParameterElement implements AccessLogValveAccessLogElement {
    /**
	 * 
	 */
	private final ExtendedAccessLogValve extendedAccessLogValve;
	private final String parameter;

    public ExtendedAccessLogValveRequestParameterElement(ExtendedAccessLogValve extendedAccessLogValve, String parameter) {
        this.extendedAccessLogValve = extendedAccessLogValve;
		this.parameter = parameter;
    }
    /**
     *  urlEncode the given string. If null or empty, return null.
     */
    private String urlEncode(String value) {
        if (null==value || value.length()==0) {
            return null;
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen - all JVMs are required to support UTF-8
            return null;
        }
    }

    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        buf.append(this.extendedAccessLogValve.wrap(urlEncode(request.getParameter(parameter))));
    }
}