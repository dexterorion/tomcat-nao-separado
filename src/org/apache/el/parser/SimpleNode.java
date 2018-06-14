/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Generated By:JJTree: Do not edit this line. SimpleNode.java */

package org.apache.el.parser;

import java.util.Arrays;

import javax.el.ELException;
import javax.el.MethodInfo;
import javax.el.PropertyNotWritableException;
import javax.el.ValueReference;

import org.apache.el.lang.ELSupport;
import org.apache.el.lang.EvaluationContext;
import org.apache.el.util.MessageFactory;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 */
public abstract class SimpleNode extends ELSupport implements Node {
	private Node parent;

	private Node[] children;

	private int id;

	private String image;

	public SimpleNode(int i) {
		setIdData(i);
	}

	@Override
	public void jjtOpen() {
		// NOOP by default
	}

	@Override
	public void jjtClose() {
		// NOOP by default
	}

	@Override
	public void jjtSetParent(Node n) {
		setParentData(n);
	}

	@Override
	public Node jjtGetParent() {
		return getParentData();
	}

	@Override
	public void jjtAddChild(Node n, int i) {
		if (getChildrenData() == null) {
			setChildrenData(new Node[i + 1]);
		} else if (i >= getChildrenData().length) {
			Node c[] = new Node[i + 1];
			System.arraycopy(getChildrenData(), 0, c, 0, getChildrenData().length);
			setChildrenData(c);
		}
		getChildrenData()[i] = n;
	}

	@Override
	public Node jjtGetChild(int i) {
		return getChildrenData()[i];
	}

	@Override
	public int jjtGetNumChildren() {
		return (getChildrenData() == null) ? 0 : getChildrenData().length;
	}

	/*
	 * You can override these two methods in subclasses of SimpleNode to
	 * customize the way the node appears when the tree is dumped. If your
	 * output uses more than one line you should override toString(String),
	 * otherwise overriding toString() is probably all you need to do.
	 */

	@Override
	public String toString() {
		if (this.getImageData() != null) {
			return ELParserTreeConstants.jjtNodeName[getIdData()] + "[" + this.getImageData()
					+ "]";
		}
		return ELParserTreeConstants.jjtNodeName[getIdData()];
	}

	public String toString(String prefix) {
		return prefix + toString();
	}

	@Override
	public String getImage() {
		return getImageData();
	}

	public void setImage(String image) {
		this.setImageData(image);
	}

	@Override
	public Class<?> getType(EvaluationContext ctx) throws ELException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getValue(EvaluationContext ctx) throws ELException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReadOnly(EvaluationContext ctx) throws ELException {
		return true;
	}

	@Override
	public void setValue(EvaluationContext ctx, Object value)
			throws ELException {
		throw new PropertyNotWritableException(
				MessageFactory.get("error.syntax.set"));
	}

	@Override
	public void accept(NodeVisitor visitor) throws Exception {
		visitor.visit(this);
		if (this.getChildrenData() != null && this.getChildrenData().length > 0) {
			for (int i = 0; i < this.getChildrenData().length; i++) {
				this.getChildrenData()[i].accept(visitor);
			}
		}
	}

	@Override
	public Object invoke(EvaluationContext ctx, Class<?>[] paramTypes,
			Object[] paramValues) throws ELException {
		throw new UnsupportedOperationException();
	}

	@Override
	public MethodInfo getMethodInfo(EvaluationContext ctx, Class<?>[] paramTypes)
			throws ELException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(getChildrenData());
		result = prime * result + getIdData();
		result = prime * result + ((getImageData() == null) ? 0 : getImageData().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SimpleNode)) {
			return false;
		}
		SimpleNode other = (SimpleNode) obj;
		if (!Arrays.equals(getChildrenData(), other.getChildrenData())) {
			return false;
		}
		if (getIdData() != other.getIdData()) {
			return false;
		}
		if (getImageData() == null) {
			if (other.getImageData() != null) {
				return false;
			}
		} else if (!getImageData().equals(other.getImageData())) {
			return false;
		}
		return true;
	}

	/**
	 * @since EL 2.2
	 */
	@Override
	public ValueReference getValueReference(EvaluationContext ctx) {
		return null;
	}

	/**
	 * @since EL 2.2
	 */
	@Override
	public boolean isParametersProvided() {
		return false;
	}

	public Node getParent() {
		return getParentData();
	}

	public void setParent(Node parent) {
		this.setParentData(parent);
	}

	public Node[] getChildren() {
		return getChildrenData();
	}

	public void setChildren(Node[] children) {
		this.setChildrenData(children);
	}

	public int getId() {
		return getIdData();
	}

	public void setId(int id) {
		this.setIdData(id);
	}

	public Node getParentData() {
		return parent;
	}

	public void setParentData(Node parent) {
		this.parent = parent;
	}

	public Node[] getChildrenData() {
		return children;
	}

	public void setChildrenData(Node[] children) {
		this.children = children;
	}

	public int getIdData() {
		return id;
	}

	public void setIdData(int id) {
		this.id = id;
	}

	public String getImageData() {
		return image;
	}

	public void setImageData(String image) {
		this.image = image;
	}
	
	
}
