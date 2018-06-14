package org.apache.catalina.ssi;

public final class ExpressionParseTreeAndNode extends
		ExpressionParseTreeOppNode {
	@Override
	public boolean evaluate() {
		if (!getLeft().evaluate()) // Short circuit
			return false;
		return getRight().evaluate();
	}

	@Override
	public int getPrecedence() {
		return ExpressionParseTree.getPrecedenceLogical();
	}

	@Override
	public String toString() {
		return getLeft() + " " + getRight() + " AND";
	}

	public ExpressionParseTreeAndNode() {

	}
}