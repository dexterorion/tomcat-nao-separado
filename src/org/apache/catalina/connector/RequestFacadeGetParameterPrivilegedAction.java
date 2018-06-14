package org.apache.catalina.connector;

import java.security.PrivilegedAction;

public final class RequestFacadeGetParameterPrivilegedAction
        implements PrivilegedAction<String> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;
	private String name;

    public RequestFacadeGetParameterPrivilegedAction(RequestFacade requestFacade, String name){
        this.requestFacade = requestFacade;
		this.name = name;
    }

    @Override
    public String run() {
        return this.requestFacade.getRequest().getParameter(name);
    }
}