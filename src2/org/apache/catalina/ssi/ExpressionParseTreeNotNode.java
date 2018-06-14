package org.apache.catalina.ssi;

import java.util.List;

public final class ExpressionParseTreeNotNode extends ExpressionParseTreeOppNode {
	@Override
	public boolean evaluate() {
		return !getLeft().evaluate();
	}

	@Override
	public int getPrecedence() {
		return ExpressionParseTree.getPrecedenceNot();
	}

	/**
	 * Overridden to pop only one value.
	 */
	@Override
	public void popValues(List<ExpressionParseTreeNode> values) {
		setLeft(values.remove(0));
	}

	@Override
	public String toString() {
		return getLeft() + " NOT";
	}

	public ExpressionParseTreeNotNode() {

	}
}