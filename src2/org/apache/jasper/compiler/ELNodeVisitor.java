package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ELNodeVisitor {

    public void visit(ELNodeRoot n) throws JasperException {
        n.getExpression().visit(this);
    }

    @SuppressWarnings("unused")
    public void visit(ELNodeFunction n) throws JasperException {
        // NOOP by default
    }

    @SuppressWarnings("unused")
    public void visit(ELNodeText n) throws JasperException {
        // NOOP by default
    }

    @SuppressWarnings("unused")
    public void visit(ELNodeELText n) throws JasperException {
        // NOOP by default
    }
}