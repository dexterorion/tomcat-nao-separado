package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeDoBodyAction extends Node {

    public NodeDoBodyAction(Attributes attrs, Mark start, Node parent) {
        this(JSP_DOBODY_ACTION, attrs, null, null, start, parent);
    }

    public NodeDoBodyAction(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, DOBODY_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}