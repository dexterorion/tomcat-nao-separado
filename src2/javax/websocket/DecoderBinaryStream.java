package javax.websocket;

import java.io.IOException;
import java.io.InputStream;

public interface DecoderBinaryStream<T> extends Decoder {

	public T decode(InputStream is) throws DecodeException, IOException;
}