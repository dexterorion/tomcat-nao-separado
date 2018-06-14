package org.apache.catalina.tribes.transport;

import java.util.List;

public class PooledSenderSenderQueue {
    private int limit = 25;

    private PooledSender parent = null;

    private List<DataSender> notinuse = null;

    private List<DataSender> inuse = null;

    private boolean isOpen = true;

    public PooledSenderSenderQueue(PooledSender parent, int limit) {
        this.limit = limit;
        this.parent = parent;
        notinuse = new java.util.LinkedList<DataSender>();
        inuse = new java.util.LinkedList<DataSender>();
    }

    /**
     * @return Returns the limit.
     */
    public int getLimit() {
        return limit;
    }
    /**
     * @param limit The limit to set.
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getInUsePoolSize() {
        return inuse.size();
    }

    public int getInPoolSize() {
        return notinuse.size();
    }
    
    public synchronized boolean checkIdleKeepAlive() {
        DataSender[] list = new DataSender[notinuse.size()];
        notinuse.toArray(list);
        boolean result = false;
        for (int i=0; i<list.length; i++) {
            result = result | list[i].keepalive();
        }
        return result;
    }

    public synchronized DataSender getSender(long timeout) {
        long start = System.currentTimeMillis();
        while ( true ) {
            if (!isOpen)throw new IllegalStateException("Queue is closed");
            DataSender sender = null;
            if (notinuse.size() == 0 && inuse.size() < limit) {
                sender = parent.getNewDataSender();
            } else if (notinuse.size() > 0) {
                sender = notinuse.remove(0);
            }
            if (sender != null) {
                inuse.add(sender);
                return sender;
            }//end if
            long delta = System.currentTimeMillis() - start;
            if ( delta > timeout && timeout>0) return null;
            else {
                try {
                    wait(Math.max(timeout - delta,1));
                }catch (InterruptedException x){}
            }//end if
        }
    }

    public synchronized void returnSender(DataSender sender) {
        if ( !isOpen) {
            sender.disconnect();
            return;
        }
        //to do
        inuse.remove(sender);
        //just in case the limit has changed
        if ( notinuse.size() < this.getLimit() ) notinuse.add(sender);
        else
            try {
                sender.disconnect();
            } catch (Exception e) {
                if (PooledSender.getLog().isDebugEnabled()) {
                    PooledSender.getLog().debug(PooledSender.getSm().getString(
                            "PooledSender.senderDisconnectFail"), e);
                }
            }
        notify();
    }

    public synchronized void close() {
        isOpen = false;
        Object[] unused = notinuse.toArray();
        Object[] used = inuse.toArray();
        for (int i = 0; i < unused.length; i++) {
            DataSender sender = (DataSender) unused[i];
            sender.disconnect();
        }//for
        for (int i = 0; i < used.length; i++) {
            DataSender sender = (DataSender) used[i];
            sender.disconnect();
        }//for
        notinuse.clear();
        inuse.clear();
        notify();


    }

    public synchronized void open() {
        isOpen = true;
        notify();
    }
}