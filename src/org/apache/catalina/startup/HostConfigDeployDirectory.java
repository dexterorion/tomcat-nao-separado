package org.apache.catalina.startup;

import java.io.File;

import org.apache.catalina.util.ContextName;

public class HostConfigDeployDirectory implements Runnable {

    private HostConfig config;
    private ContextName cn;
    private File dir;

    public HostConfigDeployDirectory(HostConfig config, ContextName cn, File dir) {
        this.config = config;
        this.cn = cn;
        this.dir = dir;
    }

    @Override
    public void run() {
        config.deployDirectory(cn, dir);
    }
}