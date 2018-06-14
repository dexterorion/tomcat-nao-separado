package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ELNodeTextBuilder extends ELNodeVisitor {

	private final boolean isDeferredSyntaxAllowedAsLiteral;
	private final StringBuilder output = new StringBuilder();

	public ELNodeTextBuilder(boolean isDeferredSyntaxAllowedAsLiteral) {
		this.isDeferredSyntaxAllowedAsLiteral = isDeferredSyntaxAllowedAsLiteral;
	}

	public String getText() {
		return output.toString();
	}

	@Override
	public void visit(ELNodeRoot n) throws JasperException {
		output.append(n.getType());
		output.append('{');
		n.getExpression().visit(this);
		output.append('}');
	}

	@Override
	public void visit(ELNodeFunction n) throws JasperException {
		output.append(ELParser2.escapeLiteralExpression(n.getOriginalText(),
				isDeferredSyntaxAllowedAsLiteral));
		output.append('(');
	}

	@Override
	public void visit(ELNodeText n) throws JasperException {
		output.append(ELParser2.escapeLiteralExpression(n.getText(),
				isDeferredSyntaxAllowedAsLiteral));
	}

	@Override
	public void visit(ELNodeELText n) throws JasperException {
		output.append(ELParser2.escapeELText(n.getText()));
	}

	public boolean isDeferredSyntaxAllowedAsLiteral() {
		return isDeferredSyntaxAllowedAsLiteral;
	}

	public StringBuilder getOutput() {
		return output;
	}

}