package org.apache.tomcat.util.http.fileupload;

import org.apache.tomcat.util.http.fileupload.FileCleaningTrackerTracker;

//-----------------------------------------------------------------------
/**
 * The reaper thread.
 */
public final class FileCleaningTrackerReaper extends Thread {
    /**
	 * 
	 */
	private final FileCleaningTracker fileCleaningTracker;

	/** Construct a new FileCleaningTrackerReaper 
     * @param fileCleaningTracker TODO*/
    public FileCleaningTrackerReaper(FileCleaningTracker fileCleaningTracker) {
        super("File Reaper");
		this.fileCleaningTracker = fileCleaningTracker;
        setPriority(Thread.MAX_PRIORITY);
        setDaemon(true);
    }

    /**
     * Run the reaper thread that will delete files as their associated
     * marker objects are reclaimed by the garbage collector.
     */
    @Override
    public void run() {
        // thread exits when exitWhenFinished is true and there are no more tracked objects
        while (this.fileCleaningTracker.isExitWhenFinished() == false || this.fileCleaningTracker.getTrackers().size() > 0) {
            try {
                // Wait for a tracker to remove.
            	FileCleaningTrackerTracker tracker = (FileCleaningTrackerTracker) this.fileCleaningTracker.getQ().remove(); // cannot return null
                this.fileCleaningTracker.getTrackers().remove(tracker);
                if (!tracker.delete()) {
                    this.fileCleaningTracker.getDeleteFailures().add(tracker.getPath());
                }
                tracker.clear();
            } catch (InterruptedException e) {
                continue;
            }
        }
    }
}