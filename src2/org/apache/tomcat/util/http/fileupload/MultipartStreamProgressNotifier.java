package org.apache.tomcat.util.http.fileupload;

/**
 * Internal class, which is used to invoke the
 * {@link ProgressListener}.
 */
public class MultipartStreamProgressNotifier {
    /**
     * The listener to invoke.
     */
    private final ProgressListener listener;

    /**
     * Number of expected bytes, if known, or -1.
     */
    private final long contentLength;

    /**
     * Number of bytes, which have been read so far.
     */
    private long bytesRead;

    /**
     * Number of items, which have been read so far.
     */
    private int items;

    /**
     * Creates a new instance with the given listener
     * and content length.
     *
     * @param pListener The listener to invoke.
     * @param pContentLength The expected content length.
     */
    public MultipartStreamProgressNotifier(ProgressListener pListener, long pContentLength) {
        listener = pListener;
        contentLength = pContentLength;
    }

    /**
     * Called to indicate that bytes have been read.
     *
     * @param pBytes Number of bytes, which have been read.
     */
    public void noteBytesRead(int pBytes) {
        /* Indicates, that the given number of bytes have been read from
         * the input stream.
         */
        bytesRead += pBytes;
        notifyListener();
    }

    /**
     * Called to indicate, that a new file item has been detected.
     */
    public void noteItem() {
        ++items;
        notifyListener();
    }

    /**
     * Called for notifying the listener.
     */
    private void notifyListener() {
        if (listener != null) {
            listener.update(bytesRead, contentLength, items);
        }
    }

}