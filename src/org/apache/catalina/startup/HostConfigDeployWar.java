package org.apache.catalina.startup;

import java.io.File;

import org.apache.catalina.util.ContextName;

public class HostConfigDeployWar implements Runnable {

    private HostConfig config;
    private ContextName cn;
    private File war;

    public HostConfigDeployWar(HostConfig config, ContextName cn, File war) {
        this.config = config;
        this.cn = cn;
        this.war = war;
    }

    @Override
    public void run() {
        config.deployWAR(cn, war);
    }
}