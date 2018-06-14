package org.apache.tomcat.util.http.fileupload;

public class FileUploadBaseSizeLimitExceededException extends FileUploadBaseSizeException {

	/**
	 * The exceptions UID, for serializing an instance.
	 */
	private static final long serialVersionUID = -2474893167098052828L;

	/**
	 * Constructs a <code>SizeExceededException</code> with the specified
	 * detail message, and actual and permitted sizes.
	 *
	 * @param message
	 *            The detail message.
	 * @param actual
	 *            The actual request size.
	 * @param permitted
	 *            The maximum permitted request size.
	 */
	public FileUploadBaseSizeLimitExceededException(String message, long actual,
			long permitted) {
		super(message, actual, permitted);
	}

}