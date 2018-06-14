package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ValidatorValidateVisitorXmlEscapeNonELVisitor extends ELNodeTextBuilder {

    public ValidatorValidateVisitorXmlEscapeNonELVisitor(
            boolean isDeferredSyntaxAllowedAsLiteral) {
        super(isDeferredSyntaxAllowedAsLiteral);
    }

    @Override
    public void visit(ELNodeText n) throws JasperException {
        getOutput().append(ELParser2.escapeLiteralExpression(
                Validator.xmlEscape(n.getText()),
                isDeferredSyntaxAllowedAsLiteral()));
    }
}