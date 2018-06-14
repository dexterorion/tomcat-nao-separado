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

package org.apache.tomcat.util.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.tomcat.util.res.StringManager3;

/**
 *
 * Base class for a SocketChannel wrapper used by the endpoint. This way, logic
 * for a SSL socket channel remains the same as for a non SSL, making sure we
 * don't need to code for any exception cases.
 *
 * @author Filip Hanik
 * @version 1.0
 */
public class NioChannel implements ByteChannel {

	private static final StringManager3 sm = StringManager3
			.getManager("org.apache.tomcat.util.net.res");

	private static ByteBuffer emptyBuf = ByteBuffer.allocate(0);

	private SocketChannel sc = null;

	private ApplicationBufferHandler bufHandler;

	private NioEndpointPoller poller;

	private boolean sendFile = false;

	public NioChannel(SocketChannel channel, ApplicationBufferHandler bufHandler)
			throws IOException {
		this.setSc(channel);
		this.bufHandler = bufHandler;
	}

	public void reset() throws IOException {
		bufHandler.getReadBuffer().clear();
		bufHandler.getWriteBuffer().clear();
		this.sendFile = false;
	}

	public int getBufferSize() {
		if (bufHandler == null)
			return 0;
		int size = 0;
		size += bufHandler.getReadBuffer() != null ? bufHandler.getReadBuffer()
				.capacity() : 0;
		size += bufHandler.getWriteBuffer() != null ? bufHandler
				.getWriteBuffer().capacity() : 0;
		return size;
	}

	/**
	 * Returns true if the network buffer has been flushed out and is empty.
	 *
	 * @param block
	 *            Unused. May be used when overridden
	 * @param s
	 *            Unused. May be used when overridden
	 * @param timeout
	 *            Unused. May be used when overridden
	 * @return Always returns <code>true</code> since there is no network buffer
	 *         in the regular channel
	 * @throws IOException
	 */
	public boolean flush(boolean block, Selector s, long timeout)
			throws IOException {
		return true;
	}

	/**
	 * Closes this channel.
	 *
	 * @throws IOException
	 *             If an I/O error occurs TODO Implement this
	 *             java.nio.channels.Channel method
	 */
	@Override
	public void close() throws IOException {
		getIOChannel().socket().close();
		getIOChannel().close();
	}

	public void close(boolean force) throws IOException {
		if (isOpen() || force)
			close();
	}

	/**
	 * Tells whether or not this channel is open.
	 *
	 * @return <tt>true</tt> if, and only if, this channel is open TODO
	 *         Implement this java.nio.channels.Channel method
	 */
	@Override
	public boolean isOpen() {
		return getSc().isOpen();
	}

	/**
	 * Writes a sequence of bytes to this channel from the given buffer.
	 *
	 * @param src
	 *            The buffer from which bytes are to be retrieved
	 * @return The number of bytes written, possibly zero
	 * @throws IOException
	 *             If some other I/O error occurs TODO Implement this
	 *             java.nio.channels.WritableByteChannel method
	 */
	@Override
	public int write(ByteBuffer src) throws IOException {
		checkInterruptStatus();
		return getSc().write(src);
	}

	/**
	 * Reads a sequence of bytes from this channel into the given buffer.
	 *
	 * @param dst
	 *            The buffer into which bytes are to be transferred
	 * @return The number of bytes read, possibly zero, or <tt>-1</tt> if the
	 *         channel has reached end-of-stream
	 * @throws IOException
	 *             If some other I/O error occurs TODO Implement this
	 *             java.nio.channels.ReadableByteChannel method
	 */
	@Override
	public int read(ByteBuffer dst) throws IOException {
		return getSc().read(dst);
	}

	public Object getAttachment(boolean remove) {
		NioEndpointPoller pol = getPoller();
		Selector sel = pol != null ? pol.getSelector() : null;
		SelectionKey key = sel != null ? getIOChannel().keyFor(sel) : null;
		Object att = key != null ? key.attachment() : null;
		if (key != null && att != null && remove)
			key.attach(null);
		return att;
	}

	/**
	 * getBufHandler
	 *
	 * @return ApplicationBufferHandler TODO Implement this
	 *         org.apache.tomcat.util.net.SecureNioChannel method
	 */
	public ApplicationBufferHandler getBufHandler() {
		return bufHandler;
	}

	public NioEndpointPoller getPoller() {
		return poller;
	}

	/**
	 * getIOChannel
	 *
	 * @return SocketChannel TODO Implement this
	 *         org.apache.tomcat.util.net.SecureNioChannel method
	 */
	public SocketChannel getIOChannel() {
		return getSc();
	}

	/**
	 * isClosing
	 *
	 * @return boolean TODO Implement this
	 *         org.apache.tomcat.util.net.SecureNioChannel method
	 */
	public boolean isClosing() {
		return false;
	}

	/**
	 * isInitHandshakeComplete
	 *
	 * @return boolean
	 */
	public boolean isHandshakeComplete() {
		return true;
	}

	public int handshake(boolean read, boolean write) throws IOException {
		return 0;
	}

	public void setPoller(NioEndpointPoller poller) {
		this.poller = poller;
	}

	public void setIOChannel(SocketChannel IOChannel) {
		this.setSc(IOChannel);
	}

	@Override
	public String toString() {
		return super.toString() + ":" + this.getSc().toString();
	}

	public int getOutboundRemaining() {
		return 0;
	}

	/**
	 * Return true if the buffer wrote data
	 * 
	 * @throws IOException
	 */
	public boolean flushOutbound() throws IOException {
		return false;
	}

	public boolean isSendFile() {
		return sendFile;
	}

	public void setSendFile(boolean s) {
		this.sendFile = s;
	}

	/**
	 * This method should be used to check the interrupt status before
	 * attempting a write.
	 *
	 * If a thread has been interrupted and the interrupt has not been cleared
	 * then an attempt to write to the socket will fail. When this happens the
	 * socket is removed from the poller without the socket being selected. This
	 * results in a connection limit leak for NIO as the endpoint expects the
	 * socket to be selected even in error conditions.
	 */
	protected void checkInterruptStatus() throws IOException {
		if (Thread.interrupted()) {
			throw new IOException(sm.getString("channel.nio.interrupted"));
		}
	}

	public SocketChannel getSc() {
		return sc;
	}

	public void setSc(SocketChannel sc) {
		this.sc = sc;
	}

	public static ByteBuffer getEmptyBuf() {
		return emptyBuf;
	}

	public static void setEmptyBuf(ByteBuffer emptyBuf) {
		NioChannel.emptyBuf = emptyBuf;
	}

	public static StringManager3 getSm() {
		return sm;
	}

	public void setBufHandler(ApplicationBufferHandler bufHandler) {
		this.bufHandler = bufHandler;
	}

}
