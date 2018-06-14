package org.apache.coyote.ajp;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.Processor;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.tomcat.util.net.AprEndpointHandler;
import org.apache.tomcat.util.net.SocketWrapper;

public class AjpAprProtocolAjpConnectionHandler extends
		AbstractAjpProtocolAbstractAjpConnectionHandler<Long, AjpAprProcessor>
		implements AprEndpointHandler {

	private AjpAprProtocol proto;

	public AjpAprProtocolAjpConnectionHandler(AjpAprProtocol proto) {
		this.proto = proto;
	}

	@Override
	protected AbstractProtocol<Long> getProtocol() {
		return proto;
	}

	@Override
	protected Log getLog() {
		return AjpAprProtocol.getLogVariable();
	}

	/**
	 * Expected to be used by the handler once the processor is no longer
	 * required.
	 */
	@Override
	public void release(SocketWrapper<Long> socket, Processor<Long> processor,
			boolean isSocketClosing, boolean addToPoller) {
		processor.recycle(isSocketClosing);
		getRecycledProcessors().offer(processor);
		if (addToPoller) {
			((AprEndpoint) proto.getEndpoint()).getPoller().add(
					socket.getSocket().longValue(),
					proto.getEndpoint().getKeepAliveTimeout(), true, false);
		}
	}

	@Override
	protected AjpAprProcessor createProcessor() {
		AjpAprProcessor processor = new AjpAprProcessor(proto.getPacketSize(),
				(AprEndpoint) proto.getEndpoint());
		processor.setAdapter(proto.getAdapter());
		processor.setTomcatAuthentication(proto.getTomcatAuthentication());
		processor.setRequiredSecret(proto.getRequiredSecret());
		processor.setKeepAliveTimeout(proto.getKeepAliveTimeout());
		processor.setClientCertProvider(proto.getClientCertProvider());
		register(processor);
		return processor;
	}
}