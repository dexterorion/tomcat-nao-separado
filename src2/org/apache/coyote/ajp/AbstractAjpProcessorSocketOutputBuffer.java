package org.apache.coyote.ajp;

import java.io.IOException;

import org.apache.coyote.ErrorState;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response3;
import org.apache.tomcat.util.buf.ByteChunk;

public class AbstractAjpProcessorSocketOutputBuffer<S> implements OutputBuffer {

	/**
	 * 
	 */
	private final AbstractAjpProcessor<S> abstractAjpProcessor;

	/**
	 * @param abstractAjpProcessor
	 */
	public AbstractAjpProcessorSocketOutputBuffer(
			AbstractAjpProcessor<S> abstractAjpProcessor) {
		this.abstractAjpProcessor = abstractAjpProcessor;
	}

	/**
	 * Write chunk.
	 */
	@Override
	public int doWrite(ByteChunk chunk, Response3 res) throws IOException {

		if (!this.abstractAjpProcessor.getResponse().isCommitted()) {
			// Validate and write response headers
			try {
				this.abstractAjpProcessor.prepareResponse();
			} catch (IOException e) {
				this.abstractAjpProcessor.setErrorState(ErrorState.CLOSE_NOW, e);
			}
		}

		if (!this.abstractAjpProcessor.isSwallowResponse()) {
			int len = chunk.getLength();
			// 4 - hardcoded, byte[] marshaling overhead
			// Adjust allowed size if packetSize != default
			// (Constants.MAX_PACKET_SIZE)
			int chunkSize = Constants25.getMaxSendSize() + this.abstractAjpProcessor.getPacketSize()
					- Constants25.getMaxPacketSize();
			int off = 0;
			while (len > 0) {
				int thisTime = len;
				if (thisTime > chunkSize) {
					thisTime = chunkSize;
				}
				len -= thisTime;
				this.abstractAjpProcessor.getResponseMessage().reset();
				this.abstractAjpProcessor.getResponseMessage()
						.appendByte(Constants25.getJkAjp13SendBodyChunk());
				this.abstractAjpProcessor.getResponseMessage().appendBytes(chunk.getBytes(),
						chunk.getOffset() + off, thisTime);
				this.abstractAjpProcessor.getResponseMessage().end();
				this.abstractAjpProcessor.output(this.abstractAjpProcessor.getResponseMessage().getBuffer(), 0,
						this.abstractAjpProcessor.getResponseMessage().getLen());

				off += thisTime;
			}

			this.abstractAjpProcessor.setPacketSize(this.abstractAjpProcessor.getPacketSize()+chunk.getLength());
		}
		return chunk.getLength();
	}

	@Override
	public long getBytesWritten() {
		return this.abstractAjpProcessor.getBytesWritten();
	}
}