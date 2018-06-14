package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;

/**
 * Thrown to indicate that the input stream fails to follow the
 * required syntax.
 */
public class MultipartStreamMalformedStreamException extends IOException {

    /**
     * The UID to use when serializing this instance.
     */
    private static final long serialVersionUID = 6466926458059796677L;

    /**
     * Constructs a <code>MalformedStreamException</code> with no
     * detail message.
     */
    public MultipartStreamMalformedStreamException() {
        super();
    }

    /**
     * Constructs an <code>MalformedStreamException</code> with
     * the specified detail message.
     *
     * @param message The detail message.
     */
    public MultipartStreamMalformedStreamException(String message) {
        super(message);
    }

}