package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class NodeVisitor {

    /**
     * This method provides a place to put actions that are common to all
     * nodes. Override this in the child visitor class if need to.
     */
    @SuppressWarnings("unused")
    protected void doVisit(Node n) throws JasperException {
        // NOOP by default
    }

    /**
     * Visit the body of a node, using the current visitor
     */
    protected void visitBody(Node n) throws JasperException {
        if (n.getBody() != null) {
            n.getBody().visit(this);
        }
    }

    public void visit(NodeRoot n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeJspRoot n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodePageDirective n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeTagDirective n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeIncludeDirective n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeTaglibDirective n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeAttributeDirective n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeVariableDirective n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeComment n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeDeclaration n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeExpression n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeScriptlet n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeELExpression n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeIncludeAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeForwardAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeGetProperty n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeSetProperty n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeParamAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeParamsAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeFallBackAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeUseBean n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodePlugIn n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeCustomTag n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeUninterpretedTag n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeJspElement n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeJspText n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeNamedAttribute n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeJspBody n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeInvokeAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeDoBodyAction n) throws JasperException {
        doVisit(n);
        visitBody(n);
    }

    public void visit(NodeTemplateText n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeJspOutput n) throws JasperException {
        doVisit(n);
    }

    public void visit(NodeAttributeGenerator n) throws JasperException {
        doVisit(n);
    }
}