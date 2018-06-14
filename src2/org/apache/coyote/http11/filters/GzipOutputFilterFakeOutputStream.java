package org.apache.coyote.http11.filters;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.tomcat.util.buf.ByteChunk;

public class GzipOutputFilterFakeOutputStream extends OutputStream {
	/**
	 * 
	 */
	private final GzipOutputFilter gzipOutputFilter;

	/**
	 * @param gzipOutputFilter
	 */
	public GzipOutputFilterFakeOutputStream(GzipOutputFilter gzipOutputFilter) {
		this.gzipOutputFilter = gzipOutputFilter;
	}

	private ByteChunk outputChunk = new ByteChunk();
	private byte[] singleByteBuffer = new byte[1];

	@Override
	public void write(int b) throws IOException {
		// Shouldn't get used for good performance, but is needed for
		// compatibility with Sun JDK 1.4.0
		singleByteBuffer[0] = (byte) (b & 0xff);
		outputChunk.setBytes(singleByteBuffer, 0, 1);
		this.gzipOutputFilter.getBuffer().doWrite(outputChunk, null);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		outputChunk.setBytes(b, off, len);
		this.gzipOutputFilter.getBuffer().doWrite(outputChunk, null);
	}

	@Override
	public void flush() throws IOException {/* NOOP */
	}

	@Override
	public void close() throws IOException {/* NOOP */
	}
}