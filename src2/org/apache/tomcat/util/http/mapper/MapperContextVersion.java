package org.apache.tomcat.util.http.mapper;

import javax.naming.Context;

public final class MapperContextVersion extends MapperMapElement {
    private String path = null;
    private int slashCount;
    private String[] welcomeResources = new String[0];
    private Context resources = null;
    private MapperWrapper defaultWrapper = null;
    private MapperWrapper[] exactWrappers = new MapperWrapper[0];
    private MapperWrapper[] wildcardWrappers = new MapperWrapper[0];
    private MapperWrapper[] extensionWrappers = new MapperWrapper[0];
    private int nesting = 0;
    private volatile boolean paused;

    public MapperContextVersion() {
        super(null, null);
    }

    public MapperContextVersion(String version, Object context) {
        super(version, context);
    }

    public boolean isPaused() {
        return paused;
    }

    public void markPaused() {
        paused = true;
    }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getSlashCount() {
		return slashCount;
	}

	public void setSlashCount(int slashCount) {
		this.slashCount = slashCount;
	}

	public String[] getWelcomeResources() {
		return welcomeResources;
	}

	public void setWelcomeResources(String[] welcomeResources) {
		this.welcomeResources = welcomeResources;
	}

	public Context getResources() {
		return resources;
	}

	public void setResources(Context resources) {
		this.resources = resources;
	}

	public MapperWrapper getDefaultWrapper() {
		return defaultWrapper;
	}

	public void setDefaultWrapper(MapperWrapper defaultWrapper) {
		this.defaultWrapper = defaultWrapper;
	}

	public MapperWrapper[] getExactWrappers() {
		return exactWrappers;
	}

	public void setExactWrappers(MapperWrapper[] exactWrappers) {
		this.exactWrappers = exactWrappers;
	}

	public MapperWrapper[] getWildcardWrappers() {
		return wildcardWrappers;
	}

	public void setWildcardWrappers(MapperWrapper[] wildcardWrappers) {
		this.wildcardWrappers = wildcardWrappers;
	}

	public MapperWrapper[] getExtensionWrappers() {
		return extensionWrappers;
	}

	public void setExtensionWrappers(MapperWrapper[] extensionWrappers) {
		this.extensionWrappers = extensionWrappers;
	}

	public int getNesting() {
		return nesting;
	}

	public void setNesting(int nesting) {
		this.nesting = nesting;
	}
}