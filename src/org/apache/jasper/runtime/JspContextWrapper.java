/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException2;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.compiler.Localizer;

/**
 * Implementation of a JSP Context Wrapper.
 * 
 * The JSP Context Wrapper is a JspContext created and maintained by a tag
 * handler implementation. It wraps the Invoking JSP Context, that is, the
 * JspContext instance passed to the tag handler by the invoking page via
 * setJspContext().
 * 
 * @author Kin-man Chung
 * @author Jan Luehe
 * @author Jacob Hookom
 */
public class JspContextWrapper extends PageContext implements VariableResolver {

    // Invoking JSP context
    private PageContext invokingJspCtxt;

    private transient HashMap<String, Object> pageAttributes;

    // ArrayList of NESTED scripting variables
    private ArrayList<String> nestedVars;

    // ArrayList of AT_BEGIN scripting variables
    private ArrayList<String> atBeginVars;

    // ArrayList of AT_END scripting variables
    private ArrayList<String> atEndVars;

    private Map<String,String> aliases;

    private HashMap<String, Object> originalNestedVars;

    private ServletContext servletContext = null;

    private ELContext elContext = null;

    private PageContext rootJspCtxt;

    public JspContextWrapper(JspContext jspContext,
            ArrayList<String> nestedVars, ArrayList<String> atBeginVars,
            ArrayList<String> atEndVars, Map<String,String> aliases) {
        this.invokingJspCtxt = (PageContext) jspContext;
        if (jspContext instanceof JspContextWrapper) {
            setRootJspCtxtData(((JspContextWrapper)jspContext).getRootJspCtxtData());
        }
        else {
            setRootJspCtxtData(invokingJspCtxt);
        }
        this.nestedVars = nestedVars;
        this.atBeginVars = atBeginVars;
        this.atEndVars = atEndVars;
        this.pageAttributes = new HashMap<String, Object>(16);
        this.aliases = aliases;

        if (nestedVars != null) {
            this.originalNestedVars = new HashMap<String, Object>(nestedVars.size());
        }
        syncBeginTagFile();
    }

    @Override
    public void initialize(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException {
    }

    @Override
    public Object getAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        return pageAttributes.get(name);
    }

    @Override
    public Object getAttribute(String name, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == getPageScope()) {
            return pageAttributes.get(name);
        }

        return getRootJspCtxtData().getAttribute(name, scope);
    }

    @Override
    public void setAttribute(String name, Object value) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (value != null) {
            pageAttributes.put(name, value);
        } else {
            removeAttribute(name, getPageScope());
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == getPageScope()) {
            if (value != null) {
                pageAttributes.put(name, value);
            } else {
                removeAttribute(name, getPageScope());
            }
        } else {
            getRootJspCtxtData().setAttribute(name, value, scope);
        }
    }

    @Override
    public Object findAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        Object o = pageAttributes.get(name);
        if (o == null) {
            o = getRootJspCtxtData().getAttribute(name, getRequestScope());
            if (o == null) {
                if (getSession() != null) {
                    o = getRootJspCtxtData().getAttribute(name, getSessionScope());
                }
                if (o == null) {
                    o = getRootJspCtxtData().getAttribute(name, getApplicationScope());
                }
            }
        }

        return o;
    }

    @Override
    public void removeAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        pageAttributes.remove(name);
        getRootJspCtxtData().removeAttribute(name, getRequestScope());
        if (getSession() != null) {
            getRootJspCtxtData().removeAttribute(name, getSessionScope());
        }
        getRootJspCtxtData().removeAttribute(name, getApplicationScope());
    }

    @Override
    public void removeAttribute(String name, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == getPageScope()) {
            pageAttributes.remove(name);
        } else {
            getRootJspCtxtData().removeAttribute(name, scope);
        }
    }

    @Override
    public int getAttributesScope(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (pageAttributes.get(name) != null) {
            return getPageScope();
        } else {
            return getRootJspCtxtData().getAttributesScope(name);
        }
    }

    @Override
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        if (scope == getPageScope()) {
            return Collections.enumeration(pageAttributes.keySet());
        }

        return getRootJspCtxtData().getAttributeNamesInScope(scope);
    }

    @Override
    public void release() {
        invokingJspCtxt.release();
    }

    @Override
    public JspWriter getOut() {
        return getRootJspCtxtData().getOut();
    }

    @Override
    public HttpSession getSession() {
        return getRootJspCtxtData().getSession();
    }

    @Override
    public Object getPage() {
        return invokingJspCtxt.getPage();
    }

    @Override
    public ServletRequest getRequest() {
        return invokingJspCtxt.getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return getRootJspCtxtData().getResponse();
    }

    @Override
    public Exception getException() {
        return invokingJspCtxt.getException();
    }

    @Override
    public ServletConfig getServletConfig() {
        return invokingJspCtxt.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        if (servletContext == null) {
            servletContext = getRootJspCtxtData().getServletContext();
        }
        return servletContext;
    }

    @Override
    public void forward(String relativeUrlPath) throws ServletException,
            IOException {
        invokingJspCtxt.forward(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath) throws ServletException,
            IOException {
        invokingJspCtxt.include(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath, boolean flush)
            throws ServletException, IOException {
        invokingJspCtxt.include(relativeUrlPath, false);
    }

    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        return this;
    }

    @Override
    public BodyContent pushBody() {
        return invokingJspCtxt.pushBody();
    }

    @Override
    public JspWriter pushBody(Writer writer) {
        return invokingJspCtxt.pushBody(writer);
    }

    @Override
    public JspWriter popBody() {
        return invokingJspCtxt.popBody();
    }

    @Override
    @Deprecated
    public ExpressionEvaluator getExpressionEvaluator() {
        return invokingJspCtxt.getExpressionEvaluator();
    }

    @Override
    public void handlePageException(Exception ex) throws IOException,
            ServletException {
        // Should never be called since handleException() called with a
        // Throwable in the generated servlet.
        handlePageException((Throwable) ex);
    }

    @Override
    public void handlePageException(Throwable t) throws IOException,
            ServletException {
        invokingJspCtxt.handlePageException(t);
    }

    /**
     * VariableResolver interface
     */
    @Override
    @Deprecated
    public Object resolveVariable(String pName) throws ELException2 {
        ELContext ctx = this.getELContext();
        return ctx.getELResolver().getValue(ctx, null, pName);
    }

    /**
     * Synchronize variables at begin of tag file
     */
    public void syncBeginTagFile() {
        saveNestedVariables();
    }

    /**
     * Synchronize variables before fragment invocation
     */
    public void syncBeforeInvoke() {
        copyTagToPageScope(VariableInfo.getNested());
        copyTagToPageScope(VariableInfo.getAtBegin());
    }

    /**
     * Synchronize variables at end of tag file
     */
    public void syncEndTagFile() {
        copyTagToPageScope(VariableInfo.getAtBegin());
        copyTagToPageScope(VariableInfo.getAtEnd());
        restoreNestedVariables();
    }

    /**
     * Copies the variables of the given scope from the virtual page scope of
     * this JSP context wrapper to the page scope of the invoking JSP context.
     * 
     * @param scope
     *            variable scope (one of NESTED, AT_BEGIN, or AT_END)
     */
    private void copyTagToPageScope(int scope) {
        Iterator<String> iter = null;

        switch (scope) {
        case 0:
            if (nestedVars != null) {
                iter = nestedVars.iterator();
            }
            break;
        case 1:
            if (atBeginVars != null) {
                iter = atBeginVars.iterator();
            }
            break;
        case 2:
            if (atEndVars != null) {
                iter = atEndVars.iterator();
            }
            break;
        }

        while ((iter != null) && iter.hasNext()) {
            String varName = iter.next();
            Object obj = getAttribute(varName);
            varName = findAlias(varName);
            if (obj != null) {
                invokingJspCtxt.setAttribute(varName, obj);
            } else {
                invokingJspCtxt.removeAttribute(varName, getPageScope());
            }
        }
    }

    /**
     * Saves the values of any NESTED variables that are present in the invoking
     * JSP context, so they can later be restored.
     */
    private void saveNestedVariables() {
        if (nestedVars != null) {
            Iterator<String> iter = nestedVars.iterator();
            while (iter.hasNext()) {
                String varName = iter.next();
                varName = findAlias(varName);
                Object obj = invokingJspCtxt.getAttribute(varName);
                if (obj != null) {
                    originalNestedVars.put(varName, obj);
                }
            }
        }
    }

    /**
     * Restores the values of any NESTED variables in the invoking JSP context.
     */
    private void restoreNestedVariables() {
        if (nestedVars != null) {
            Iterator<String> iter = nestedVars.iterator();
            while (iter.hasNext()) {
                String varName = iter.next();
                varName = findAlias(varName);
                Object obj = originalNestedVars.get(varName);
                if (obj != null) {
                    invokingJspCtxt.setAttribute(varName, obj);
                } else {
                    invokingJspCtxt.removeAttribute(varName, getPageScope());
                }
            }
        }
    }

    /**
     * Checks to see if the given variable name is used as an alias, and if so,
     * returns the variable name for which it is used as an alias.
     * 
     * @param varName
     *            The variable name to check
     * @return The variable name for which varName is used as an alias, or
     *         varName if it is not being used as an alias
     */
    private String findAlias(String varName) {

        if (aliases == null)
            return varName;

        String alias = aliases.get(varName);
        if (alias == null) {
            return varName;
        }
        return alias;
    }

    //private ELContextImpl elContext;

    @Override
    public ELContext getELContext() {
        // instead decorate!!!
        
        if (elContext == null) {
            elContext = getRootJspCtxtData().getELContext();
        }
        return elContext;
        
        /*
        if (this.elContext != null) {
            JspFactory jspFact = JspFactory.getDefaultFactory();
            ServletContext servletContext = this.getServletContext();
            JspApplicationContextImpl jspCtx = (JspApplicationContextImpl) jspFact
                    .getJspApplicationContext(servletContext);
            this.elContext = jspCtx.createELContext(this);
        }
        return this.elContext;
        */
    }

	public PageContext getRootJspCtxtData() {
		return rootJspCtxt;
	}

	public void setRootJspCtxtData(PageContext rootJspCtxt) {
		this.rootJspCtxt = rootJspCtxt;
	}
}
