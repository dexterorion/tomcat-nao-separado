package org.apache.catalina.ha.context;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.apache.catalina.core.ApplicationContext;

public class ReplicatedContextReplApplContext extends ApplicationContext {
    private ConcurrentHashMap<String, Object> tomcatAttributes =
        new ConcurrentHashMap<String, Object>();

    public ReplicatedContextReplApplContext(ReplicatedContext context) {
        super(context);
    }

    protected ReplicatedContext getParent() {
        return (ReplicatedContext)getContext();
    }

    @Override
    protected ServletContext getFacade() {
         return super.getFacade();
    }

    public Map<String,Object> getAttributeMap() {
        return this.getAttributes();
    }
    public void setAttributeMap(Map<String,Object> map) {
        this.setAttributes(map);
    }

    @Override
    public void removeAttribute(String name) {
        tomcatAttributes.remove(name);
        //do nothing
        super.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if ( (!getParent().getState().isAvailable()) || "org.apache.jasper.runtime.JspApplicationContextImpl".equals(name) ){
            tomcatAttributes.put(name,value);
        } else
            super.setAttribute(name,value);
    }

    @Override
    public Object getAttribute(String name) {
        Object obj = tomcatAttributes.get(name);
        if (obj == null) {
            return super.getAttribute(name);
        } else {
            return obj;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> names = new HashSet<String>();
        names.addAll(getAttributes().keySet());

        return new ReplicatedContextMultiEnumeration<String>(new Enumeration[] {
                super.getAttributeNames(),
                Collections.enumeration(names) });
    }
}