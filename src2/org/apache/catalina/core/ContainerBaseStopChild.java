package org.apache.catalina.core;

import java.util.concurrent.Callable;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;

public class ContainerBaseStopChild implements Callable<Void> {

	private Container child;

	public ContainerBaseStopChild(Container child) {
		this.child = child;
	}

	@Override
	public Void call() throws LifecycleException {
		if (child.getState().isAvailable()) {
			child.stop();
		}
		return null;
	}
}