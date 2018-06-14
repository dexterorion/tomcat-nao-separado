package org.apache.jasper.runtime;

import javax.servlet.jsp.PageContext;

public final class JspFactoryImplPageContextPool  {

    private PageContext[] pool;

    private int current = -1;

    public JspFactoryImplPageContextPool() {
        this.pool = new PageContext[JspFactoryImpl.getPoolSize()];
    }

    public void put(PageContext o) {
        if (current < (JspFactoryImpl.getPoolSize() - 1)) {
            current++;
            pool[current] = o;
        }
    }

    public PageContext get() {
        PageContext item = null;
        if (current >= 0) {
            item = pool[current];
            current--;
        }
        return item;
    }

}