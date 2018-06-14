package org.apache.catalina.startup;

import java.lang.reflect.Method;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public final class WebRuleSetSetPublicIdRule extends Rule {

	public WebRuleSetSetPublicIdRule(String method) {
		this.method = method;
	}

	private String method = null;

	@Override
	public void begin(String namespace, String name, Attributes attributes)
			throws Exception {

		Object top = getDigester().peek();
		Class<?> paramClasses[] = new Class[1];
		paramClasses[0] = "String".getClass();
		String paramValues[] = new String[1];
		paramValues[0] = getDigester().getPublicId();

		Method m = null;
		try {
			m = top.getClass().getMethod(method, paramClasses);
		} catch (NoSuchMethodException e) {
			getDigester().getLogger().error(
					"Can't find method " + method + " in " + top + " CLASS "
							+ top.getClass());
			return;
		}

		m.invoke(top, (Object[]) paramValues);
		if (getDigester().getLogger().isDebugEnabled())
			getDigester().getLogger().debug(
					"" + top.getClass().getName() + "." + method + "("
							+ paramValues[0] + ")");

	}

}