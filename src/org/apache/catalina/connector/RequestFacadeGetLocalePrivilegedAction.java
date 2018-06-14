package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Locale;

public final class RequestFacadeGetLocalePrivilegedAction
        implements PrivilegedAction<Locale> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetLocalePrivilegedAction(RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Locale run() {
        return this.requestFacade.getRequest().getLocale();
    }
}