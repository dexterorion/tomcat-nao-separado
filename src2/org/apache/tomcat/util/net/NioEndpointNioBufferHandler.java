package org.apache.tomcat.util.net;

import java.nio.ByteBuffer;

// ------------------------------------------------ Application Buffer Handler
public class NioEndpointNioBufferHandler implements ApplicationBufferHandler {
    private ByteBuffer readbuf = null;
    private ByteBuffer writebuf = null;

    public NioEndpointNioBufferHandler(int readsize, int writesize, boolean direct) {
        if ( direct ) {
            readbuf = ByteBuffer.allocateDirect(readsize);
            writebuf = ByteBuffer.allocateDirect(writesize);
        }else {
            readbuf = ByteBuffer.allocate(readsize);
            writebuf = ByteBuffer.allocate(writesize);
        }
    }

    @Override
    public ByteBuffer expand(ByteBuffer buffer, int remaining) {return buffer;}
    @Override
    public ByteBuffer getReadBuffer() {return readbuf;}
    @Override
    public ByteBuffer getWriteBuffer() {return writebuf;}

}