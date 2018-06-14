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
package org.apache.tomcat.util.http.fileupload;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Keeps track of files awaiting deletion, and deletes them when an associated
 * marker object is reclaimed by the garbage collector.
 * <p>
 * This utility creates a background thread to handle file deletion. Each file
 * to be deleted is registered with a handler object. When the handler object is
 * garbage collected, the file is deleted.
 * <p>
 * In an environment with multiple class loaders (a servlet container, for
 * example), you should consider stopping the background thread if it is no
 * longer needed. This is done by invoking the method {@link #exitWhenFinished},
 * typically in {@link javax.servlet.ServletContextListener#contextDestroyed} or
 * similar.
 */
public class FileCleaningTracker {
	/**
	 * Queue of <code>Tracker</code> instances being watched.
	 */
	private final ReferenceQueue<Object> q = new ReferenceQueue<Object>();
	/**
	 * Collection of <code>Tracker</code> instances in existence.
	 */
	private final Collection<FileCleaningTrackerTracker> trackers = Collections
			.synchronizedSet(new HashSet<FileCleaningTrackerTracker>()); // synchronized
	/**
	 * Collection of File paths that failed to delete.
	 */
	private final List<String> deleteFailures = Collections
			.synchronizedList(new ArrayList<String>());
	/**
	 * Whether to terminate the thread when the tracking is complete.
	 */
	private volatile boolean exitWhenFinished = false;
	/**
	 * The thread that will clean up registered files.
	 */
	private Thread reaper;

	// -----------------------------------------------------------------------
	/**
	 * Track the specified file, using the provided marker, deleting the file
	 * when the marker instance is garbage collected. The
	 * {@link FileDeleteStrategy#NORMAL normal} deletion strategy will be used.
	 *
	 * @param file
	 *            the file to be tracked, not null
	 * @param marker
	 *            the marker object used to track the file, not null
	 * @throws NullPointerException
	 *             if the file is null
	 */
	public void track(File file, Object marker) {
		track(file, marker, (FileDeleteStrategy) null);
	}

	/**
	 * Track the specified file, using the provided marker, deleting the file
	 * when the marker instance is garbage collected. The speified deletion
	 * strategy is used.
	 *
	 * @param file
	 *            the file to be tracked, not null
	 * @param marker
	 *            the marker object used to track the file, not null
	 * @param deleteStrategy
	 *            the strategy to delete the file, null means normal
	 * @throws NullPointerException
	 *             if the file is null
	 */
	public void track(File file, Object marker,
			FileDeleteStrategy deleteStrategy) {
		if (file == null) {
			throw new NullPointerException("The file must not be null");
		}
		addTracker(file.getPath(), marker, deleteStrategy);
	}

	/**
	 * Adds a tracker to the list of trackers.
	 *
	 * @param path
	 *            the full path to the file to be tracked, not null
	 * @param marker
	 *            the marker object used to track the file, not null
	 * @param deleteStrategy
	 *            the strategy to delete the file, null means normal
	 */
	private synchronized void addTracker(String path, Object marker,
			FileDeleteStrategy deleteStrategy) {
		// synchronized block protects reaper
		if (exitWhenFinished) {
			throw new IllegalStateException(
					"No new trackers can be added once exitWhenFinished() is called");
		}
		if (reaper == null) {
			reaper = new FileCleaningTrackerReaper(this);
			reaper.start();
		}
		trackers.add(new FileCleaningTrackerTracker(path, deleteStrategy,
				marker, q));
	}

	public List<String> getDeleteFailures() {
		return deleteFailures;
	}

	public boolean isExitWhenFinished() {
		return exitWhenFinished;
	}

	public void setExitWhenFinished(boolean exitWhenFinished) {
		this.exitWhenFinished = exitWhenFinished;
	}

	public Thread getReaper() {
		return reaper;
	}

	public void setReaper(Thread reaper) {
		this.reaper = reaper;
	}

	public ReferenceQueue<Object> getQ() {
		return q;
	}

	public Collection<FileCleaningTrackerTracker> getTrackers() {
		return trackers;
	}

}
