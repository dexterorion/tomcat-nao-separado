package org.apache.tomcat.util.net;

import java.nio.ByteBuffer;

/**
 * Callback interface to be able to expand buffers
 * when buffer overflow exceptions happen
 */
public interface ApplicationBufferHandler {
    public ByteBuffer expand(ByteBuffer buffer, int remaining);
    public ByteBuffer getReadBuffer();
    public ByteBuffer getWriteBuffer();
}