package javax.websocket;

import java.util.Collections;
import java.util.List;

public final class ClientEndpointConfigBuilder {

    private static final ClientEndpointConfigConfigurator DEFAULT_CONFIGURATOR = new ClientEndpointConfigConfigurator();


    public static ClientEndpointConfigBuilder create() {
        return new ClientEndpointConfigBuilder();
    }


    public ClientEndpointConfigBuilder() {
        // Hide default constructor
    }

    private ClientEndpointConfigConfigurator configurator = DEFAULT_CONFIGURATOR;
    private List<String> preferredSubprotocols = Collections.emptyList();
    private List<Extension> extensions = Collections.emptyList();
    private List<Class<? extends Encoder>> encoders =
            Collections.emptyList();
    private List<Class<? extends Decoder>> decoders =
            Collections.emptyList();


    public ClientEndpointConfig build() {
        return new DefaultClientEndpointConfig(preferredSubprotocols,
                extensions, encoders, decoders, configurator);
    }


    public ClientEndpointConfigBuilder configurator(ClientEndpointConfigConfigurator configurator) {
        if (configurator == null) {
            this.configurator = DEFAULT_CONFIGURATOR;
        } else {
            this.configurator = configurator;
        }
        return this;
    }


    public ClientEndpointConfigBuilder preferredSubprotocols(
            List<String> preferredSubprotocols) {
        if (preferredSubprotocols == null ||
                preferredSubprotocols.size() == 0) {
            this.preferredSubprotocols = Collections.emptyList();
        } else {
            this.preferredSubprotocols =
                    Collections.unmodifiableList(preferredSubprotocols);
        }
        return this;
    }


    public ClientEndpointConfigBuilder extensions(
            List<Extension> extensions) {
        if (extensions == null || extensions.size() == 0) {
            this.extensions = Collections.emptyList();
        } else {
            this.extensions = Collections.unmodifiableList(extensions);
        }
        return this;
    }


    public ClientEndpointConfigBuilder encoders(List<Class<? extends Encoder>> encoders) {
        if (encoders == null || encoders.size() == 0) {
            this.encoders = Collections.emptyList();
        } else {
            this.encoders = Collections.unmodifiableList(encoders);
        }
        return this;
    }


    public ClientEndpointConfigBuilder decoders(List<Class<? extends Decoder>> decoders) {
        if (decoders == null || decoders.size() == 0) {
            this.decoders = Collections.emptyList();
        } else {
            this.decoders = Collections.unmodifiableList(decoders);
        }
        return this;
    }
}