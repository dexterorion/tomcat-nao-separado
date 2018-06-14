package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.NodeVisitor;
import org.xml.sax.Attributes;

public class NodeExpression extends NodeScriptingElement {

    public NodeExpression(String text, Mark start, Node parent) {
        super(JSP_EXPRESSION_ACTION, EXPRESSION_ACTION, text, start, parent);
    }

    public NodeExpression(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, EXPRESSION_ACTION, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}