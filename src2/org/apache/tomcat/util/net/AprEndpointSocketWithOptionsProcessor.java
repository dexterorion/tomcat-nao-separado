package org.apache.tomcat.util.net;

public class AprEndpointSocketWithOptionsProcessor implements Runnable {

	/**
 * 
 */
	private final AprEndpoint aprEndpoint;
	private SocketWrapper<Long> socket = null;

	public AprEndpointSocketWithOptionsProcessor(AprEndpoint aprEndpoint,
			SocketWrapper<Long> socket) {
		this.aprEndpoint = aprEndpoint;
		this.socket = socket;
	}

	@Override
	public void run() {

		synchronized (socket) {
			if (!this.aprEndpoint.getDeferAccept()) {
				if (this.aprEndpoint.setSocketOptions(socket.getSocket()
						.longValue())) {
					this.aprEndpoint.getPoller().add(
							socket.getSocket().longValue(),
							this.aprEndpoint.getSoTimeout(), true, false);
				} else {
					// Close socket and pool
					this.aprEndpoint
							.closeSocket(socket.getSocket().longValue());
					socket = null;
				}
			} else {
				// Process the request from this socket
				if (!this.aprEndpoint.setSocketOptions(socket.getSocket()
						.longValue())) {
					// Close socket and pool
					this.aprEndpoint
							.closeSocket(socket.getSocket().longValue());
					socket = null;
					return;
				}
				// Process the request from this socket
				SocketState state = this.aprEndpoint.getHandler().process(
						socket, SocketStatus.OPEN_READ);
				if (state == SocketState.CLOSED) {
					// Close socket and pool
					this.aprEndpoint
							.closeSocket(socket.getSocket().longValue());
					socket = null;
				} else if (state == SocketState.LONG) {
					socket.access();
					if (socket.isAsync()) {
						this.aprEndpoint.getWaitingRequests().add(socket);
					}
				}
			}
		}
	}
}