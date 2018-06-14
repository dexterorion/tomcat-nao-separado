package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeTaglibDirective extends Node {

    public NodeTaglibDirective(Attributes attrs, Mark start, Node parent) {
        super(JSP_TAGLIB_DIRECTIVE_ACTION, TAGLIB_DIRECTIVE_ACTION, attrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}