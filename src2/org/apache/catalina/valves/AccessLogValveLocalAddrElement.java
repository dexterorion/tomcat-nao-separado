package org.apache.catalina.valves;

import java.net.InetAddress;
import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.ExceptionUtils2;

public class AccessLogValveLocalAddrElement implements
		AccessLogValveAccessLogElement {

	private static final String LOCAL_ADDR_VALUE;

	static {
		String init;
		try {
			init = InetAddress.getLocalHost().getHostAddress();
		} catch (Throwable e) {
			ExceptionUtils2.handleThrowable(e);
			init = "127.0.0.1";
		}
		LOCAL_ADDR_VALUE = init;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		buf.append(LOCAL_ADDR_VALUE);
	}
}