package javax.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

public interface RemoteEndpointBasic extends RemoteEndpoint {

    /**
     * Send the message, blocking until the message is sent.
     * @param text  The text message to send.
     * @throws IllegalArgumentException if {@code text} is {@code null}.
     * @throws IOException if an I/O error occurs during the sending of the
     *                     message.
     */
	public void sendText(String text) throws IOException;

    /**
     * Send the message, blocking until the message is sent.
     * @param data  The binary message to send
     * @throws IllegalArgumentException if {@code data} is {@code null}.
     * @throws IOException if an I/O error occurs during the sending of the
     *                     message.
     */
	public void sendBinary(ByteBuffer data) throws IOException;

    /**
     * Sends part of a text message to the remote endpoint. Once the first part
     * of a message has been sent, no other text or binary messages may be sent
     * until all remaining parts of this message have been sent.
     *
     * @param fragment  The partial message to send
     * @param isLast    <code>true</code> if this is the last part of the
     *                  message, otherwise <code>false</code>
     * @throws IllegalArgumentException if {@code fragment} is {@code null}.
     * @throws IOException if an I/O error occurs during the sending of the
     *                     message.
     */
	public void sendText(String fragment, boolean isLast) throws IOException;

    /**
     * Sends part of a binary message to the remote endpoint. Once the first
     * part of a message has been sent, no other text or binary messages may be
     * sent until all remaining parts of this message have been sent.
     *
     * @param partialByte   The partial message to send
     * @param isLast        <code>true</code> if this is the last part of the
     *                      message, otherwise <code>false</code>
     * @throws IllegalArgumentException if {@code partialByte} is
     *                     {@code null}.
     * @throws IOException if an I/O error occurs during the sending of the
     *                     message.
     */
	public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException;

	public OutputStream getSendStream() throws IOException;

	public Writer getSendWriter() throws IOException;

    /**
     * Encodes object as a message and sends it to the remote endpoint.
     * @param data  The object to be sent.
     * @throws EncodeException if there was a problem encoding the
     *                     {@code data} object as a websocket message.
     * @throws IllegalArgumentException if {@code data} is {@code null}.
     * @throws IOException if an I/O error occurs during the sending of the
     *                     message.
     */
	public void sendObject(Object data) throws IOException, EncodeException;

}