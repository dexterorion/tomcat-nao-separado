package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.NodeVisitor;
import org.xml.sax.Attributes;

public class NodeGetProperty extends Node {

    public NodeGetProperty(Attributes attrs, Mark start, Node parent) {
        this(JSP_GET_PROPERTY_ACTION, attrs, null, null, start, parent);
    }

    public NodeGetProperty(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, GET_PROPERTY_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}