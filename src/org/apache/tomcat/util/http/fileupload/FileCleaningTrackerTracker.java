package org.apache.tomcat.util.http.fileupload;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public final class FileCleaningTrackerTracker extends PhantomReference<Object> {

    /**
     * The full path to the file being tracked.
     */
    private final String path;
    /**
     * The strategy for deleting files.
     */
    private final FileDeleteStrategy deleteStrategy;

    /**
     * Constructs an instance of this class from the supplied parameters.
     *
     * @param path  the full path to the file to be tracked, not null
     * @param deleteStrategy  the strategy to delete the file, null means normal
     * @param marker  the marker object used to track the file, not null
     * @param queue  the queue on to which the tracker will be pushed, not null
     */
    public FileCleaningTrackerTracker(String path, FileDeleteStrategy deleteStrategy, Object marker, ReferenceQueue<? super Object> queue) {
        super(marker, queue);
        this.path = path;
        this.deleteStrategy = deleteStrategy == null ? FileDeleteStrategy.getNormal() : deleteStrategy;
    }

    /**
     * Return the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Deletes the file associated with this tracker instance.
     *
     * @return {@code true} if the file was deleted successfully;
     *         {@code false} otherwise.
     */
    public boolean delete() {
        return deleteStrategy.deleteQuietly(new File(path));
    }
}