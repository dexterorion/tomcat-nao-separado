package org.apache.catalina.core;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterRegistrationDynamic;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistrationDynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

public class StandardContextNoPluggabilityServletContext implements
		ServletContext {

	private final ServletContext sc;

	public StandardContextNoPluggabilityServletContext(ServletContext sc) {
		this.sc = sc;
	}

	@Override
	public String getContextPath() {
		return sc.getContextPath();
	}

	@Override
	public ServletContext getContext(String uripath) {
		return sc.getContext(uripath);
	}

	@Override
	public int getMajorVersion() {
		return sc.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return sc.getMinorVersion();
	}

	@Override
	public int getEffectiveMajorVersion() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public int getEffectiveMinorVersion() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public String getMimeType(String file) {
		return sc.getMimeType(file);
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		return sc.getResourcePaths(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return sc.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return sc.getResourceAsStream(path);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return sc.getRequestDispatcher(path);
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		return sc.getNamedDispatcher(name);
	}

	@Override
	@Deprecated
	public Servlet getServlet(String name) throws ServletException {
		return sc.getServlet(name);
	}

	@Override
	@Deprecated
	public Enumeration<Servlet> getServlets() {
		return sc.getServlets();
	}

	@Override
	@Deprecated
	public Enumeration<String> getServletNames() {
		return sc.getServletNames();
	}

	@Override
	public void log(String msg) {
		sc.log(msg);
	}

	@Override
	@Deprecated
	public void log(Exception exception, String msg) {
		sc.log(exception, msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		sc.log(message, throwable);
	}

	@Override
	public String getRealPath(String path) {
		return sc.getRealPath(path);
	}

	@Override
	public String getServerInfo() {
		return sc.getServerInfo();
	}

	@Override
	public String getInitParameter(String name) {
		return sc.getInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return sc.getInitParameterNames();
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public Object getAttribute(String name) {
		return sc.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return sc.getAttributeNames();
	}

	@Override
	public void setAttribute(String name, Object object) {
		sc.setAttribute(name, object);
	}

	@Override
	public void removeAttribute(String name) {
		sc.removeAttribute(name);
	}

	@Override
	public String getServletContextName() {
		return sc.getServletContextName();
	}

	@Override
	public ServletRegistrationDynamic addServlet(String servletName,
			String className) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public ServletRegistrationDynamic addServlet(String servletName,
			Servlet servlet) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public ServletRegistrationDynamic addServlet(String servletName,
			Class<? extends Servlet> servletClass) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> c)
			throws ServletException {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public FilterRegistrationDynamic addFilter(
			String filterName, String className) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public FilterRegistrationDynamic addFilter(
			String filterName, Filter filter) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public FilterRegistrationDynamic addFilter(
			String filterName, Class<? extends Filter> filterClass) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> c)
			throws ServletException {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public void setSessionTrackingModes(
			Set<SessionTrackingMode> sessionTrackingModes) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public void addListener(String className) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> c)
			throws ServletException {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public ClassLoader getClassLoader() {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}

	@Override
	public void declareRoles(String... roleNames) {
		throw new UnsupportedOperationException(
				StandardContext.sm
						.getString("noPluggabilityServletContext.notAllowed"));
	}
}