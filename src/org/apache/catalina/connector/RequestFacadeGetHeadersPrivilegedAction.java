package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Enumeration;

public final class RequestFacadeGetHeadersPrivilegedAction
        implements PrivilegedAction<Enumeration<String>> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;
	private final String name;

    public RequestFacadeGetHeadersPrivilegedAction(RequestFacade requestFacade, String name){
        this.requestFacade = requestFacade;
		this.name = name;
    }

    @Override
    public Enumeration<String> run() {
        return this.requestFacade.getRequest().getHeaders(name);
    }
}