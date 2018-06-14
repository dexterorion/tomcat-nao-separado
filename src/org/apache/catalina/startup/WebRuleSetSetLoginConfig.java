package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

/**
 * Rule to check that the <code>login-config</code> is occurring only 1 time
 * within the web.xml
 */
public final class WebRuleSetSetLoginConfig extends Rule {
	private boolean isLoginConfigSet = false;

	public WebRuleSetSetLoginConfig() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		if (isLoginConfigSet) {
			throw new IllegalArgumentException(
					"<login-config> element is limited to 1 occurrence");
		}
		isLoginConfigSet = true;
	}

	public boolean isLoginConfigSet() {
		return isLoginConfigSet;
	}

	public void setLoginConfigSet(boolean isLoginConfigSet) {
		this.isLoginConfigSet = isLoginConfigSet;
	}

}