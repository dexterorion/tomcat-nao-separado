package org.apache.tomcat.util.net;

import java.util.Iterator;

/**
 * Async timeout thread
 */
public class AprEndpointAsyncTimeout implements Runnable {

    /**
	 * 
	 */
	private final AprEndpoint aprEndpoint;

	/**
	 * @param aprEndpoint
	 */
	public AprEndpointAsyncTimeout(AprEndpoint aprEndpoint) {
		this.aprEndpoint = aprEndpoint;
	}


	private volatile boolean asyncTimeoutRunning = true;

    /**
     * The background thread that checks async requests and fires the
     * timeout if there has been no activity.
     */
    @Override
    public void run() {

        // Loop until we receive a shutdown command
        while (asyncTimeoutRunning) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            long now = System.currentTimeMillis();
            Iterator<SocketWrapper<Long>> sockets =
                this.aprEndpoint.getWaitingRequests().iterator();
            while (sockets.hasNext()) {
                SocketWrapper<Long> socket = sockets.next();
                if (socket.isAsync()) {
                    long access = socket.getLastAccess();
                    if (socket.getTimeout() > 0 &&
                            (now-access)>socket.getTimeout()) {
                        this.aprEndpoint.processSocketAsync(socket,SocketStatus.TIMEOUT);
                    }
                }
            }

            // Loop if endpoint is paused
            while (this.aprEndpoint.isPaused() && asyncTimeoutRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

        }
    }


    protected void stop() {
        asyncTimeoutRunning = false;
    }
}