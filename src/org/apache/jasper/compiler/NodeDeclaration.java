package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeDeclaration extends NodeScriptingElement {

    public NodeDeclaration(String text, Mark start, Node parent) {
        super(JSP_DECLARATION_ACTION, DECLARATION_ACTION, text, start,
                parent);
    }

    public NodeDeclaration(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, DECLARATION_ACTION, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}