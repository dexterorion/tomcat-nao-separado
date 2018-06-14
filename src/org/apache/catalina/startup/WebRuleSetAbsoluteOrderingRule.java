package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetAbsoluteOrderingRule extends Rule {

	private boolean isAbsoluteOrderingSet = false;
	private final boolean fragment;

	public WebRuleSetAbsoluteOrderingRule(boolean fragment) {
		this.fragment = fragment;
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		if (fragment) {
			getDigester().getLogger()
					.warn(WebRuleSet.getSm().getString(
							"webRuleSet.absoluteOrdering"));
		}
		if (isAbsoluteOrderingSet) {
			throw new IllegalArgumentException(WebRuleSet.getSm().getString(
					"webRuleSet.absoluteOrderingCount"));
		} else {
			isAbsoluteOrderingSet = true;
			WebXml webXml = (WebXml) getDigester().peek();
			webXml.createAbsoluteOrdering();
			if (getDigester().getLogger().isDebugEnabled()) {
				getDigester().getLogger().debug(
						webXml.getClass().getName() + ".setAbsoluteOrdering()");
			}
		}
	}

	public boolean isAbsoluteOrderingSet() {
		return isAbsoluteOrderingSet;
	}

	public void setAbsoluteOrderingSet(boolean isAbsoluteOrderingSet) {
		this.isAbsoluteOrderingSet = isAbsoluteOrderingSet;
	}

	public boolean isFragment() {
		return fragment;
	}

}