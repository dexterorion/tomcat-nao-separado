package javax.websocket;

public interface MessageHandlerWhole<T> extends MessageHandler {

    /**
     * Called when a whole message is available to be processed.
     *
     * @param message   The message
     */
	public void onMessage(T message);
}