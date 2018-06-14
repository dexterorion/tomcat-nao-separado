package org.apache.tomcat.util.codec.binary;

/**
 * Holds thread context so classes can be thread-safe.
 *
 * This class is not itself thread-safe; each thread must allocate its own copy.
 *
 * @since 1.7
 */
public class BaseNCodecContext {

    /**
     * Place holder for the bytes we're dealing with for our based logic.
     * Bitwise operations store and extract the encoding or decoding from this variable.
     */
	private int ibitWorkArea;

    /**
     * Place holder for the bytes we're dealing with for our based logic.
     * Bitwise operations store and extract the encoding or decoding from this variable.
     */
	private long lbitWorkArea;

    /**
     * Buffer for streaming.
     */
	private byte[] buffer;

    /**
     * Position where next character should be written in the buffer.
     */
	private int pos;

    /**
     * Position where next character should be read from the buffer.
     */
	private int readPos;

    /**
     * Boolean flag to indicate the EOF has been reached. Once EOF has been reached, this object becomes useless,
     * and must be thrown away.
     */
	private boolean eof;

    /**
     * Variable tracks how many characters have been written to the current line. Only used when encoding. We use
     * it to make sure each encoded line never goes beyond lineLength (if lineLength > 0).
     */
	private int currentLinePos;

    /**
     * Writes to the buffer only occur after every 3/5 reads when encoding, and every 4/8 reads when decoding. This
     * variable helps track that.
     */
	private int modulus;

    public BaseNCodecContext() {
    }

    /**
     * Returns a String useful for debugging (especially within a debugger.)
     *
     * @return a String useful for debugging.
     */
    @SuppressWarnings("boxing") // OK to ignore boxing here
    @Override
    public String toString() {
        return String.format("%s[buffer=%s, currentLinePos=%s, eof=%s, ibitWorkArea=%s, lbitWorkArea=%s, " +
                "modulus=%s, pos=%s, readPos=%s]", this.getClass().getSimpleName(), buffer, currentLinePos, eof,
                ibitWorkArea, lbitWorkArea, modulus, pos, readPos);
    }

	public int getIbitWorkArea() {
		return ibitWorkArea;
	}

	public void setIbitWorkArea(int ibitWorkArea) {
		this.ibitWorkArea = ibitWorkArea;
	}

	public long getLbitWorkArea() {
		return lbitWorkArea;
	}

	public void setLbitWorkArea(long lbitWorkArea) {
		this.lbitWorkArea = lbitWorkArea;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public int getReadPos() {
		return readPos;
	}

	public void setReadPos(int readPos) {
		this.readPos = readPos;
	}

	public boolean isEof() {
		return eof;
	}

	public void setEof(boolean eof) {
		this.eof = eof;
	}

	public int getCurrentLinePos() {
		return currentLinePos;
	}

	public void setCurrentLinePos(int currentLinePos) {
		this.currentLinePos = currentLinePos;
	}

	public int getModulus() {
		return modulus;
	}

	public void setModulus(int modulus) {
		this.modulus = modulus;
	}
}