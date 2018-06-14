package org.apache.catalina.connector;

import javax.servlet.DispatcherType;

public class RequestSpecialAttributeAdapter1 implements RequestSpecialAttributeAdapter{
	@Override
	public Object get(Request request, String name) {
		return (request.getInternalDispatcherType() == null) ? DispatcherType.REQUEST
				: request.getInternalDispatcherType();
	}

	@Override
	public void set(Request request, String name, Object value) {
		request.setInternalDispatcherType((DispatcherType) value);
	}
}