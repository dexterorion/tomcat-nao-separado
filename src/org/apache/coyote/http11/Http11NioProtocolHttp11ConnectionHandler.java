package org.apache.coyote.http11;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.AbstractProtocolAbstractConnectionHandler;
import org.apache.coyote.Processor;
import org.apache.coyote.http11.upgrade.NioProcessor;
import org.apache.coyote.http11.upgrade.UpgradeNioProcessor;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioEndpointHandler;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SecureNioChannel;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.coyote.http11.upgrade.UpgradeInbound;

public class Http11NioProtocolHttp11ConnectionHandler
        extends AbstractProtocolAbstractConnectionHandler<NioChannel,Http11NioProcessor>
        implements NioEndpointHandler {

    private Http11NioProtocol proto;

    public Http11NioProtocolHttp11ConnectionHandler(Http11NioProtocol proto) {
        this.proto = proto;
    }

    @Override
    protected AbstractProtocol<NioChannel> getProtocol() {
        return proto;
    }

    @Override
    protected Log getLog() {
        return Http11NioProtocol.getLogVariable();
    }


    @Override
    public SSLImplementation getSslImplementation() {
        return proto.getSslImplementation();
    }

    /**
     * Expected to be used by the Poller to release resources on socket
     * close, errors etc.
     */
    @Override
    public void release(SocketChannel socket) {
        if (Http11NioProtocol.getLogVariable().isDebugEnabled())
            Http11NioProtocol.getLogVariable().debug("Iterating through our connections to release a socket channel:"+socket);
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
        if (Http11NioProtocol.getLogVariable().isDebugEnabled())
            Http11NioProtocol.getLogVariable().debug("Done iterating through our connections to release a socket channel:"+socket +" released:"+released);
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
     *
     * @param socket
     * @param processor
     * @param isSocketClosing   Not used in HTTP
     * @param addToPoller
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
    protected void initSsl(SocketWrapper<NioChannel> socket,
            Processor<NioChannel> processor) {
        if (proto.isSSLEnabled() &&
                (proto.getSslImplementation() != null)
                && (socket.getSocket() instanceof SecureNioChannel)) {
            SecureNioChannel ch = (SecureNioChannel)socket.getSocket();
            processor.setSslSupport(
                    proto.getSslImplementation().getSSLSupport(
                            ch.getSslEngine().getSession()));
        } else {
            processor.setSslSupport(null);
        }

    }

    @Override
    protected void longPoll(SocketWrapper<NioChannel> socket,
            Processor<NioChannel> processor) {

        if (processor.isAsync()) {
            socket.setAsync(true);
        } else {
            // Either:
            //  - this is comet request
            //  - this is an upgraded connection
            //  - the request line/headers have not been completely
            //    read
            socket.getSocket().getPoller().add(socket.getSocket());
        }
    }

    @Override
    public Http11NioProcessor createProcessor() {
        Http11NioProcessor processor = new Http11NioProcessor(
                proto.getMaxHttpHeaderSize(), (NioEndpoint)proto.getEndpoint(),
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
        register(processor);
        return processor;
    }

    /**
     * @deprecated  Will be removed in Tomcat 8.0.x.
     */
    @Deprecated
    @Override
    protected Processor<NioChannel> createUpgradeProcessor(
            SocketWrapper<NioChannel> socket,
            UpgradeInbound inbound)
            throws IOException {
        return new UpgradeNioProcessor(
                socket, inbound,
                ((Http11NioProtocol) getProtocol()).getEndpoint().getSelectorPool());
    }
    
    @Override
    protected Processor<NioChannel> createUpgradeProcessor(
            SocketWrapper<NioChannel> socket,
            HttpUpgradeHandler httpUpgradeProcessor)
            throws IOException {
        return new NioProcessor(socket, httpUpgradeProcessor,
                proto.getEndpoint().getSelectorPool(),
                proto.getUpgradeAsyncWriteBufferSize());
    }
}