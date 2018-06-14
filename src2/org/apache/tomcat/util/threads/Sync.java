package org.apache.tomcat.util.threads;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Sync extends AbstractQueuedSynchronizer {
    /**
	 * 
	 */
	private final LimitLatch limitLatch;
	private static final long serialVersionUID = 1L;

    public Sync(LimitLatch limitLatch) {
		this.limitLatch = limitLatch;
    }

    @Override
    protected int tryAcquireShared(int ignored) {
        long newCount = this.limitLatch.getCount2().incrementAndGet();
        if (!this.limitLatch.isReleased() && newCount > this.limitLatch.getLimit()) {
            // Limit exceeded
            this.limitLatch.getCount2().decrementAndGet();
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    protected boolean tryReleaseShared(int arg) {
        this.limitLatch.getCount2().decrementAndGet();
        return true;
    }
}