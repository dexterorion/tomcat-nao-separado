package org.apache.catalina.connector;

public class RequestSpecialAttributeAdapter3 implements RequestSpecialAttributeAdapter{
	@Override
	public Object get(Request request, String name) {
		return request.getAsyncSupported();
	}

	@Override
	public void set(Request request, String name, Object value) {
		Boolean oldValue = request.getAsyncSupported();
		request.setAsyncSupported((Boolean) value);
		request.notifyAttributeAssigned(name, value, oldValue);
	}
}