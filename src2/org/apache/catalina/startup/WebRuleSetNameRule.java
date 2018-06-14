package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetNameRule extends Rule {

	private boolean isNameSet = false;

	public WebRuleSetNameRule() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		if (isNameSet) {
			throw new IllegalArgumentException(WebRuleSet.getSm().getString(
					"webRuleSet.nameCount"));
		}
		isNameSet = true;
	}

	@Override
	public void body(String namespace, String name, String text)
			throws Exception {
		super.body(namespace, name, text);
		((WebXml) getDigester().peek()).setName(text);
	}

	public boolean isNameSet() {
		return isNameSet;
	}

	public void setNameSet(boolean isNameSet) {
		this.isNameSet = isNameSet;
	}

}