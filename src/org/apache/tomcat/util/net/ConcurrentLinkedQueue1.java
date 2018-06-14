package org.apache.tomcat.util.net;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLinkedQueue1 extends ConcurrentLinkedQueue<NioEndpointSocketProcessor>{
	private static final long serialVersionUID = 1L;
	private NioEndpoint nioEndpoint;
	protected AtomicInteger size = new AtomicInteger(0);

	public ConcurrentLinkedQueue1(NioEndpoint nioEndpoint){
		this.nioEndpoint = nioEndpoint;
	}
	
	@Override
	public boolean offer(NioEndpointSocketProcessor sc) {
		sc.reset(null, null);
		boolean offer = nioEndpoint.getSocketProperties().getProcessorCache() == -1 ? true
				: size.get() < nioEndpoint.getSocketProperties().getProcessorCache();
		// avoid over growing our cache or add after we have stopped
		if (nioEndpoint.isRunning() && (!nioEndpoint.isPaused()) && (offer)) {
			boolean result = super.offer(sc);
			if (result) {
				size.incrementAndGet();
			}
			return result;
		} else
			return false;
	}

	@Override
	public NioEndpointSocketProcessor poll() {
		NioEndpointSocketProcessor result = super.poll();
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
