package javax.websocket;

public interface EncoderText<T> extends Encoder {

	public String encode(T object) throws EncodeException;
}