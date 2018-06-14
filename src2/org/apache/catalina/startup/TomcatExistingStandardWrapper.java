package org.apache.catalina.startup;

import java.util.Stack;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.core.StandardWrapper;

/**
 * Helper class for wrapping existing servlets. This disables servlet
 * lifecycle and normal reloading, but also reduces overhead and provide
 * more direct control over the servlet.
 */
public class TomcatExistingStandardWrapper extends StandardWrapper {
	private final Servlet existing;

	@SuppressWarnings("deprecation")
	public TomcatExistingStandardWrapper(Servlet existing) {
		this.existing = existing;
		if (existing instanceof javax.servlet.SingleThreadModel) {
			setSingleThreadModel(true);
			setInstancePool(new Stack<Servlet>());
		}
	}

	@Override
	public synchronized Servlet loadServlet() throws ServletException {
		if (isSingleThreadModel()) {
			Servlet instance;
			try {
				instance = existing.getClass().newInstance();
			} catch (InstantiationException e) {
				throw new ServletException(e);
			} catch (IllegalAccessException e) {
				throw new ServletException(e);
			}
			instance.init(getFacade());
			return instance;
		} else {
			if (!isInstanceInitialized()) {
				existing.init(getFacade());
				setInstanceInitialized(true);
			}
			return existing;
		}
	}

	@Override
	public long getAvailable() {
		return 0;
	}

	@Override
	public boolean isUnavailable() {
		return false;
	}

	@Override
	public Servlet getServlet() {
		return existing;
	}

	@Override
	public String getServletClass() {
		return existing.getClass().getName();
	}
}