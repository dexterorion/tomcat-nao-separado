package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetRelativeOrderingRule extends Rule {

	private boolean isRelativeOrderingSet = false;
	private final boolean fragment;

	public WebRuleSetRelativeOrderingRule(boolean fragment) {
		this.fragment = fragment;
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		if (!fragment) {
			getDigester().getLogger()
					.warn(WebRuleSet.getSm().getString(
							"webRuleSet.relativeOrdering"));
		}
		if (isRelativeOrderingSet) {
			throw new IllegalArgumentException(WebRuleSet.getSm().getString(
					"webRuleSet.relativeOrderingCount"));
		} else {
			isRelativeOrderingSet = true;
		}
	}

	public boolean isRelativeOrderingSet() {
		return isRelativeOrderingSet;
	}

	public void setRelativeOrderingSet(boolean isRelativeOrderingSet) {
		this.isRelativeOrderingSet = isRelativeOrderingSet;
	}

	public boolean isFragment() {
		return fragment;
	}

}