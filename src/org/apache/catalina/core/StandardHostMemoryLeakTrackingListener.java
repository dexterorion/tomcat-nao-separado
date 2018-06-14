package org.apache.catalina.core;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

/**
 * Used to ensure the regardless of {@link Context} implementation, a record
 * is kept of the class loader used every time a context starts.
 */
public class StandardHostMemoryLeakTrackingListener implements LifecycleListener {
	/**
	 * 
	 */
	private final StandardHost standardHost;

	/**
	 * @param standardHost
	 */
	public StandardHostMemoryLeakTrackingListener(StandardHost standardHost) {
		this.standardHost = standardHost;
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (event.getType().equals(Lifecycle.AFTER_START_EVENT)) {
			if (event.getSource() instanceof Context) {
				Context context = ((Context) event.getSource());
				this.standardHost.getChildClassLoaders().put(context.getLoader().getClassLoader(),
						context.getServletContext().getContextPath());
			}
		}
	}
}