package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Locale;

public final class RequestFacadeGetLocalesPrivilegedAction
        implements PrivilegedAction<Enumeration<Locale>> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetLocalesPrivilegedAction(RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Enumeration<Locale> run() {
        return this.requestFacade.getRequest().getLocales();
    }
}