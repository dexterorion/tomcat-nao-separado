package org.apache.catalina.loader;

import java.security.PrivilegedAction;

public class WebappClassLoaderPrivilegedFindResourceByName implements
		PrivilegedAction<ResourceEntry> {

	/**
	 * 
	 */
	private final WebappClassLoader webappClassLoader;
	private String name;
	private String path;

	public WebappClassLoaderPrivilegedFindResourceByName(
			WebappClassLoader webappClassLoader, String name, String path) {
		this.webappClassLoader = webappClassLoader;
		this.name = name;
		this.path = path;
	}

	@Override
	public ResourceEntry run() {
		return this.webappClassLoader.findResourceInternal(name, path);
	}

}