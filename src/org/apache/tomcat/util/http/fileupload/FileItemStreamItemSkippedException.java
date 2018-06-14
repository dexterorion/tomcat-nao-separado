package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;
import java.io.InputStream;

/**
 * This exception is thrown, if an attempt is made to read
 * data from the {@link InputStream}, which has been returned
 * by {@link FileItemStream#openStream()}, after
 * {@link java.util.Iterator#hasNext()} has been invoked on the
 * iterator, which created the {@link FileItemStream}.
 */
public class FileItemStreamItemSkippedException extends IOException {

    /**
     * The exceptions serial version UID, which is being used
     * when serializing an exception instance.
     */
    private static final long serialVersionUID = -7280778431581963740L;

}