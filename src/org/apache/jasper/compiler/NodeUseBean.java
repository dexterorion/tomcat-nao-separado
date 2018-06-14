package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeUseBean extends Node {

    private NodeJspAttribute beanName;

    public NodeUseBean(Attributes attrs, Mark start, Node parent) {
        this(JSP_USE_BEAN_ACTION, attrs, null, null, start, parent);
    }

    public NodeUseBean(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, USE_BEAN_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public void setBeanName(NodeJspAttribute beanName) {
        this.beanName = beanName;
    }

    public NodeJspAttribute getBeanName() {
        return beanName;
    }
}