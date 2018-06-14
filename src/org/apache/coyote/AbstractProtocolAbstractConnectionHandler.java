package org.apache.coyote;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.coyote.http11.upgrade.UpgradeInbound;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.coyote.http11.upgrade.servlet31.WebConnection;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.modeler.Registry2;
import org.apache.tomcat.util.net.AbstractEndpointHandler;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;

public abstract class AbstractProtocolAbstractConnectionHandler<S, P>
		implements AbstractEndpointHandler {

	protected abstract Log getLog();

	private RequestGroupInfo global = new RequestGroupInfo();
	private AtomicLong registerCount = new AtomicLong(0);

	private ConcurrentHashMap<S, Processor<S>> connections = new ConcurrentHashMap<S, Processor<S>>();

	private AbstractProtocolRecycledProcessors<P, S> recycledProcessors = new AbstractProtocolRecycledProcessors<P, S>(
			this);

	protected abstract AbstractProtocol<S> getProtocol();

	@Override
	public Object getGlobal() {
		return global;
	}

	@Override
	public void recycle() {
		recycledProcessors.clear();
	}

	@SuppressWarnings("deprecation")
	// Old HTTP upgrade method has been deprecated
	public SocketState process(SocketWrapper<S> wrapper, SocketStatus status) {
		if (wrapper == null) {
			// Nothing to do. Socket has been closed.
			return SocketState.CLOSED;
		}

		S socket = wrapper.getSocket();
		if (socket == null) {
			// Nothing to do. Socket has been closed.
			return SocketState.CLOSED;
		}

		Processor<S> processor = connections.get(socket);
		if (status == SocketStatus.DISCONNECT && processor == null) {
			// Nothing to do. Endpoint requested a close and there is no
			// longer a processor associated with this socket.
			return SocketState.CLOSED;
		}

		wrapper.setAsync(false);
		ContainerThreadMarker.markAsContainerThread();

		try {
			if (processor == null) {
				processor = recycledProcessors.poll();
			}
			if (processor == null) {
				processor = (Processor<S>) createProcessor();
			}

			initSsl(wrapper, processor);

			SocketState state = SocketState.CLOSED;
			do {
				if (status == SocketStatus.CLOSE_NOW) {
					processor.errorDispatch();
					state = SocketState.CLOSED;
				} else if (status == SocketStatus.DISCONNECT
						&& !processor.isComet()) {
					// Do nothing here, just wait for it to get recycled
					// Don't do this for Comet we need to generate an end
					// event (see BZ 54022)
				} else if (processor.isAsync()
						|| state == SocketState.ASYNC_END) {
					state = processor.asyncDispatch(status);
				} else if (processor.isComet()) {
					state = processor.event(status);
				} else if (processor.getUpgradeInbound() != null) {
					state = processor.upgradeDispatch();
				} else if (processor.isUpgrade()) {
					state = processor.upgradeDispatch(status);
				} else {
					state = processor.process(wrapper);
				}

				if (state != SocketState.CLOSED && processor.isAsync()) {
					state = processor.asyncPostProcess();
				}

				if (state == SocketState.UPGRADING) {
					// Get the HTTP upgrade handler
					HttpUpgradeHandler httpUpgradeHandler = processor
							.getHttpUpgradeHandler();
					// Release the Http11 processor to be re-used
					release(wrapper, processor, false, false);
					// Create the upgrade processor
					processor = createUpgradeProcessor(wrapper,
							httpUpgradeHandler);
					// Mark the connection as upgraded
					wrapper.setUpgraded(true);
					// Associate with the processor with the connection
					connections.put(socket, processor);
					// Initialise the upgrade handler (which may trigger
					// some IO using the new protocol which is why the lines
					// above are necessary)
					// This cast should be safe. If it fails the error
					// handling for the surrounding try/catch will deal with
					// it.
					httpUpgradeHandler.init((WebConnection) processor);
				} else if (state == SocketState.UPGRADING_TOMCAT) {
					// Get the UpgradeInbound handler
					UpgradeInbound inbound = processor
							.getUpgradeInbound();
					// Release the Http11 processor to be re-used
					release(wrapper, processor, false, false);
					// Create the light-weight upgrade processor
					processor = createUpgradeProcessor(wrapper, inbound);
					inbound.onUpgradeComplete();
				}
				if (getLog().isDebugEnabled()) {
					getLog().debug(
							"Socket: [" + wrapper + "], Status in: [" + status
									+ "], State out: [" + state + "]");
				}
			} while (state == SocketState.ASYNC_END
					|| state == SocketState.UPGRADING
					|| state == SocketState.UPGRADING_TOMCAT);

			if (state == SocketState.LONG) {
				// In the middle of processing a request/response. Keep the
				// socket associated with the processor. Exact requirements
				// depend on type of long poll
				connections.put(socket, processor);
				longPoll(wrapper, processor);
			} else if (state == SocketState.OPEN) {
				// In keep-alive but between requests. OK to recycle
				// processor. Continue to poll for the next request.
				connections.remove(socket);
				release(wrapper, processor, false, true);
			} else if (state == SocketState.SENDFILE) {
				// Sendfile in progress. If it fails, the socket will be
				// closed. If it works, the socket will be re-added to the
				// poller
				connections.remove(socket);
				release(wrapper, processor, false, false);
			} else if (state == SocketState.UPGRADED) {
				// Need to keep the connection associated with the processor
				connections.put(socket, processor);
				// Don't add sockets back to the poller if this was a
				// non-blocking write otherwise the poller may trigger
				// multiple read events which may lead to thread starvation
				// in the connector. The write() method will add this socket
				// to the poller if necessary.
				if (status != SocketStatus.OPEN_WRITE) {
					longPoll(wrapper, processor);
				}
			} else {
				// Connection closed. OK to recycle the processor. Upgrade
				// processors are not recycled.
				connections.remove(socket);
				if (processor.isUpgrade()) {
					processor.getHttpUpgradeHandler().destroy();
				} else if (processor instanceof org.apache.coyote.http11.upgrade.UpgradeProcessor) {
					// NO-OP
				} else {
					release(wrapper, processor, true, false);
				}
			}
			return state;
		} catch (java.net.SocketException e) {
			// SocketExceptions are normal
			getLog().debug(
					AbstractProtocol.getSm().getString(
							"abstractConnectionHandler.socketexception.debug"),
					e);
		} catch (java.io.IOException e) {
			// IOExceptions are normal
			getLog().debug(
					AbstractProtocol.getSm().getString(
							"abstractConnectionHandler.ioexception.debug"), e);
		}
		// Future developers: if you discover any other
		// rare-but-nonfatal exceptions, catch them here, and log as
		// above.
		catch (Throwable e) {
			ExceptionUtils2.handleThrowable(e);
			// any other exception or error is odd. Here we log it
			// with "ERROR" level, so it will show up even on
			// less-than-verbose logs.
			getLog().error(
					AbstractProtocol.getSm().getString(
							"abstractConnectionHandler.error"), e);
		}
		// Make sure socket/processor is removed from the list of current
		// connections
		connections.remove(socket);
		// Don't try to add upgrade processors back into the pool
		if (!(processor instanceof org.apache.coyote.http11.upgrade.UpgradeProcessor)
				&& !processor.isUpgrade()) {
			release(wrapper, processor, true, false);
		}
		return SocketState.CLOSED;
	}

	protected abstract P createProcessor();

	protected abstract void initSsl(SocketWrapper<S> socket,
			Processor<S> processor);

	protected abstract void longPoll(SocketWrapper<S> socket,
			Processor<S> processor);

	protected abstract void release(SocketWrapper<S> socket,
			Processor<S> processor, boolean socketClosing, boolean addToPoller);

	/**
	 * @deprecated Will be removed in Tomcat 8.0.x.
	 */
	@Deprecated
	protected abstract Processor<S> createUpgradeProcessor(
			SocketWrapper<S> socket,
			UpgradeInbound inbound)
			throws IOException;

	protected abstract Processor<S> createUpgradeProcessor(
			SocketWrapper<S> socket, HttpUpgradeHandler httpUpgradeProcessor)
			throws IOException;

	protected void register(AbstractProcessor<S> processor) {
		if (getProtocol().getDomain() != null) {
			synchronized (this) {
				try {
					long count = registerCount.incrementAndGet();
					RequestInfo rp = processor.getRequest()
							.getRequestProcessor();
					rp.setGlobalProcessor(global);
					ObjectName rpName = new ObjectName(getProtocol()
							.getDomain()
							+ ":type=RequestProcessor,worker="
							+ getProtocol().getName()
							+ ",name="
							+ getProtocol().getProtocolName()
							+ "Request"
							+ count);
					if (getLog().isDebugEnabled()) {
						getLog().debug("Register " + rpName);
					}
					Registry2.getRegistry(null, null).registerComponent(rp,
							rpName, null);
					rp.setRpName(rpName);
				} catch (Exception e) {
					getLog().warn("Error registering request");
				}
			}
		}
	}

	protected void unregister(Processor<S> processor) {
		if (getProtocol().getDomain() != null) {
			synchronized (this) {
				try {
					Request2 r = processor.getRequest();
					if (r == null) {
						// Probably an UpgradeProcessor
						return;
					}
					RequestInfo rp = r.getRequestProcessor();
					rp.setGlobalProcessor(null);
					ObjectName rpName = rp.getRpName();
					if (getLog().isDebugEnabled()) {
						getLog().debug("Unregister " + rpName);
					}
					Registry2.getRegistry(null, null)
							.unregisterComponent(rpName);
					rp.setRpName(null);
				} catch (Exception e) {
					getLog().warn("Error unregistering request", e);
				}
			}
		}
	}

	public AtomicLong getRegisterCount() {
		return registerCount;
	}

	public void setRegisterCount(AtomicLong registerCount) {
		this.registerCount = registerCount;
	}

	public ConcurrentHashMap<S, Processor<S>> getConnections() {
		return connections;
	}

	public void setConnections(ConcurrentHashMap<S, Processor<S>> connections) {
		this.connections = connections;
	}

	public AbstractProtocolRecycledProcessors<P, S> getRecycledProcessors() {
		return recycledProcessors;
	}

	public void setRecycledProcessors(
			AbstractProtocolRecycledProcessors<P, S> recycledProcessors) {
		this.recycledProcessors = recycledProcessors;
	}

	public void setGlobal(RequestGroupInfo global) {
		this.global = global;
	}

}