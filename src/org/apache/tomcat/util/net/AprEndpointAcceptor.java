package org.apache.tomcat.util.net;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.util.ExceptionUtils2;

// --------------------------------------------------- Acceptor Inner Class
/**
 * The background thread that listens for incoming TCP/IP connections and
 * hands them off to an appropriate processor.
 */
public class AprEndpointAcceptor extends AbstractEndpointAcceptor {

    /**
	 * 
	 */
	private final AprEndpoint aprEndpoint;

	/**
	 * @param aprEndpoint
	 */
	public AprEndpointAcceptor(AprEndpoint aprEndpoint) {
		this.aprEndpoint = aprEndpoint;
	}

	private final Log log = LogFactory.getLog(AprEndpointAcceptor.class);

    @Override
    public void run() {

        int errorDelay = 0;

        // Loop until we receive a shutdown command
        while (this.aprEndpoint.isRunning()) {

            // Loop if endpoint is paused
            while (this.aprEndpoint.isPaused() && this.aprEndpoint.isRunning()) {
                setState(AcceptorState.PAUSED);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            if (!this.aprEndpoint.isRunning()) {
                break;
            }
            setState(AcceptorState.RUNNING);

            try {
                //if we have reached max connections, wait
                this.aprEndpoint.countUpOrAwaitConnection();

                long socket = 0;
                try {
                    // Accept the next incoming connection from the server
                    // socket
                    socket = Socket.accept(this.aprEndpoint.getServerSock());
                    if (log.isDebugEnabled()) {
                        long sa = Address.get(Socket.getAprRemote(), socket);
                        Sockaddr addr = Address.getInfo(sa);
                        log.debug(AprEndpoint.getSm().getString("endpoint.apr.remoteport",
                                Long.valueOf(socket),
                                Long.valueOf(addr.getPort())));
                    }
                } catch (Exception e) {
                    //we didn't get a socket
                    this.aprEndpoint.countDownConnection();
                    // Introduce delay if necessary
                    errorDelay = this.aprEndpoint.handleExceptionWithDelay(errorDelay);
                    // re-throw
                    throw e;
                }
                // Successful accept, reset the error delay
                errorDelay = 0;

                if (this.aprEndpoint.isRunning() && !this.aprEndpoint.isPaused()) {
                    // Hand this socket off to an appropriate processor
                    if (!this.aprEndpoint.processSocketWithOptions(socket)) {
                        // Close socket right away
                        this.aprEndpoint.closeSocket(socket);
                    }
                } else {
                    // Close socket right away
                    // No code path could have added the socket to the
                    // Poller so use destroySocket()
                    this.aprEndpoint.destroySocket(socket);
                }
            } catch (Throwable t) {
                ExceptionUtils2.handleThrowable(t);
                if (this.aprEndpoint.isRunning()) {
                    String msg = AprEndpoint.getSm().getString("endpoint.accept.fail");
                    if (t instanceof Error) {
                        Error e = (Error) t;
                        if (e.getError() == 233) {
                            // Not an error on HP-UX so log as a warning
                            // so it can be filtered out on that platform
                            // See bug 50273
                            log.warn(msg, t);
                        } else {
                            log.error(msg, t);
                        }
                    } else {
                            log.error(msg, t);
                    }
                }
            }
            // The processor will recycle itself when it finishes
        }
        setState(AcceptorState.ENDED);
    }
}