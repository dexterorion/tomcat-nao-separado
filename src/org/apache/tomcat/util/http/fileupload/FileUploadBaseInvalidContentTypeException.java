package org.apache.tomcat.util.http.fileupload;

public class FileUploadBaseInvalidContentTypeException extends FileUploadException {

	/**
	 * The exceptions UID, for serializing an instance.
	 */
	private static final long serialVersionUID = -9073026332015646668L;

	/**
	 * Constructs a <code>InvalidContentTypeException</code> with no detail
	 * message.
	 */
	public FileUploadBaseInvalidContentTypeException() {
		super();
	}

	/**
	 * Constructs an <code>InvalidContentTypeException</code> with the
	 * specified detail message.
	 *
	 * @param message
	 *            The detail message.
	 */
	public FileUploadBaseInvalidContentTypeException(String message) {
		super(message);
	}

	public FileUploadBaseInvalidContentTypeException(String msg, Throwable cause) {
		super(msg, cause);
	}
}