package org.apache.catalina.connector;

import java.security.PrivilegedAction;

public final class ResponseFacadeSetContentTypePrivilegedAction implements
		PrivilegedAction<Void> {

	/**
	 * 
	 */
	private final ResponseFacade responseFacade;
	private final String contentType;

	public ResponseFacadeSetContentTypePrivilegedAction(ResponseFacade responseFacade, String contentType) {
		this.responseFacade = responseFacade;
		this.contentType = contentType;
	}

	@Override
	public Void run() {
		this.responseFacade.getResponse().setContentType(contentType);
		return null;
	}
}