package org.apache.tomcat.util.net;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLinkedQueue2 extends ConcurrentLinkedQueue<NioEndpointKeyAttachment>{
	private static final long serialVersionUID = 1L;
	protected AtomicInteger size = new AtomicInteger(0);
	private NioEndpoint nioEndpoint;
	
	public ConcurrentLinkedQueue2(NioEndpoint nioEndpoint){
		this.nioEndpoint = nioEndpoint;
	}

	@Override
	public boolean offer(NioEndpointKeyAttachment ka) {
		ka.reset();
		boolean offer = nioEndpoint.getSocketProperties().getKeyCache() == -1 ? true
				: size.get() < nioEndpoint.getSocketProperties().getKeyCache();
		// avoid over growing our cache or add after we have stopped
		if (nioEndpoint.isRunning() && (!nioEndpoint.isPaused()) && (offer)) {
			boolean result = super.offer(ka);
			if (result) {
				size.incrementAndGet();
			}
			return result;
		} else
			return false;
	}

	@Override
	public NioEndpointKeyAttachment poll() {
		NioEndpointKeyAttachment result = super.poll();
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
