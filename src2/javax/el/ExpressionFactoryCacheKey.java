package javax.el;

import java.lang.ref.WeakReference;

/**
 * Key used to cache ExpressionFactory discovery information per class
 * loader. The class loader reference is never {@code null}, because
 * {@code null} tccl is handled separately.
 */
public class ExpressionFactoryCacheKey {
    private final int hash;
    private final WeakReference<ClassLoader> ref;

    public ExpressionFactoryCacheKey(ClassLoader cl) {
        hash = cl.hashCode();
        ref = new WeakReference<ClassLoader>(cl);
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
        if (!(obj instanceof ExpressionFactoryCacheKey)) {
            return false;
        }
        ClassLoader thisCl = getRefData().get();
        if (thisCl == null) {
            return false;
        }
        return thisCl == ((ExpressionFactoryCacheKey) obj).getRefData().get();
    }

	public WeakReference<ClassLoader> getRefData() {
		return ref;
	}
}