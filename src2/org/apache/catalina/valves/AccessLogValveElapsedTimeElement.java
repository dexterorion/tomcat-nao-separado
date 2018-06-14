package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveElapsedTimeElement implements
		AccessLogValveAccessLogElement {
	private final boolean millis;

	/**
	 * if millis is true, write time in millis - %D if millis is false,
	 * write time in seconds - %T
	 */
	public AccessLogValveElapsedTimeElement(boolean millis) {
		this.millis = millis;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		if (millis) {
			buf.append(time);
		} else {
			// second
			buf.append(time / 1000);
			buf.append('.');
			int remains = (int) (time % 1000);
			buf.append(remains / 100);
			remains = remains % 100;
			buf.append(remains / 10);
			buf.append(remains % 10);
		}
	}
}