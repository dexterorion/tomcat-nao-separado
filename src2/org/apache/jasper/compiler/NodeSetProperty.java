package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeSetProperty extends Node {

    private NodeJspAttribute value;

    public NodeSetProperty(Attributes attrs, Mark start, Node parent) {
        this(JSP_SET_PROPERTY_ACTION, attrs, null, null, start, parent);
    }

    public NodeSetProperty(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, SET_PROPERTY_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public void setValue(NodeJspAttribute value) {
        this.value = value;
    }

    public NodeJspAttribute getValue() {
        return value;
    }
}