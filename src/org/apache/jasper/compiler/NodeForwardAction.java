package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeForwardAction extends Node {

    private NodeJspAttribute page;

    public NodeForwardAction(Attributes attrs, Mark start, Node parent) {
        this(JSP_FORWARD_ACTION, attrs, null, null, start, parent);
    }

    public NodeForwardAction(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, FORWARD_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public void setPage(NodeJspAttribute page) {
        this.page = page;
    }

    public NodeJspAttribute getPage() {
        return page;
    }
}