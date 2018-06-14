package org.apache.catalina.ssi;

public final class ExpressionParseTreeLessThanNode extends ExpressionParseTreeCompareNode {
	@Override
	public boolean evaluate() {
		return (compareBranches() < 0);
	}

	@Override
	public int getPrecedence() {
		return ExpressionParseTree.getPrecedenceCompare();
	}

	@Override
	public String toString() {
		return getLeft() + " " + getRight() + " LT";
	}
	
	public ExpressionParseTreeLessThanNode(){
		
	}
}