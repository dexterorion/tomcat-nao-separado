package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;

/**
 * Thrown upon attempt of setting an invalid boundary token.
 */
public class MultipartStreamIllegalBoundaryException extends IOException {


    /**
     * The UID to use when serializing this instance.
     */
    private static final long serialVersionUID = -161533165102632918L;

    /**
     * Constructs an <code>IllegalBoundaryException</code> with no
     * detail message.
     */
    public MultipartStreamIllegalBoundaryException() {
        super();
    }

    /**
     * Constructs an <code>IllegalBoundaryException</code> with
     * the specified detail message.
     *
     * @param message The detail message.
     */
    public MultipartStreamIllegalBoundaryException(String message) {
        super(message);
    }

}