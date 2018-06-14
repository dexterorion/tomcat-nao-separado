package org.apache.tomcat.util.net;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;

import org.apache.tomcat.util.ExceptionUtils2;

// --------------------------------------------------- Acceptor Inner Class
/**
 * The background thread that listens for incoming TCP/IP connections and
 * hands them off to an appropriate processor.
 */
public class NioEndpointAcceptor extends AbstractEndpointAcceptor {

    /**
	 * 
	 */
	private final NioEndpoint nioEndpoint;

	/**
	 * @param nioEndpoint
	 */
	public NioEndpointAcceptor(NioEndpoint nioEndpoint) {
		this.nioEndpoint = nioEndpoint;
	}

	@Override
    public void run() {

        int errorDelay = 0;

        // Loop until we receive a shutdown command
        while (this.nioEndpoint.isRunning()) {

            // Loop if endpoint is paused
            while (this.nioEndpoint.isPaused() && this.nioEndpoint.isRunning()) {
                setState(AcceptorState.PAUSED);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            if (!this.nioEndpoint.isRunning()) {
                break;
            }
            setState(AcceptorState.RUNNING);

            try {
                //if we have reached max connections, wait
                this.nioEndpoint.countUpOrAwaitConnection();

                SocketChannel socket = null;
                try {
                    // Accept the next incoming connection from the server
                    // socket
                    socket = this.nioEndpoint.getServerSock().accept();
                } catch (IOException ioe) {
                    //we didn't get a socket
                    this.nioEndpoint.countDownConnection();
                    // Introduce delay if necessary
                    errorDelay = this.nioEndpoint.handleExceptionWithDelay(errorDelay);
                    // re-throw
                    throw ioe;
                }
                // Successful accept, reset the error delay
                errorDelay = 0;

                // setSocketOptions() will add channel to the poller
                // if successful
                if (this.nioEndpoint.isRunning() && !this.nioEndpoint.isPaused()) {
                    if (!this.nioEndpoint.setSocketOptions(socket)) {
                        this.nioEndpoint.countDownConnection();
                        this.nioEndpoint.closeSocket(socket);
                    }
                } else {
                    this.nioEndpoint.countDownConnection();
                    this.nioEndpoint.closeSocket(socket);
                }
            } catch (SocketTimeoutException sx) {
                // Ignore: Normal condition
            } catch (IOException x) {
                if (this.nioEndpoint.isRunning()) {
                    NioEndpoint.getLogVariable().error(NioEndpoint.getSm().getString("endpoint.accept.fail"), x);
                }
            } catch (OutOfMemoryError oom) {
                try {
                    this.nioEndpoint.setOomParachuteData(null);
                    this.nioEndpoint.releaseCaches();
                    NioEndpoint.getLogVariable().error("", oom);
                }catch ( Throwable oomt ) {
                    try {
                        try {
                            System.err.println(NioEndpoint.getOomparachutemsg());
                            oomt.printStackTrace();
                        }catch (Throwable letsHopeWeDontGetHere){
                            ExceptionUtils2.handleThrowable(letsHopeWeDontGetHere);
                        }
                    }catch (Throwable letsHopeWeDontGetHere){
                        ExceptionUtils2.handleThrowable(letsHopeWeDontGetHere);
                    }
                }
            } catch (Throwable t) {
                ExceptionUtils2.handleThrowable(t);
                NioEndpoint.getLogVariable().error(NioEndpoint.getSm().getString("endpoint.accept.fail"), t);
            }
        }
        setState(AcceptorState.ENDED);
    }
}