package javax.el;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExpressionFactoryCacheValue {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private String className;
    private WeakReference<Class<?>> ref;

    public ExpressionFactoryCacheValue() {
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public String getFactoryClassName() {
        return className;
    }

    public void setFactoryClassName(String className) {
        this.className = className;
    }

    public Class<?> getFactoryClass() {
        return ref != null ? ref.get() : null;
    }

    public void setFactoryClass(Class<?> clazz) {
        ref = new WeakReference<Class<?>>(clazz);
    }
}