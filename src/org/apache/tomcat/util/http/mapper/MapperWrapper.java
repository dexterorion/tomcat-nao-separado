package org.apache.tomcat.util.http.mapper;

public class MapperWrapper extends MapperMapElement {

	private final boolean jspWildCard;
	private final boolean resourceOnly;

    public MapperWrapper(String name, /* Wrapper */Object wrapper,
            boolean jspWildCard, boolean resourceOnly) {
        super(name, wrapper);
        this.jspWildCard = jspWildCard;
        this.resourceOnly = resourceOnly;
    }

	public boolean isJspWildCard() {
		return jspWildCard;
	}

	public boolean isResourceOnly() {
		return resourceOnly;
	}
}