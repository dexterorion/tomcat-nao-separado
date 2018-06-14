package org.apache.catalina.connector;

import java.security.PrivilegedAction;

public final class RequestFacadeGetCharacterEncodingPrivilegedAction
        implements PrivilegedAction<String> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetCharacterEncodingPrivilegedAction(
			RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public String run() {
        return this.requestFacade.getRequest().getCharacterEncoding();
    }
}