package org.apache.catalina.startup;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;

import org.apache.tomcat.JarScannerCallback;

public class TldConfigTldJarScannerCallback implements JarScannerCallback {

    /**
	 * 
	 */
	private final TldConfig tldConfig;

	/**
	 * @param tldConfig
	 */
	public TldConfigTldJarScannerCallback(TldConfig tldConfig) {
		this.tldConfig = tldConfig;
	}

	@Override
    public void scan(JarURLConnection urlConn) throws IOException {
        this.tldConfig.tldScanJar(urlConn);
    }

    @Override
    public void scan(File file) {
        File metaInf = new File(file, "META-INF");
        if (metaInf.isDirectory()) {
            this.tldConfig.tldScanDir(metaInf);
        }
    }
}