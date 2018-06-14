package org.apache.catalina.core;

import org.apache.catalina.AccessLog;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

// ----------------------------------------------------------- Inner classes
public final class StandardEngineNoopAccessLog implements AccessLog {

    @Override
    public void log(Request request, Response response, long time) {
        // NOOP
    }

    @Override
    public void setRequestAttributesEnabled(
            boolean requestAttributesEnabled) {
        // NOOP
        
    }

    @Override
    public boolean getRequestAttributesEnabled() {
        // NOOP
        return false;
    }
}