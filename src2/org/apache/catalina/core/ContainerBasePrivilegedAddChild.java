package org.apache.catalina.core;

import java.security.PrivilegedAction;

import org.apache.catalina.Container;

/**
 * Perform addChild with the permissions of this class. addChild can be
 * called with the XML parser on the stack, this allows the XML parser to
 * have fewer privileges than Tomcat.
 */
public class ContainerBasePrivilegedAddChild implements PrivilegedAction<Void> {

	/**
	 * 
	 */
	private final ContainerBase containerBase;
	private Container child;

	public ContainerBasePrivilegedAddChild(ContainerBase containerBase, Container child) {
		this.containerBase = containerBase;
		this.child = child;
	}

	@Override
	public Void run() {
		this.containerBase.addChildInternal(child);
		return null;
	}

}