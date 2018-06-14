package org.apache.catalina.startup;

import java.util.ArrayList;

import org.apache.tomcat.util.digester.CallParamRule;

public final class WebRuleSetCallParamMultiRule extends CallParamRule {

	public WebRuleSetCallParamMultiRule(int paramIndex) {
		super(paramIndex);
	}

	@Override
	public void end(String namespace, String name) {
		if (getBodyTextStack() != null && !getBodyTextStack().empty()) {
			// what we do now is push one parameter onto the top set of
			// parameters
			Object parameters[] = (Object[]) getDigester().peekParams();
			@SuppressWarnings("unchecked")
			ArrayList<String> params = (ArrayList<String>) parameters[getParamIndex()];
			if (params == null) {
				params = new ArrayList<String>();
				parameters[getParamIndex()] = params;
			}
			params.add(getBodyTextStack().pop());
		}
	}

}