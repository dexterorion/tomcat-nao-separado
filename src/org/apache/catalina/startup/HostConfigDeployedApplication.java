package org.apache.catalina.startup;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * This class represents the state of a deployed application, as well as
 * the monitored resources.
 */
public class HostConfigDeployedApplication {
    public HostConfigDeployedApplication(String name, boolean hasDescriptor) {
        this.setName(name);
        this.hasDescriptor = hasDescriptor;
    }

    public boolean isLoggedDirWarning() {
		return loggedDirWarning;
	}

	public void setLoggedDirWarning(boolean loggedDirWarning) {
		this.loggedDirWarning = loggedDirWarning;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public HashMap<String, Long> getReloadResources() {
		return reloadResources;
	}

	public void setReloadResources(HashMap<String, Long> reloadResources) {
		this.reloadResources = reloadResources;
	}

	public LinkedHashMap<String, Long> getRedeployResources() {
		return redeployResources;
	}

	public void setRedeployResources(LinkedHashMap<String, Long> redeployResources) {
		this.redeployResources = redeployResources;
	}

	public boolean isHasDescriptor() {
		return hasDescriptor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
     * Application context path. The assertion is that
     * (host.getChild(name) != null).
     */
    private String name;

    /**
     * Does this application have a context.xml descriptor file on the
     * host's configBase?
     */
    private final boolean hasDescriptor;

    /**
     * Any modification of the specified (static) resources will cause a
     * redeployment of the application. If any of the specified resources is
     * removed, the application will be undeployed. Typically, this will
     * contain resources like the context.xml file, a compressed WAR path.
     * The value is the last modification time.
     */
    private LinkedHashMap<String, Long> redeployResources =
        new LinkedHashMap<String, Long>();

    /**
     * Any modification of the specified (static) resources will cause a
     * reload of the application. This will typically contain resources
     * such as the web.xml of a webapp, but can be configured to contain
     * additional descriptors.
     * The value is the last modification time.
     */
    private HashMap<String, Long> reloadResources =
        new HashMap<String, Long>();

    /**
     * Instant where the application was last put in service.
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * In some circumstances, such as when unpackWARs is true, a directory
     * may be added to the appBase that is ignored. This flag indicates that
     * the user has been warned so that the warning is not logged on every
     * run of the auto deployer.
     */
    private boolean loggedDirWarning = false;
}