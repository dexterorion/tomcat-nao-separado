package org.apache.tomcat.util.net;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;

import org.apache.tomcat.util.ExceptionUtils2;

public class NioEndpointSocketProcessor implements Runnable {

    /**
	 * 
	 */
	private final NioEndpoint nioEndpoint;
	private NioChannel socket = null;
    private SocketStatus status = null;

    public NioEndpointSocketProcessor(NioEndpoint nioEndpoint, NioChannel socket, SocketStatus status) {
        this.nioEndpoint = nioEndpoint;
		reset(socket,status);
    }

    public void reset(NioChannel socket, SocketStatus status) {
        this.socket = socket;
        this.status = status;
    }

    @Override
    public void run() {
        SelectionKey key = socket.getIOChannel().keyFor(
                socket.getPoller().getSelector());
        NioEndpointKeyAttachment ka = null;

        if (key != null) {
            ka = (NioEndpointKeyAttachment)key.attachment();
        }

        // Upgraded connections need to allow multiple threads to access the
        // connection at the same time to enable blocking IO to be used when
        // NIO has been configured
        if (ka != null && ka.isUpgraded() &&
                SocketStatus.OPEN_WRITE == status) {
            synchronized (ka.getWriteThreadLock()) {
                doRun(key, ka);
            }
        } else {
            synchronized (socket) {
                doRun(key, ka);
            }
        }
    }

    private void doRun(SelectionKey key, NioEndpointKeyAttachment ka) {
        try {
            int handshake = -1;

            try {
                if (key != null) {
                    // For STOP there is no point trying to handshake as the
                    // Poller has been stopped.
                    if (socket.isHandshakeComplete() ||
                            status == SocketStatus.STOP) {
                        handshake = 0;
                    } else {
                        handshake = socket.handshake(
                                key.isReadable(), key.isWritable());
                        // The handshake process reads/writes from/to the
                        // socket. status may therefore be OPEN_WRITE once
                        // the handshake completes. However, the handshake
                        // happens when the socket is opened so the status
                        // must always be OPEN_READ after it completes. It
                        // is OK to always set this as it is only used if
                        // the handshake completes.
                        status = SocketStatus.OPEN_READ;
                    }
                }
            }catch ( IOException x ) {
                handshake = -1;
                if ( NioEndpoint.getLogVariable().isDebugEnabled() ) NioEndpoint.getLogVariable().debug("Error during SSL handshake",x);
            }catch ( CancelledKeyException ckx ) {
                handshake = -1;
            }
            if ( handshake == 0 ) {
                SocketState state = SocketState.OPEN;
                // Process the request from this socket
                if (status == null) {
                    state = this.nioEndpoint.getHandler().process(ka, SocketStatus.OPEN_READ);
                } else {
                    state = this.nioEndpoint.getHandler().process(ka, status);
                }
                if (state == SocketState.CLOSED) {
                    // Close socket and pool
                    try {
                        if (ka!=null) ka.setComet(false);
                        socket.getPoller().cancelledKey(key, SocketStatus.ERROR, false);
                        if (this.nioEndpoint.isRunning() && !this.nioEndpoint.isPaused()) {
                            this.nioEndpoint.getNioChannels().offer(socket);
                        }
                        socket = null;
                        if (this.nioEndpoint.isRunning() && !this.nioEndpoint.isPaused() && ka!=null) {
                            this.nioEndpoint.getKeyCache().offer(ka);
                        }
                        ka = null;
                    } catch ( Exception x ) {
                        NioEndpoint.getLogVariable().error("",x);
                    }
                }
            } else if (handshake == -1 ) {
                if (key != null) {
                    socket.getPoller().cancelledKey(key, SocketStatus.DISCONNECT, false);
                }
                this.nioEndpoint.getNioChannels().offer(socket);
                socket = null;
                if ( ka!=null ) this.nioEndpoint.getKeyCache().offer(ka);
                ka = null;
            } else {
                ka.getPoller().add(socket, handshake);
            }
        }catch(CancelledKeyException cx) {
            socket.getPoller().cancelledKey(key,null,false);
        } catch (OutOfMemoryError oom) {
            try {
                this.nioEndpoint.setOomParachuteData(null);
                NioEndpoint.getLogVariable().error("", oom);
                if (socket != null) {
                    socket.getPoller().cancelledKey(key,SocketStatus.ERROR, false);
                }
                this.nioEndpoint.releaseCaches();
            }catch ( Throwable oomt ) {
                try {
                    System.err.println(NioEndpoint.getOomparachutemsg());
                    oomt.printStackTrace();
                }catch (Throwable letsHopeWeDontGetHere){
                    ExceptionUtils2.handleThrowable(letsHopeWeDontGetHere);
                }
            }
        } catch (VirtualMachineError vme) {
            ExceptionUtils2.handleThrowable(vme);
        }catch ( Throwable t ) {
            NioEndpoint.getLogVariable().error("",t);
            if (socket != null) {
                socket.getPoller().cancelledKey(key,SocketStatus.ERROR,false);
            }
        } finally {
            socket = null;
            status = null;
            //return to cache
            if (this.nioEndpoint.isRunning() && !this.nioEndpoint.isPaused()) {
                this.nioEndpoint.getProcessorCache().offer(this);
            }
        }
    }
}