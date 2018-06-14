package javax.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class CompositeElResolverFeatureIterator implements Iterator<FeatureDescriptor> {

    private final ELContext context;

    private final Object base;

    private final ELResolver[] resolvers;

    private final int size;

    private Iterator<FeatureDescriptor> itr;

    private int idx;

    private FeatureDescriptor next;

    public CompositeElResolverFeatureIterator(ELContext context, Object base,
            ELResolver[] resolvers, int size) {
        this.context = context;
        this.base = base;
        this.resolvers = resolvers;
        this.size = size;

        this.idx = 0;
        this.guaranteeIterator();
    }
    
    private void guaranteeIterator() {
        while (this.itr == null && this.idx < this.size) {
            this.itr = this.resolvers[this.idx].getFeatureDescriptors(
                    this.context, this.base);
            this.idx++;
        }
    }

    @Override
    public boolean hasNext() {          
        if (this.next != null)
            return true;
        if (this.itr != null){
            while (this.next == null && itr.hasNext()) {
                this.next = itr.next();
            }
        } else {
            return false;
        }
        if (this.next == null) {
            this.itr = null;
            this.guaranteeIterator();
        }
        return hasNext();
    }

    @Override
    public FeatureDescriptor next() {
        if (!hasNext())
            throw new NoSuchElementException();
        FeatureDescriptor result = this.next;
        this.next = null;
        return result;

    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}