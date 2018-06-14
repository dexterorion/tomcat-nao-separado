package org.apache.naming.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This specialized resource implementation avoids opening the InputStream
 * to the file right away (which would put a lock on the file).
 */
public class FileDirContextFileResource extends Resource {


    // -------------------------------------------------------- Constructor


    public FileDirContextFileResource(File file) {
        this.file = file;
    }


    // --------------------------------------------------- Member Variables


    /**
     * Associated file object.
     */
    private File file;


    // --------------------------------------------------- Resource Methods


    /**
     * Content accessor.
     *
     * @return InputStream
     */
    @Override
    public InputStream streamContent()
        throws IOException {
        if (getBinaryContent() == null) {
            FileInputStream fis = new FileInputStream(file);
            setInputStream(fis);
            return fis;
        }
        return super.streamContent();
    }


}