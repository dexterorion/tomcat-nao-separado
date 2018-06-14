package org.apache.jasper.xmlparser;

import java.io.IOException;
import java.io.InputStream;

// Adapted from:
// org.apache.xerces.impl.XMLEntityManager.XMLEncodingDetectorRewindableInputStream
/**
 * This class wraps the byte inputstreams we're presented with.
 * We need it because java.io.InputStreams don't provide
 * functionality to reread processed bytes, and they have a habit
 * of reading more than one character when you call their read()
 * methods.  This means that, once we discover the true (declared)
 * encoding of a document, we can neither backtrack to read the
 * whole doc again nor start reading where we are with a new
 * reader.
 *
 * This class allows rewinding an inputStream by allowing a mark
 * to be set, and the stream reset to that position.  <strong>The
 * class assumes that it needs to read one character per
 * invocation when it's read() method is invoked, but uses the
 * underlying InputStream's read(char[], offset length) method--it
 * won't buffer data read this way!</strong>
 *
 * @author Neil Graham, IBM
 * @author Glenn Marcy, IBM
 */
public final class XMLEncodingDetectorRewindableInputStream extends InputStream {

    /**
	 * 
	 */
	private final XMLEncodingDetector xmlEncodingDetector;
	private InputStream fInputStream;
    private byte[] fData;
    private int fEndOffset;
    private int fOffset;
    private int fLength;
    private int fMark;

    public XMLEncodingDetectorRewindableInputStream(XMLEncodingDetector xmlEncodingDetector, InputStream is) {
        this.xmlEncodingDetector = xmlEncodingDetector;
		fData = new byte[XMLEncodingDetector.getDefaultXmldeclBufferSize()];
        fInputStream = is;
        fEndOffset = -1;
        fOffset = 0;
        fLength = 0;
        fMark = 0;
    }

    @Override
    public int read() throws IOException {
        int b = 0;
        if (fOffset < fLength) {
            return fData[fOffset++] & 0xff;
        }
        if (fOffset == fEndOffset) {
            return -1;
        }
        if (fOffset == fData.length) {
            byte[] newData = new byte[fOffset << 1];
            System.arraycopy(fData, 0, newData, 0, fOffset);
            fData = newData;
        }
        b = fInputStream.read();
        if (b == -1) {
            fEndOffset = fOffset;
            return -1;
        }
        fData[fLength++] = (byte)b;
        fOffset++;
        return b & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesLeft = fLength - fOffset;
        if (bytesLeft == 0) {
            if (fOffset == fEndOffset) {
                return -1;
            }
            // better get some more for the voracious reader...
            if (this.xmlEncodingDetector.getfCurrentEntity().isMayReadChunks()) {
                return fInputStream.read(b, off, len);
            }
            int returnedVal = read();
            if (returnedVal == -1) {
                fEndOffset = fOffset;
                return -1;
            }
            b[off] = (byte)returnedVal;
            return 1;
        }
        if (len < bytesLeft) {
            if (len <= 0) {
                return 0;
            }
        }
        else {
            len = bytesLeft;
        }
        if (b != null) {
            System.arraycopy(fData, fOffset, b, off, len);
        }
        fOffset += len;
        return len;
    }

    @Override
    public long skip(long n)
        throws IOException
    {
        int bytesLeft;
        if (n <= 0) {
            return 0;
        }
        bytesLeft = fLength - fOffset;
        if (bytesLeft == 0) {
            if (fOffset == fEndOffset) {
                return 0;
            }
            return fInputStream.skip(n);
        }
        if (n <= bytesLeft) {
            fOffset += n;
            return n;
        }
        fOffset += bytesLeft;
        if (fOffset == fEndOffset) {
            return bytesLeft;
        }
        n -= bytesLeft;
        /*
         * In a manner of speaking, when this class isn't permitting more
         * than one byte at a time to be read, it is "blocking".  The
         * available() method should indicate how much can be read without
         * blocking, so while we're in this mode, it should only indicate
         * that bytes in its buffer are available; otherwise, the result of
         * available() on the underlying InputStream is appropriate.
         */
        return fInputStream.skip(n) + bytesLeft;
    }

    @Override
    public int available() throws IOException {
        int bytesLeft = fLength - fOffset;
        if (bytesLeft == 0) {
            if (fOffset == fEndOffset) {
                return -1;
            }
            return this.xmlEncodingDetector.getfCurrentEntity().isMayReadChunks() ? fInputStream.available()
                : 0;
        }
        return bytesLeft;
    }

    @Override
    public synchronized void mark(int howMuch) {
        fMark = fOffset;
    }

    @Override
    public synchronized void reset() {
        fOffset = fMark;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void close() throws IOException {
        if (fInputStream != null) {
            fInputStream.close();
            fInputStream = null;
        }
    }
}