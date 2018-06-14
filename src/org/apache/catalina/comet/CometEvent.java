/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.comet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The CometEvent interface.
 *
 * @author Filip Hanik
 * @author Remy Maucherat
 */
public interface CometEvent {
	/**
     * Returns the HttpServletRequest.
     *
     * @return HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest();

    /**
     * Returns the HttpServletResponse.
     *
     * @return HttpServletResponse
     */
    public HttpServletResponse getHttpServletResponse();

    /**
     * Returns the event type.
     *
     * @return EventType
     */
    public CometEventEventType getEventType();

    /**
     * Returns the sub type of this event.
     *
     * @return EventSubType
     */
    public CometEventEventSubType getEventSubType();

    /**
     * Ends the Comet session. This signals to the container that
     * the container wants to end the comet session. This will send back to the
     * client a notice that the server has no more data to send as part of this
     * request. The servlet should perform any needed cleanup as if it had received
     * an END or ERROR event.
     *
     * @throws IOException if an IO exception occurs
     */
    public void close() throws IOException;

    /**
     * Sets the timeout for this Comet connection. Please NOTE, that the implementation
     * of a per connection timeout is OPTIONAL and MAY NOT be implemented.<br/>
     * This method sets the timeout in milliseconds of idle time on the connection.
     * The timeout is reset every time data is received from the connection or data is flushed
     * using <code>response.flushBuffer()</code>. If a timeout occurs, the
     * <code>error(HttpServletRequest, HttpServletResponse)</code> method is invoked. The
     * web application SHOULD NOT attempt to reuse the request and response objects after a timeout
     * as the <code>error(HttpServletRequest, HttpServletResponse)</code> method indicates.<br/>
     * This method should not be called asynchronously, as that will have no effect.
     *
     * @param timeout The timeout in milliseconds for this connection, must be a positive value, larger than 0
     * @throws IOException An IOException may be thrown to indicate an IO error,
     *         or that the EOF has been reached on the connection
     * @throws ServletException An exception has occurred, as specified by the root
     *         cause
     * @throws UnsupportedOperationException if per connection timeout is not supported, either at all or at this phase
     *         of the invocation.
     */
    public void setTimeout(int timeout)
        throws IOException, ServletException, UnsupportedOperationException;

}
