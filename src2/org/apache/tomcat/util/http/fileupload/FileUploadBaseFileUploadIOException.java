package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;

/**
 * This exception is thrown for hiding an inner {@link FileUploadException}
 * in an {@link IOException}.
 */
public class FileUploadBaseFileUploadIOException extends IOException {

	private static final long serialVersionUID = -3082868232248803474L;

	public FileUploadBaseFileUploadIOException() {
		super();
	}

	public FileUploadBaseFileUploadIOException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileUploadBaseFileUploadIOException(String message) {
		super(message);
	}

	public FileUploadBaseFileUploadIOException(Throwable cause) {
		super(cause);
	}
}