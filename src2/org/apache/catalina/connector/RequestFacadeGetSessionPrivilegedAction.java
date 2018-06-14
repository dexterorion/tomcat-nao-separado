package org.apache.catalina.connector;

import java.security.PrivilegedAction;

import javax.servlet.http.HttpSession;

public final class RequestFacadeGetSessionPrivilegedAction
        implements PrivilegedAction<HttpSession> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;
	private final boolean create;

    public RequestFacadeGetSessionPrivilegedAction(RequestFacade requestFacade, boolean create){
        this.requestFacade = requestFacade;
		this.create = create;
    }

    @Override
    public HttpSession run() {
        return this.requestFacade.getRequest().getSession(create);
    }
}