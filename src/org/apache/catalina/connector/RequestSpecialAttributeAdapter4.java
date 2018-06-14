package org.apache.catalina.connector;

import org.apache.catalina.realm.GenericPrincipal;

public class RequestSpecialAttributeAdapter4 implements RequestSpecialAttributeAdapter{
	@Override
	public Object get(Request request, String name) {
		if (request.getUserPrincipal() instanceof GenericPrincipal) {
			return ((GenericPrincipal) request.getUserPrincipal())
					.getGssCredential();
		}
		return null;
	}

	@Override
	public void set(Request request, String name, Object value) {
		// NO-OP
	}
}