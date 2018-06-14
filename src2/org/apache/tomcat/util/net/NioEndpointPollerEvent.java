package org.apache.tomcat.util.net;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;

/**
 *
 * PollerEvent, cacheable object for poller events to avoid GC
 */
public class NioEndpointPollerEvent implements Runnable {

    private NioChannel socket;
    private int interestOps;
    private NioEndpointKeyAttachment key;
    public NioEndpointPollerEvent(NioChannel ch, NioEndpointKeyAttachment k, int intOps) {
        reset(ch, k, intOps);
    }

    public void reset(NioChannel ch, NioEndpointKeyAttachment k, int intOps) {
        socket = ch;
        interestOps = intOps;
        key = k;
    }

    public void reset() {
        reset(null, null, 0);
    }

    @Override
    public void run() {
        if ( interestOps == NioEndpoint.getOpRegister() ) {
            try {
                socket.getIOChannel().register(socket.getPoller().getSelector(), SelectionKey.OP_READ, key);
            } catch (Exception x) {
                NioEndpoint.getLogVariable().error("", x);
            }
        } else {
            final SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
            try {
                boolean cancel = false;
                if (key != null) {
                    final NioEndpointKeyAttachment att = (NioEndpointKeyAttachment) key.attachment();
                    if ( att!=null ) {
                        //handle callback flag
                        if (att.isComet() && (interestOps & NioEndpoint.getOpCallback()) == NioEndpoint.getOpCallback() ) {
                            att.setCometNotify(true);
                        } else {
                            att.setCometNotify(false);
                        }
                        interestOps = (interestOps & (~NioEndpoint.getOpCallback()));//remove the callback flag
                        att.access();//to prevent timeout
                        //we are registering the key to start with, reset the fairness counter.
                        int ops = key.interestOps() | interestOps;
                        att.interestOps(ops);
                        key.interestOps(ops);
                    } else {
                        cancel = true;
                    }
                } else {
                    cancel = true;
                }
                if ( cancel ) socket.getPoller().cancelledKey(key,SocketStatus.ERROR,false);
            }catch (CancelledKeyException ckx) {
                try {
                    socket.getPoller().cancelledKey(key,SocketStatus.DISCONNECT,true);
                }catch (Exception ignore) {}
            }
        }//end if
    }

    @Override
    public String toString() {
        return super.toString()+"[intOps="+this.interestOps+"]";
    }
}