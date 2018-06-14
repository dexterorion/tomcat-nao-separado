package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/**
 * Child classes
 */


public class ELNodeText extends ELNode {

    private final String text;

    public ELNodeText(String text) {
        this.text = text;
    }

    @Override
    public void accept(ELNodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public String getText() {
        return text;
    }
}