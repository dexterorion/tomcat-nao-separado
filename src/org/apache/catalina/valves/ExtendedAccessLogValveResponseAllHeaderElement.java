package org.apache.catalina.valves;

import java.util.Date;
import java.util.Iterator;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveResponseAllHeaderElement implements AccessLogValveAccessLogElement {
    /**
	 * 
	 */
	private final ExtendedAccessLogValve extendedAccessLogValve;
	private final String header;

    public ExtendedAccessLogValveResponseAllHeaderElement(ExtendedAccessLogValve extendedAccessLogValve, String header) {
        this.extendedAccessLogValve = extendedAccessLogValve;
		this.header = header;
    }

    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        if (null != response) {
            Iterator<String> iter = response.getHeaders(header).iterator();
            if (iter.hasNext()) {
                StringBuilder buffer = new StringBuilder();
                boolean first = true;
                while (iter.hasNext()) {
                    if (first) {
                        first = false;
                    } else {
                        buffer.append(",");
                    }
                    buffer.append(iter.next());
                }
                buf.append(this.extendedAccessLogValve.wrap(buffer.toString()));
            }
            return ;
        }
        buf.append("-");
    }
}