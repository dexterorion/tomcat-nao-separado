package org.apache.coyote.ajp;

import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.Processor;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioEndpointHandler;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SocketWrapper;

public class AjpNioProtocolAjpConnectionHandler
        extends AbstractAjpProtocolAbstractAjpConnectionHandler<NioChannel, AjpNioProcessor>
        implements NioEndpointHandler {

    private AjpNioProtocol proto;

    public AjpNioProtocolAjpConnectionHandler(AjpNioProtocol proto) {
        this.proto = proto;
    }

    @Override
    protected AbstractProtocol<NioChannel> getProtocol() {
        return proto;
    }

    @Override
    protected Log getLog() {
        return AjpNioProtocol.getLogVariable();
    }

    @Override
    public SSLImplementation getSslImplementation() {
        // AJP does not support SSL
        return null;
    }

    /**
     * Expected to be used by the Poller to release resources on socket
     * close, errors etc.
     */
    @Override
    public void release(SocketChannel socket) {
        if (AjpNioProtocol.getLogVariable().isDebugEnabled()) 
            AjpNioProtocol.getLogVariable().debug("Iterating through our connections to release a socket channel:"+socket);
        boolean released = false;
        Iterator<java.util.Map.Entry<NioChannel, Processor<NioChannel>>> it = getConnections().entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<NioChannel, Processor<NioChannel>> entry = it.next();
            if (entry.getKey().getIOChannel()==socket) {
                it.remove();
                Processor<NioChannel> result = entry.getValue();
                result.recycle(true);
                unregister(result);
                released = true;
                break;
            }
        }
        if (AjpNioProtocol.getLogVariable().isDebugEnabled()) 
            AjpNioProtocol.getLogVariable().debug("Done iterating through our connections to release a socket channel:"+socket +" released:"+released);
    }
    
    /**
     * Expected to be used by the Poller to release resources on socket
     * close, errors etc.
     */
    @Override
    public void release(SocketWrapper<NioChannel> socket) {
        Processor<NioChannel> processor =
                getConnections().remove(socket.getSocket());
        if (processor != null) {
            processor.recycle(true);
            getRecycledProcessors().offer(processor);
        }
    }

    /**
     * Expected to be used by the handler once the processor is no longer
     * required.
     */
    @Override
    public void release(SocketWrapper<NioChannel> socket,
            Processor<NioChannel> processor, boolean isSocketClosing,
            boolean addToPoller) {
        processor.recycle(isSocketClosing);
        getRecycledProcessors().offer(processor);
        if (addToPoller) {
            socket.getSocket().getPoller().add(socket.getSocket());
        }
    }


    @Override
    protected AjpNioProcessor createProcessor() {
        AjpNioProcessor processor = new AjpNioProcessor(proto.getPacketSize(), (NioEndpoint)proto.getEndpoint());
        processor.setAdapter(proto.getAdapter());
        processor.setTomcatAuthentication(proto.getTomcatAuthentication());
        processor.setRequiredSecret(proto.getRequiredSecret());
        processor.setKeepAliveTimeout(proto.getKeepAliveTimeout());
        processor.setClientCertProvider(proto.getClientCertProvider());
        register(processor);
        return processor;
    }
}