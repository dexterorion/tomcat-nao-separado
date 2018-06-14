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
package org.apache.jasper.compiler;

import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

/**
 * An internal data representation of a JSP page or a JSP document (XML). Also
 * included here is a visitor class for traversing nodes.
 * 
 * @author Kin-man Chung
 * @author Jan Luehe
 * @author Shawn Bayern
 * @author Mark Roth
 */

public abstract class Node implements TagConstants {

	private static final VariableInfo[] ZERO_VARIABLE_INFO = {};

	private Attributes attrs;

	// xmlns attributes that represent tag libraries (only in XML syntax)
	private Attributes taglibAttrs;

	/*
	 * xmlns attributes that do not represent tag libraries (only in XML syntax)
	 */
	private Attributes nonTaglibXmlnsAttrs;

	private NodeNodes body;

	private String text;

	private Mark startMark;

	private int beginJavaLine;

	private int endJavaLine;

	private Node parent;

	private NodeNodes namedAttributeNodeNodes; // cached for performance

	private String qName;

	private String localName;

	/*
	 * The name of the inner class to which the codes for this node and its body
	 * are generated. For instance, for <jsp:body> in foo.jsp, this is
	 * "foo_jspHelper". This is primarily used for communicating such info from
	 * Generator to Smap generator.
	 */
	private String innerClassName;

	private boolean isDummy;

	/**
	 * Zero-arg Constructor.
	 */
	public Node() {
		this.isDummy = true;
	}

	/**
	 * Constructor.
	 * 
	 * @param start
	 *            The location of the jsp page
	 * @param parent
	 *            The enclosing node
	 */
	public Node(Mark start, Node parent) {
		this.startMark = start;
		this.isDummy = (start == null);
		addToParent(parent);
	}

	/**
	 * Constructor for NodeNodes parsed from standard syntax.
	 * 
	 * @param qName
	 *            The action's qualified name
	 * @param localName
	 *            The action's local name
	 * @param attrs
	 *            The attributes for this node
	 * @param start
	 *            The location of the jsp page
	 * @param parent
	 *            The enclosing node
	 */
	public Node(String qName, String localName, Attributes attrs, Mark start,
			Node parent) {
		this.qName = qName;
		this.localName = localName;
		this.attrs = attrs;
		this.startMark = start;
		this.isDummy = (start == null);
		addToParent(parent);
	}

	/**
	 * Constructor for NodeNodes parsed from XML syntax.
	 * 
	 * @param qName
	 *            The action's qualified name
	 * @param localName
	 *            The action's local name
	 * @param attrs
	 *            The action's attributes whose name does not start with xmlns
	 * @param nonTaglibXmlnsAttrs
	 *            The action's xmlns attributes that do not represent tag
	 *            libraries
	 * @param taglibAttrs
	 *            The action's xmlns attributes that represent tag libraries
	 * @param start
	 *            The location of the jsp page
	 * @param parent
	 *            The enclosing node
	 */
	public Node(String qName, String localName, Attributes attrs,
			Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs, Mark start,
			Node parent) {
		this.qName = qName;
		this.localName = localName;
		this.attrs = attrs;
		this.nonTaglibXmlnsAttrs = nonTaglibXmlnsAttrs;
		this.taglibAttrs = taglibAttrs;
		this.startMark = start;
		this.isDummy = (start == null);
		addToParent(parent);
	}

	/*
	 * Constructor.
	 * 
	 * @param qName The action's qualified name @param localName The action's
	 * local name @param text The text associated with this node @param start
	 * The location of the jsp page @param parent The enclosing node
	 */
	public Node(String qName, String localName, String text, Mark start,
			Node parent) {
		this.qName = qName;
		this.localName = localName;
		this.text = text;
		this.startMark = start;
		this.isDummy = (start == null);
		addToParent(parent);
	}

	public String getQName() {
		return this.qName;
	}

	public String getLocalName() {
		return this.localName;
	}

	/*
	 * Gets this Node's attributes.
	 * 
	 * In the case of a Node parsed from standard syntax, this method returns
	 * all the Node's attributes.
	 * 
	 * In the case of a Node parsed from XML syntax, this method returns only
	 * those attributes whose name does not start with xmlns.
	 */
	public Attributes getAttributes() {
		return this.attrs;
	}

	/*
	 * Gets this Node's xmlns attributes that represent tag libraries (only
	 * meaningful for NodeNodes parsed from XML syntax)
	 */
	public Attributes getTaglibAttributes() {
		return this.taglibAttrs;
	}

	/*
	 * Gets this Node's xmlns attributes that do not represent tag libraries
	 * (only meaningful for NodeNodes parsed from XML syntax)
	 */
	public Attributes getNonTaglibXmlnsAttributes() {
		return this.nonTaglibXmlnsAttrs;
	}

	public void setAttributes(Attributes attrs) {
		this.attrs = attrs;
	}

	public String getAttributeValue(String name) {
		return (attrs == null) ? null : attrs.getValue(name);
	}

	/**
	 * Get the attribute that is non request time expression, either from the
	 * attribute of the node, or from a jsp:attrbute
	 */
	public String getTextAttribute(String name) {

		String attr = getAttributeValue(name);
		if (attr != null) {
			return attr;
		}

		NodeNamedAttribute namedAttribute = getNodeNamedAttributeNode(name);
		if (namedAttribute == null) {
			return null;
		}

		return namedAttribute.getText();
	}

	/**
	 * Searches all subnodes of this node for jsp:attribute standard actions
	 * with the given name, and returns the NodeNamedAttribute node of the
	 * matching named attribute, nor null if no such node is found.
	 * <p>
	 * This should always be called and only be called for nodes that accept
	 * dynamic runtime attribute expressions.
	 */
	public NodeNamedAttribute getNodeNamedAttributeNode(String name) {
		NodeNamedAttribute result = null;

		// Look for the attribute in NodeNamedAttribute children
		NodeNodes nodes = getNodeNamedAttributeNodeNodes();
		int numChildNodeNodes = nodes.size();
		for (int i = 0; i < numChildNodeNodes; i++) {
			NodeNamedAttribute na = (NodeNamedAttribute) nodes.getNode(i);
			boolean found = false;
			int index = name.indexOf(':');
			if (index != -1) {
				// qualified name
				found = na.getName().equals(name);
			} else {
				found = na.getLocalName().equals(name);
			}
			if (found) {
				result = na;
				break;
			}
		}

		return result;
	}

	/**
	 * Searches all subnodes of this node for jsp:attribute standard actions,
	 * and returns that set of nodes as a Node.NodeNodes object.
	 * 
	 * @return Possibly empty Node.NodeNodes object containing any jsp:attribute
	 *         subnodes of this Node
	 */
	public NodeNodes getNodeNamedAttributeNodeNodes() {

		if (namedAttributeNodeNodes != null) {
			return namedAttributeNodeNodes;
		}

		NodeNodes result = new NodeNodes();

		// Look for the attribute in NodeNamedAttribute children
		NodeNodes nodes = getBody();
		if (nodes != null) {
			int numChildNodeNodes = nodes.size();
			for (int i = 0; i < numChildNodeNodes; i++) {
				Node n = nodes.getNode(i);
				if (n instanceof NodeNamedAttribute) {
					result.add(n);
				} else if (!(n instanceof NodeComment)) {
					// Nothing can come before jsp:attribute, and only
					// jsp:body can come after it.
					break;
				}
			}
		}

		namedAttributeNodeNodes = result;
		return result;
	}

	public NodeNodes getBody() {
		return body;
	}

	public void setBody(NodeNodes body) {
		this.body = body;
	}

	public String getText() {
		return text;
	}

	public Mark getStart() {
		return startMark;
	}

	public Node getParent() {
		return parent;
	}

	public int getBeginJavaLine() {
		return beginJavaLine;
	}

	public void setBeginJavaLine(int begin) {
		beginJavaLine = begin;
	}

	public int getEndJavaLine() {
		return endJavaLine;
	}

	public void setEndJavaLine(int end) {
		endJavaLine = end;
	}

	public boolean isDummy() {
		return isDummy;
	}

	public NodeRoot getRoot() {
		Node n = this;
		while (!(n instanceof NodeRoot)) {
			n = n.getParent();
		}
		return (NodeRoot) n;
	}

	public String getInnerClassName() {
		return innerClassName;
	}

	public void setInnerClassName(String icn) {
		innerClassName = icn;
	}

	/**
	 * Selects and invokes a method in the visitor class based on the node type.
	 * This is abstract and should be overrode by the extending classes.
	 * 
	 * @param v
	 *            The visitor class
	 */
	public abstract void accept(NodeVisitor v) throws JasperException;

	// *********************************************************************
	// Private utility methods

	/*
	 * Adds this Node to the body of the given parent.
	 */
	private void addToParent(Node parent) {
		if (parent != null) {
			this.parent = parent;
			NodeNodes parentBody = parent.getBody();
			if (parentBody == null) {
				parentBody = new NodeNodes();
				parent.setBody(parentBody);
			}
			parentBody.add(this);
		}
	}

	public static VariableInfo[] getZeroVariableInfo() {
		return ZERO_VARIABLE_INFO;
	}

	public Attributes getAttrs() {
		return attrs;
	}

	public void setAttrs(Attributes attrs) {
		this.attrs = attrs;
	}

	public Attributes getTaglibAttrs() {
		return taglibAttrs;
	}

	public void setTaglibAttrs(Attributes taglibAttrs) {
		this.taglibAttrs = taglibAttrs;
	}

	public Attributes getNonTaglibXmlnsAttrs() {
		return nonTaglibXmlnsAttrs;
	}

	public void setNonTaglibXmlnsAttrs(Attributes nonTaglibXmlnsAttrs) {
		this.nonTaglibXmlnsAttrs = nonTaglibXmlnsAttrs;
	}

	public Mark getStartMark() {
		return startMark;
	}

	public void setStartMark(Mark startMark) {
		this.startMark = startMark;
	}

	public NodeNodes getNamedAttributeNodeNodes() {
		return namedAttributeNodeNodes;
	}

	public void setNamedAttributeNodeNodes(NodeNodes namedAttributeNodeNodes) {
		this.namedAttributeNodeNodes = namedAttributeNodeNodes;
	}

	public String getqName() {
		return qName;
	}

	public void setqName(String qName) {
		this.qName = qName;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public void setDummy(boolean isDummy) {
		this.isDummy = isDummy;
	}

}