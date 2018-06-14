package org.apache.catalina.ssi;

/**
 * A node in the expression parse tree.
 */
public abstract class ExpressionParseTreeNode {
	/**
	 * Return true if the node evaluates to true.
	 */
	public abstract boolean evaluate();
}