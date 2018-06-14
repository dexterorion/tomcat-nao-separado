package org.apache.catalina.startup;

public class UserConfigDeployUserDirectory implements Runnable {

	private UserConfig config;
	private String user;
	private String home;

	public UserConfigDeployUserDirectory(UserConfig config, String user, String home) {
		this.config = config;
		this.user = user;
		this.home = home;
	}

	@Override
	public void run() {
		config.deploy(user, home);
	}
}