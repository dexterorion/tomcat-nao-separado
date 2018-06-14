package javax.websocket.server;

import java.util.Collections;
import java.util.List;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;

public final class ServerEndpointConfigBuilder {

    public static ServerEndpointConfigBuilder create(
            Class<?> endpointClass, String path) {
        return new ServerEndpointConfigBuilder(endpointClass, path);
    }


    private final Class<?> endpointClass;
    private final String path;
    private List<Class<? extends Encoder>> encoders =
            Collections.emptyList();
    private List<Class<? extends Decoder>> decoders =
            Collections.emptyList();
    private List<String> subprotocols = Collections.emptyList();
    private List<Extension> extensions = Collections.emptyList();
    private ServerEndpointConfigConfigurator configurator =
    		ServerEndpointConfigConfigurator.fetchContainerDefaultConfigurator();


    public ServerEndpointConfigBuilder(Class<?> endpointClass,
            String path) {
        this.endpointClass = endpointClass;
        this.path = path;
    }

    public ServerEndpointConfig build() {
        return new DefaultServerEndpointConfig(endpointClass, path,
                subprotocols, extensions, encoders, decoders, configurator);
    }


    public ServerEndpointConfigBuilder encoders(
            List<Class<? extends Encoder>> encoders) {
        if (encoders == null || encoders.size() == 0) {
            this.encoders = Collections.emptyList();
        } else {
            this.encoders = Collections.unmodifiableList(encoders);
        }
        return this;
    }


    public ServerEndpointConfigBuilder decoders(
            List<Class<? extends Decoder>> decoders) {
        if (decoders == null || decoders.size() == 0) {
            this.decoders = Collections.emptyList();
        } else {
            this.decoders = Collections.unmodifiableList(decoders);
        }
        return this;
    }


    public ServerEndpointConfigBuilder subprotocols(
            List<String> subprotocols) {
        if (subprotocols == null || subprotocols.size() == 0) {
            this.subprotocols = Collections.emptyList();
        } else {
            this.subprotocols = Collections.unmodifiableList(subprotocols);
        }
        return this;
    }


    public ServerEndpointConfigBuilder extensions(
            List<Extension> extensions) {
        if (extensions == null || extensions.size() == 0) {
            this.extensions = Collections.emptyList();
        } else {
            this.extensions = Collections.unmodifiableList(extensions);
        }
        return this;
    }


    public ServerEndpointConfigBuilder configurator(ServerEndpointConfigConfigurator serverEndpointConfigurator) {
        if (serverEndpointConfigurator == null) {
            this.configurator = ServerEndpointConfigConfigurator.fetchContainerDefaultConfigurator();
        } else {
            this.configurator = serverEndpointConfigurator;
        }
        return this;
    }
}