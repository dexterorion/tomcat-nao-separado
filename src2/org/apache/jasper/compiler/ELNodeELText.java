package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ELNodeELText extends ELNode {

    private final String text;

    public ELNodeELText(String text) {
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