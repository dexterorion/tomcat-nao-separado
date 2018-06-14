package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetVersionRule extends Rule {

	public WebRuleSetVersionRule() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		WebXml webxml = (WebXml) getDigester().peek(
				getDigester().getCount() - 1);
		webxml.setVersion(attributes.getValue("version"));

		if (getDigester().getLogger().isDebugEnabled()) {
			getDigester().getLogger().debug(
					webxml.getClass().getName() + ".setVersion( "
							+ webxml.getVersion() + ")");
		}
	}

}