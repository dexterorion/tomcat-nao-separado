package org.apache.catalina.ssi;

public final class ExpressionParseTreeEqualNode extends ExpressionParseTreeCompareNode {
	@Override
	public boolean evaluate() {
		return (compareBranches() == 0);
	}

	@Override
	public int getPrecedence() {
		return ExpressionParseTree.getPrecedenceCompare();
	}

	@Override
	public String toString() {
		return getLeft() + " " + getRight() + " EQ";
	}
	
	public ExpressionParseTreeEqualNode(){
	}
}