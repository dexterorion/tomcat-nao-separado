package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request2;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an input buffer which will read its data from an input
 * stream.
 */
public class InternalNioInputBufferSocketInputBuffer implements InputBuffer {

	/**
	 * 
	 */
	private final InternalNioInputBuffer internalNioInputBuffer;

	/**
	 * @param internalNioInputBuffer
	 */
	public InternalNioInputBufferSocketInputBuffer(
			InternalNioInputBuffer internalNioInputBuffer) {
		this.internalNioInputBuffer = internalNioInputBuffer;
	}

	/**
	 * Read bytes into the specified chunk.
	 */
	@Override
	public int doRead(ByteChunk chunk, Request2 req) throws IOException {

		if (this.internalNioInputBuffer.getPos() >= this.internalNioInputBuffer.getLastValid()) {
			if (!this.internalNioInputBuffer.fill(true, true)) // read body, must be blocking, as the
									// thread is inside the app
				return -1;
		}

		int length = this.internalNioInputBuffer.getLastValid() - this.internalNioInputBuffer.getPos();
		chunk.setBytes(this.internalNioInputBuffer.getBuf(), this.internalNioInputBuffer.getPos(), length);
		this.internalNioInputBuffer.setPos(this.internalNioInputBuffer.getLastValid());

		return (length);
	}
}