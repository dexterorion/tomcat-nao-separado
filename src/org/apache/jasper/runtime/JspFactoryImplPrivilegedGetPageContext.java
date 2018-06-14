package org.apache.jasper.runtime;

import java.security.PrivilegedAction;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.PageContext;

public class JspFactoryImplPrivilegedGetPageContext
        implements PrivilegedAction<PageContext> {

    private JspFactoryImpl factory;
    private Servlet servlet;
    private ServletRequest request;
    private ServletResponse response;
    private String errorPageURL;
    private boolean needsSession;
    private int bufferSize;
    private boolean autoflush;

    public JspFactoryImplPrivilegedGetPageContext(JspFactoryImpl factory, Servlet servlet,
            ServletRequest request, ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoflush) {
        this.factory = factory;
        this.servlet = servlet;
        this.request = request;
        this.response = response;
        this.errorPageURL = errorPageURL;
        this.needsSession = needsSession;
        this.bufferSize = bufferSize;
        this.autoflush = autoflush;
    }

    @Override
    public PageContext run() {
        return factory.internalGetPageContext(servlet, request, response,
                errorPageURL, needsSession, bufferSize, autoflush);
    }
}