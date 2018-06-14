package org.apache.tomcat.util.net;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLinkedQueue3 extends ConcurrentLinkedQueue<NioEndpointPollerEvent>{
	private NioEndpoint nioEndpoint;
	
	public ConcurrentLinkedQueue3(NioEndpoint nioEndpoint){
		this.nioEndpoint = nioEndpoint;
	}

	private static final long serialVersionUID = 1L;
	protected AtomicInteger size = new AtomicInteger(0);

	@Override
	public boolean offer(NioEndpointPollerEvent pe) {
		pe.reset();
		boolean offer = nioEndpoint.getSocketProperties().getEventCache() == -1 ? true
				: size.get() < nioEndpoint.getSocketProperties().getEventCache();
		// avoid over growing our cache or add after we have stopped
		if (nioEndpoint.isRunning() && (!nioEndpoint.isPaused()) && (offer)) {
			boolean result = super.offer(pe);
			if (result) {
				size.incrementAndGet();
			}
			return result;
		} else
			return false;
	}

	@Override
	public NioEndpointPollerEvent poll() {
		NioEndpointPollerEvent result = super.poll();
		if (result != null) {
			size.decrementAndGet();
		}
		return result;
	}

	@Override
	public void clear() {
		super.clear();
		size.set(0);
	}
}
