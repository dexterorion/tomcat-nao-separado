package javax.servlet.jsp.el;

import java.util.Map;

public class ImplicitObjectELResolverScopeMapScopeEntry<V> implements Map.Entry<String,V> {

    /**
	 * 
	 */
	private final ImplicitObjectELResolverScopeMap<V> implicitObjectELResolverScopeMap;
	private final String key;

    public ImplicitObjectELResolverScopeMapScopeEntry(ImplicitObjectELResolverScopeMap<V> implicitObjectELResolverScopeMap, String key) {
        this.implicitObjectELResolverScopeMap = implicitObjectELResolverScopeMap;
		this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public V getValue() {
        return this.implicitObjectELResolverScopeMap.getAttribute(this.key);
    }

    @Override
    public V setValue(Object value) {
        if (value == null) {
            this.implicitObjectELResolverScopeMap.removeAttribute(this.key);
        } else {
            this.implicitObjectELResolverScopeMap.setAttribute(this.key, value);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null && this.hashCode() == obj.hashCode());
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

}