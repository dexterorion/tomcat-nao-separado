package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ValidatorValidateVisitorNamedAttributeVisitor extends NodeVisitor {
    private boolean hasDynamicContent;

    @Override
    public void doVisit(Node n) throws JasperException {
        if (!(n instanceof NodeJspText)
                && !(n instanceof NodeTemplateText)) {
            hasDynamicContent = true;
        }
        visitBody(n);
    }

    public boolean hasDynamicContent() {
        return hasDynamicContent;
    }
}