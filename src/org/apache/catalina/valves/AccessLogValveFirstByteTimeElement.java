package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveFirstByteTimeElement implements
		AccessLogValveAccessLogElement {
	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		long commitTime = response.getCoyoteResponse().getCommitTime();
		if (commitTime == -1) {
			buf.append('-');
		} else {
			long delta = commitTime
					- request.getCoyoteRequest().getStartTime();
			buf.append(Long.toString(delta));
		}
	}
}