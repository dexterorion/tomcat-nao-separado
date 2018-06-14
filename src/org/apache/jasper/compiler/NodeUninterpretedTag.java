package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeUninterpretedTag extends Node {

    private NodeJspAttribute[] jspAttrs;

    public NodeUninterpretedTag(String qName, String localName,
            Attributes attrs, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, localName, attrs, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public void setJspAttributes(NodeJspAttribute[] jspAttrs) {
        this.jspAttrs = jspAttrs;
    }

    public NodeJspAttribute[] getJspAttributes() {
        return jspAttrs;
    }
}