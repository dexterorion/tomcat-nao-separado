package javax.el;

import java.lang.ref.WeakReference;

/**
 * Key used to cache default ExpressionFactory information per class
 * loader. The class loader reference is never {@code null}, because
 * {@code null} tccl is handled separately.
 */
public class UtilCacheKey {
    private final int hash;
    private final WeakReference<ClassLoader> ref;

    public UtilCacheKey(ClassLoader key) {
        hash = key.hashCode();
        ref = new WeakReference<ClassLoader>(key);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UtilCacheKey)) {
            return false;
        }
        ClassLoader thisKey = getRefData().get();
        if (thisKey == null) {
            return false;
        }
        return thisKey == ((UtilCacheKey) obj).getRefData().get();
    }

	public WeakReference<ClassLoader> getRefData() {
		return ref;
	}
}