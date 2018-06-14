package org.apache.catalina.core;

import java.util.concurrent.Callable;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;

public class ContainerBaseStartChild implements Callable<Void> {

	private Container child;

	public ContainerBaseStartChild(Container child) {
		this.child = child;
	}

	@Override
	public Void call() throws LifecycleException {
		child.start();
		return null;
	}
}