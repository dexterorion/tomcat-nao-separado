package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeJspText extends Node {

    public NodeJspText(String qName, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent) {
        super(qName, TEXT_ACTION, null, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }
}