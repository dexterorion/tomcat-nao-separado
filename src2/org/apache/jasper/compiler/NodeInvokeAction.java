package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeInvokeAction extends Node {

    public NodeInvokeAction(Attributes attrs, Mark start, Node parent) {
        this(JSP_INVOKE_ACTION, attrs, null, null, start, parent);
    }

    public NodeInvokeAction(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, INVOKE_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}