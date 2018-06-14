package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.AbstractProtocolAbstractConnectionHandler;
import org.apache.coyote.Processor;
import org.apache.coyote.http11.upgrade.AprProcessor;
import org.apache.coyote.http11.upgrade.UpgradeAprProcessor;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.tomcat.util.net.AprEndpointHandler;
import org.apache.tomcat.util.net.AprEndpointPoller;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.coyote.http11.upgrade.UpgradeInbound;

public class Http11AprProtocolHttp11ConnectionHandler
        extends AbstractProtocolAbstractConnectionHandler<Long,Http11AprProcessor> implements AprEndpointHandler {
    
    private Http11AprProtocol proto;
    
    public Http11AprProtocolHttp11ConnectionHandler(Http11AprProtocol proto) {
        this.proto = proto;
    }

    @Override
    protected AbstractProtocol<Long> getProtocol() {
        return proto;
    }

    @Override
    protected Log getLog() {
        return Http11AprProtocol.getLogVariable();
    }
    
    @Override
    public void recycle() {
        getRecycledProcessors().clear();
    }

    /**
     * Expected to be used by the handler once the processor is no longer
     * required.
     * 
     * @param socket
     * @param processor
     * @param isSocketClosing   Not used in HTTP
     * @param addToPoller
     */
    @Override
    public void release(SocketWrapper<Long> socket,
            Processor<Long> processor, boolean isSocketClosing,
            boolean addToPoller) {
        processor.recycle(isSocketClosing);
        getRecycledProcessors().offer(processor);
        if (addToPoller && proto.getEndpoint().isRunning()) {
            ((AprEndpoint)proto.getEndpoint()).getPoller().add(
                    socket.getSocket().longValue(),
                    proto.getEndpoint().getKeepAliveTimeout(), true, false);
        }
    }

    @Override
    protected void initSsl(SocketWrapper<Long> socket,
            Processor<Long> processor) {
        // NOOP for APR
    }

    @SuppressWarnings("deprecation") // Inbound/Outbound based upgrade
    @Override
    protected void longPoll(SocketWrapper<Long> socket,
            Processor<Long> processor) {

        if (processor.isAsync()) {
            // Async
            socket.setAsync(true);
        } else if (processor.isComet()) {
            // Comet
            if (proto.getEndpoint().isRunning()) {
                socket.setComet(true);
                ((AprEndpoint) proto.getEndpoint()).getPoller().add(
                        socket.getSocket().longValue(),
                        proto.getEndpoint().getSoTimeout(), true, false);
            } else {
                // Process a STOP directly
                ((AprEndpoint) proto.getEndpoint()).processSocket(
                        socket.getSocket().longValue(),
                        SocketStatus.STOP);
            }
        } else if (processor.isUpgrade()) {
            // Upgraded
        	AprEndpointPoller p = ((AprEndpoint) proto.getEndpoint()).getPoller();
            if (p == null) {
                // Connector has been stopped
                release(socket, processor, true, false);
            } else {
                p.add(socket.getSocket().longValue(), -1, true, false);
            }
        } else {
            // Tomcat 7 proprietary upgrade
            ((AprEndpoint) proto.getEndpoint()).getPoller().add(
                    socket.getSocket().longValue(),
                    processor.getUpgradeInbound().getReadTimeout(),
                    true, false);
        }
    }

    @Override
    protected Http11AprProcessor createProcessor() {
        Http11AprProcessor processor = new Http11AprProcessor(
                proto.getMaxHttpHeaderSize(), (AprEndpoint)proto.getEndpoint(),
                proto.getMaxTrailerSize(), proto.getMaxExtensionSize(),
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
        processor.setClientCertProvider(proto.getClientCertProvider());
        register(processor);
        return processor;
    }

    /**
     * @deprecated  Will be removed in Tomcat 8.0.x.
     */
    @Deprecated
    @Override
    protected Processor<Long> createUpgradeProcessor(
            SocketWrapper<Long> socket,
            UpgradeInbound inbound)
            throws IOException {
        return new UpgradeAprProcessor(
                socket, inbound);
    }
    
    @Override
    protected Processor<Long> createUpgradeProcessor(
            SocketWrapper<Long> socket,
            HttpUpgradeHandler httpUpgradeProcessor)
            throws IOException {
        return new AprProcessor(socket, httpUpgradeProcessor,
                (AprEndpoint) proto.getEndpoint(),
                proto.getUpgradeAsyncWriteBufferSize());
    }
}