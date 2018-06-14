package javax.el;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UtilCacheValue {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private WeakReference<ExpressionFactory> ref;

    public UtilCacheValue() {
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public ExpressionFactory getExpressionFactory() {
        return ref != null ? ref.get() : null;
    }

    public void setExpressionFactory(ExpressionFactory factory) {
        ref = new WeakReference<ExpressionFactory>(factory);
    }
}