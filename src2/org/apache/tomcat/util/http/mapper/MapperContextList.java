package org.apache.tomcat.util.http.mapper;


public final class MapperContextList {

    private final MapperContext[] contexts;
    private final int nesting;

    public MapperContextList() {
        this(new MapperContext[0], 0);
    }

    public MapperContextList(MapperContext[] contexts, int nesting) {
        this.contexts = contexts;
        this.nesting = nesting;
    }

    public MapperContextList addContext(MapperContext mappedContext, int slashCount) {
    	MapperContext[] newContexts = new MapperContext[contexts.length + 1];
        if (Mapper.insertMap(contexts, newContexts, mappedContext)) {
            return new MapperContextList(newContexts, Math.max(nesting,
                    slashCount));
        }
        return null;
    }

    public MapperContextList removeContext(String path) {
    	MapperContext[] newContexts = new MapperContext[contexts.length - 1];
        if (Mapper.removeMap(contexts, newContexts, path)) {
            int newNesting = 0;
            for (MapperContext context : newContexts) {
                newNesting = Math.max(newNesting, Mapper.slashCount(context.getName()));
            }
            return new MapperContextList(newContexts, newNesting);
        }
        return null;
    }

	public MapperContext[] getContexts() {
		return contexts;
	}

	public int getNesting() {
		return nesting;
	}
    
    
}