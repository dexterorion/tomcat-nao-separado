package org.apache.tomcat.util.http.fileupload;

public class FileUploadBaseIOFileUploadException extends FileUploadException {

	private static final long serialVersionUID = -5858565745868986701L;

	public FileUploadBaseIOFileUploadException() {
		super();
	}

	public FileUploadBaseIOFileUploadException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileUploadBaseIOFileUploadException(String message) {
		super(message);
	}

	public FileUploadBaseIOFileUploadException(Throwable cause) {
		super(cause);
	}
}