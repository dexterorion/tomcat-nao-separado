package org.apache.catalina.connector;

import java.security.PrivilegedAction;

public final class RequestFacadeGetParameterValuePrivilegedAction
        implements PrivilegedAction<String[]> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;
	private String name;

    public RequestFacadeGetParameterValuePrivilegedAction(RequestFacade requestFacade, String name){
        this.requestFacade = requestFacade;
		this.name = name;
    }

    @Override
    public String[] run() {
        return this.requestFacade.getRequest().getParameterValues(name);
    }
}