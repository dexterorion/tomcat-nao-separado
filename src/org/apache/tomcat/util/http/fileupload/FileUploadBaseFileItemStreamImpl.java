package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tomcat.util.http.fileupload.util.Closeable;
import org.apache.tomcat.util.http.fileupload.util.LimitedInputStream2;
import org.apache.tomcat.util.http.fileupload.util.Streams;

/**
 * Default implementation of {@link FileItemStream}.
 */
public class FileUploadBaseFileItemStreamImpl implements FileItemStream {

    /**
	 * 
	 */
	private final FileUploadBaseFileItemIteratorImpl fileItemIteratorImpl;

	/**
     * The file items content type.
     */
    private final String contentType;

    /**
     * The file items field name.
     */
    private final String fieldName;

    /**
     * The file items file name.
     */
    private final String name;

    /**
     * Whether the file item is a form field.
     */
    private final boolean formField;

    /**
     * The file items input stream.
     */
    private final InputStream stream;

    /**
     * Whether the file item was already opened.
     */
    private boolean opened;

    /**
     * The headers, if any.
     */
    private FileItemHeaders headers;

    /**
     * Creates a new instance.
     *
     * @param pName The items file name, or null.
     * @param pFieldName The items field name.
     * @param pContentType The items content type, or null.
     * @param pFormField Whether the item is a form field.
     * @param pContentLength The items content length, if known, or -1
     * @param fileItemIteratorImpl TODO
     * @throws IOException Creating the file item failed.
     */
    public FileUploadBaseFileItemStreamImpl(FileUploadBaseFileItemIteratorImpl fileItemIteratorImpl, String pName, String pFieldName,
            String pContentType, boolean pFormField,
            long pContentLength) throws IOException {
        this.fileItemIteratorImpl = fileItemIteratorImpl;
		name = pName;
        fieldName = pFieldName;
        contentType = pContentType;
        formField = pFormField;
        final MultipartStreamItemInputStream itemStream = this.fileItemIteratorImpl.getMulti().newInputStream();
        InputStream istream = itemStream;
        if (this.fileItemIteratorImpl.getFileUploadBase().getFileSizeMax() != -1) {
            if (pContentLength != -1
                    &&  pContentLength > this.fileItemIteratorImpl.getFileUploadBase().getFileSizeMax()) {
                FileUploadBaseFileSizeLimitExceededException e =
                    new FileUploadBaseFileSizeLimitExceededException(
                        String.format("The field %s exceeds its maximum permitted size of %s bytes.",
                                fieldName, Long.valueOf(this.fileItemIteratorImpl.getFileUploadBase().getFileSizeMax())),
                        pContentLength, this.fileItemIteratorImpl.getFileUploadBase().getFileSizeMax());
                e.setFileName(pName);
                e.setFieldName(pFieldName);
                throw new FileUploadBaseFileUploadIOException(e);
            }
            istream = new LimitedInputStream2(istream, this.fileItemIteratorImpl.getFileUploadBase().getFileSizeMax(), itemStream, fieldName, name);
        }
        stream = istream;
    }

    /**
     * Returns the items content type, or null.
     *
     * @return Content type, if known, or null.
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the items field name.
     *
     * @return Field name.
     */
    @Override
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the items file name.
     *
     * @return File name, if known, or null.
     * @throws InvalidFileNameException The file name contains a NUL character,
     *   which might be an indicator of a security attack. If you intend to
     *   use the file name anyways, catch the exception and use
     *   InvalidFileNameException#getName().
     */
    @Override
    public String getName() {
        return Streams.checkFileName(name);
    }

    /**
     * Returns, whether this is a form field.
     *
     * @return True, if the item is a form field,
     *   otherwise false.
     */
    @Override
    public boolean isFormField() {
        return formField;
    }

    /**
     * Returns an input stream, which may be used to
     * read the items contents.
     *
     * @return Opened input stream.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public InputStream openStream() throws IOException {
        if (opened) {
            throw new IllegalStateException(
                    "The stream was already opened.");
        }
        if (((Closeable) stream).isClosed()) {
            throw new FileItemStreamItemSkippedException();
        }
        return stream;
    }

    /**
     * Closes the file item.
     *
     * @throws IOException An I/O error occurred.
     */
    public void close() throws IOException {
        stream.close();
    }

    /**
     * Returns the file item headers.
     *
     * @return The items header object
     */
    @Override
    public FileItemHeaders getHeaders() {
        return headers;
    }

    /**
     * Sets the file item headers.
     *
     * @param pHeaders The items header object
     */
    @Override
    public void setHeaders(FileItemHeaders pHeaders) {
        headers = pHeaders;
    }

}