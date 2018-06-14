package org.apache.tomcat.util.net;

import java.nio.channels.SocketChannel;

public interface NioEndpointHandler extends AbstractEndpointHandler {
    public SocketState process(SocketWrapper<NioChannel> socket,
            SocketStatus status);
    public void release(SocketWrapper<NioChannel> socket);
    public void release(SocketChannel socket);
    public SSLImplementation getSslImplementation();
}