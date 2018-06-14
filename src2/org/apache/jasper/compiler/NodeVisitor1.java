package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class NodeVisitor1 extends NodeVisitor {
    private final TagPluginManager manager;
    private final PageInfo pageInfo;

    public NodeVisitor1(TagPluginManager manager, PageInfo pageInfo) {
        this.manager = manager;
        this.pageInfo = pageInfo;
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        manager.invokePlugin(n, pageInfo);
        visitBody(n);
    }
}