package org.apache.catalina.core;


public final class DefaultInstanceManagerAnnotationCacheEntry {
    private final String accessibleObjectName;
    private final Class<?>[] paramTypes;
    private final String name;
    private final DefaultInstanceManagerAnnotationCacheEntryType type;

    public DefaultInstanceManagerAnnotationCacheEntry(String accessibleObjectName,
            Class<?>[] paramTypes, String name,
            DefaultInstanceManagerAnnotationCacheEntryType type) {
        this.accessibleObjectName = accessibleObjectName;
        this.paramTypes = paramTypes;
        this.name = name;
        this.type = type;
    }

    public String getAccessibleObjectName() {
        return accessibleObjectName;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public String getName() {
        return name;
    }
    public DefaultInstanceManagerAnnotationCacheEntryType getType() {
        return type;
    }
}