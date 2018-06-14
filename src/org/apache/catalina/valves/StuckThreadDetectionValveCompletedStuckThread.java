package org.apache.catalina.valves;

public class StuckThreadDetectionValveCompletedStuckThread {

    private final String threadName;
    private final long threadId;
    private final long totalActiveTime;

    public StuckThreadDetectionValveCompletedStuckThread(Thread thread, long totalActiveTime) {
        this.threadName = thread.getName();
        this.threadId = thread.getId();
        this.totalActiveTime = totalActiveTime;
    }

    public String getName() {
        return this.threadName;
    }

    public long getId() {
        return this.threadId;
    }

    public long getTotalActiveTime() {
        return this.totalActiveTime;
    }
}