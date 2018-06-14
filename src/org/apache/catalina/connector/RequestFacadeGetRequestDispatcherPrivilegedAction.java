package org.apache.catalina.connector;

import java.security.PrivilegedAction;

import javax.servlet.RequestDispatcher;

public final class RequestFacadeGetRequestDispatcherPrivilegedAction
        implements PrivilegedAction<RequestDispatcher> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;
	private final String path;

    public RequestFacadeGetRequestDispatcherPrivilegedAction(RequestFacade requestFacade, String path){
        this.requestFacade = requestFacade;
		this.path = path;
    }

    @Override
    public RequestDispatcher run() {
        return this.requestFacade.getRequest().getRequestDispatcher(path);
    }
}