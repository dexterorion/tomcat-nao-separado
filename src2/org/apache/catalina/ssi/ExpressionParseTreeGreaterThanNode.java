package org.apache.catalina.ssi;

public final class ExpressionParseTreeGreaterThanNode extends ExpressionParseTreeCompareNode {
	@Override
	public boolean evaluate() {
		return (compareBranches() > 0);
	}

	@Override
	public int getPrecedence() {
		return ExpressionParseTree.getPrecedenceCompare();
	}

	@Override
	public String toString() {
		return getLeft() + " " + getRight() + " GT";
	}
	
	public ExpressionParseTreeGreaterThanNode(){
		
	}
}