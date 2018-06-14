package javax.el;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class BeanElResolverConcurrentCache<K,V> {

    private final int size;
    private final Map<K,V> eden;
    private final Map<K,V> longterm;
    
    public BeanElResolverConcurrentCache(int size) {
        this.size = size;
        this.eden = new ConcurrentHashMap<K,V>(size);
        this.longterm = new WeakHashMap<K,V>(size);
    }
    
    public V get(K key) {
        V value = this.eden.get(key);
        if (value == null) {
            synchronized (longterm) {
                value = this.longterm.get(key);
            }
            if (value != null) {
                this.eden.put(key, value);
            }
        }
        return value;
    }
    
    public void put(K key, V value) {
        if (this.eden.size() >= this.size) {
            synchronized (longterm) {
                this.longterm.putAll(this.eden);
            }
            this.eden.clear();
        }
        this.eden.put(key, value);
    }

}