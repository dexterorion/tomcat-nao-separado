package org.apache.juli;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public final class ClassLoaderLogManagerLogNode {
	private Logger logger;

	private final Map<String, ClassLoaderLogManagerLogNode> children = new HashMap<String, ClassLoaderLogManagerLogNode>();

	private final ClassLoaderLogManagerLogNode parent;

	public ClassLoaderLogManagerLogNode(
			final ClassLoaderLogManagerLogNode parent, final Logger logger) {
		this.parent = parent;
		this.setLoggerData(logger);
	}

	public ClassLoaderLogManagerLogNode(
			final ClassLoaderLogManagerLogNode parent) {
		this(parent, null);
	}

	public ClassLoaderLogManagerLogNode findNode(String name) {
		ClassLoaderLogManagerLogNode currentNode = this;
		if (getLoggerData().getName().equals(name)) {
			return this;
		}
		while (name != null) {
			final int dotIndex = name.indexOf('.');
			final String nextName;
			if (dotIndex < 0) {
				nextName = name;
				name = null;
			} else {
				nextName = name.substring(0, dotIndex);
				name = name.substring(dotIndex + 1);
			}
			ClassLoaderLogManagerLogNode childNode = currentNode.getChildrenData()
					.get(nextName);
			if (childNode == null) {
				childNode = new ClassLoaderLogManagerLogNode(currentNode);
				currentNode.getChildrenData().put(nextName, childNode);
			}
			currentNode = childNode;
		}
		return currentNode;
	}

	public Logger findParentLogger() {
		Logger logger = null;
		ClassLoaderLogManagerLogNode node = getParentData();
		while (node != null && logger == null) {
			logger = node.getLoggerData();
			node = node.getParentData();
		}
		return logger;
	}

	public void setParentLogger(final Logger parent) {
		for (final Iterator<ClassLoaderLogManagerLogNode> iter = getChildrenData()
				.values().iterator(); iter.hasNext();) {
			final ClassLoaderLogManagerLogNode childNode = iter.next();
			if (childNode.getLoggerData() == null) {
				childNode.setParentLogger(parent);
			} else {
				ClassLoaderLogManager.doSetParentLogger(childNode.getLoggerData(),
						parent);
			}
		}
	}

	public Logger getLogger() {
		return getLoggerData();
	}

	public void setLogger(Logger logger) {
		this.setLoggerData(logger);
	}

	public Map<String, ClassLoaderLogManagerLogNode> getChildren() {
		return getChildrenData();
	}

	public ClassLoaderLogManagerLogNode getParent() {
		return getParentData();
	}

	public Logger getLoggerData() {
		return logger;
	}

	public void setLoggerData(Logger logger) {
		this.logger = logger;
	}

	public Map<String, ClassLoaderLogManagerLogNode> getChildrenData() {
		return children;
	}

	public ClassLoaderLogManagerLogNode getParentData() {
		return parent;
	}

}