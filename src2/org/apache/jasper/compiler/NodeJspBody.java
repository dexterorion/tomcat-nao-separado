package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeJspBody extends Node {

    private NodeChildInfo childInfo;

    public NodeJspBody(Mark start, Node parent) {
        this(JSP_BODY_ACTION, null, null, start, parent);
    }

    public NodeJspBody(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, BODY_ACTION, null, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
        this.childInfo = new NodeChildInfo();
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public NodeChildInfo getNodeChildInfo() {
        return childInfo;
    }
}