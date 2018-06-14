package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodePlugIn extends Node {

    private NodeJspAttribute width;

    private NodeJspAttribute height;

    public NodePlugIn(Attributes attrs, Mark start, Node parent) {
        this(JSP_PLUGIN_ACTION, attrs, null, null, start, parent);
    }

    public NodePlugIn(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, PLUGIN_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public void setHeight(NodeJspAttribute height) {
        this.height = height;
    }

    public void setWidth(NodeJspAttribute width) {
        this.width = width;
    }

    public NodeJspAttribute getHeight() {
        return height;
    }

    public NodeJspAttribute getWidth() {
        return width;
    }
}