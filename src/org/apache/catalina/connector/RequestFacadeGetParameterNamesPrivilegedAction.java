package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Enumeration;

public final class RequestFacadeGetParameterNamesPrivilegedAction
        implements PrivilegedAction<Enumeration<String>> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetParameterNamesPrivilegedAction(
			RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Enumeration<String> run() {
        return this.requestFacade.getRequest().getParameterNames();
    }
}