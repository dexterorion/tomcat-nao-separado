package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogValveByteSentElement implements AccessLogValveAccessLogElement {
	private final boolean conversion;

	/**
	 * if conversion is true, write '-' instead of 0 - %b
	 */
	public AccessLogValveByteSentElement(boolean conversion) {
		this.conversion = conversion;
	}

	@Override
	public void addElement(StringBuilder buf, Date date, Request request,
			Response response, long time) {
		// Don't need to flush since trigger for log message is after the
		// response has been committed
		long length = response.getBytesWritten(false);
		if (length <= 0) {
			// Protect against nulls and unexpected types as these values
			// may be set by untrusted applications
			Object start = request.getAttribute(Globals
					.getSendfileFileStartAttr());
			if (start instanceof Long) {
				Object end = request.getAttribute(Globals
						.getSendfileFileEndAttr());
				if (end instanceof Long) {
					length = ((Long) end).longValue()
							- ((Long) start).longValue();
				}
			}
		}
		if (length <= 0 && conversion) {
			buf.append('-');
		} else {
			buf.append(length);
		}
	}
}