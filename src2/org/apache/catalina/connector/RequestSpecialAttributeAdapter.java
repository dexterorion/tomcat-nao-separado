package org.apache.catalina.connector;

public interface RequestSpecialAttributeAdapter {
	public Object get(Request request, String name);

	public void set(Request request, String name, Object value);

	// None of special attributes support removal
	// void remove(Request request, String name);
}