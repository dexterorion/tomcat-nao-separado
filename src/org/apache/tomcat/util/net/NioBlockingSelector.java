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

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class NioBlockingSelector {

    private static final Log log = LogFactory.getLog(NioBlockingSelector.class);

    private static int threadCounter = 0;

    private Queue<NioBlockingSelectorKeyReference> keyReferenceQueue =
            new ConcurrentLinkedQueue<NioBlockingSelectorKeyReference>();

    private Selector sharedSelector;

    private NioBlockingSelectorBlockPoller poller;
    public NioBlockingSelector() {

    }

    public void open(Selector selector) {
        sharedSelector = selector;
        poller = new NioBlockingSelectorBlockPoller();
        poller.setSelector(sharedSelector);
        poller.setDaemon(true);
        poller.setName("NioBlockingSelector.BlockPoller-"+(++threadCounter));
        poller.start();
    }

    public void close() {
        if (poller!=null) {
            poller.disable();
            poller.interrupt();
            poller = null;
        }
    }

    /**
     * Performs a blocking write using the bytebuffer for data to be written
     * If the <code>selector</code> parameter is null, then it will perform a busy write that could
     * take up a lot of CPU cycles.
     * @param buf ByteBuffer - the buffer containing the data, we will write as long as <code>(buf.hasRemaining()==true)</code>
     * @param socket SocketChannel - the socket to write data to
     * @param writeTimeout long - the timeout for this write operation in milliseconds, -1 means no timeout
     * @return int - returns the number of bytes written
     * @throws EOFException if write returns -1
     * @throws SocketTimeoutException if the write times out
     * @throws IOException if an IO Exception occurs in the underlying socket logic
     */
    public int write(ByteBuffer buf, NioChannel socket, long writeTimeout)
            throws IOException {
        SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
        if ( key == null ) throw new IOException("Key no longer registered");
        NioBlockingSelectorKeyReference reference = keyReferenceQueue.poll();
        if (reference == null) {
            reference = new NioBlockingSelectorKeyReference();
        }
        NioEndpointKeyAttachment att = (NioEndpointKeyAttachment) key.attachment();
        int written = 0;
        boolean timedout = false;
        int keycount = 1; //assume we can write
        long time = System.currentTimeMillis(); //start the timeout timer
        try {
            while ( (!timedout) && buf.hasRemaining()) {
                if (keycount > 0) { //only write if we were registered for a write
                    int cnt = socket.write(buf); //write the data
                    if (cnt == -1)
                        throw new EOFException();
                    written += cnt;
                    if (cnt > 0) {
                        time = System.currentTimeMillis(); //reset our timeout timer
                        continue; //we successfully wrote, try again without a selector
                    }
                }
                try {
                    if ( att.getWriteLatch()==null || att.getWriteLatch().getCount()==0) att.startWriteLatch(1);
                    poller.add(att,SelectionKey.OP_WRITE,reference);
                    if (writeTimeout < 0) {
                        att.awaitWriteLatch(Long.MAX_VALUE,TimeUnit.MILLISECONDS);
                    } else {
                        att.awaitWriteLatch(writeTimeout,TimeUnit.MILLISECONDS);
                    }
                }catch (InterruptedException ignore) {
                    Thread.interrupted();
                }
                if ( att.getWriteLatch()!=null && att.getWriteLatch().getCount()> 0) {
                    //we got interrupted, but we haven't received notification from the poller.
                    keycount = 0;
                }else {
                    //latch countdown has happened
                    keycount = 1;
                    att.resetWriteLatch();
                }

                if (writeTimeout > 0 && (keycount == 0))
                    timedout = (System.currentTimeMillis() - time) >= writeTimeout;
            }
            if (timedout)
                throw new SocketTimeoutException();
        } finally {
            poller.remove(att,SelectionKey.OP_WRITE);
            if (timedout && reference.getKey()!=null) {
                poller.cancelKey(reference.getKey());
            }
            reference.setKey(null);
            keyReferenceQueue.add(reference);
        }
        return written;
    }

    /**
     * Performs a blocking read using the bytebuffer for data to be read
     * If the <code>selector</code> parameter is null, then it will perform a busy read that could
     * take up a lot of CPU cycles.
     * @param buf ByteBuffer - the buffer containing the data, we will read as until we have read at least one byte or we timed out
     * @param socket SocketChannel - the socket to write data to
     * @param readTimeout long - the timeout for this read operation in milliseconds, -1 means no timeout
     * @return int - returns the number of bytes read
     * @throws EOFException if read returns -1
     * @throws SocketTimeoutException if the read times out
     * @throws IOException if an IO Exception occurs in the underlying socket logic
     */
    public int read(ByteBuffer buf, NioChannel socket, long readTimeout) throws IOException {
        SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
        if ( key == null ) throw new IOException("Key no longer registered");
        NioBlockingSelectorKeyReference reference = keyReferenceQueue.poll();
        if (reference == null) {
            reference = new NioBlockingSelectorKeyReference();
        }
        NioEndpointKeyAttachment att = (NioEndpointKeyAttachment) key.attachment();
        int read = 0;
        boolean timedout = false;
        int keycount = 1; //assume we can read
        long time = System.currentTimeMillis(); //start the timeout timer
        try {
            while(!timedout) {
                if (keycount > 0) { //only read if we were registered for a read
                    read = socket.read(buf);
                    if (read == -1)
                        throw new EOFException();
                    if (read > 0)
                        break;
                }
                try {
                    if ( att.getReadLatch()==null || att.getReadLatch().getCount()==0) att.startReadLatch(1);
                    poller.add(att,SelectionKey.OP_READ, reference);
                    if (readTimeout < 0) {
                        att.awaitReadLatch(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    } else {
                        att.awaitReadLatch(readTimeout, TimeUnit.MILLISECONDS);
                    }
                }catch (InterruptedException ignore) {
                    Thread.interrupted();
                }
                if ( att.getReadLatch()!=null && att.getReadLatch().getCount()> 0) {
                    //we got interrupted, but we haven't received notification from the poller.
                    keycount = 0;
                }else {
                    //latch countdown has happened
                    keycount = 1;
                    att.resetReadLatch();
                }
                if (readTimeout >= 0 && (keycount == 0))
                    timedout = (System.currentTimeMillis() - time) >= readTimeout;
            }
            if (timedout)
                throw new SocketTimeoutException();
        } finally {
            poller.remove(att,SelectionKey.OP_READ);
            if (timedout && reference.getKey()!=null) {
                poller.cancelKey(reference.getKey());
            }
            reference.setKey(null);
            keyReferenceQueue.add(reference);
        }
        return read;
    }

	public static Log getLog() {
		return log;
	}

}
