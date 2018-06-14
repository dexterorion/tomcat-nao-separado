package org.apache.catalina.tribes.transport;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// ---------------------------------------------- ThreadFactory Inner Class
public class ReceiverBaseTaskThreadFactory implements ThreadFactory {
    /**
	 * 
	 */
	private final ReceiverBase receiverBase;
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

    public ReceiverBaseTaskThreadFactory(ReceiverBase receiverBase, String namePrefix) {
        this.receiverBase = receiverBase;
		SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(this.receiverBase.isDaemon());
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}