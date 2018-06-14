package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class NodeComment extends Node {

    public NodeComment(String text, Mark start, Node parent) {
        super(null, null, text, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}