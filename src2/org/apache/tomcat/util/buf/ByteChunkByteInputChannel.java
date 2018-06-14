package org.apache.tomcat.util.buf;

import java.io.IOException;

/** Input interface, used when the buffer is empty
 *
 * Same as java.nio.channel.ReadableByteChannel
 */
public interface ByteChunkByteInputChannel {
    /**
     * Read new bytes ( usually the internal conversion buffer ).
     * The implementation is allowed to ignore the parameters,
     * and mutate the chunk if it wishes to implement its own buffering.
     */
    public int realReadBytes(byte cbuf[], int off, int len)
        throws IOException;
}