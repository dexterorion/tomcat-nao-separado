package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response3;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an output buffer which will write data to an output
 * stream.
 */
public class InternalAprOutputBufferSocketOutputBuffer implements OutputBuffer {


    /**
	 * 
	 */
	private final InternalAprOutputBuffer internalAprOutputBuffer;

	/**
	 * @param internalAprOutputBuffer
	 */
	public InternalAprOutputBufferSocketOutputBuffer(
			InternalAprOutputBuffer internalAprOutputBuffer) {
		this.internalAprOutputBuffer = internalAprOutputBuffer;
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
        while (len > 0) {
            int thisTime = len;
            if (this.internalAprOutputBuffer.getBbuf().position() == this.internalAprOutputBuffer.getBbuf().capacity()) {
                this.internalAprOutputBuffer.flushBuffer();
            }
            if (thisTime > this.internalAprOutputBuffer.getBbuf().capacity() - this.internalAprOutputBuffer.getBbuf().position()) {
                thisTime = this.internalAprOutputBuffer.getBbuf().capacity() - this.internalAprOutputBuffer.getBbuf().position();
            }
            this.internalAprOutputBuffer.getBbuf().put(b, start, thisTime);
            len = len - thisTime;
            start = start + thisTime;
        }
        this.internalAprOutputBuffer.setByteCount(this.internalAprOutputBuffer.getByteCount() + chunk.getLength());
        return chunk.getLength();
    }

    @Override
    public long getBytesWritten() {
        return this.internalAprOutputBuffer.getByteCount();
    }
}