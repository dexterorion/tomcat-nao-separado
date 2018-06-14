package org.apache.tomcat.util.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;

import org.apache.tomcat.util.ExceptionUtils2;

/**
 * This class is the equivalent of the Worker, but will simply use in an
 * external Executor thread pool.
 */
public class JloEndpointSocketProcessor implements Runnable {

    /**
	 * 
	 */
	private final JIoEndpoint jIoEndpoint;
	private SocketWrapper<Socket> socket = null;
    private SocketStatus status = null;

    public JloEndpointSocketProcessor(JIoEndpoint jIoEndpoint, SocketWrapper<Socket> socket) {
        this.jIoEndpoint = jIoEndpoint;
		if (socket==null) throw new NullPointerException();
        this.socket = socket;
    }

    public JloEndpointSocketProcessor(JIoEndpoint jIoEndpoint, SocketWrapper<Socket> socket, SocketStatus status) {
        this(jIoEndpoint, socket);
        this.status = status;
    }

    @Override
    public void run() {
        boolean launch = false;
        synchronized (socket) {
            try {
                SocketState state = SocketState.OPEN;

                try {
                    // SSL handshake
                    this.jIoEndpoint.getServerSocketFactory().handshake(socket.getSocket());
                } catch (Throwable t) {
                    ExceptionUtils2.handleThrowable(t);
                    if (jIoEndpoint.getLog().isDebugEnabled()) {
                        jIoEndpoint.getLog().debug(JIoEndpoint.getSm().getString("endpoint.err.handshake"), t);
                    }
                    // Tell to close the socket
                    state = SocketState.CLOSED;
                }

                if ((state != SocketState.CLOSED)) {
                    if (status == null) {
                        state = this.jIoEndpoint.getHandler().process(socket, SocketStatus.OPEN_READ);
                    } else {
                        state = this.jIoEndpoint.getHandler().process(socket,status);
                    }
                }
                if (state == SocketState.CLOSED) {
                    // Close socket
                    if (jIoEndpoint.getLog().isTraceEnabled()) {
                        jIoEndpoint.getLog().trace("Closing socket:"+socket);
                    }
                    this.jIoEndpoint.countDownConnection();
                    try {
                        socket.getSocket().close();
                    } catch (IOException e) {
                        // Ignore
                    }
                } else if (state == SocketState.OPEN ||
                        state == SocketState.UPGRADING ||
                        state == SocketState.UPGRADING_TOMCAT  ||
                        state == SocketState.UPGRADED){
                    socket.setKeptAlive(true);
                    socket.access();
                    launch = true;
                } else if (state == SocketState.LONG) {
                    socket.access();
                    this.jIoEndpoint.getWaitingRequestsData().add(socket);
                }
            } finally {
                if (launch) {
                    try {
                        this.jIoEndpoint.getExecutor().execute(new JloEndpointSocketProcessor(this.jIoEndpoint, socket, SocketStatus.OPEN_READ));
                    } catch (RejectedExecutionException x) {
                        jIoEndpoint.getLog().warn("Socket reprocessing request was rejected for:"+socket,x);
                        try {
                            //unable to handle connection at this time
                            this.jIoEndpoint.getHandler().process(socket, SocketStatus.DISCONNECT);
                        } finally {
                            this.jIoEndpoint.countDownConnection();
                        }


                    } catch (NullPointerException npe) {
                        if (this.jIoEndpoint.isRunning()) {
                            jIoEndpoint.getLog().error(JIoEndpoint.getSm().getString("endpoint.launch.fail"),
                                    npe);
                        }
                    }
                }
            }
        }
        socket = null;
        // Finish up this request
    }

}