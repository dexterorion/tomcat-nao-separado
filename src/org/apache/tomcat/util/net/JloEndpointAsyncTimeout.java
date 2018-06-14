package org.apache.tomcat.util.net;

import java.net.Socket;
import java.util.Iterator;

/**
 * Async timeout thread
 */
public class JloEndpointAsyncTimeout implements Runnable {
    /**
	 * 
	 */
	private final JIoEndpoint jIoEndpoint;

	/**
	 * @param jIoEndpoint
	 */
	public JloEndpointAsyncTimeout(JIoEndpoint jIoEndpoint) {
		this.jIoEndpoint = jIoEndpoint;
	}

	/**
     * The background thread that checks async requests and fires the
     * timeout if there has been no activity.
     */
    @Override
    public void run() {

        // Loop until we receive a shutdown command
        while (this.jIoEndpoint.isRunning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            long now = System.currentTimeMillis();
            Iterator<SocketWrapper<Socket>> sockets =
                this.jIoEndpoint.getWaitingRequestsData().iterator();
            while (sockets.hasNext()) {
                SocketWrapper<Socket> socket = sockets.next();
                long access = socket.getLastAccess();
                if (socket.getTimeout() > 0 &&
                        (now-access)>socket.getTimeout()) {
                    this.jIoEndpoint.processSocketAsync(socket,SocketStatus.TIMEOUT);
                }
            }

            // Loop if endpoint is paused
            while (this.jIoEndpoint.isPaused() && this.jIoEndpoint.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

        }
    }
}