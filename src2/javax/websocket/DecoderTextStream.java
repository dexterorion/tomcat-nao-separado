package javax.websocket;

import java.io.IOException;
import java.io.Reader;

public interface DecoderTextStream<T> extends Decoder {

	public T decode(Reader reader) throws DecodeException, IOException;
}