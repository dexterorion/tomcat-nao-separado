package org.apache.tomcat.util.net;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLinkedQueue4 extends ConcurrentLinkedQueue<NioChannel>{
	private NioEndpoint nioEndpoint;
	
	private static final long serialVersionUID = 1L;
	protected AtomicInteger size = new AtomicInteger(0);
	protected AtomicInteger bytes = new AtomicInteger(0);

	public ConcurrentLinkedQueue4(NioEndpoint nioEndpoint){
		this.nioEndpoint = nioEndpoint;
	}
	
	@Override
	public boolean offer(NioChannel socket) {
		boolean offer = nioEndpoint.getSocketProperties().getBufferPool() == -1 ? true
				: size.get() < nioEndpoint.getSocketProperties().getBufferPool();
		offer = offer
				&& (nioEndpoint.getSocketProperties().getBufferPoolSize() == -1 ? true
						: (bytes.get() + socket.getBufferSize()) < nioEndpoint.getSocketProperties()
								.getBufferPoolSize());
		// avoid over growing our cache or add after we have stopped
		if (nioEndpoint.isRunning() && (!nioEndpoint.isPaused()) && (offer)) {
			boolean result = super.offer(socket);
			if (result) {
				size.incrementAndGet();
				bytes.addAndGet(socket.getBufferSize());
			}
			return result;
		} else
			return false;
	}

	@Override
	public NioChannel poll() {
		NioChannel result = super.poll();
		if (result != null) {
			size.decrementAndGet();
			bytes.addAndGet(-result.getBufferSize());
		}
		return result;
	}

	@Override
	public void clear() {
		super.clear();
		size.set(0);
		bytes.set(0);
	}
}
