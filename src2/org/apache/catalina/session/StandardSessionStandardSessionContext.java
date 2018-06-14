package org.apache.catalina.session;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpSession;

/**
 * This class is a dummy implementation of the <code>HttpSessionContext</code>
 * interface, to conform to the requirement that such an object be returned when
 * <code>HttpSession.getSessionContext()</code> is called.
 *
 * @author Craig R. McClanahan
 *
 * @deprecated As of Java Servlet API 2.1 with no replacement. The interface
 *             will be removed in a future version of this API.
 */

@Deprecated
public final class StandardSessionStandardSessionContext implements
		javax.servlet.http.HttpSessionContext {

	private static final List<String> emptyString = Collections.emptyList();

	/**
	 * Return the session identifiers of all sessions defined within this
	 * context.
	 *
	 * @deprecated As of Java Servlet API 2.1 with no replacement. This method
	 *             must return an empty <code>Enumeration</code> and will be
	 *             removed in a future version of the API.
	 */
	@Override
	@Deprecated
	public Enumeration<String> getIds() {
		return Collections.enumeration(emptyString);
	}

	/**
	 * Return the <code>HttpSession</code> associated with the specified session
	 * identifier.
	 *
	 * @param id
	 *            Session identifier for which to look up a session
	 *
	 * @deprecated As of Java Servlet API 2.1 with no replacement. This method
	 *             must return null and will be removed in a future version of
	 *             the API.
	 */
	@Override
	@Deprecated
	public HttpSession getSession(String id) {
		return (null);
	}
}