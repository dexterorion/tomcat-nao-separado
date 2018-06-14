package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response3;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an output buffer which will write data to an output
 * stream.
 */
public class InternalOutputBufferOutputStreamOutputBuffer 
    implements OutputBuffer {


    /**
	 * 
	 */
	private final InternalOutputBuffer internalOutputBuffer;

	/**
	 * @param internalOutputBuffer
	 */
	public InternalOutputBufferOutputStreamOutputBuffer(
			InternalOutputBuffer internalOutputBuffer) {
		this.internalOutputBuffer = internalOutputBuffer;
	}

	/**
     * Write chunk.
     */
    @Override
    public int doWrite(ByteChunk chunk, Response3 res) 
        throws IOException {

        int length = chunk.getLength();
        if (this.internalOutputBuffer.isUseSocketBuffer()) {
            this.internalOutputBuffer.getSocketBuffer().append(chunk.getBuffer(), chunk.getStart(), 
                                length);
        } else {
            this.internalOutputBuffer.getOutputStream().write(chunk.getBuffer(), chunk.getStart(), 
                               length);
        }
        this.internalOutputBuffer.setByteCount(this.internalOutputBuffer.getByteCount() + chunk.getLength());
        return chunk.getLength();
    }

    @Override
    public long getBytesWritten() {
        return this.internalOutputBuffer.getByteCount();
    }
}