package org.apache.catalina.tribes.io;

public interface BufferPoolBufferPoolAPI {
    public void setMaxSize(int bytes);

    public XByteBuffer getBuffer(int minSize, boolean discard);

    public void returnBuffer(XByteBuffer buffer);

    public void clear();
}