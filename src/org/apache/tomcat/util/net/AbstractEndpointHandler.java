package org.apache.tomcat.util.net;

public interface AbstractEndpointHandler {
    /**
     * Obtain the GlobalRequestProcessor associated with the handler.
     */
    public Object getGlobal();


    /**
     * Recycle resources associated with the handler.
     */
    public void recycle();
}