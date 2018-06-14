package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class NodeELExpression extends Node {

    private ELNodeNodes el;

    private final char type;

    public NodeELExpression(char type, String text, Mark start, Node parent) {
        super(null, null, text, start, parent);
        this.type = type;
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public void setEL(ELNodeNodes el) {
        this.el = el;
    }

    public ELNodeNodes getEL() {
        return el;
    }

    public char getType() {
        return this.type;
    }
}