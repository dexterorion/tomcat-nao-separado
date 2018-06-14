package org.apache.jasper.runtime;

import java.security.PrivilegedAction;

import javax.servlet.jsp.PageContext;

public class JspFactoryImplPrivilegedReleasePageContext
        implements PrivilegedAction<Void> {

    private JspFactoryImpl factory;
    private PageContext pageContext;

    public JspFactoryImplPrivilegedReleasePageContext(JspFactoryImpl factory,
            PageContext pageContext) {
        this.factory = factory;
        this.pageContext = pageContext;
    }

    @Override
    public Void run() {
        factory.internalReleasePageContext(pageContext);
        return null;
    }
}