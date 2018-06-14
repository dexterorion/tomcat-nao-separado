package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeJspElement extends Node {

    private NodeJspAttribute[] jspAttrs;

    private NodeJspAttribute nameAttr;

    public NodeJspElement(Attributes attrs, Mark start, Node parent) {
        this(JSP_ELEMENT_ACTION, attrs, null, null, start, parent);
    }

    public NodeJspElement(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, ELEMENT_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
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

    /*
     * Sets the XML-style 'name' attribute
     */
    public void setNameAttribute(NodeJspAttribute nameAttr) {
        this.nameAttr = nameAttr;
    }

    /*
     * Gets the XML-style 'name' attribute
     */
    public NodeJspAttribute getNameAttribute() {
        return this.nameAttr;
    }
}