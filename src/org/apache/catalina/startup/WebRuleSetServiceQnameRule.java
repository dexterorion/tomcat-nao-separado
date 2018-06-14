package org.apache.catalina.startup;

import org.apache.catalina.deploy.ContextService;
import org.apache.tomcat.util.digester.Rule;

public final class WebRuleSetServiceQnameRule extends Rule {

	public WebRuleSetServiceQnameRule() {
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
		String namespaceuri = null;
		String localpart = text;
		int colon = text.indexOf(':');
		if (colon >= 0) {
			String prefix = text.substring(0, colon);
			namespaceuri = getDigester().findNamespaceURI(prefix);
			localpart = text.substring(colon + 1);
		}
		ContextService contextService = (ContextService) getDigester().peek();
		contextService.setServiceqnameLocalpart(localpart);
		contextService.setServiceqnameNamespaceURI(namespaceuri);
	}

}