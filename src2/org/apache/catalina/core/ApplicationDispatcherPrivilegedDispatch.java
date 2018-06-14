package org.apache.catalina.core;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ApplicationDispatcherPrivilegedDispatch implements
        PrivilegedExceptionAction<Void> {
    /**
	 * 
	 */
	private final ApplicationDispatcher applicationDispatcher;
	private final ServletRequest request;
    private final ServletResponse response;

    public ApplicationDispatcherPrivilegedDispatch(ApplicationDispatcher applicationDispatcher, ServletRequest request, ServletResponse response) {
        this.applicationDispatcher = applicationDispatcher;
		this.request = request;
        this.response = response;
    }

    @Override
    public Void run() throws ServletException, IOException {
        this.applicationDispatcher.doDispatch(request, response);
        return null;
    }
}