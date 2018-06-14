package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.CallMethodRule;

public final class WebRuleSetLifecycleCallbackRule extends CallMethodRule {

	private final boolean postConstruct;

	public WebRuleSetLifecycleCallbackRule(String methodName, int paramCount,
			boolean postConstruct) {
		super(methodName, paramCount);
		this.postConstruct = postConstruct;
	}

	@Override
	public void end(String namespace, String name) throws Exception {
		Object[] params = (Object[]) getDigester().peekParams();
		if (params != null && params.length == 2) {
			WebXml webXml = (WebXml) getDigester().peek();
			if (postConstruct) {
				if (webXml.getPostConstructMethods().containsKey(params[0])) {
					throw new IllegalArgumentException(WebRuleSet.getSm().getString(
							"webRuleSet.postconstruct.duplicate", params[0]));
				}
			} else {
				if (webXml.getPreDestroyMethods().containsKey(params[0])) {
					throw new IllegalArgumentException(WebRuleSet.getSm().getString(
							"webRuleSet.predestroy.duplicate", params[0]));
				}
			}
		}
		super.end(namespace, name);
	}
}