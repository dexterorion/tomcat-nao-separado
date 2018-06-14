package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response3;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an output buffer which will write data to an output
 * stream.
 */
public class InternalNioOutputBufferSocketOutputBuffer 
    implements OutputBuffer {


    /**
	 * 
	 */
	private final InternalNioOutputBuffer internalNioOutputBuffer;

	/**
	 * @param internalNioOutputBuffer
	 */
	public InternalNioOutputBufferSocketOutputBuffer(
			InternalNioOutputBuffer internalNioOutputBuffer) {
		this.internalNioOutputBuffer = internalNioOutputBuffer;
	}

	/**
     * Write chunk.
     */
    @Override
    public int doWrite(ByteChunk chunk, Response3 res) 
        throws IOException {

        int len = chunk.getLength();
        int start = chunk.getStart();
        byte[] b = chunk.getBuffer();
        this.internalNioOutputBuffer.addToBB(b, start, len);
        this.internalNioOutputBuffer.setByteCount(this.internalNioOutputBuffer.getByteCount() + chunk.getLength());
        return chunk.getLength();
    }

    @Override
    public long getBytesWritten() {
        return this.internalNioOutputBuffer.getByteCount();
    }
}