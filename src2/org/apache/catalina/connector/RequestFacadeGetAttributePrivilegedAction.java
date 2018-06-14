package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Enumeration;

public final class RequestFacadeGetAttributePrivilegedAction
        implements PrivilegedAction<Enumeration<String>> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetAttributePrivilegedAction(RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Enumeration<String> run() {
        return this.requestFacade.getRequest().getAttributeNames();
    }
}