package org.apache.catalina.ha.deploy;

import java.io.File;

/**
 * File name filter for war files
 */
public class WarWatcherWarFilter implements java.io.FilenameFilter {
	@Override
	public boolean accept(File path, String name) {
		if (name == null)
			return false;
		return name.endsWith(".war");
	}
}