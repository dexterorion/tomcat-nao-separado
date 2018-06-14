package org.apache.catalina.mbeans;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

public class JmxRemoteLifecycleListenerRmiClientLocalhostSocketFactory
        implements RMIClientSocketFactory, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORCED_HOST = "localhost";

    private RMIClientSocketFactory factory = null;
    
    public JmxRemoteLifecycleListenerRmiClientLocalhostSocketFactory(RMIClientSocketFactory theFactory) {
        factory = theFactory;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        if (factory == null) {
            return new Socket(FORCED_HOST, port);
        } else {
            return factory.createSocket(FORCED_HOST, port);
        }
    }
}