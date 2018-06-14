package org.apache.catalina.startup;

import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetSetOverrideRule extends Rule {

	public WebRuleSetSetOverrideRule() {
		// no-op
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		ContextEnvironment envEntry = (ContextEnvironment) getDigester().peek();
		envEntry.setOverride(false);
		if (getDigester().getLogger().isDebugEnabled()) {
			getDigester().getLogger().debug(
					envEntry.getClass().getName() + ".setOverride(false)");
		}
	}
}