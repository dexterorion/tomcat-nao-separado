package org.apache.catalina.connector;

import java.security.PrivilegedAction;

import javax.servlet.http.Cookie;

public final class RequestFacadeGetCookiesPrivilegedAction
        implements PrivilegedAction<Cookie[]> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetCookiesPrivilegedAction(RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Cookie[] run() {
        return this.requestFacade.getRequest().getCookies();
    }
}