/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.ssi;

import java.text.ParseException;
import java.util.LinkedList;

/**
 * Represents a parsed expression.
 * 
 * @author Paul Speed
 */
public class ExpressionParseTree {
	/**
	 * Contains the current set of completed nodes. This is a workspace for the
	 * parser.
	 */
	private LinkedList<ExpressionParseTreeNode> nodeStack = new LinkedList<ExpressionParseTreeNode>();
	/**
	 * Contains operator nodes that don't yet have values. This is a workspace
	 * for the parser.
	 */
	private LinkedList<ExpressionParseTreeOppNode> oppStack = new LinkedList<ExpressionParseTreeOppNode>();
	/**
	 * The root node after the expression has been parsed.
	 */
	private ExpressionParseTreeNode root;
	/**
	 * The SSIMediator to use when evaluating the expressions.
	 */
	private SSIMediator ssiMediator;

	/**
	 * Creates a new parse tree for the specified expression.
	 */
	public ExpressionParseTree(String expr, SSIMediator ssiMediator)
			throws ParseException {
		this.setSsiMediator(ssiMediator);
		parseExpression(expr);
	}

	/**
	 * Evaluates the tree and returns true or false. The specified SSIMediator
	 * is used to resolve variable references.
	 */
	public boolean evaluateTree() {
		return root.evaluate();
	}

	/**
	 * Pushes a new operator onto the opp stack, resolving existing opps as
	 * needed.
	 */
	private void pushOpp(ExpressionParseTreeOppNode node) {
		// If node is null then it's just a group marker
		if (node == null) {
			oppStack.add(0, node);
			return;
		}
		while (true) {
			if (oppStack.size() == 0)
				break;
			ExpressionParseTreeOppNode top = oppStack.get(0);
			// If the top is a spacer then don't pop
			// anything
			if (top == null)
				break;
			// If the top node has a lower precedence then
			// let it stay
			if (top.getPrecedence() < node.getPrecedence())
				break;
			// Remove the top node
			oppStack.remove(0);
			// Let it fill its branches
			top.popValues(nodeStack);
			// Stick it on the resolved node stack
			nodeStack.add(0, top);
		}
		// Add the new node to the opp stack
		oppStack.add(0, node);
	}

	/**
	 * Resolves all pending opp nodes on the stack until the next group marker
	 * is reached.
	 */
	private void resolveGroup() {
		ExpressionParseTreeOppNode top = null;
		while ((top = oppStack.remove(0)) != null) {
			// Let it fill its branches
			top.popValues(nodeStack);
			// Stick it on the resolved node stack
			nodeStack.add(0, top);
		}
	}

	/**
	 * Parses the specified expression into a tree of parse nodes.
	 */
	private void parseExpression(String expr) throws ParseException {
		ExpressionParseTreeStringNode currStringNode = null;
		// We cheat a little and start an artificial
		// group right away. It makes finishing easier.
		pushOpp(null);
		ExpressionTokenizer et = new ExpressionTokenizer(expr);
		while (et.hasMoreTokens()) {
			int token = et.nextToken();
			if (token != ExpressionTokenizer.getTokenString())
				currStringNode = null;
			switch (token) {
			case 0:
				if (currStringNode == null) {
					currStringNode = new ExpressionParseTreeStringNode(this, et.getTokenValue());
					nodeStack.add(0, currStringNode);
				} else {
					// Add to the existing
					currStringNode.getValueVariable().append(" ");
					currStringNode.getValueVariable().append(et.getTokenValue());
				}
				break;
			case 1:
				pushOpp(new ExpressionParseTreeAndNode());
				break;
			case 2:
				pushOpp(new ExpressionParseTreeOrNode());
				break;
			case 3:
				pushOpp(new ExpressionParseTreeNotNode());
				break;
			case 4:
				pushOpp(new ExpressionParseTreeEqualNode());
				break;
			case 5:
				pushOpp(new ExpressionParseTreeNotNode());
				// Sneak the regular node in. The NOT will
				// be resolved when the next opp comes along.
				oppStack.add(0, new ExpressionParseTreeEqualNode());
				break;
			case 6:
				// Closeout the current group
				resolveGroup();
				break;
			case 7:
				// Push a group marker
				pushOpp(null);
				break;
			case 8:
				pushOpp(new ExpressionParseTreeNotNode());
				// Similar strategy to NOT_EQ above, except this
				// is NOT less than
				oppStack.add(0, new ExpressionParseTreeLessThanNode());
				break;
			case 9:
				pushOpp(new ExpressionParseTreeNotNode());
				// Similar strategy to NOT_EQ above, except this
				// is NOT greater than
				oppStack.add(0, new ExpressionParseTreeGreaterThanNode());
				break;
			case 10:
				pushOpp(new ExpressionParseTreeGreaterThanNode());
				break;
			case 11:
				pushOpp(new ExpressionParseTreeLessThanNode());
				break;
			case 12:
				break;
			}
		}
		// Finish off the rest of the opps
		resolveGroup();
		if (nodeStack.size() == 0) {
			throw new ParseException("No nodes created.", et.getIndex());
		}
		if (nodeStack.size() > 1) {
			throw new ParseException("Extra nodes created.", et.getIndex());
		}
		if (oppStack.size() != 0) {
			throw new ParseException("Unused opp nodes exist.", et.getIndex());
		}
		root = nodeStack.get(0);
	}

	public static int getPrecedenceNot() {
		return PRECEDENCE_NOT;
	}

	public static int getPrecedenceCompare() {
		return PRECEDENCE_COMPARE;
	}

	public static int getPrecedenceLogical() {
		return PRECEDENCE_LOGICAL;
	}

	public SSIMediator getSsiMediator() {
		return ssiMediator;
	}

	public void setSsiMediator(SSIMediator ssiMediator) {
		this.ssiMediator = ssiMediator;
	}

	private static final int PRECEDENCE_NOT = 5;
	private static final int PRECEDENCE_COMPARE = 4;
	private static final int PRECEDENCE_LOGICAL = 1;
}