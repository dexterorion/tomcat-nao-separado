package javax.websocket;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

public interface RemoteEndpointAsync extends RemoteEndpoint {

    /**
     * Obtain the timeout (in milliseconds) for sending a message
     * asynchronously. The default value is determined by
     * {@link WebSocketContainer#getDefaultAsyncSendTimeout()}.
     * @return  The current send timeout in milliseconds. A non-positive
     *          value means an infinite timeout.
     */
	public long getSendTimeout();

    /**
     * Set the timeout (in milliseconds) for sending a message
     * asynchronously. The default value is determined by
     * {@link WebSocketContainer#getDefaultAsyncSendTimeout()}.
     * @param timeout   The new timeout for sending messages asynchronously
     *                  in milliseconds. A non-positive value means an
     *                  infinite timeout.
     */
	public void setSendTimeout(long timeout);

    /**
     * Send the message asynchronously, using the SendHandler to signal to the
     * client when the message has been sent.
     * @param text          The text message to send
     * @param completion    Used to signal to the client when the message has
     *                      been sent
     */
	public void sendText(String text, SendHandler completion);

    /**
     * Send the message asynchronously, using the Future to signal to the
     * client when the message has been sent.
     * @param text          The text message to send
     * @return A Future that signals when the message has been sent.
     */
	public Future<Void> sendText(String text);

    /**
     * Send the message asynchronously, using the Future to signal to the client
     * when the message has been sent.
     * @param data          The text message to send
     * @return A Future that signals when the message has been sent.
     * @throws IllegalArgumentException if {@code data} is {@code null}.
     */
	public Future<Void> sendBinary(ByteBuffer data);

    /**
     * Send the message asynchronously, using the SendHandler to signal to the
     * client when the message has been sent.
     * @param data          The text message to send
     * @param completion    Used to signal to the client when the message has
     *                      been sent
     * @throws IllegalArgumentException if {@code data} or {@code completion}
     *                      is {@code null}.
     */
	public void sendBinary(ByteBuffer data, SendHandler completion);

    /**
     * Encodes object as a message and sends it asynchronously, using the
     * Future to signal to the client when the message has been sent.
     * @param obj           The object to be sent.
     * @return A Future that signals when the message has been sent.
     * @throws IllegalArgumentException if {@code obj} is {@code null}.
     */
	public Future<Void> sendObject(Object obj);

    /**
     * Encodes object as a message and sends it asynchronously, using the
     * SendHandler to signal to the client when the message has been sent.
     * @param obj           The object to be sent.
     * @param completion    Used to signal to the client when the message has
     *                      been sent
     * @throws IllegalArgumentException if {@code obj} or
     *                      {@code completion} is {@code null}.
     */
	public void sendObject(Object obj, SendHandler completion);

}