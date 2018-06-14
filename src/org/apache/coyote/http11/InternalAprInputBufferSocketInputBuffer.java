package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request2;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an input buffer which will read its data from an input
 * stream.
 */
public class InternalAprInputBufferSocketInputBuffer 
    implements InputBuffer {


    /**
	 * 
	 */
	private final InternalAprInputBuffer internalAprInputBuffer;

	/**
	 * @param internalAprInputBuffer
	 */
	public InternalAprInputBufferSocketInputBuffer(
			InternalAprInputBuffer internalAprInputBuffer) {
		this.internalAprInputBuffer = internalAprInputBuffer;
	}

	/**
     * Read bytes into the specified chunk.
     */
    @Override
    public int doRead(ByteChunk chunk, Request2 req ) 
        throws IOException {

        if (this.internalAprInputBuffer.getPos() >= this.internalAprInputBuffer.getLastValid()) {
            if (!this.internalAprInputBuffer.fill())
                return -1;
        }

        int length = this.internalAprInputBuffer.getLastValid() - this.internalAprInputBuffer.getPos();
        chunk.setBytes(this.internalAprInputBuffer.getBuf(), this.internalAprInputBuffer.getPos(), length);
        this.internalAprInputBuffer.setPos(this.internalAprInputBuffer.getLastValid());

        return (length);
    }
}