package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetTaglibLocationRule extends Rule {

	private final boolean isServlet24OrLater;

	public WebRuleSetTaglibLocationRule(boolean isServlet24OrLater) {
		this.isServlet24OrLater = isServlet24OrLater;
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {
		WebXml webXml = (WebXml) getDigester().peek(
				getDigester().getCount() - 1);
		// If we have a public ID, this is not a 2.4 or later webapp
		boolean havePublicId = (webXml.getPublicId() != null);
		// havePublicId and isServlet24OrLater should be mutually exclusive
		if (havePublicId == isServlet24OrLater) {
			throw new IllegalArgumentException(
					"taglib definition not consistent with specification version");
		}
	}
}