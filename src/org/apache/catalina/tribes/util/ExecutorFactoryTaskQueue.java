package org.apache.catalina.tribes.util;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

public class ExecutorFactoryTaskQueue extends LinkedBlockingQueue<Runnable> {
    private static final long serialVersionUID = 1L;

    private ThreadPoolExecutor parent = null;

    public ExecutorFactoryTaskQueue() {
        super();
    }

    public ExecutorFactoryTaskQueue(int initialCapacity) {
        super(initialCapacity);
    }

    public ExecutorFactoryTaskQueue(Collection<? extends Runnable> c) {
        super(c);
    }

    public void setParent(ThreadPoolExecutor tp) {
        parent = tp;
    }
    
    public boolean force(Runnable o) {
        if ( parent.isShutdown() ) throw new RejectedExecutionException("Executor not running, can't force a command into the queue");
        return super.offer(o); //forces the item onto the queue, to be used if the task is rejected
    }

    @Override
    public boolean offer(Runnable o) {
        //we can't do any checks
        if (parent==null) return super.offer(o);
        //we are maxed out on threads, simply queue the object
        if (parent.getPoolSize() == parent.getMaximumPoolSize()) return super.offer(o);
        //we have idle threads, just add it to the queue
        //this is an approximation, so it could use some tuning
        if (parent.getActiveCount()<(parent.getPoolSize())) return super.offer(o);
        //if we have less threads than maximum force creation of a new thread
        if (parent.getPoolSize()<parent.getMaximumPoolSize()) return false;
        //if we reached here, we need to add it to the queue
        return super.offer(o);
    }
}