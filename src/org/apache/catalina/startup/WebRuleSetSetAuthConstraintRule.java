package org.apache.catalina.startup;

import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetSetAuthConstraintRule extends Rule {

	public WebRuleSetSetAuthConstraintRule() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		SecurityConstraint securityConstraint = (SecurityConstraint) getDigester()
				.peek();
		securityConstraint.setAuthConstraint(true);
		if (getDigester().getLogger().isDebugEnabled()) {
			getDigester().getLogger().debug(
					"Calling SecurityConstraint.setAuthConstraint(true)");
		}
	}

}