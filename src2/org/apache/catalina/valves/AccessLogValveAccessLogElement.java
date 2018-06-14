package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * AccessLogElement writes the partial message into the buffer.
 */
public interface AccessLogValveAccessLogElement {
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time);

}