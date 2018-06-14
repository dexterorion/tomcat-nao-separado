package org.apache.catalina.connector;

import java.security.PrivilegedAction;
import java.util.Map;

public final class RequestFacadeGetParameterMapPrivilegedAction
        implements PrivilegedAction<Map<String,String[]>> {

    /**
	 * 
	 */
	private final RequestFacade requestFacade;

	/**
	 * @param requestFacade
	 */
	public RequestFacadeGetParameterMapPrivilegedAction(RequestFacade requestFacade) {
		this.requestFacade = requestFacade;
	}

	@Override
    public Map<String,String[]> run() {
        return this.requestFacade.getRequest().getParameterMap();
    }
}