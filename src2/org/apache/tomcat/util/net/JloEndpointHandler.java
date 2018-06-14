package org.apache.tomcat.util.net;

import java.net.Socket;

/**
 * Bare bones interface used for socket processing. Per thread data is to be
 * stored in the ThreadWithAttributes extra folders, or alternately in
 * thread local fields.
 */
public interface JloEndpointHandler extends AbstractEndpointHandler {
    public SocketState process(SocketWrapper<Socket> socket,
            SocketStatus status);
    public SSLImplementation getSslImplementation();
}