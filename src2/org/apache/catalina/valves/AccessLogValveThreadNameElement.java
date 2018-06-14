package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.coyote.RequestInfo;

public class AccessLogValveThreadNameElement implements
		AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		RequestInfo info = request.getCoyoteRequest().getRequestProcessor();
		if (info != null) {
			buf.append(info.getWorkerThreadName());
		} else {
			buf.append("-");
		}
	}
}