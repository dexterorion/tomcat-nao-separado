package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/**
 * Represents an EL expression: anything in ${ and }.
 */
public class ELNodeRoot extends ELNode {

    private final ELNodeNodes expr;
    private final char type;

    public ELNodeRoot(ELNodeNodes expr, char type) {
        this.expr = expr;
    this.type = type;
    }

    @Override
    public void accept(ELNodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public ELNodeNodes getExpression() {
        return expr;
    }

    public char getType() {
        return type;
    }
}