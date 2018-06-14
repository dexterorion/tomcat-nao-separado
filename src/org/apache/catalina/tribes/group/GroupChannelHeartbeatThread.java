package org.apache.catalina.tribes.group;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class GroupChannelHeartbeatThread extends Thread {
    private static final Log log = LogFactory.getLog(GroupChannelHeartbeatThread.class);
    private static int counter = 1;
    protected static synchronized int inc() {
        return counter++;
    }

    private volatile boolean doRun = true;
    private GroupChannel channel;
    private long sleepTime;
    public GroupChannelHeartbeatThread(GroupChannel channel, long sleepTime) {
        super();
        this.setPriority(MIN_PRIORITY);
        setName("GroupChannel-Heartbeat-"+inc());
        setDaemon(true);
        this.channel = channel;
        this.sleepTime = sleepTime;
    }
    public void stopHeartbeat() {
        doRun = false;
        interrupt();
    }

    @Override
    public void run() {
        while (doRun) {
            try {
                Thread.sleep(sleepTime);
                channel.heartbeat();
            } catch ( InterruptedException x ) {
                interrupted();
            } catch ( Exception x ) {
                log.error("Unable to send heartbeat through Tribes interceptor stack. Will try to sleep again.",x);
            }//catch
        }//while
    }
    
    @Override
    public void start(){
    	super.start();
    }
}