package org.apache.catalina.connector;

public class RequestSpecialAttributeAdapter5 implements RequestSpecialAttributeAdapter{
	@Override
	public Object get(Request request, String name) {
		if (request.getCoyoteRequest().getParameters()
				.isParseFailed()) {
			return Boolean.TRUE;
		}
		return null;
	}

	@Override
	public void set(Request request, String name, Object value) {
		// NO-OP
	}
}