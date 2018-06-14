package javax.websocket;

public interface DecoderText<T> extends Decoder {

	public T decode(String s) throws DecodeException;

	public boolean willDecode(String s);
}