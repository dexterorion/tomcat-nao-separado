package org.apache.catalina.ha.deploy;

import java.io.File;

public class WarWatcherWarInfo {
	private File war = null;

	private long lastChecked = 0;

	private long lastState = 0;

	public WarWatcherWarInfo(File war) {
		this.war = war;
		this.lastChecked = war.lastModified();
		if (!war.exists())
			lastState = -1;
	}

	public boolean modified() {
		return war.exists() && war.lastModified() > lastChecked;
	}

	public boolean exists() {
		return war.exists();
	}

	/**
	 * Returns 1 if the file has been added/modified, 0 if the file is unchanged
	 * and -1 if the file has been removed
	 * 
	 * @return int 1=file added; 0=unchanged; -1=file removed
	 */
	public int check() {
		// file unchanged by default
		int result = 0;

		if (modified()) {
			// file has changed - timestamp
			result = 1;
			lastState = result;
		} else if ((!exists()) && (!(lastState == -1))) {
			// file was removed
			result = -1;
			lastState = result;
		} else if ((lastState == -1) && exists()) {
			// file was added
			result = 1;
			lastState = result;
		}
		this.lastChecked = System.currentTimeMillis();
		return result;
	}

	public File getWar() {
		return war;
	}

	@Override
	public int hashCode() {
		return war.getAbsolutePath().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WarWatcherWarInfo) {
			WarWatcherWarInfo wo = (WarWatcherWarInfo) other;
			return wo.getWar().equals(getWar());
		} else {
			return false;
		}
	}

	protected void setLastState(int lastState) {
		this.lastState = lastState;
	}

}