package org.apache.tomcat.util.http.fileupload;

import java.io.File;
import java.io.IOException;

//-----------------------------------------------------------------------
/**
 * Force file deletion strategy.
 */
public class FileDeleteStrategyForceFileDeleteStrategy extends FileDeleteStrategy {
    /** Default Constructor */
	public FileDeleteStrategyForceFileDeleteStrategy() {
        super("Force");
    }

    /**
     * Deletes the file object.
     * <p>
     * This implementation uses <code>FileUtils.forceDelete() <code>
     * if the file exists.
     *
     * @param fileToDelete  the file to delete, not null
     * @return Always returns {@code true}
     * @throws NullPointerException if the file is null
     * @throws IOException if an error occurs during file deletion
     */
    @Override
    protected boolean doDelete(File fileToDelete) throws IOException {
        FileUtils.forceDelete(fileToDelete);
        return true;
    }
}