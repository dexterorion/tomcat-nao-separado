package org.apache.catalina.core;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApplicationDispatcherState {
	public ApplicationDispatcherState(ServletRequest request, ServletResponse response, boolean including) {
        this.setOuterRequest(request);
        this.setOuterResponse(response);
        this.setIncluding(including);
    }

    public ServletRequest getOuterRequest() {
		return outerRequest;
	}

	public void setOuterRequest(ServletRequest outerRequest) {
		this.outerRequest = outerRequest;
	}

	public ServletResponse getOuterResponse() {
		return outerResponse;
	}

	public void setOuterResponse(ServletResponse outerResponse) {
		this.outerResponse = outerResponse;
	}

	public ServletRequest getWrapRequest() {
		return wrapRequest;
	}

	public void setWrapRequest(ServletRequest wrapRequest) {
		this.wrapRequest = wrapRequest;
	}

	public ServletResponse getWrapResponse() {
		return wrapResponse;
	}

	public void setWrapResponse(ServletResponse wrapResponse) {
		this.wrapResponse = wrapResponse;
	}

	public boolean isIncluding() {
		return including;
	}

	public void setIncluding(boolean including) {
		this.including = including;
	}

	public HttpServletRequest getHrequest() {
		return hrequest;
	}

	public void setHrequest(HttpServletRequest hrequest) {
		this.hrequest = hrequest;
	}

	public HttpServletResponse getHresponse() {
		return hresponse;
	}

	public void setHresponse(HttpServletResponse hresponse) {
		this.hresponse = hresponse;
	}

	/**
     * The outermost request that will be passed on to the invoked servlet.
     */
    private ServletRequest outerRequest = null;


    /**
     * The outermost response that will be passed on to the invoked servlet.
     */
    private ServletResponse outerResponse = null;
    
    /**
     * The request wrapper we have created and installed (if any).
     */
    private ServletRequest wrapRequest = null;


    /**
     * The response wrapper we have created and installed (if any).
     */
    private ServletResponse wrapResponse = null;
    
    /**
     * Are we performing an include() instead of a forward()?
     */
    private boolean including = false;

    /**
     * Outermost HttpServletRequest in the chain
     */
    private HttpServletRequest hrequest = null;

    /**
     * Outermost HttpServletResponse in the chain
     */
    private HttpServletResponse hresponse = null;
}