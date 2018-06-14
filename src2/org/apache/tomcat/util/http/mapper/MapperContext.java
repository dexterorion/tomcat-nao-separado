package org.apache.tomcat.util.http.mapper;


public final class MapperContext extends MapperMapElement {
    private volatile MapperContextVersion[] versions;

    public MapperContext(String name, MapperContextVersion firstVersion) {
        super(name, null);
        setVersions(new MapperContextVersion[] { firstVersion });
    }

	public MapperContextVersion[] getVersions() {
		return versions;
	}

	public void setVersions(MapperContextVersion[] versions) {
		this.versions = versions;
	}
}