package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

/**
 * Represents a variable directive
 */
public class NodeVariableDirective extends Node {

    public NodeVariableDirective(Attributes attrs, Mark start, Node parent) {
        this(JSP_VARIABLE_DIRECTIVE_ACTION, attrs, null, null, start,
                parent);
    }

    public NodeVariableDirective(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, VARIABLE_DIRECTIVE_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}