package org.apache.catalina.ssi;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class ExpressionParseTreeCompareNode extends
		ExpressionParseTreeOppNode {
	/**
	 * 
	 */
	private ExpressionParseTree expressionParseTree;

	protected int compareBranches() {
		String val1 = ((ExpressionParseTreeStringNode) getLeft()).getValue();
		String val2 = ((ExpressionParseTreeStringNode) getRight()).getValue();

		int val2Len = val2.length();
		if (val2Len > 1 && val2.charAt(0) == '/'
				&& val2.charAt(val2Len - 1) == '/') {
			// Treat as a regular expression
			String expr = val2.substring(1, val2Len - 1);
			try {
				Pattern pattern = Pattern.compile(expr);
				// Regular expressions will only ever be used with ExpressionParseTreeEqualNode
				// so return zero for equal and non-zero for not equal
				if (pattern.matcher(val1).find()) {
					return 0;
				} else {
					return -1;
				}
			} catch (PatternSyntaxException pse) {
				this.expressionParseTree.getSsiMediator().log("Invalid expression: " + expr, pse);
				return 0;
			}
		}
		return val1.compareTo(val2);
	}

	public ExpressionParseTreeCompareNode(ExpressionParseTree expressionParseTree) {
		this.expressionParseTree = expressionParseTree;

	}
	
	public ExpressionParseTreeCompareNode(){
		
	}
}