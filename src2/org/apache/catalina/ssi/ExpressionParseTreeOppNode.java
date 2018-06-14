package org.apache.catalina.ssi;

import java.util.List;

/**
 * A node implementation that represents an operation.
 */
public abstract class ExpressionParseTreeOppNode extends ExpressionParseTreeNode {
	/**
	 * The left branch.
	 */
	private ExpressionParseTreeNode left;
	/**
	 * The right branch.
	 */
	private ExpressionParseTreeNode right;

	/**
	 * Returns a preference level suitable for comparison to other
	 * ExpressionParseTreeOppNode preference levels.
	 */
	public abstract int getPrecedence();

	/**
	 * Lets the node pop its own branch nodes off the front of the specified
	 * list. The default pulls two.
	 */
	public void popValues(List<ExpressionParseTreeNode> values) {
		setRight(values.remove(0));
		setLeft(values.remove(0));
	}
	
	public ExpressionParseTreeOppNode(){
		
	}

	public ExpressionParseTreeNode getLeft() {
		return left;
	}

	public void setLeft(ExpressionParseTreeNode left) {
		this.left = left;
	}

	public ExpressionParseTreeNode getRight() {
		return right;
	}

	public void setRight(ExpressionParseTreeNode right) {
		this.right = right;
	}
}