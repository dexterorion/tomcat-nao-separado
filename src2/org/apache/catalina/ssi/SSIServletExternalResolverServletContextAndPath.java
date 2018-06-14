package org.apache.catalina.ssi;

import javax.servlet.ServletContext;

public class SSIServletExternalResolverServletContextAndPath {
    private ServletContext servletContext;
    private String path;


    public SSIServletExternalResolverServletContextAndPath(ServletContext servletContext,
                                 String path) {
        this.servletContext = servletContext;
        this.path = path;
    }


    public ServletContext getServletContext() {
        return servletContext;
    }


    public String getPath() {
        return path;
    }
}