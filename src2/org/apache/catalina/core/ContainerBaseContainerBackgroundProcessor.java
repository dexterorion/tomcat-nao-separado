package org.apache.catalina.core;

import org.apache.catalina.Container;
import org.apache.tomcat.util.ExceptionUtils2;

/**
 * Private thread class to invoke the backgroundProcess method of this
 * container and its children after a fixed delay.
 */
public class ContainerBaseContainerBackgroundProcessor implements Runnable {

	/**
	 * 
	 */
	private final ContainerBase containerBase;

	/**
	 * @param containerBase
	 */
	public ContainerBaseContainerBackgroundProcessor(ContainerBase containerBase) {
		this.containerBase = containerBase;
	}

	@Override
	public void run() {
		Throwable t = null;
		String unexpectedDeathMessage = ContainerBase.sm.getString(
				"containerBase.backgroundProcess.unexpectedThreadDeath",
				Thread.currentThread().getName());
		try {
			while (!this.getContainerBaseData().isThreadDoneData()) {
				try {
					Thread.sleep(this.getContainerBaseData().getBackgroundProcessorDelay() * 1000L);
				} catch (InterruptedException e) {
					// Ignore
				}
				if (!this.getContainerBaseData().isThreadDoneData()) {
					Container parent = (Container) this.getContainerBaseData().getMappingObject();
					ClassLoader cl = Thread.currentThread()
							.getContextClassLoader();
					if (parent.getLoader() != null) {
						cl = parent.getLoader().getClassLoader();
					}
					processChildren(parent, cl);
				}
			}
		} catch (RuntimeException e) {
			t = e;
			throw e;
		} catch (Error e) {
			t = e;
			throw e;
		} finally {
			if (!this.getContainerBaseData().isThreadDoneData()) {
				ContainerBase.getLog().error(unexpectedDeathMessage, t);
			}
		}
	}

	protected void processChildren(Container container, ClassLoader cl) {
		try {
			if (container.getLoader() != null) {
				Thread.currentThread().setContextClassLoader(
						container.getLoader().getClassLoader());
			}
			container.backgroundProcess();
		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			ContainerBase.getLog().error("Exception invoking periodic operation: ", t);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
		Container[] children = container.findChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getBackgroundProcessorDelay() <= 0) {
				processChildren(children[i], cl);
			}
		}
	}

	public ContainerBase getContainerBaseData() {
		return containerBase;
	}
}