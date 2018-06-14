package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Enumeration;

public final class RequestFacadeGetHeaderNamesPrivilegedAction
        implements PrivilegedAction<Enumeration<String>> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetHeaderNamesPrivilegedAction(RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Enumeration<String> run() {
        return this.requestFacade.getRequest().getHeaderNames();
    }
}