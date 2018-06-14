package org.apache.tomcat.util.net;

public class AprEndpointSocketProcessor implements Runnable {

    /**
 * 
 */
private final AprEndpoint aprEndpoint;
	private final SocketWrapper<Long> socket;
    private final SocketStatus status;

    public AprEndpointSocketProcessor(AprEndpoint aprEndpoint, SocketWrapper<Long> socket,
            SocketStatus status) {
        this.aprEndpoint = aprEndpoint;
		this.socket = socket;
        if (status == null) {
            // Should never happen
            throw new NullPointerException();
        }
        this.status = status;
    }

    @Override
    public void run() {

        // Upgraded connections need to allow multiple threads to access the
        // connection at the same time to enable blocking IO to be used when
        // Servlet 3.1 NIO has been configured
        if (socket.isUpgraded() && SocketStatus.OPEN_WRITE == status) {
            synchronized (socket.getWriteThreadLock()) {
                doRun();
            }
        } else {
            synchronized (socket) {
                doRun();
            }
        }
    }

    private void doRun() {
        // Process the request from this socket
        if (socket.getSocket() == null) {
            // Closed in another thread
            return;
        }
        SocketState state = this.aprEndpoint.getHandler().process(socket, status);
        if (state == SocketState.CLOSED) {
            // Close socket and pool
            this.aprEndpoint.closeSocket(socket.getSocket().longValue());
            socket.setSocket(null);
        } else if (state == SocketState.LONG) {
            socket.access();
            if (socket.isAsync()) {
                this.aprEndpoint.getWaitingRequests().add(socket);
            }
        } else if (state == SocketState.ASYNC_END) {
            socket.access();
            AprEndpointSocketProcessor proc = new AprEndpointSocketProcessor(this.aprEndpoint, socket,
                    SocketStatus.OPEN_READ);
            this.aprEndpoint.getExecutor().execute(proc);
        }
    }
}