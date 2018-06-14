package org.apache.jasper.runtime;

import java.security.PrivilegedExceptionAction;

import javax.servlet.ServletRequest;

import org.apache.jasper.JasperException;

public class JspRuntimeLibraryPrivilegedIntrospectHelper
    implements PrivilegedExceptionAction<Void> {

    private Object bean;
    private String prop;
    private String value;
    private ServletRequest request;
    private String param;
    private boolean ignoreMethodNF;

    public JspRuntimeLibraryPrivilegedIntrospectHelper(Object bean, String prop,
                               String value, ServletRequest request,
                               String param, boolean ignoreMethodNF)
    {
        this.bean = bean;
        this.prop = prop;
        this.value = value;
        this.request = request;
        this.param = param;
        this.ignoreMethodNF = ignoreMethodNF;
    }
     
    @Override
    public Void run() throws JasperException {
        JspRuntimeLibrary.internalIntrospecthelper(
            bean,prop,value,request,param,ignoreMethodNF);
        return null;
    }
}