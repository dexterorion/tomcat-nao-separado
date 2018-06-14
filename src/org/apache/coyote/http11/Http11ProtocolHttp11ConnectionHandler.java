package org.apache.coyote.http11;

import java.io.IOException;
import java.net.Socket;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.AbstractProtocolAbstractConnectionHandler;
import org.apache.coyote.Processor;
import org.apache.coyote.http11.upgrade.BioProcessor;
import org.apache.coyote.http11.upgrade.UpgradeBioProcessor;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.apache.tomcat.util.net.JloEndpointHandler;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SocketWrapper;

public class Http11ProtocolHttp11ConnectionHandler
        extends AbstractProtocolAbstractConnectionHandler<Socket, Http11Processor> implements JloEndpointHandler {

    private Http11Protocol proto;
        
    public Http11ProtocolHttp11ConnectionHandler(Http11Protocol proto) {
        this.proto = proto;
    }

    @Override
    protected AbstractProtocol<Socket> getProtocol() {
        return proto;
    }

    @Override
    protected Log getLog() {
        return Http11Protocol.getLogVariable();
    }
    
    @Override
    public SSLImplementation getSslImplementation() {
        return proto.getSslImplementation();
    }

    /**
     * Expected to be used by the handler once the processor is no longer
     * required.
     * 
     * @param socket            Not used in BIO
     * @param processor
     * @param isSocketClosing   Not used in HTTP
     * @param addToPoller       Not used in BIO
     */
    @Override
    public void release(SocketWrapper<Socket> socket,
            Processor<Socket> processor, boolean isSocketClosing,
            boolean addToPoller) {
        processor.recycle(isSocketClosing);
        getRecycledProcessors().offer(processor);
    }

    @Override
    protected void initSsl(SocketWrapper<Socket> socket,
            Processor<Socket> processor) {
        if (proto.isSSLEnabled() && (proto.getSslImplementation() != null)) {
            processor.setSslSupport(
                    proto.getSslImplementation().getSSLSupport(
                            socket.getSocket()));
        } else {
            processor.setSslSupport(null);
        }

    }

    @Override
    protected void longPoll(SocketWrapper<Socket> socket,
            Processor<Socket> processor) {
        // NO-OP
    }

    @Override
    protected Http11Processor createProcessor() {
        Http11Processor processor = new Http11Processor(
                proto.getMaxHttpHeaderSize(), (JIoEndpoint)proto.getEndpoint(),
                proto.getMaxTrailerSize(),proto.getMaxExtensionSize(),
                proto.getMaxSwallowSize());
        processor.setAdapter(proto.getAdapter());
        processor.setMaxKeepAliveRequests(proto.getMaxKeepAliveRequests());
        processor.setKeepAliveTimeout(proto.getKeepAliveTimeout());
        processor.setConnectionUploadTimeout(
                proto.getConnectionUploadTimeout());
        processor.setDisableUploadTimeout(proto.getDisableUploadTimeout());
        processor.setCompressionMinSize(proto.getCompressionMinSize());
        processor.setCompression(proto.getCompression());
        processor.setNoCompressionUserAgents(proto.getNoCompressionUserAgents());
        processor.setCompressableMimeTypes(proto.getCompressableMimeTypes());
        processor.setRestrictedUserAgents(proto.getRestrictedUserAgents());
        processor.setSocketBuffer(proto.getSocketBuffer());
        processor.setMaxSavePostSize(proto.getMaxSavePostSize());
        processor.setServer(proto.getServer());
        processor.setDisableKeepAlivePercentage(
                proto.getDisableKeepAlivePercentage());
        register(processor);
        return processor;
    }

    /**
     * @deprecated  Will be removed in Tomcat 8.0.x.
     */
    @Deprecated
    @Override
    protected Processor<Socket> createUpgradeProcessor(
            SocketWrapper<Socket> socket,
            org.apache.coyote.http11.upgrade.UpgradeInbound inbound)
            throws IOException {
        return new UpgradeBioProcessor(
                socket, inbound);
    }
    
    @Override
    protected Processor<Socket> createUpgradeProcessor(
            SocketWrapper<Socket> socket,
            HttpUpgradeHandler httpUpgradeProcessor)
            throws IOException {
        return new BioProcessor(socket, httpUpgradeProcessor,
                proto.getUpgradeAsyncWriteBufferSize());
    }
}