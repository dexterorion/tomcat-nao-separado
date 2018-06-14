package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetSetDistributableRule extends Rule {

	public WebRuleSetSetDistributableRule() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		WebXml webXml = (WebXml) getDigester().peek();
		webXml.setDistributable(true);
		if (getDigester().getLogger().isDebugEnabled()) {
			getDigester().getLogger().debug(
					webXml.getClass().getName() + ".setDistributable(true)");
		}
	}

}