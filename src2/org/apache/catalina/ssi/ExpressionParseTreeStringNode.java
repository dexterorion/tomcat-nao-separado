package org.apache.catalina.ssi;

/**
 * A node the represents a String value
 */
public class ExpressionParseTreeStringNode extends ExpressionParseTreeNode {
	/**
	 * 
	 */
	private final ExpressionParseTree expressionParseTree;
	private StringBuilder value;
	private String resolved = null;

	public ExpressionParseTreeStringNode(
			ExpressionParseTree expressionParseTree, String value) {
		this.expressionParseTree = expressionParseTree;
		this.value = new StringBuilder(value);
	}

	/**
	 * Resolves any variable references and returns the value string.
	 */
	public String getValue() {
		if (resolved == null)
			resolved = this.expressionParseTree.getSsiMediator()
					.substituteVariables(value.toString());
		return resolved;
	}

	/**
	 * Returns true if the string is not empty.
	 */
	@Override
	public boolean evaluate() {
		return !(getValue().length() == 0);
	}

	@Override
	public String toString() {
		return value.toString();
	}

	public String getResolved() {
		return resolved;
	}

	public void setResolved(String resolved) {
		this.resolved = resolved;
	}

	public ExpressionParseTree getExpressionParseTree() {
		return expressionParseTree;
	}

	public void setValue(StringBuilder value) {
		this.value = value;
	}
	
	public StringBuilder getValueVariable(){
		return value;
	}

}