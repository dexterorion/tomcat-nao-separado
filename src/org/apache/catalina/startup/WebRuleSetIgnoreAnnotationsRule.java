package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetIgnoreAnnotationsRule extends Rule {

	public WebRuleSetIgnoreAnnotationsRule() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		WebXml webxml = (WebXml) getDigester().peek(
				getDigester().getCount() - 1);
		String value = attributes.getValue("metadata-complete");
		if ("true".equals(value)) {
			webxml.setMetadataComplete(true);
		} else if ("false".equals(value)) {
			webxml.setMetadataComplete(false);
		}
		if (getDigester().getLogger().isDebugEnabled()) {
			getDigester().getLogger().debug(
					webxml.getClass().getName() + ".setMetadataComplete( "
							+ webxml.isMetadataComplete() + ")");
		}
	}

}