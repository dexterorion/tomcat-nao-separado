package javax.websocket;

import java.util.List;
import java.util.Map;

public class ClientEndpointConfigConfigurator {

    /**
     * Provides the client with a mechanism to inspect and/or modify the headers
     * that are sent to the server to start the WebSocket handshake.
     *
     * @param headers   The HTTP headers
     */
    public void beforeRequest(Map<String, List<String>> headers) {
        // NO-OP
    }

    /**
     * Provides the client with a mechanism to inspect the handshake response
     * that is returned from the server.
     *
     * @param handshakeResponse The response
     */
    public void afterResponse(HandshakeResponse handshakeResponse) {
        // NO-OP
    }
}