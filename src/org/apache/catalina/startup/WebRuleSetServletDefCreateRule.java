package org.apache.catalina.startup;

import org.apache.catalina.deploy.ServletDef;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetServletDefCreateRule extends Rule {

	public WebRuleSetServletDefCreateRule() {
		// NO-OP
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		ServletDef servletDef = new ServletDef();
		getDigester().push(servletDef);
		if (getDigester().getLogger().isDebugEnabled())
			getDigester().getLogger().debug(
					"new " + servletDef.getClass().getName());
	}

	@Override
	public void end(String namespace, String name) throws Exception {
		ServletDef servletDef = (ServletDef) getDigester().pop();
		if (getDigester().getLogger().isDebugEnabled())
			getDigester().getLogger().debug(
					"pop " + servletDef.getClass().getName());
	}

}