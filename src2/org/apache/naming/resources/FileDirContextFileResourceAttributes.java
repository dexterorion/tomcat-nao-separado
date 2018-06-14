package org.apache.naming.resources;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * This specialized resource attribute implementation does some lazy
 * reading (to speed up simple checks, like checking the last modified
 * date).
 */
public class FileDirContextFileResourceAttributes extends ResourceAttributes {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------- Constructor

    public FileDirContextFileResourceAttributes(File file) {
        this.file = file;
        getCreation();
        getLastModified();
    }

    // --------------------------------------------------- Member Variables


    private File file;


    private boolean accessed = false;


    private String canonicalPath = null;


    // ----------------------------------------- ResourceAttributes Methods


    /**
     * Is collection.
     */
    @Override
    public boolean isCollection() {
        if (!accessed) {
            setCollection(file.isDirectory());
            accessed = true;
        }
        return super.isCollection();
    }


    /**
     * Get content length.
     *
     * @return content length value
     */
    @Override
    public long getContentLength() {
        if (super.getContentLength() != -1L)
            return super.getContentLength();
        setContentLength(file.length());
        return super.getContentLength();
    }


    /**
     * Get creation time.
     *
     * @return creation time value
     */
    @Override
    public long getCreation() {
        if (super.getCreation() != -1L)
            return super.getCreation();
        setCreation(getLastModified());
        return super.getCreation();
    }


    /**
     * Get creation date.
     *
     * @return Creation date value
     */
    @Override
    public Date getCreationDate() {
        if (super.getCreation() == -1L) {
            setCreation(getCreation());
        }
        return super.getCreationDate();
    }


    /**
     * Get last modified time.
     *
     * @return lastModified time value
     */
    @Override
    public long getLastModified() {
        if (super.getLastModified() != -1L)
            return super.getLastModified();
        setLastModified(file.lastModified());
        return super.getLastModified();
    }


    /**
     * Get lastModified date.
     *
     * @return LastModified date value
     */
    @Override
    public Date getLastModifiedDate() {
        if (super.getLastModified() == -1L) {
            setLastModified(getLastModified());
        }
        return super.getLastModifiedDate();
    }


    /**
     * Get name.
     *
     * @return Name value
     */
    @Override
    public String getName() {
        if (super.getName() == null)
            setName(file.getName());
        return super.getName();
    }


    /**
     * Get resource type.
     *
     * @return String resource type
     */
    @Override
    public String getResourceType() {
        if (!accessed) {
            setCollection(file.isDirectory());
            accessed = true;
        }
        return super.getResourceType();
    }

    
    /**
     * Get canonical path.
     * 
     * @return String the file's canonical path
     */
    @Override
    public String getCanonicalPath() {
        if (canonicalPath == null) {
            try {
                canonicalPath = file.getCanonicalPath();
            } catch (IOException e) {
                // Ignore
            }
        }
        return canonicalPath;
    }
    

}