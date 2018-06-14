package javax.servlet.jsp.el;

import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ImplicitObjectELResolverScopeMap<V> extends AbstractMap<String,V> {

    protected abstract Enumeration<String> getAttributeNames();

    protected abstract V getAttribute(String name);

    @SuppressWarnings("unused")
    protected void removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    protected void setAttribute(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Set<Map.Entry<String,V>> entrySet() {
        Enumeration<String> e = getAttributeNames();
        Set<Map.Entry<String, V>> set = new HashSet<Map.Entry<String, V>>();
        if (e != null) {
            while (e.hasMoreElements()) {
                set.add(new ImplicitObjectELResolverScopeMapScopeEntry<V>(this, e.nextElement()));
            }
        }
        return set;
    }

    @Override
    public final int size() {
        int size = 0;
        Enumeration<String> e = getAttributeNames();
        if (e != null) {
            while (e.hasMoreElements()) {
                e.nextElement();
                size++;
            }
        }
        return size;
    }

    @Override
    public final boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        Enumeration<String> e = getAttributeNames();
        if (e != null) {
            while (e.hasMoreElements()) {
                if (key.equals(e.nextElement())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final V get(Object key) {
        if (key != null) {
            return getAttribute((String) key);
        }
        return null;
    }

    @Override
    public final V put(String key, V value) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (value == null) {
            this.removeAttribute(key);
        } else {
            this.setAttribute(key, value);
        }
        return null;
    }

    @Override
    public final V remove(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.removeAttribute((String) key);
        return null;
    }

}