package org.apache.jasper.compiler;

public class AttributeVisitor extends NodeVisitor {
    String attrValue = null;

    @Override
    public void visit(NodeTemplateText txt) {
        attrValue = txt.getText();
    }

    public String getAttrValue() {
        return attrValue;
    }
}