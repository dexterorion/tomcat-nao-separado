package org.apache.el.parser;

public class AstValueTarget {
    private Object base;

    private Object property;

	public Object getBase() {
		return base;
	}

	public void setBase(Object base) {
		this.base = base;
	}

	public Object getProperty() {
		return property;
	}

	public void setProperty(Object property) {
		this.property = property;
	}
}