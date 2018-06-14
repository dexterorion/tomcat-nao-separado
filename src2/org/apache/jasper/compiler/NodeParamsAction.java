package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeParamsAction extends Node {

    public NodeParamsAction(Mark start, Node parent) {
        this(JSP_PARAMS_ACTION, null, null, start, parent);
    }

    public NodeParamsAction(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, PARAMS_ACTION, null, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}