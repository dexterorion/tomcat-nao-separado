package org.apache.catalina.startup;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

public class TomcatDefaultWebXmlListener implements LifecycleListener {
	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
			Tomcat.initWebappDefaults((Context) event.getLifecycle());
		}
	}
}