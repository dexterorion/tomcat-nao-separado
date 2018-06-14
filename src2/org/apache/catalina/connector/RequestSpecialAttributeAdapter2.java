package org.apache.catalina.connector;

public class RequestSpecialAttributeAdapter2 implements RequestSpecialAttributeAdapter{
	@Override
	public Object get(Request request, String name) {
		return (request.getRequestDispatcherPath() == null) ? request
				.getRequestPathMB().toString()
				: request.getRequestDispatcherPath().toString();
	}

	@Override
	public void set(Request request, String name, Object value) {
		request.setRequestDispatcherPath(value);
	}
}