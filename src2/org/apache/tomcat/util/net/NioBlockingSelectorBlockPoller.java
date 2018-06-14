package org.apache.tomcat.util.net;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tomcat.util.ExceptionUtils2;

public class NioBlockingSelectorBlockPoller extends Thread {
	private volatile boolean run = true;
	private Selector selector = null;
	private ConcurrentLinkedQueue<Runnable> events = new ConcurrentLinkedQueue<Runnable>();

	public void disable() {
		run = false;
		selector.wakeup();
	}

	private AtomicInteger wakeupCounter = new AtomicInteger(0);

	public void cancelKey(final SelectionKey key) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				key.cancel();
			}
		};
		events.offer(r);
		wakeup();
	}

	public void wakeup() {
		if (wakeupCounter.addAndGet(1) == 0)
			selector.wakeup();
	}

	public void cancel(SelectionKey sk, NioEndpointKeyAttachment key, int ops) {
		if (sk != null) {
			sk.cancel();
			sk.attach(null);
			if (SelectionKey.OP_WRITE == (ops & SelectionKey.OP_WRITE))
				countDown(key.getWriteLatch());
			if (SelectionKey.OP_READ == (ops & SelectionKey.OP_READ))
				countDown(key.getReadLatch());
		}
	}

	public void add(final NioEndpointKeyAttachment key, final int ops,
			final NioBlockingSelectorKeyReference ref) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (key == null)
					return;
				NioChannel nch = key.getChannel();
				if (nch == null)
					return;
				SocketChannel ch = nch.getIOChannel();
				if (ch == null)
					return;
				SelectionKey sk = ch.keyFor(selector);
				try {
					if (sk == null) {
						sk = ch.register(selector, ops, key);
						ref.setKey(sk);
					} else if (!sk.isValid()) {
						cancel(sk, key, ops);
					} else {
						sk.interestOps(sk.interestOps() | ops);
					}
				} catch (CancelledKeyException cx) {
					cancel(sk, key, ops);
				} catch (ClosedChannelException cx) {
					cancel(sk, key, ops);
				}
			}
		};
		events.offer(r);
		wakeup();
	}

	public void remove(final NioEndpointKeyAttachment key, final int ops) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (key == null)
					return;
				NioChannel nch = key.getChannel();
				if (nch == null)
					return;
				SocketChannel ch = nch.getIOChannel();
				if (ch == null)
					return;
				SelectionKey sk = ch.keyFor(selector);
				try {
					if (sk == null) {
						if (SelectionKey.OP_WRITE == (ops & SelectionKey.OP_WRITE))
							countDown(key.getWriteLatch());
						if (SelectionKey.OP_READ == (ops & SelectionKey.OP_READ))
							countDown(key.getReadLatch());
					} else {
						if (sk.isValid()) {
							sk.interestOps(sk.interestOps() & (~ops));
							if (SelectionKey.OP_WRITE == (ops & SelectionKey.OP_WRITE))
								countDown(key.getWriteLatch());
							if (SelectionKey.OP_READ == (ops & SelectionKey.OP_READ))
								countDown(key.getReadLatch());
							if (sk.interestOps() == 0) {
								sk.cancel();
								sk.attach(null);
							}
						} else {
							sk.cancel();
							sk.attach(null);
						}
					}
				} catch (CancelledKeyException cx) {
					if (sk != null) {
						sk.cancel();
						sk.attach(null);
					}
				}
			}
		};
		events.offer(r);
		wakeup();
	}

	public boolean events() {
		boolean result = false;
		Runnable r = null;
		result = (events.size() > 0);
		while ((r = events.poll()) != null) {
			r.run();
			result = true;
		}
		return result;
	}

	@Override
	public void run() {
		while (run) {
			try {
				events();
				int keyCount = 0;
				try {
					int i = wakeupCounter.get();
					if (i > 0)
						keyCount = selector.selectNow();
					else {
						wakeupCounter.set(-1);
						keyCount = selector.select(1000);
					}
					wakeupCounter.set(0);
					if (!run)
						break;
				} catch (NullPointerException x) {
					// sun bug 5076772 on windows JDK 1.5
					if (selector == null)
						throw x;
					if (NioBlockingSelector.getLog().isDebugEnabled())
						NioBlockingSelector.getLog()
								.debug("Possibly encountered sun bug 5076772 on windows JDK 1.5",
										x);
					continue;
				} catch (CancelledKeyException x) {
					// sun bug 5076772 on windows JDK 1.5
					if (NioBlockingSelector.getLog().isDebugEnabled())
						NioBlockingSelector.getLog()
								.debug("Possibly encountered sun bug 5076772 on windows JDK 1.5",
										x);
					continue;
				} catch (Throwable x) {
					ExceptionUtils2.handleThrowable(x);
					NioBlockingSelector.getLog().error("", x);
					continue;
				}

				Iterator<SelectionKey> iterator = keyCount > 0 ? selector
						.selectedKeys().iterator() : null;

				// Walk through the collection of ready keys and dispatch
				// any active event.
				while (run && iterator != null && iterator.hasNext()) {
					SelectionKey sk = iterator.next();
					NioEndpointKeyAttachment attachment = (NioEndpointKeyAttachment) sk.attachment();
					try {
						attachment.access();
						iterator.remove();
						sk.interestOps(sk.interestOps() & (~sk.readyOps()));
						if (sk.isReadable()) {
							countDown(attachment.getReadLatch());
						}
						if (sk.isWritable()) {
							countDown(attachment.getWriteLatch());
						}
					} catch (CancelledKeyException ckx) {
						sk.cancel();
						countDown(attachment.getReadLatch());
						countDown(attachment.getWriteLatch());
					}
				}// while
			} catch (Throwable t) {
				NioBlockingSelector.getLog().error("", t);
			}
		}
		events.clear();
		try {
			selector.selectNow();// cancel all remaining keys
		} catch (Exception ignore) {
			if (NioBlockingSelector.getLog().isDebugEnabled())
				NioBlockingSelector.getLog().debug("", ignore);
		}
		try {
			selector.close();// Close the connector
		} catch (Exception ignore) {
			if (NioBlockingSelector.getLog().isDebugEnabled())
				NioBlockingSelector.getLog().debug("", ignore);
		}
	}

	public void countDown(CountDownLatch latch) {
		if (latch == null)
			return;
		latch.countDown();
	}

	public boolean isRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
	}

	public Selector getSelector() {
		return selector;
	}

	public void setSelector(Selector selector) {
		this.selector = selector;
	}

	public ConcurrentLinkedQueue<Runnable> getEvents() {
		return events;
	}

	public void setEvents(ConcurrentLinkedQueue<Runnable> events) {
		this.events = events;
	}

	public AtomicInteger getWakeupCounter() {
		return wakeupCounter;
	}

	public void setWakeupCounter(AtomicInteger wakeupCounter) {
		this.wakeupCounter = wakeupCounter;
	}
}