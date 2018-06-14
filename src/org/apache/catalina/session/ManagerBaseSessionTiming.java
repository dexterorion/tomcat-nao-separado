package org.apache.catalina.session;

public final class ManagerBaseSessionTiming {
    private final long timestamp;
    private final int duration;
    
    public ManagerBaseSessionTiming(long timestamp, int duration) {
        this.timestamp = timestamp;
        this.duration = duration;
    }
    
    /**
     * Time stamp associated with this piece of timing information in
     * milliseconds.
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Duration associated with this piece of timing information in seconds.
     */
    public int getDuration() {
        return duration;
    }
}