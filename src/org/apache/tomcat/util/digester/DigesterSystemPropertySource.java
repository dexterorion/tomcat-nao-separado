package org.apache.tomcat.util.digester;

import org.apache.tomcat.util.IntrospectionUtilsPropertySource;

// ---------------------------------------------------------- Static Fields
public class DigesterSystemPropertySource implements
		IntrospectionUtilsPropertySource {
	@Override
	public String getProperty(String key) {
		return System.getProperty(key);
	}
}