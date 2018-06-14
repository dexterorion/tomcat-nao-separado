package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeParamAction extends Node {

    private NodeJspAttribute value;

    public NodeParamAction(Attributes attrs, Mark start, Node parent) {
        this(JSP_PARAM_ACTION, attrs, null, null, start, parent);
    }

    public NodeParamAction(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, PARAM_ACTION, attrs, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
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