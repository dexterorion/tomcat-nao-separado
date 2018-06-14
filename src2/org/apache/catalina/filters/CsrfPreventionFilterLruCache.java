package org.apache.catalina.filters;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class CsrfPreventionFilterLruCache<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // Although the internal implementation uses a Map, this cache
    // implementation is only concerned with the keys.
    private final Map<T,T> cache;

    public CsrfPreventionFilterLruCache(final int cacheSize) {
        cache = new LinkedHashMap<T,T>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<T,T> eldest) {
                if (size() > cacheSize) {
                    return true;
                }
                return false;
            }
        };
    }

    public void add(T key) {
        synchronized (cache) {
            cache.put(key, null);
        }
    }

    public boolean contains(T key) {
        synchronized (cache) {
            return cache.containsKey(key);
        }
    }
}