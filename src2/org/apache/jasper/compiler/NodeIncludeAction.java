package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeIncludeAction extends Node {

    private NodeJspAttribute page;

    public NodeIncludeAction(Attributes attrs, Mark start, Node parent) {
        this(JSP_INCLUDE_ACTION, attrs, null, null, start, parent);
    }

    public NodeIncludeAction(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, INCLUDE_ACTION, attrs, nonTaglibXmlnsAttrs,
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