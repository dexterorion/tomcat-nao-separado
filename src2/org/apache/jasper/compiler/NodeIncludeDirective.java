package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeIncludeDirective extends Node {

    public NodeIncludeDirective(Attributes attrs, Mark start, Node parent) {
        this(JSP_INCLUDE_DIRECTIVE_ACTION, attrs, null, null, start, parent);
    }

    public NodeIncludeDirective(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, INCLUDE_DIRECTIVE_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}