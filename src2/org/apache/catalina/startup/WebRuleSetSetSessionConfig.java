package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetSetSessionConfig extends Rule {
	private boolean isSessionConfigSet = false;

	public WebRuleSetSetSessionConfig() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		if (isSessionConfigSet) {
			throw new IllegalArgumentException(
					"<session-config> element is limited to 1 occurrence");
		}
		isSessionConfigSet = true;
	}

	public boolean isSessionConfigSet() {
		return isSessionConfigSet;
	}

	public void setSessionConfigSet(boolean isSessionConfigSet) {
		this.isSessionConfigSet = isSessionConfigSet;
	}

}