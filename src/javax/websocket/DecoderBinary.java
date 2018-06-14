package javax.websocket;

import java.nio.ByteBuffer;

public interface DecoderBinary<T> extends Decoder {

    public T decode(ByteBuffer bytes) throws DecodeException;

    public boolean willDecode(ByteBuffer bytes);
}