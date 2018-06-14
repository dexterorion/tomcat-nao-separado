package org.apache.tomcat.util.http.mapper;

public abstract class MapperMapElement {

	private final String name;
	private final Object object;

    public MapperMapElement(String name, Object object) {
        this.name = name;
        this.object = object;
    }

	public String getName() {
		return name;
	}

	public Object getObject() {
		return object;
	}
}