package javax.websocket;

public interface MessageHandlerPartial<T> extends MessageHandler {

    /**
     * Called when part of a message is available to be processed.
     *
     * @param messagePart   The message part
     * @param last          <code>true</code> if this is the last part of
     *                      this message, else <code>false</code>
     */
    public void onMessage(T messagePart, boolean last);
}