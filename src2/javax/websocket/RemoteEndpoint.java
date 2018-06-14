/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;


public interface RemoteEndpoint {

	/**
     * Enable or disable the batching of outgoing messages for this endpoint. If
     * batching is disabled when it was previously enabled then this method will
     * block until any currently batched messages have been written.
     *
     * @param batchingAllowed   New setting
     * @throws IOException      If changing the value resulted in a call to
     *                          {@link #flushBatch()} and that call threw an
     *                          {@link IOException}.
     */
    public void setBatchingAllowed(boolean batchingAllowed) throws IOException;

    /**
     * Obtains the current batching status of the endpoint.
     *
     * @return <code>true</code> if batching is enabled, otherwise
     *         <code>false</code>.
     */
    public boolean getBatchingAllowed();

    /**
     * Flush any currently batched messages to the remote endpoint. This method
     * will block until the flush completes.
     *
     * @throws IOException If an I/O error occurs while flushing
     */
    public void flushBatch() throws IOException;

    /**
     * Send a ping message blocking until the message has been sent. Note that
     * if a message is in the process of being sent asynchronously, this method
     * will block until that message and this ping has been sent.
     *
     * @param applicationData   The payload for the ping message
     *
     * @throws IOException If an I/O error occurs while sending the ping
     * @throws IllegalArgumentException if the applicationData is too large for
     *         a control message (max 125 bytes)
     */
    public void sendPing(ByteBuffer applicationData)
            throws IOException, IllegalArgumentException;

    /**
     * Send a pong message blocking until the message has been sent. Note that
     * if a message is in the process of being sent asynchronously, this method
     * will block until that message and this pong has been sent.
     *
     * @param applicationData   The payload for the pong message
     *
     * @throws IOException If an I/O error occurs while sending the pong
     * @throws IllegalArgumentException if the applicationData is too large for
     *         a control message (max 125 bytes)
     */
    public void sendPong(ByteBuffer applicationData)
            throws IOException, IllegalArgumentException;
}

