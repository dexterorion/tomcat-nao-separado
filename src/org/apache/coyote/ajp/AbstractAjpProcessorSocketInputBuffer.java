package org.apache.coyote.ajp;

import java.io.IOException;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request2;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * This class is an input buffer which will read its data from an input
 * stream.
 */
public class AbstractAjpProcessorSocketInputBuffer<S> implements InputBuffer {

	/**
	 * 
	 */
	private final AbstractAjpProcessor<S> abstractAjpProcessor;

	/**
	 * @param abstractAjpProcessor
	 */
	public AbstractAjpProcessorSocketInputBuffer(
			AbstractAjpProcessor<S> abstractAjpProcessor) {
		this.abstractAjpProcessor = abstractAjpProcessor;
	}

	/**
	 * Read bytes into the specified chunk.
	 */
	@Override
	public int doRead(ByteChunk chunk, Request2 req) throws IOException {

		if (this.abstractAjpProcessor.isEndOfStream()) {
			return -1;
		}
		if (this.abstractAjpProcessor.isFirst() && req.getContentLengthLong() > 0) {
			// Handle special first-body-chunk
			if (!this.abstractAjpProcessor.receive()) {
				return 0;
			}
		} else if (this.abstractAjpProcessor.isEmpty()) {
			if (!this.abstractAjpProcessor.refillReadBuffer()) {
				return -1;
			}
		}
		ByteChunk bc = this.abstractAjpProcessor.getBodyBytes().getByteChunk();
		chunk.setBytes(bc.getBuffer(), bc.getStart(), bc.getLength());
		this.abstractAjpProcessor.setEmpty(true);
		return chunk.getLength();

	}

}