/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.ha.deploy;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * <p>
 * The <b>WarWatcher </b> watches the deployDir for changes made to the
 * directory (adding new WAR files->deploy or remove WAR files->undeploy) And
 * notifies a listener of the changes made
 * </p>
 * 
 * @author Filip Hanik
 * @author Peter Rossbach
 * @version 1.1
 */

public class WarWatcher {

	/*--Static Variables----------------------------------------*/
	private static final Log log = LogFactory.getLog(WarWatcher.class);

	/*--Instance Variables--------------------------------------*/
	/**
	 * Directory to watch for war files
	 */
	private File watchDir = null;

	/**
	 * Parent to be notified of changes
	 */
	private FileChangeListener listener = null;

	/**
	 * Currently deployed files
	 */
	private Map<String, WarWatcherWarInfo> currentStatus = new HashMap<String, WarWatcherWarInfo>();

	/*--Constructor---------------------------------------------*/

	public WarWatcher() {
	}

	public WarWatcher(FileChangeListener listener, File watchDir) {
		this.listener = listener;
		this.watchDir = watchDir;
	}

	/*--Logic---------------------------------------------------*/

	/**
	 * check for modification and send notification to listener
	 */
	public void check() {
		if (log.isDebugEnabled())
			log.debug("check cluster wars at " + watchDir);
		File[] list = watchDir.listFiles(new WarWatcherWarFilter());
		if (list == null)
			list = new File[0];
		// first make sure all the files are listed in our current status
		for (int i = 0; i < list.length; i++) {
			addWarInfo(list[i]);
		}

		// check all the status codes and update the FarmDeployer
		for (Iterator<Map.Entry<String, WarWatcherWarInfo>> i = currentStatus
				.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, WarWatcherWarInfo> entry = i.next();
			WarWatcherWarInfo info = entry.getValue();
			int check = info.check();
			if (check == 1) {
				listener.fileModified(info.getWar());
			} else if (check == -1) {
				listener.fileRemoved(info.getWar());
				// no need to keep in memory
				i.remove();
			}
		}

	}

	/**
	 * add cluster war to the watcher state
	 * 
	 * @param warfile
	 */
	protected void addWarInfo(File warfile) {
		WarWatcherWarInfo info = currentStatus.get(warfile.getAbsolutePath());
		if (info == null) {
			info = new WarWatcherWarInfo(warfile);
			info.setLastState(-1); // assume file is non existent
			currentStatus.put(warfile.getAbsolutePath(), info);
		}
	}

	/**
	 * clear watcher state
	 */
	public void clear() {
		currentStatus.clear();
	}

	/**
	 * @return Returns the watchDir.
	 */
	public File getWatchDir() {
		return watchDir;
	}

	/**
	 * @param watchDir
	 *            The watchDir to set.
	 */
	public void setWatchDir(File watchDir) {
		this.watchDir = watchDir;
	}

	/**
	 * @return Returns the listener.
	 */
	public FileChangeListener getListener() {
		return listener;
	}

	/**
	 * @param listener
	 *            The listener to set.
	 */
	public void setListener(FileChangeListener listener) {
		this.listener = listener;
	}
}