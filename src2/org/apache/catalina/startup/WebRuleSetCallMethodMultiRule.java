package org.apache.catalina.startup;

import java.util.ArrayList;

import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.CallMethodRule;

public final class WebRuleSetCallMethodMultiRule extends CallMethodRule {

	private int multiParamIndex = 0;

	public WebRuleSetCallMethodMultiRule(String methodName, int paramCount,
			int multiParamIndex) {
		super(methodName, paramCount);
		this.multiParamIndex = multiParamIndex;
	}

	/**
	 * Process the end of this element.
	 * 
	 * @param namespace
	 *            the namespace URI of the matching element, or an empty string
	 *            if the parser is not namespace aware or the element has no
	 *            namespace
	 * @param name
	 *            the local name if the parser is namespace aware, or just the
	 *            element name otherwise
	 */
	@Override
	public void end(String namespace, String name) throws Exception {

		// Retrieve or construct the parameter values array
		Object parameters[] = null;
		if (getParamCount() > 0) {
			parameters = (Object[]) getDigester().popParams();
		} else {
			parameters = new Object[0];
			super.end(namespace, name);
		}

		ArrayList<?> multiParams = (ArrayList<?>) parameters[multiParamIndex];

		// Construct the parameter values array we will need
		// We only do the conversion if the param value is a String and
		// the specified paramType is not String.
		Object paramValues[] = new Object[getParamTypes().length];
		for (int i = 0; i < getParamTypes().length; i++) {
			if (i != multiParamIndex) {
				// convert nulls and convert stringy parameters
				// for non-stringy param types
				if (parameters[i] == null
						|| (parameters[i] instanceof String && !String.class
								.isAssignableFrom(getParamTypes()[i]))) {
					paramValues[i] = IntrospectionUtils.convert(
							(String) parameters[i], getParamTypes()[i]);
				} else {
					paramValues[i] = parameters[i];
				}
			}
		}

		// Determine the target object for the method call
		Object target;
		if (getTargetOffset() >= 0) {
			target = getDigester().peek(getTargetOffset());
		} else {
			target = getDigester().peek(
					getDigester().getCount() + getTargetOffset());
		}

		if (target == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[CallMethodRule]{");
			sb.append("");
			sb.append("} Call target is null (");
			sb.append("targetOffset=");
			sb.append(getTargetOffset());
			sb.append(",stackdepth=");
			sb.append(getDigester().getCount());
			sb.append(")");
			throw new org.xml.sax.SAXException(sb.toString());
		}

		if (multiParams == null) {
			paramValues[multiParamIndex] = null;
			IntrospectionUtils.callMethodN(target, getMethodName(),
					paramValues, getParamTypes());
			return;
		}

		for (int j = 0; j < multiParams.size(); j++) {
			Object param = multiParams.get(j);
			if (param == null
					|| (param instanceof String && !String.class
							.isAssignableFrom(getParamTypes()[multiParamIndex]))) {
				paramValues[multiParamIndex] = IntrospectionUtils.convert(
						(String) param, getParamTypes()[multiParamIndex]);
			} else {
				paramValues[multiParamIndex] = param;
			}
			IntrospectionUtils.callMethodN(target, getMethodName(),
					paramValues, getParamTypes());
		}

	}

}