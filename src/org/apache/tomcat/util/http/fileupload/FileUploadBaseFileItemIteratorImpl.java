package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.tomcat.util.http.fileupload.util.LimitedInputStream1;

/**
 * The iterator, which is returned by
 * {@link FileUploadBase#getItemIterator(RequestContext)}.
 */
public class FileUploadBaseFileItemIteratorImpl implements FileItemIterator {

	/**
	 * 
	 */
	private FileUploadBase fileUploadBase = null;

	/**
	 * @param fileUploadBase
	 */
	public FileUploadBaseFileItemIteratorImpl(FileUploadBase fileUploadBase) {
		this.fileUploadBase = fileUploadBase;
	}

	/**
	 * The multi part stream to process.
	 */
	private MultipartStream multi = null;

	/**
	 * The notifier, which used for triggering the {@link ProgressListener}.
	 */
	private MultipartStreamProgressNotifier notifier = null;

	/**
	 * The boundary, which separates the various parts.
	 */
	private byte[] boundary = null;

	/**
	 * The item, which we currently process.
	 */
	private FileUploadBaseFileItemStreamImpl currentItem = null;

	/**
	 * The current items field name.
	 */
	private String currentFieldName = null;

	/**
	 * Whether we are currently skipping the preamble.
	 */
	private boolean skipPreamble;

	/**
	 * Whether the current item may still be read.
	 */
	private boolean itemValid;

	/**
	 * Whether we have seen the end of the file.
	 */
	private boolean eof;

	/**
	 * Creates a new instance.
	 *
	 * @param ctx
	 *            The request context.
	 * @throws FileUploadException
	 *             An error occurred while parsing the request.
	 * @throws IOException
	 *             An I/O error occurred.
	 */
	public FileUploadBaseFileItemIteratorImpl(RequestContext ctx) throws FileUploadException, IOException {
		if (ctx == null) {
			throw new NullPointerException("ctx parameter");
		}

		String contentType = ctx.getContentType();
		if ((null == contentType)
				|| (!contentType.toLowerCase(Locale.ENGLISH).startsWith(
						FileUploadBase.getMultipart()))) {
			throw new FileUploadBaseInvalidContentTypeException(
					String.format(
							"the request doesn't contain a %s or %s stream, content type header is %s",
							FileUploadBase.getMultipartFormData(),
							FileUploadBase.getMultipartMixed(), contentType));
		}

		InputStream input = ctx.getInputStream();

		final long requestSize = ((UploadContext) ctx).contentLength();

		if (this.fileUploadBase.getSizeMax() >= 0) {
			if (requestSize != -1 && requestSize > this.fileUploadBase.getSizeMax()) {
				throw new FileUploadBaseSizeLimitExceededException(
						String.format(
								"the request was rejected because its size (%s) exceeds the configured maximum (%s)",
								Long.valueOf(requestSize),
								Long.valueOf(this.fileUploadBase.getSizeMax())),
						requestSize, this.fileUploadBase.getSizeMax());
			}
			input = new LimitedInputStream1(input, this.fileUploadBase.getSizeMax());
		}

		String charEncoding = this.fileUploadBase.getHeaderEncoding();
		if (charEncoding == null) {
			charEncoding = ctx.getCharacterEncoding();
		}

		boundary = this.fileUploadBase.getBoundary(contentType);
		if (boundary == null) {
			throw new FileUploadException(
					"the request was rejected because no multipart boundary was found");
		}

		notifier = new MultipartStreamProgressNotifier(
				this.fileUploadBase.getListener(), requestSize);
		try {
			multi = new MultipartStream(input, boundary, notifier);
		} catch (IllegalArgumentException iae) {
			throw new FileUploadBaseInvalidContentTypeException(String.format(
					"The boundary specified in the %s header is too long",
					FileUploadBase.getContentType()), iae);
		}
		multi.setHeaderEncoding(charEncoding);

		skipPreamble = true;
		findNextItem();
	}

	/**
	 * Called for finding the next item, if any.
	 *
	 * @return True, if an next item was found, otherwise false.
	 * @throws IOException
	 *             An I/O error occurred.
	 */
	private boolean findNextItem() throws IOException {
		if (eof) {
			return false;
		}
		if (currentItem != null) {
			currentItem.close();
			currentItem = null;
		}
		for (;;) {
			boolean nextPart;
			if (skipPreamble) {
				nextPart = multi.skipPreamble();
			} else {
				nextPart = multi.readBoundary();
			}
			if (!nextPart) {
				if (currentFieldName == null) {
					// Outer multipart terminated -> No more data
					eof = true;
					return false;
				}
				// Inner multipart terminated -> Return to parsing the outer
				multi.setBoundary(boundary);
				currentFieldName = null;
				continue;
			}
			FileItemHeaders headers = this.fileUploadBase
					.getParsedHeaders(multi.readHeaders());
			if (currentFieldName == null) {
				// We're parsing the outer multipart
				String fieldName = this.fileUploadBase.getFieldName(headers);
				if (fieldName != null) {
					String subContentType = headers
							.getHeader(FileUploadBase.getContentType());
					if (subContentType != null
							&& subContentType.toLowerCase(Locale.ENGLISH)
									.startsWith(FileUploadBase.getMultipartMixed())) {
						currentFieldName = fieldName;
						// Multiple files associated with this field name
						byte[] subBoundary = this.fileUploadBase
								.getBoundary(subContentType);
						multi.setBoundary(subBoundary);
						skipPreamble = true;
						continue;
					}
					String fileName = this.fileUploadBase.getFileName(headers);
					currentItem = new FileUploadBaseFileItemStreamImpl(this,
							fileName, fieldName,
							headers.getHeader(FileUploadBase.getContentType()),
							fileName == null, getContentLength(headers));
					currentItem.setHeaders(headers);
					notifier.noteItem();
					itemValid = true;
					return true;
				}
			} else {
				String fileName = this.fileUploadBase.getFileName(headers);
				if (fileName != null) {
					currentItem = new FileUploadBaseFileItemStreamImpl(this,
							fileName, currentFieldName,
							headers.getHeader(FileUploadBase.getContentType()),
							false, getContentLength(headers));
					currentItem.setHeaders(headers);
					notifier.noteItem();
					itemValid = true;
					return true;
				}
			}
			multi.discardBodyData();
		}
	}

	private long getContentLength(FileItemHeaders pHeaders) {
		try {
			return Long.parseLong(pHeaders
					.getHeader(FileUploadBase.getContentLength()));
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Returns, whether another instance of {@link FileItemStream} is available.
	 *
	 * @throws FileUploadException
	 *             Parsing or processing the file item failed.
	 * @throws IOException
	 *             Reading the file item failed.
	 * @return True, if one or more additional file items are available,
	 *         otherwise false.
	 */
	@Override
	public boolean hasNext() throws FileUploadException, IOException {
		if (eof) {
			return false;
		}
		if (itemValid) {
			return true;
		}
		try {
			return findNextItem();
		} catch (FileUploadBaseFileUploadIOException e) {
			// unwrap encapsulated SizeException
			throw (FileUploadException) e.getCause();
		}
	}

	/**
	 * Returns the next available {@link FileItemStream}.
	 *
	 * @throws java.util.NoSuchElementException
	 *             No more items are available. Use {@link #hasNext()} to
	 *             prevent this exception.
	 * @throws FileUploadException
	 *             Parsing or processing the file item failed.
	 * @throws IOException
	 *             Reading the file item failed.
	 * @return FileItemStream instance, which provides access to the next file
	 *         item.
	 */
	@Override
	public FileItemStream next() throws FileUploadException, IOException {
		if (eof || (!itemValid && !hasNext())) {
			throw new NoSuchElementException();
		}
		itemValid = false;
		return currentItem;
	}

	public FileUploadBaseFileItemStreamImpl getCurrentItem() {
		return currentItem;
	}

	public void setCurrentItem(FileUploadBaseFileItemStreamImpl currentItem) {
		this.currentItem = currentItem;
	}

	public String getCurrentFieldName() {
		return currentFieldName;
	}

	public void setCurrentFieldName(String currentFieldName) {
		this.currentFieldName = currentFieldName;
	}

	public boolean isSkipPreamble() {
		return skipPreamble;
	}

	public void setSkipPreamble(boolean skipPreamble) {
		this.skipPreamble = skipPreamble;
	}

	public boolean isItemValid() {
		return itemValid;
	}

	public void setItemValid(boolean itemValid) {
		this.itemValid = itemValid;
	}

	public boolean isEof() {
		return eof;
	}

	public void setEof(boolean eof) {
		this.eof = eof;
	}

	public FileUploadBase getFileUploadBase() {
		return fileUploadBase;
	}

	public MultipartStream getMulti() {
		return multi;
	}

	public MultipartStreamProgressNotifier getNotifier() {
		return notifier;
	}

	public byte[] getBoundary() {
		return boundary;
	}

}