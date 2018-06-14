package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeFallBackAction extends Node {

    public NodeFallBackAction(Mark start, Node parent) {
        this(JSP_FALLBACK_ACTION, null, null, start, parent);
    }

    public NodeFallBackAction(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, FALLBACK_ACTION, null, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}