package org.apache.tomcat.util.net;

import java.io.IOException;
import java.net.Socket;

import org.apache.tomcat.util.ExceptionUtils2;

// --------------------------------------------------- Acceptor Inner Class
/**
 * The background thread that listens for incoming TCP/IP connections and
 * hands them off to an appropriate processor.
 */
public class JloEndpointAcceptor extends AbstractEndpointAcceptor {

    /**
	 * 
	 */
	private final JIoEndpoint jIoEndpoint;

	/**
	 * @param jIoEndpoint
	 */
	public JloEndpointAcceptor(JIoEndpoint jIoEndpoint) {
		this.jIoEndpoint = jIoEndpoint;
	}

	@Override
    public void run() {

        int errorDelay = 0;

        // Loop until we receive a shutdown command
        while (this.jIoEndpoint.isRunning()) {

            // Loop if endpoint is paused
            while (this.jIoEndpoint.isPaused() && this.jIoEndpoint.isRunning()) {
                setState(AcceptorState.PAUSED);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            if (!this.jIoEndpoint.isRunning()) {
                break;
            }
            setState(AcceptorState.RUNNING);

            try {
                //if we have reached max connections, wait
                this.jIoEndpoint.countUpOrAwaitConnection();

                Socket socket = null;
                try {
                    // Accept the next incoming connection from the server
                    // socket
                    socket = this.jIoEndpoint.getServerSocketFactory().acceptSocket(this.jIoEndpoint.getServerSocket());
                } catch (IOException ioe) {
                    this.jIoEndpoint.countDownConnection();
                    // Introduce delay if necessary
                    errorDelay = this.jIoEndpoint.handleExceptionWithDelay(errorDelay);
                    // re-throw
                    throw ioe;
                }
                // Successful accept, reset the error delay
                errorDelay = 0;

                // Configure the socket
                if (this.jIoEndpoint.isRunning() && !this.jIoEndpoint.isPaused() && this.jIoEndpoint.setSocketOptions(socket)) {
                    // Hand this socket off to an appropriate processor
                    if (!this.jIoEndpoint.processSocket(socket)) {
                        this.jIoEndpoint.countDownConnection();
                        // Close socket right away
                        this.jIoEndpoint.closeSocket(socket);
                    }
                } else {
                    this.jIoEndpoint.countDownConnection();
                    // Close socket right away
                    this.jIoEndpoint.closeSocket(socket);
                }
            } catch (IOException x) {
                if (this.jIoEndpoint.isRunning()) {
                    jIoEndpoint.getLog().error(JIoEndpoint.getSm().getString("endpoint.accept.fail"), x);
                }
            } catch (NullPointerException npe) {
                if (this.jIoEndpoint.isRunning()) {
                    jIoEndpoint.getLog().error(JIoEndpoint.getSm().getString("endpoint.accept.fail"), npe);
                }
            } catch (Throwable t) {
                ExceptionUtils2.handleThrowable(t);
                jIoEndpoint.getLog().error(JIoEndpoint.getSm().getString("endpoint.accept.fail"), t);
            }
        }
        setState(AcceptorState.ENDED);
    }
}