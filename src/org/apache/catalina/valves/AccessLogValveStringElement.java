package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveStringElement implements
		AccessLogValveAccessLogElement {
	private final String str;

	public AccessLogValveStringElement(String str) {
		this.str = str;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		buf.append(str);
	}
}