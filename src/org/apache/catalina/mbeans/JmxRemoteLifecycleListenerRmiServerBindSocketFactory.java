package org.apache.catalina.mbeans;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

public class JmxRemoteLifecycleListenerRmiServerBindSocketFactory
        implements RMIServerSocketFactory {

    private final InetAddress bindAddress;

    public JmxRemoteLifecycleListenerRmiServerBindSocketFactory(InetAddress address) {
        bindAddress = address;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException  {
        return new ServerSocket(port, 0, bindAddress);
    }
}