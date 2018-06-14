package org.apache.jasper.compiler;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;

import org.apache.tomcat.JarScannerCallback;

public class TldLocationsCacheTldJarScannerCallback implements JarScannerCallback {

    /**
	 * 
	 */
	private final TldLocationsCache tldLocationsCache;

	/**
	 * @param tldLocationsCache
	 */
	public TldLocationsCacheTldJarScannerCallback(
			TldLocationsCache tldLocationsCache) {
		this.tldLocationsCache = tldLocationsCache;
	}

	@Override
    public void scan(JarURLConnection urlConn) throws IOException {
        this.tldLocationsCache.tldScanJar(urlConn);
    }

    @Override
    public void scan(File file) throws IOException {
        File metaInf = new File(file, "META-INF");
        if (metaInf.isDirectory()) {
            this.tldLocationsCache.tldScanDir(metaInf);
        }
    }
}