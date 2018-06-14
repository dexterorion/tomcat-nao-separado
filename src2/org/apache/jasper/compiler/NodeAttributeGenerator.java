package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class NodeAttributeGenerator extends Node {
    private String name; // name of the attribute

    private NodeCustomTag tag; // The tag this attribute belongs to

    public NodeAttributeGenerator(Mark start, String name, NodeCustomTag tag) {
        super(start, null);
        this.name = name;
        this.tag = tag;
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public String getName() {
        return name;
    }

    public NodeCustomTag getTag() {
        return tag;
    }
}