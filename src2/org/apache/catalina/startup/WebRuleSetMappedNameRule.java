package org.apache.catalina.startup;

import org.apache.catalina.deploy.ResourceBase;
import org.apache.tomcat.util.digester.Rule;

public final class WebRuleSetMappedNameRule extends Rule {

	public WebRuleSetMappedNameRule() {
		// NO-OP
	}

	/**
	 * Process the body text of this element.
	 *
	 * @param namespace
	 *            the namespace URI of the matching element, or an empty string
	 *            if the parser is not namespace aware or the element has no
	 *            namespace
	 * @param name
	 *            the local name if the parser is namespace aware, or just the
	 *            element name otherwise
	 * @param text
	 *            The body text of this element
	 */
	@Override
	public void body(String namespace, String name, String text)
			throws Exception {
		ResourceBase resourceBase = (ResourceBase) getDigester().peek();
		resourceBase.setProperty("mappedName", text.trim());
	}
}