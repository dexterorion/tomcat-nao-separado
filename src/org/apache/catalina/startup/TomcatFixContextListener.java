package org.apache.catalina.startup;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.catalina.deploy.LoginConfig;

/**
 * Fix startup sequence - required if you don't use web.xml.
 * 
 * The start() method in context will set 'configured' to false - and
 * expects a listener to set it back to true.
 */
public class TomcatFixContextListener implements LifecycleListener {

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		try {
			Context context = (Context) event.getLifecycle();
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				context.setConfigured(true);
			}
			// LoginConfig is required to process @ServletSecurity
			// annotations
			if (context.getLoginConfig() == null) {
				context.setLoginConfig(new LoginConfig("NONE", null, null,
						null));
				context.getPipeline().addValve(new NonLoginAuthenticator());
			}
		} catch (ClassCastException e) {
			return;
		}
	}

}