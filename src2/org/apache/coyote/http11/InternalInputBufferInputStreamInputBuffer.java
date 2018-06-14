package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request2;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an input buffer which will read its data from an input
 * stream.
 */
public class InternalInputBufferInputStreamInputBuffer 
    implements InputBuffer {


    /**
	 * 
	 */
	private final InternalInputBuffer internalInputBuffer;

	/**
	 * @param internalInputBuffer
	 */
	public InternalInputBufferInputStreamInputBuffer(
			InternalInputBuffer internalInputBuffer) {
		this.internalInputBuffer = internalInputBuffer;
	}

	/**
     * Read bytes into the specified chunk.
     */
    @Override
    public int doRead(ByteChunk chunk, Request2 req ) 
        throws IOException {

        if (this.internalInputBuffer.getPos() >= this.internalInputBuffer.getLastValid()) {
            if (!this.internalInputBuffer.fill())
                return -1;
        }

        int length = this.internalInputBuffer.getLastValid() - this.internalInputBuffer.getPos();
        chunk.setBytes(this.internalInputBuffer.getBuf(), this.internalInputBuffer.getPos(), length);
        this.internalInputBuffer.setPos(this.internalInputBuffer.getLastValid());

        return (length);
    }
}