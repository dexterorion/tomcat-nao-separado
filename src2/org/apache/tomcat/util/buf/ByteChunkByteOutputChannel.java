package org.apache.tomcat.util.buf;

import java.io.IOException;

public interface ByteChunkByteOutputChannel {
    /**
     * Send the bytes ( usually the internal conversion buffer ).
     * Expect 8k output if the buffer is full.
     */
    public void realWriteBytes(byte cbuf[], int off, int len)
        throws IOException;
}