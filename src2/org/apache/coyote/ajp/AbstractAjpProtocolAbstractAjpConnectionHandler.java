package org.apache.coyote.ajp;

import org.apache.coyote.AbstractProtocolAbstractConnectionHandler;
import org.apache.coyote.Processor;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.coyote.http11.upgrade.UpgradeInbound;

public abstract class AbstractAjpProtocolAbstractAjpConnectionHandler<S, P>
		extends AbstractProtocolAbstractConnectionHandler<S, P> {

	@Override
	protected void initSsl(SocketWrapper<S> socket, Processor<S> processor) {
		// NOOP for AJP
	}

	@Override
	protected void longPoll(SocketWrapper<S> socket, Processor<S> processor) {
		// Same requirements for all AJP connectors
		socket.setAsync(true);
	}

	/**
	 * @deprecated Will be removed in Tomcat 8.0.x.
	 */
	@Deprecated
	@Override
	protected AbstractAjpProcessor<S> createUpgradeProcessor(SocketWrapper<S> socket,
			UpgradeInbound inbound) {
		// TODO should fail - throw IOE
		return null;
	}

	@Override
	protected AbstractAjpProcessor<S> createUpgradeProcessor(SocketWrapper<S> socket,
			HttpUpgradeHandler httpUpgradeHandler) {
		// TODO should fail - throw IOE
		return null;
	}
}