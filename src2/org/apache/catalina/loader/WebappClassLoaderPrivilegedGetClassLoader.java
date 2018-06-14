package org.apache.catalina.loader;

import java.security.PrivilegedAction;

public final class WebappClassLoaderPrivilegedGetClassLoader implements
		PrivilegedAction<ClassLoader> {

	private Class<?> clazz;

	public WebappClassLoaderPrivilegedGetClassLoader(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public ClassLoader run() {
		return clazz.getClassLoader();
	}
}