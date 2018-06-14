package javax.websocket;

import java.nio.ByteBuffer;

public interface EncoderBinary<T> extends Encoder {

    public ByteBuffer encode(T object) throws EncodeException;
}