package org.apache.catalina.startup;

import java.io.File;

import org.apache.catalina.util.ContextName;

public class HostConfigDeployDescriptor implements Runnable {

    private HostConfig config;
    private ContextName cn;
    private File descriptor;

    public HostConfigDeployDescriptor(HostConfig config, ContextName cn,
            File descriptor) {
        this.config = config;
        this.cn = cn;
        this.descriptor= descriptor;
    }

    @Override
    public void run() {
        config.deployDescriptor(cn, descriptor);
    }
}