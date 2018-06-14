package org.apache.tomcat.util.net;

/**
 * Bare bones interface used for socket processing. Per thread data is to be
 * stored in the ThreadWithAttributes extra folders, or alternately in
 * thread local fields.
 */
public interface AprEndpointHandler extends AbstractEndpointHandler {
    public SocketState process(SocketWrapper<Long> socket,
            SocketStatus status);
}