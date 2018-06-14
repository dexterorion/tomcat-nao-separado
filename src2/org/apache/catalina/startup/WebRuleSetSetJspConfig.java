package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetSetJspConfig extends Rule {
	private boolean isJspConfigSet = false;

	public WebRuleSetSetJspConfig() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		if (isJspConfigSet) {
			throw new IllegalArgumentException(
					"<jsp-config> element is limited to 1 occurrence");
		}
		isJspConfigSet = true;
	}

	public boolean isJspConfigSet() {
		return isJspConfigSet;
	}

	public void setJspConfigSet(boolean isJspConfigSet) {
		this.isJspConfigSet = isJspConfigSet;
	}

}