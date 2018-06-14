package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeScriptlet extends NodeScriptingElement {

    public NodeScriptlet(String text, Mark start, Node parent) {
        super(JSP_SCRIPTLET_ACTION, SCRIPTLET_ACTION, text, start, parent);
    }

    public NodeScriptlet(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, SCRIPTLET_ACTION, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}