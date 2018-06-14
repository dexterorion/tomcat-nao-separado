package javax.el;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public final class BeanElResolverBeanProperty {
    private final Class<?> type;

    private final Class<?> owner;

    private final PropertyDescriptor descriptor;

    private Method read;

    private Method write;

    public BeanElResolverBeanProperty(Class<?> owner, PropertyDescriptor descriptor) {
        this.owner = owner;
        this.descriptor = descriptor;
        this.type = descriptor.getPropertyType();
    }

    // Can't use Class<?> because API needs to match specification
    @SuppressWarnings("rawtypes")
    public Class getPropertyType() {
        return this.type;
    }

    public boolean isReadOnly() {
        return this.write == null
            && (null == (this.write = Util.getMethod(this.owner, descriptor.getWriteMethod())));
    }

    public Method getWriteMethod() {
        return write(null);
    }

    public Method getReadMethod() {
        return this.read(null);
    }

    public Method write(ELContext ctx) {
        if (this.write == null) {
            this.write = Util.getMethod(this.owner, descriptor.getWriteMethod());
            if (this.write == null) {
                throw new PropertyNotFoundException(Util.message(ctx,
                        "propertyNotWritable", new Object[] {
                                owner.getName(), descriptor.getName() }));
            }
        }
        return this.write;
    }

    public Method read(ELContext ctx) {
        if (this.read == null) {
            this.read = Util.getMethod(this.owner, descriptor.getReadMethod());
            if (this.read == null) {
                throw new PropertyNotFoundException(Util.message(ctx,
                        "propertyNotReadable", new Object[] {
                                owner.getName(), descriptor.getName() }));
            }
        }
        return this.read;
    }
}