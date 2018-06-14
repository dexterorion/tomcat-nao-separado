package org.apache.juli;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Logger;

public final class ClassLoaderLogManagerClassLoaderLogInfo {
    private final ClassLoaderLogManagerLogNode rootNode;
    private final Map<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();
    private final Map<String, Handler> handlers = new HashMap<String, Handler>();
    private final Properties props = new Properties();

    public ClassLoaderLogManagerClassLoaderLogInfo(final ClassLoaderLogManagerLogNode rootNode) {
        this.rootNode = rootNode;
    }

	public ClassLoaderLogManagerLogNode getRootNode() {
		return rootNode;
	}

	public Map<String, Logger> getLoggers() {
		return loggers;
	}

	public Map<String, Handler> getHandlers() {
		return handlers;
	}

	public Properties getProps() {
		return props;
	}

}