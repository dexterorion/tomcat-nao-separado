package org.apache.coyote.ajp;

import java.net.Socket;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.Processor;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.apache.tomcat.util.net.JloEndpointHandler;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SocketWrapper;

public class AjpProtocolAjpConnectionHandler
        extends AbstractAjpProtocolAbstractAjpConnectionHandler<Socket,AjpProcessor>
        implements JloEndpointHandler {

    private AjpProtocol proto;

    public AjpProtocolAjpConnectionHandler(AjpProtocol proto) {
        this.proto = proto;
    }
    
    @Override
    protected AbstractProtocol<Socket> getProtocol() {
        return proto;
    }

    @Override
    protected Log getLog() {
        return AjpProtocol.getLogVariable();
    }

    @Override
    public SSLImplementation getSslImplementation() {
        // AJP does not support SSL
        return null;
    }

    /**
     * Expected to be used by the handler once the processor is no longer
     * required.
     * 
     * @param socket            Ignored for BIO
     * @param processor
     * @param isSocketClosing
     * @param addToPoller       Ignored for BIO
     */
    @Override
    public void release(SocketWrapper<Socket> socket,
            Processor<Socket> processor, boolean isSocketClosing,
            boolean addToPoller) {
        processor.recycle(isSocketClosing);
        getRecycledProcessors().offer(processor);
    }


    @Override
    protected AjpProcessor createProcessor() {
        AjpProcessor processor = new AjpProcessor(proto.getPacketSize(), (JIoEndpoint)proto.getEndpoint());
        processor.setAdapter(proto.getAdapter());
        processor.setTomcatAuthentication(proto.getTomcatAuthentication());
        processor.setRequiredSecret(proto.getRequiredSecret());
        processor.setKeepAliveTimeout(proto.getKeepAliveTimeout());
        processor.setClientCertProvider(proto.getClientCertProvider());
        register(processor);
        return processor;
    }
}