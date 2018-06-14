package javax.websocket;

import java.io.IOException;
import java.io.Writer;

public interface EncoderTextStream<T> extends Encoder {

	public void encode(T object, Writer writer)
            throws EncodeException, IOException;
}