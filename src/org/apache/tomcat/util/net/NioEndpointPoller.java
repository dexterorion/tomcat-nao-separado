package org.apache.tomcat.util.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.tomcat.util.ExceptionUtils2;

public class NioEndpointPoller implements Runnable {

    /**
	 * 
	 */
	private NioEndpoint nioEndpoint;

	/**
	 * @param nioEndpoint
	 */
	public NioEndpointPoller(NioEndpoint nioEndpoint) {
		this.nioEndpoint = nioEndpoint;
	}

	private Selector selector;
    private ConcurrentLinkedQueue<Runnable> events = new ConcurrentLinkedQueue<Runnable>();

    private volatile boolean close = false;
    private long nextExpiration = 0;//optimize expiration handling

    private AtomicLong wakeupCounter = new AtomicLong(0l);

    private volatile int keyCount = 0;

    public NioEndpointPoller() throws IOException {
        synchronized (Selector.class) {
            // Selector.open() isn't thread safe
            // http://bugs.sun.com/view_bug.do?bug_id=6427854
            // Affects 1.6.0_29, fixed in 1.7.0_01
            this.selector = Selector.open();
        }
    }

    public int getKeyCount() { return keyCount; }

    public Selector getSelector() { return selector;}

    /**
     * Destroy the poller.
     */
    protected void destroy() {
        // Wait for polltime before doing anything, so that the poller threads
        // exit, otherwise parallel closure of sockets which are still
        // in the poller can cause problems
        close = true;
        selector.wakeup();
    }

    /**
     * Only used in this class. Will be made private in Tomcat 8.0.x
     * @deprecated
     */
    @Deprecated
    public void addEvent(Runnable event) {
        events.offer(event);
        if ( wakeupCounter.incrementAndGet() == 0 ) selector.wakeup();
    }

    /**
     * Unused. Will be removed in Tomcat 8.0.x
     * @deprecated
     */
    @Deprecated
    public void cometInterest(NioChannel socket) {
    	NioEndpointKeyAttachment att = (NioEndpointKeyAttachment)socket.getAttachment(false);
        add(socket,att.getCometOps());
        if ( (att.getCometOps()&NioEndpoint.getOpCallback()) == NioEndpoint.getOpCallback() ) {
            nextExpiration = 0; //force the check for faster callback
            selector.wakeup();
        }
    }

    /**
     * Add specified socket and associated pool to the poller. The socket will
     * be added to a temporary array, and polled first after a maximum amount
     * of time equal to pollTime (in most cases, latency will be much lower,
     * however).
     *
     * @param socket to add to the poller
     */
    public void add(final NioChannel socket) {
        add(socket,SelectionKey.OP_READ);
    }

    public void add(final NioChannel socket, final int interestOps) {
    	NioEndpointPollerEvent r = this.nioEndpoint.getEventCache().poll();
        if ( r==null) r = new NioEndpointPollerEvent(socket,null,interestOps);
        else r.reset(socket,null,interestOps);
        addEvent(r);
        if (close) {
            this.nioEndpoint.processSocket(socket, SocketStatus.STOP, false);
        }
    }

    /**
     * Processes events in the event queue of the Poller.
     *
     * @return <code>true</code> if some events were processed,
     *   <code>false</code> if queue was empty
     */
    public boolean events() {
        boolean result = false;

        Runnable r = null;
        while ( (r = events.poll()) != null ) {
            result = true;
            try {
                r.run();
                if ( r instanceof NioEndpointPollerEvent ) {
                    ((NioEndpointPollerEvent)r).reset();
                    this.nioEndpoint.getEventCache().offer((NioEndpointPollerEvent)r);
                }
            } catch ( Throwable x ) {
                NioEndpoint.getLogVariable().error("",x);
            }
        }

        return result;
    }

    public void register(final NioChannel socket) {
        socket.setPoller(this);
        NioEndpointKeyAttachment key = this.nioEndpoint.getKeyCache().poll();
        final NioEndpointKeyAttachment ka = key!=null?key:new NioEndpointKeyAttachment(socket);
        ka.reset(this,socket,this.nioEndpoint.getSocketProperties().getSoTimeout());
        ka.setKeepAliveLeft(this.nioEndpoint.getMaxKeepAliveRequests());
        ka.setSecure(this.nioEndpoint.isSSLEnabled());
        NioEndpointPollerEvent r = this.nioEndpoint.getEventCache().poll();
        ka.interestOps(SelectionKey.OP_READ);//this is what OP_REGISTER turns into.
        if ( r==null) r = new NioEndpointPollerEvent(socket,ka,NioEndpoint.getOpRegister());
        else r.reset(socket,ka,NioEndpoint.getOpRegister());
        addEvent(r);
    }
    
    public void cancelledKey(SelectionKey key, SocketStatus status, boolean dispatch) {
        try {
            if ( key == null ) return;//nothing to do
            NioEndpointKeyAttachment ka = (NioEndpointKeyAttachment) key.attachment();
            if (ka != null && ka.isComet() && status != null) {
                //the comet event takes care of clean up
                //processSocket(ka.getChannel(), status, dispatch);
                ka.setComet(false);//to avoid a loop
                if (status == SocketStatus.TIMEOUT ) {
                    if (this.nioEndpoint.processSocket(ka.getChannel(), status, true)) {
                        return; // don't close on comet timeout
                    }
                } else {
                    this.nioEndpoint.processSocket(ka.getChannel(), status, false); //don't dispatch if the lines below are cancelling the key
                }
            }
            key.attach(null);
            if (ka!=null) this.nioEndpoint.getHandler().release(ka);
            else this.nioEndpoint.getHandler().release((SocketChannel)key.channel());
            if (key.isValid()) key.cancel();
            if (key.channel().isOpen()) {
                try {
                    key.channel().close();
                } catch (Exception e) {
                    if (NioEndpoint.getLogVariable().isDebugEnabled()) {
                        NioEndpoint.getLogVariable().debug(NioEndpoint.getSm().getString(
                                "endpoint.debug.channelCloseFail"), e);
                    }
                }
            }
            try {
                if (ka!=null) {
                    ka.getSocket().close(true);
                }
            } catch (Exception e){
                if (NioEndpoint.getLogVariable().isDebugEnabled()) {
                    NioEndpoint.getLogVariable().debug(NioEndpoint.getSm().getString(
                            "endpoint.debug.socketCloseFail"), e);
                }
            }
            try {
                if (ka != null && ka.getSendfileData() != null
                        && ka.getSendfileData().getFchannel() != null
                        && ka.getSendfileData().getFchannel().isOpen()) {
                    ka.getSendfileData().getFchannel().close();
                }
            } catch (Exception ignore) {
            }
            if (ka!=null) {
                ka.reset();
                this.nioEndpoint.countDownConnection();
            }
        } catch (Throwable e) {
            ExceptionUtils2.handleThrowable(e);
            if (NioEndpoint.getLogVariable().isDebugEnabled()) NioEndpoint.getLogVariable().error("",e);
        }
    }
    /**
     * The background thread that listens for incoming TCP/IP connections and
     * hands them off to an appropriate processor.
     */
    @Override
    public void run() {
        // Loop until destroy() is called
        while (true) {
            try {
                // Loop if endpoint is paused
                while (this.nioEndpoint.isPaused() && (!close) ) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }

                boolean hasEvents = false;

                // Time to terminate?
                if (close) {
                    events();
                    timeout(0, false);
                    try {
                        selector.close();
                    } catch (IOException ioe) {
                        NioEndpoint.getLogVariable().error(NioEndpoint.getSm().getString(
                                "endpoint.nio.selectorCloseFail"), ioe);
                    }
                    break;
                } else {
                    hasEvents = events();
                }
                try {
                    if ( !close ) {
                        if (wakeupCounter.getAndSet(-1) > 0) {
                            //if we are here, means we have other stuff to do
                            //do a non blocking select
                            keyCount = selector.selectNow();
                        } else {
                            keyCount = selector.select(this.nioEndpoint.getSelectorTimeout());
                        }
                        wakeupCounter.set(0);
                    }
                    if (close) {
                        events();
                        timeout(0, false);
                        try {
                            selector.close();
                        } catch (IOException ioe) {
                            NioEndpoint.getLogVariable().error(NioEndpoint.getSm().getString(
                                    "endpoint.nio.selectorCloseFail"), ioe);
                        }
                        break;
                    }
                } catch ( NullPointerException x ) {
                    //sun bug 5076772 on windows JDK 1.5
                    if ( NioEndpoint.getLogVariable().isDebugEnabled() ) NioEndpoint.getLogVariable().debug("Possibly encountered sun bug 5076772 on windows JDK 1.5",x);
                    if ( wakeupCounter == null || selector == null ) throw x;
                    continue;
                } catch ( CancelledKeyException x ) {
                    //sun bug 5076772 on windows JDK 1.5
                    if ( NioEndpoint.getLogVariable().isDebugEnabled() ) NioEndpoint.getLogVariable().debug("Possibly encountered sun bug 5076772 on windows JDK 1.5",x);
                    if ( wakeupCounter == null || selector == null ) throw x;
                    continue;
                } catch (Throwable x) {
                    ExceptionUtils2.handleThrowable(x);
                    NioEndpoint.getLogVariable().error("",x);
                    continue;
                }
                //either we timed out or we woke up, process events first
                if ( keyCount == 0 ) hasEvents = (hasEvents | events());

                Iterator<SelectionKey> iterator =
                    keyCount > 0 ? selector.selectedKeys().iterator() : null;
                // Walk through the collection of ready keys and dispatch
                // any active event.
                while (iterator != null && iterator.hasNext()) {
                    SelectionKey sk = iterator.next();
                    NioEndpointKeyAttachment attachment = (NioEndpointKeyAttachment)sk.attachment();
                    // Attachment may be null if another thread has called
                    // cancelledKey()
                    if (attachment == null) {
                        iterator.remove();
                    } else {
                        attachment.access();
                        iterator.remove();
                        processKey(sk, attachment);
                    }
                }//while

                //process timeouts
                timeout(keyCount,hasEvents);
                if ( this.nioEndpoint.getOomParachute() > 0 && this.nioEndpoint.getOomParachuteData() == null ) this.nioEndpoint.checkParachute();
            } catch (OutOfMemoryError oom) {
                try {
                    this.nioEndpoint.setOomParachuteData(null);
                    this.nioEndpoint.releaseCaches();
                    NioEndpoint.getLogVariable().error("", oom);
                }catch ( Throwable oomt ) {
                    try {
                        System.err.println(NioEndpoint.getOomparachutemsg());
                        oomt.printStackTrace();
                    }catch (Throwable letsHopeWeDontGetHere){
                        ExceptionUtils2.handleThrowable(letsHopeWeDontGetHere);
                    }
                }
            }
        }//while
        synchronized (this) {
            this.notifyAll();
        }
        this.nioEndpoint.getStopLatch().countDown();

    }

    protected boolean processKey(SelectionKey sk, NioEndpointKeyAttachment attachment) {
        boolean result = true;
        try {
            if ( close ) {
                cancelledKey(sk, SocketStatus.STOP, attachment.isComet());
            } else if ( sk.isValid() && attachment != null ) {
                attachment.access();//make sure we don't time out valid sockets
                sk.attach(attachment);//cant remember why this is here
                NioChannel channel = attachment.getChannel();
                if (sk.isReadable() || sk.isWritable() ) {
                    if ( attachment.getSendfileData() != null ) {
                        processSendfile(sk,attachment, false);
                    } else {
                        if ( this.nioEndpoint.isWorkerAvailable() ) {
                            unreg(sk, attachment, sk.readyOps());
                            boolean closeSocket = false;
                            // Read goes before write
                            if (sk.isReadable()) {
                                if (!this.nioEndpoint.processSocket(channel, SocketStatus.OPEN_READ, true)) {
                                    closeSocket = true;
                                }
                            }
                            if (!closeSocket && sk.isWritable()) {
                                if (!this.nioEndpoint.processSocket(channel, SocketStatus.OPEN_WRITE, true)) {
                                    closeSocket = true;
                                }
                            }
                            if (closeSocket) {
                                cancelledKey(sk,SocketStatus.DISCONNECT,false);
                            }
                        } else {
                            result = false;
                        }
                    }
                }
            } else {
                //invalid key
                cancelledKey(sk, SocketStatus.ERROR,false);
            }
        } catch ( CancelledKeyException ckx ) {
            cancelledKey(sk, SocketStatus.ERROR,false);
        } catch (Throwable t) {
            ExceptionUtils2.handleThrowable(t);
            NioEndpoint.getLogVariable().error("",t);
        }
        return result;
    }

    /**
     * @deprecated Replaced by processSendfile(sk, attachment, event)
     */
    @Deprecated
    public boolean processSendfile(SelectionKey sk,
    		NioEndpointKeyAttachment attachment,
            @SuppressWarnings("unused") boolean reg, boolean event) {
        return processSendfile(sk, attachment, event);
    }

    public boolean processSendfile(SelectionKey sk, NioEndpointKeyAttachment attachment, boolean event) {
        NioChannel sc = null;
        try {
            unreg(sk, attachment, sk.readyOps());
            NioEndpointSendfileData sd = attachment.getSendfileData();

            if (NioEndpoint.getLogVariable().isTraceEnabled()) {
                NioEndpoint.getLogVariable().trace("Processing send file for: " + sd.getFileName());
            }

            //setup the file channel
            if ( sd.getFchannel() == null ) {
                File f = new File(sd.getFileName());
                if ( !f.exists() ) {
                    cancelledKey(sk,SocketStatus.ERROR,false);
                    return false;
                }
                @SuppressWarnings("resource") // Closed when channel is closed
                FileInputStream fis = new FileInputStream(f);
                sd.setFchannel(fis.getChannel());
            }

            //configure output channel
            sc = attachment.getChannel();
            sc.setSendFile(true);
            //ssl channel is slightly different
            WritableByteChannel wc = ((sc instanceof SecureNioChannel)?sc:sc.getIOChannel());

            //we still have data in the buffer
            if (sc.getOutboundRemaining()>0) {
                if (sc.flushOutbound()) {
                    attachment.access();
                }
            } else {
                long written = sd.getFchannel().transferTo(sd.getPos(),sd.getLength(),wc);
                if ( written > 0 ) {
                    sd.setPos(sd.getPos() + written);
                    sd.setLength(sd.getLength() - written);
                    attachment.access();
                } else {
                    // Unusual not to be able to transfer any bytes
                    // Check the length was set correctly
                    if (sd.getFchannel().size() <= sd.getPos()) {
                        throw new IOException("Sendfile configured to " +
                                "send more data than was available");
                    }
                }
            }
            if ( sd.getLength() <= 0 && sc.getOutboundRemaining()<=0) {
                if (NioEndpoint.getLogVariable().isDebugEnabled()) {
                    NioEndpoint.getLogVariable().debug("Send file complete for: "+sd.getFileName());
                }
                attachment.setSendfileData(null);
                try {
                    sd.getFchannel().close();
                } catch (Exception ignore) {
                }
                if ( sd.isKeepAlive() ) {
                        if (NioEndpoint.getLogVariable().isDebugEnabled()) {
                            NioEndpoint.getLogVariable().debug("Connection is keep alive, registering back for OP_READ");
                        }
                        if (event) {
                            this.add(attachment.getChannel(),SelectionKey.OP_READ);
                        } else {
                            reg(sk,attachment,SelectionKey.OP_READ);
                        }
                } else {
                    if (NioEndpoint.getLogVariable().isDebugEnabled()) {
                        NioEndpoint.getLogVariable().debug("Send file connection is being closed");
                    }
                    cancelledKey(sk,SocketStatus.STOP,false);
                    return false;
                }
            } else {
                if (NioEndpoint.getLogVariable().isDebugEnabled()) {
                    NioEndpoint.getLogVariable().debug("OP_WRITE for sendfile: " + sd.getFileName());
                }
                if (event) {
                    add(attachment.getChannel(),SelectionKey.OP_WRITE);
                } else {
                    reg(sk,attachment,SelectionKey.OP_WRITE);
                }
            }
        }catch ( IOException x ) {
            if ( NioEndpoint.getLogVariable().isDebugEnabled() ) NioEndpoint.getLogVariable().debug("Unable to complete sendfile request:", x);
            cancelledKey(sk,SocketStatus.ERROR,false);
            return false;
        }catch ( Throwable t ) {
            NioEndpoint.getLogVariable().error("",t);
            cancelledKey(sk, SocketStatus.ERROR, false);
            return false;
        }finally {
            if (sc!=null) sc.setSendFile(false);
        }
        return true;
    }

    protected void unreg(SelectionKey sk, NioEndpointKeyAttachment attachment, int readyOps) {
        //this is a must, so that we don't have multiple threads messing with the socket
        reg(sk,attachment,sk.interestOps()& (~readyOps));
    }

    protected void reg(SelectionKey sk, NioEndpointKeyAttachment attachment, int intops) {
        sk.interestOps(intops);
        attachment.interestOps(intops);
        attachment.setCometOps(intops);
    }

    protected void timeout(int keyCount, boolean hasEvents) {
        long now = System.currentTimeMillis();
        // This method is called on every loop of the Poller. Don't process
        // timeouts on every loop of the Poller since that would create too
        // much load and timeouts can afford to wait a few seconds.
        // However, do process timeouts if any of the following are true:
        // - the selector simply timed out (suggests there isn't much load)
        // - the nextExpiration time has passed
        // - the server socket is being closed
        if ((keyCount > 0 || hasEvents) && (now < nextExpiration) && !close) {
            return;
        }
        //timeout
        Set<SelectionKey> keys = selector.keys();
        int keycount = 0;
        for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();) {
            SelectionKey key = iter.next();
            keycount++;
            try {
            	NioEndpointKeyAttachment ka = (NioEndpointKeyAttachment) key.attachment();
                if ( ka == null ) {
                    cancelledKey(key, SocketStatus.ERROR,false); //we don't support any keys without attachments
                } else if ( ka.getError() ) {
                    cancelledKey(key, SocketStatus.ERROR,true);//TODO this is not yet being used
                } else if (ka.isComet() && ka.getCometNotify() ) {
                    ka.setCometNotify(false);
                    reg(key,ka,0);//avoid multiple calls, this gets reregistered after invocation
                    //if (!processSocket(ka.getChannel(), SocketStatus.OPEN_CALLBACK)) processSocket(ka.getChannel(), SocketStatus.DISCONNECT);
                    if (!this.nioEndpoint.processSocket(ka.getChannel(), SocketStatus.OPEN_READ, true)) this.nioEndpoint.processSocket(ka.getChannel(), SocketStatus.DISCONNECT, true);
                } else if ((ka.interestOps()&SelectionKey.OP_READ) == SelectionKey.OP_READ ||
                          (ka.interestOps()&SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                    //only timeout sockets that we are waiting for a read from
                    long delta = now - ka.getLastAccess();
                    long timeout = ka.getTimeout();
                    boolean isTimedout = timeout > 0 && delta > timeout;
                    if ( close ) {
                        key.interestOps(0);
                        ka.interestOps(0); //avoid duplicate stop calls
                        processKey(key,ka);
                    } else if (isTimedout) {
                        key.interestOps(0);
                        ka.interestOps(0); //avoid duplicate timeout calls
                        cancelledKey(key, SocketStatus.TIMEOUT,true);
                    }
                } else if (ka.isAsync() || ka.isComet()) {
                    if (close) {
                        key.interestOps(0);
                        ka.interestOps(0); //avoid duplicate stop calls
                        processKey(key,ka);
                    } else if (!ka.isAsync() || ka.getTimeout() > 0) {
                        // Async requests with a timeout of 0 or less never timeout
                        long delta = now - ka.getLastAccess();
                        long timeout = (ka.getTimeout()==-1)?((long) this.nioEndpoint.getSocketProperties().getSoTimeout()):(ka.getTimeout());
                        boolean isTimedout = delta > timeout;
                        if (isTimedout) {
                            // Prevent subsequent timeouts if the timeout event takes a while to process
                            ka.access(Long.MAX_VALUE);
                            this.nioEndpoint.processSocket(ka.getChannel(), SocketStatus.TIMEOUT, true);
                        }
                    }
                }//end if
            }catch ( CancelledKeyException ckx ) {
                cancelledKey(key, SocketStatus.ERROR,false);
            }
        }//for
        long prevExp = nextExpiration; //for logging purposes only
        nextExpiration = System.currentTimeMillis() +
                this.nioEndpoint.getSocketProperties().getTimeoutInterval();
        if (NioEndpoint.getLogVariable().isTraceEnabled()) {
            NioEndpoint.getLogVariable().trace("timeout completed: keys processed=" + keycount +
                    "; now=" + now + "; nextExpiration=" + prevExp +
                    "; keyCount=" + keyCount + "; hasEvents=" + hasEvents +
                    "; eval=" + ((now < prevExp) && (keyCount>0 || hasEvents) && (!close) ));
        }

    }
}