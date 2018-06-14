package org.apache.tomcat.util.net;

import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// ----------------------------------------------------- Key Attachment Class
public class NioEndpointKeyAttachment extends SocketWrapper<NioChannel> {

    public NioEndpointKeyAttachment(NioChannel channel) {
        super(channel);
    }

    public void reset(NioEndpointPoller poller, NioChannel channel, long soTimeout) {
        super.reset(channel, soTimeout);

        cometNotify = false;
        cometOps = SelectionKey.OP_READ;
        interestOps = 0;
        this.poller = poller;
        setLastRegistered(0);
        sendfileData = null;
        if (readLatch != null) {
            try {
                for (int i = 0; i < (int) readLatch.getCount(); i++) {
                    readLatch.countDown();
                }
            } catch (Exception ignore) {
            }
        }
        readLatch = null;
        sendfileData = null;
        if (writeLatch != null) {
            try {
                for (int i = 0; i < (int) writeLatch.getCount(); i++) {
                    writeLatch.countDown();
                }
            } catch (Exception ignore) {
            }
        }
        writeLatch = null;
        setWriteTimeout(soTimeout);
    }

    public void reset() {
        reset(null,null,-1);
    }

    public NioEndpointPoller getPoller() { return poller;}
    public void setPoller(NioEndpointPoller poller){this.poller = poller;}
    public void setCometNotify(boolean notify) { this.cometNotify = notify; }
    public boolean getCometNotify() { return cometNotify; }
    /**
     * @deprecated Unused (value is set but never read) - will be removed in
     * Tomcat 8
     */
    @Deprecated
    public void setCometOps(int ops) { this.cometOps = ops; }
    /**
     * @deprecated Unused - will be removed in Tomcat 8
     */
    
    @Deprecated
    public int getCometOps() { return cometOps; }
    public NioChannel getChannel() { return getSocket();}
    public void setChannel(NioChannel channel) { this.setSocket(channel);}
    private NioEndpointPoller poller = null;
    private int interestOps = 0;
    public int interestOps() { return interestOps;}
    public int interestOps(int ops) { this.interestOps  = ops; return ops; }
    public CountDownLatch getReadLatch() { return readLatch; }
    public CountDownLatch getWriteLatch() { return writeLatch; }
    protected CountDownLatch resetLatch(CountDownLatch latch) {
        if ( latch==null || latch.getCount() == 0 ) return null;
        else throw new IllegalStateException("Latch must be at count 0");
    }
    public void resetReadLatch() { readLatch = resetLatch(readLatch); }
    public void resetWriteLatch() { writeLatch = resetLatch(writeLatch); }

    protected CountDownLatch startLatch(CountDownLatch latch, int cnt) {
        if ( latch == null || latch.getCount() == 0 ) {
            return new CountDownLatch(cnt);
        }
        else throw new IllegalStateException("Latch must be at count 0 or null.");
    }
    public void startReadLatch(int cnt) { readLatch = startLatch(readLatch,cnt);}
    public void startWriteLatch(int cnt) { writeLatch = startLatch(writeLatch,cnt);}

    protected void awaitLatch(CountDownLatch latch, long timeout, TimeUnit unit) throws InterruptedException {
        if ( latch == null ) throw new IllegalStateException("Latch cannot be null");
        latch.await(timeout,unit);
    }
    public void awaitReadLatch(long timeout, TimeUnit unit) throws InterruptedException { awaitLatch(readLatch,timeout,unit);}
    public void awaitWriteLatch(long timeout, TimeUnit unit) throws InterruptedException { awaitLatch(writeLatch,timeout,unit);}

    /**
     * @deprecated Unused - will be removed in Tomcat 8
     */
    @Deprecated
    public long getLastRegistered() { return super.getLastRegistered(); }
    /**
     * @deprecated Unused - will be removed in Tomcat 8
     */
    @Deprecated
    public void setLastRegistered(long reg) { super.setLastRegistered(reg); }

    public void setSendfileData(NioEndpointSendfileData sf) { this.sendfileData = sf;}
    public NioEndpointSendfileData getSendfileData() { return this.sendfileData;}

    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
    public long getWriteTimeout() {return this.writeTimeout;}

    private boolean comet = false;
    private int cometOps = SelectionKey.OP_READ;
    private boolean cometNotify = false;
    private CountDownLatch readLatch = null;
    private CountDownLatch writeLatch = null;
    private NioEndpointSendfileData sendfileData = null;
    private long writeTimeout = -1;
}