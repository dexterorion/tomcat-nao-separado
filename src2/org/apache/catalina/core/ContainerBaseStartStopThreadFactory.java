package org.apache.catalina.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ContainerBaseStartStopThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	public ContainerBaseStartStopThreadFactory(String namePrefix) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
				.getThreadGroup();
		this.namePrefix = namePrefix;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(group, r, namePrefix
				+ threadNumber.getAndIncrement());
		thread.setDaemon(true);
		return thread;
	}
}