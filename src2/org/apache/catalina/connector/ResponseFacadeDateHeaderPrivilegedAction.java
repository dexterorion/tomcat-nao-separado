package org.apache.catalina.connector;

import java.security.PrivilegedAction;

public final class ResponseFacadeDateHeaderPrivilegedAction implements
		PrivilegedAction<Void> {

	/**
	 * 
	 */
	private final ResponseFacade responseFacade;
	private final String name;
	private final long value;
	private final boolean add;

	public ResponseFacadeDateHeaderPrivilegedAction(ResponseFacade responseFacade, String name, long value, boolean add) {
		this.responseFacade = responseFacade;
		this.name = name;
		this.value = value;
		this.add = add;
	}

	@Override
	public Void run() {
		if (add) {
			this.responseFacade.getResponse().addDateHeader(name, value);
		} else {
			this.responseFacade.getResponse().setDateHeader(name, value);
		}
		return null;
	}
}