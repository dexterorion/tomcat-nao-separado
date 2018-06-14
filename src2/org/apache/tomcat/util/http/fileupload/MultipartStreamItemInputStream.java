package org.apache.tomcat.util.http.fileupload;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tomcat.util.http.fileupload.util.Closeable;

/**
 * An {@link InputStream} for reading an items contents.
 */
public class MultipartStreamItemInputStream extends InputStream implements Closeable {

    /**
	 * 
	 */
	private final MultipartStream multipartStream;

	/**
     * The number of bytes, which have been read so far.
     */
    private long total;

    /**
     * The number of bytes, which must be hold, because
     * they might be a part of the boundary.
     */
    private int pad;

    /**
     * The current offset in the buffer.
     */
    private int pos;

    /**
     * Whether the stream is already closed.
     */
    private boolean closed;

    /**
     * Creates a new instance.
     * @param multipartStream TODO
     */
    public MultipartStreamItemInputStream(MultipartStream multipartStream) {
        this.multipartStream = multipartStream;
		findSeparator();
    }

    /**
     * Called for finding the separator.
     */
    private void findSeparator() {
        pos = this.multipartStream.findSeparator();
        if (pos == -1) {
            if (this.multipartStream.getTail() - this.multipartStream.getHead() > this.multipartStream.getKeepRegion()) {
                pad = this.multipartStream.getKeepRegion();
            } else {
                pad = this.multipartStream.getTail() - this.multipartStream.getHead();
            }
        }
    }

    /**
     * Returns the number of bytes, which have been read
     * by the stream.
     *
     * @return Number of bytes, which have been read so far.
     */
    public long getBytesRead() {
        return total;
    }

    /**
     * Returns the number of bytes, which are currently
     * available, without blocking.
     *
     * @throws IOException An I/O error occurs.
     * @return Number of bytes in the buffer.
     */
    @Override
    public int available() throws IOException {
        if (pos == -1) {
            return this.multipartStream.getTail() - this.multipartStream.getHead() - pad;
        }
        return pos - this.multipartStream.getHead();
    }

    /**
     * Offset when converting negative bytes to integers.
     */
    private static final int BYTE_POSITIVE_OFFSET = 256;

    /**
     * Returns the next byte in the stream.
     *
     * @return The next byte in the stream, as a non-negative
     *   integer, or -1 for EOF.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public int read() throws IOException {
        if (closed) {
            throw new FileItemStreamItemSkippedException();
        }
        if (available() == 0 && makeAvailable() == 0) {
            return -1;
        }
        ++total;
        this.multipartStream.setHead(this.multipartStream.getHead()+1);
        int b = this.multipartStream.getBuffer()[this.multipartStream.getHead()];
        if (b >= 0) {
            return b;
        }
        return b + BYTE_POSITIVE_OFFSET;
    }

    /**
     * Reads bytes into the given buffer.
     *
     * @param b The destination buffer, where to write to.
     * @param off Offset of the first byte in the buffer.
     * @param len Maximum number of bytes to read.
     * @return Number of bytes, which have been actually read,
     *   or -1 for EOF.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new FileItemStreamItemSkippedException();
        }
        if (len == 0) {
            return 0;
        }
        int res = available();
        if (res == 0) {
            res = makeAvailable();
            if (res == 0) {
                return -1;
            }
        }
        res = Math.min(res, len);
        System.arraycopy(this.multipartStream.getBuffer(), this.multipartStream.getHead(), b, off, res);
        this.multipartStream.setHead(this.multipartStream.getHead()+res);
        total += res;
        return res;
    }

    /**
     * Closes the input stream.
     *
     * @throws IOException An I/O error occurred.
     */
    @Override
    public void close() throws IOException {
        close(false);
    }

    /**
     * Closes the input stream.
     *
     * @param pCloseUnderlying Whether to close the underlying stream
     *   (hard close)
     * @throws IOException An I/O error occurred.
     */
    public void close(boolean pCloseUnderlying) throws IOException {
        if (closed) {
            return;
        }
        if (pCloseUnderlying) {
            closed = true;
            this.multipartStream.getInput().close();
        } else {
            for (;;) {
                int av = available();
                if (av == 0) {
                    av = makeAvailable();
                    if (av == 0) {
                        break;
                    }
                }
                skip(av);
            }
        }
        closed = true;
    }

    /**
     * Skips the given number of bytes.
     *
     * @param bytes Number of bytes to skip.
     * @return The number of bytes, which have actually been
     *   skipped.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public long skip(long bytes) throws IOException {
        if (closed) {
            throw new FileItemStreamItemSkippedException();
        }
        int av = available();
        if (av == 0) {
            av = makeAvailable();
            if (av == 0) {
                return 0;
            }
        }
        long res = Math.min(av, bytes);
        this.multipartStream.setHead(this.multipartStream.getHead()+(int)res);
        return res;
    }

    /**
     * Attempts to read more data.
     *
     * @return Number of available bytes
     * @throws IOException An I/O error occurred.
     */
    private int makeAvailable() throws IOException {
        if (pos != -1) {
            return 0;
        }

        // Move the data to the beginning of the buffer.
        total += this.multipartStream.getTail() - this.multipartStream.getHead() - pad;
        System.arraycopy(this.multipartStream.getBuffer(), this.multipartStream.getTail() - pad, this.multipartStream.getBuffer(), 0, pad);

        // Refill buffer with new data.
        this.multipartStream.setHead(0);
        this.multipartStream.setTail(pad);

        for (;;) {
            int bytesRead = this.multipartStream.getInput().read(this.multipartStream.getBuffer(), this.multipartStream.getTail(), this.multipartStream.getBufSize() - this.multipartStream.getTail());
            if (bytesRead == -1) {
                // The last pad amount is left in the buffer.
                // Boundary can't be in there so signal an error
                // condition.
                final String msg = "Stream ended unexpectedly";
                throw new MultipartStreamMalformedStreamException(msg);
            }
            if (this.multipartStream.getNotifier() != null) {
                this.multipartStream.getNotifier().noteBytesRead(bytesRead);
            }
            this.multipartStream.setTail(bytesRead+this.multipartStream.getHead());

            findSeparator();
            int av = available();

            if (av > 0 || pos != -1) {
                return av;
            }
        }
    }

    /**
     * Returns, whether the stream is closed.
     *
     * @return True, if the stream is closed, otherwise false.
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

}