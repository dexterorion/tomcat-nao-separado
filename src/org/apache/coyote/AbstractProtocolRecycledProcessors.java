package org.apache.coyote;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AbstractProtocolRecycledProcessors<P, S>
        extends ConcurrentLinkedQueue<Processor<S>> {

    private static final long serialVersionUID = 1L;
    private transient AbstractProtocolAbstractConnectionHandler<S,P> handler;
    private AtomicInteger size = new AtomicInteger(0);

    public AbstractProtocolRecycledProcessors(AbstractProtocolAbstractConnectionHandler<S,P> handler) {
        this.handler = handler;
    }

    @Override
    public boolean offer(Processor<S> processor) {
        int cacheSize = handler.getProtocol().getProcessorCache();
        boolean offer = cacheSize == -1 ? true : size.get() < cacheSize;
        //avoid over growing our cache or add after we have stopped
        boolean result = false;
        if (offer) {
            result = super.offer(processor);
            if (result) {
                size.incrementAndGet();
            }
        }
        if (!result) handler.unregister(processor);
        return result;
    }

    @Override
    public Processor<S> poll() {
        Processor<S> result = super.poll();
        if (result != null) {
            size.decrementAndGet();
        }
        return result;
    }

    @Override
    public void clear() {
        Processor<S> next = poll();
        while (next != null) {
            handler.unregister(next);
            next = poll();
        }
        super.clear();
        size.set(0);
    }
}