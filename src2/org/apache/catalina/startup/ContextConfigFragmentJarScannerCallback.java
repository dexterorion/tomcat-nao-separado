package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.Jar;
import org.apache.tomcat.util.scan.JarFactory;
import org.xml.sax.InputSource;

public class ContextConfigFragmentJarScannerCallback implements JarScannerCallback {

    /**
	 * 
	 */
	private final ContextConfig contextConfig;
	private static final String FRAGMENT_LOCATION =
        "META-INF/web-fragment.xml";
    private Map<String,WebXml> fragments = new HashMap<String,WebXml>();
    private final boolean parseRequired;

    public ContextConfigFragmentJarScannerCallback(ContextConfig contextConfig, boolean parseRequired) {
        this.contextConfig = contextConfig;
		this.parseRequired = parseRequired;
    }
    
    @Override
    public void scan(JarURLConnection jarConn) throws IOException {

        URL url = jarConn.getURL();
        URL resourceURL = jarConn.getJarFileURL();
        Jar jar = null;
        InputStream is = null;
        WebXml fragment = new WebXml();

        try {
            jar = JarFactory.newInstance(url);
            if (parseRequired || this.contextConfig.getContext().getXmlValidation()) {
                is = jar.getInputStream(FRAGMENT_LOCATION);
            }

            if (is == null) {
                // If there is no web-fragment.xml to process there is no
                // impact on distributable
                fragment.setDistributable(true);
            } else {
                InputSource source = new InputSource(
                        "jar:" + resourceURL.toString() + "!/" +
                        FRAGMENT_LOCATION);
                source.setByteStream(is);
                this.contextConfig.parseWebXml(source, fragment, true);
            }
        } finally {
            if (jar != null) {
                jar.close();
            }
            fragment.setURL(url);
            if (fragment.getName() == null) {
                fragment.setName(fragment.getURL().toString());
            }
            fragment.setJarName(extractJarFileName(url));
            fragments.put(fragment.getName(), fragment);
        }
    }

    private String extractJarFileName(URL input) {
        String url = input.toString();
        if (url.endsWith("!/")) {
            // Remove it
            url = url.substring(0, url.length() - 2);
        }

        // File name will now be whatever is after the final /
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Override
    public void scan(File file) throws IOException {

        InputStream stream = null;
        WebXml fragment = new WebXml();

        try {
            File fragmentFile = new File(file, FRAGMENT_LOCATION);
            if (fragmentFile.isFile()) {
                stream = new FileInputStream(fragmentFile);
                InputSource source =
                    new InputSource(fragmentFile.toURI().toURL().toString());
                source.setByteStream(stream);
                this.contextConfig.parseWebXml(source, fragment, true);
            } else {
                // If there is no web.xml, normal folder no impact on
                // distributable
                fragment.setDistributable(true);
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            fragment.setURL(file.toURI().toURL());
            if (fragment.getName() == null) {
                fragment.setName(fragment.getURL().toString());
            }
            fragment.setJarName(file.getName());
            fragments.put(fragment.getName(), fragment);
        }
    }

    public Map<String,WebXml> getFragments() {
        return fragments;
    }
}