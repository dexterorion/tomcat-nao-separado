package org.apache.catalina.core;

import java.security.PrivilegedExceptionAction;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ApplicationDispatcherPrivilegedForward implements PrivilegedExceptionAction<Void> {
    /**
	 * 
	 */
	private final ApplicationDispatcher applicationDispatcher;
	private ServletRequest request;
    private ServletResponse response;

    public ApplicationDispatcherPrivilegedForward(ApplicationDispatcher applicationDispatcher, ServletRequest request, ServletResponse response) {
        this.applicationDispatcher = applicationDispatcher;
		this.request = request;
        this.response = response;
    }

    @Override
    public Void run() throws java.lang.Exception {
        this.applicationDispatcher.doForward(request,response);
        return null;
    }
}