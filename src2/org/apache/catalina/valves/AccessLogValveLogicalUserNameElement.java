package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveLogicalUserNameElement implements
		AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		buf.append('-');
	}
}