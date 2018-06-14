package org.apache.catalina.ssi;

public final class ExpressionParseTreeOrNode extends
		ExpressionParseTreeOppNode {
	@Override
	public boolean evaluate() {
		if (getLeft().evaluate()) // Short circuit
			return true;
		return getRight().evaluate();
	}

	@Override
	public int getPrecedence() {
		return ExpressionParseTree.getPrecedenceLogical();
	}

	@Override
	public String toString() {
		return getLeft() + " " + getRight() + " OR";
	}

	public ExpressionParseTreeOrNode() {

	}
}