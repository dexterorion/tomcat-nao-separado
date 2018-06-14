package javax.websocket.server;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;

public class ServerEndpointConfigConfigurator {

    private static volatile ServerEndpointConfigConfigurator defaultImpl = null;
    private static final Object defaultImplLock = new Object();

    private static final String DEFAULT_IMPL_CLASSNAME =
            "org.apache.tomcat.websocket.server.DefaultServerEndpointConfigurator";

    public static ServerEndpointConfigConfigurator fetchContainerDefaultConfigurator() {
        if (defaultImpl == null) {
            synchronized (defaultImplLock) {
                if (defaultImpl == null) {
                    defaultImpl = loadDefault();
                }
            }
        }
        return defaultImpl;
    }


    private static ServerEndpointConfigConfigurator loadDefault() {
    	ServerEndpointConfigConfigurator result = null;

        ServiceLoader<ServerEndpointConfigConfigurator> serviceLoader =
                ServiceLoader.load(ServerEndpointConfigConfigurator.class);

        Iterator<ServerEndpointConfigConfigurator> iter = serviceLoader.iterator();
        while (result == null && iter.hasNext()) {
            result = iter.next();
        }

        // Fall-back. Also used by unit tests
        if (result == null) {
            try {
                @SuppressWarnings("unchecked")
                Class<ServerEndpointConfigConfigurator> clazz =
                        (Class<ServerEndpointConfigConfigurator>) Class.forName(
                                DEFAULT_IMPL_CLASSNAME);
                result = clazz.newInstance();
            } catch (ClassNotFoundException e) {
                // No options left. Just return null.
            } catch (InstantiationException e) {
                // No options left. Just return null.
            } catch (IllegalAccessException e) {
                // No options left. Just return null.
            }
        }
        return result;
    }

    public String getNegotiatedSubprotocol(List<String> supported,
            List<String> requested) {
        return fetchContainerDefaultConfigurator().getNegotiatedSubprotocol(supported, requested);
    }

    public List<Extension> getNegotiatedExtensions(List<Extension> installed,
            List<Extension> requested) {
        return fetchContainerDefaultConfigurator().getNegotiatedExtensions(installed, requested);
    }

    public boolean checkOrigin(String originHeaderValue) {
        return fetchContainerDefaultConfigurator().checkOrigin(originHeaderValue);
    }

    public void modifyHandshake(ServerEndpointConfig sec,
            HandshakeRequest request, HandshakeResponse response) {
        fetchContainerDefaultConfigurator().modifyHandshake(sec, request, response);
    }

    public <T extends Object> T getEndpointInstance(Class<T> clazz)
            throws InstantiationException {
        return fetchContainerDefaultConfigurator().getEndpointInstance(
                clazz);
    }
}