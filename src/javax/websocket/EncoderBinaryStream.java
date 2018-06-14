package javax.websocket;

import java.io.IOException;
import java.io.OutputStream;

public interface EncoderBinaryStream<T> extends Encoder {

	public void encode(T object, OutputStream os)
            throws EncodeException, IOException;
}