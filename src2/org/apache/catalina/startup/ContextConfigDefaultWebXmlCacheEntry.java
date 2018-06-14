package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;

public class ContextConfigDefaultWebXmlCacheEntry {
    private final WebXml webXml;
    private final long globalTimeStamp;
    private final long hostTimeStamp;

    public ContextConfigDefaultWebXmlCacheEntry(WebXml webXml, long globalTimeStamp,
            long hostTimeStamp) {
        this.webXml = webXml;
        this.globalTimeStamp = globalTimeStamp;
        this.hostTimeStamp = hostTimeStamp;
    }

    public WebXml getWebXml() {
        return webXml;
    }

    public long getGlobalTimeStamp() {
        return globalTimeStamp;
    }

    public long getHostTimeStamp() {
        return hostTimeStamp;
    }
}