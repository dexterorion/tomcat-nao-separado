package org.apache.catalina.valves;

import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class StuckThreadDetectionValveMonitoredThread {

    /**
     * Reference to the thread to get a stack trace from background task
     */
    private final Thread thread;
    private final String requestUri;
    private final long start;
    private final AtomicInteger state = new AtomicInteger(
    		StuckThreadDetectionValveMonitoredThreadState.RUNNING.ordinal());
    /**
     * Semaphore to synchronize the stuck thread with the background-process
     * thread. It's not used if the interruption feature is not active.
     */
    private final Semaphore interruptionSemaphore;
    /**
     * Set to true after the thread is interrupted. No need to make it
     * volatile since it is accessed right after acquiring the semaphore.
     */
    private boolean interrupted;

    public StuckThreadDetectionValveMonitoredThread(Thread thread, String requestUri,
            boolean interruptible) {
        this.thread = thread;
        this.requestUri = requestUri;
        this.start = System.currentTimeMillis();
        if (interruptible) {
            interruptionSemaphore = new Semaphore(1);
        } else {
            interruptionSemaphore = null;
        }
    }

    public Thread getThread() {
        return this.thread;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public long getActiveTimeInMillis() {
        return System.currentTimeMillis() - start;
    }

    public Date getStartTime() {
        return new Date(start);
    }

    public boolean markAsStuckIfStillRunning() {
        return this.state.compareAndSet(StuckThreadDetectionValveMonitoredThreadState.RUNNING.ordinal(),
        		StuckThreadDetectionValveMonitoredThreadState.STUCK.ordinal());
    }

    public StuckThreadDetectionValveMonitoredThreadState markAsDone() {
        int val = this.state.getAndSet(StuckThreadDetectionValveMonitoredThreadState.DONE.ordinal());
        StuckThreadDetectionValveMonitoredThreadState threadState = StuckThreadDetectionValveMonitoredThreadState.values()[val];

        if (threadState == StuckThreadDetectionValveMonitoredThreadState.STUCK
                && interruptionSemaphore != null) {
            try {
                // use the semaphore to synchronize with the background thread
                // which might try to interrupt this current thread.
                // Otherwise, the current thread might be interrupted after
                // going out from here, maybe already serving a new request
                this.interruptionSemaphore.acquire();
            } catch (InterruptedException e) {
                StuckThreadDetectionValve.getLog().debug(
                        "thread interrupted after the request is finished, ignoring",
                        e);
            }
            // no need to release the semaphore, it will be GCed
        }
        //else the request went through before being marked as stuck, no need
        //to sync agains the semaphore
        return threadState;
    }

    public boolean isMarkedAsStuck() {
        return this.state.get() == StuckThreadDetectionValveMonitoredThreadState.STUCK.ordinal();
    }

    public boolean interruptIfStuck(long interruptThreadThreshold) {
        if (!isMarkedAsStuck() || interruptionSemaphore == null
                || !this.interruptionSemaphore.tryAcquire()) {
            // if the semaphore is already acquired, it means that the
            // request thread got unstuck before we interrupted it
            return false;
        }
        try {
            if (StuckThreadDetectionValve.getLog().isWarnEnabled()) {
                String msg = StuckThreadDetectionValve.getSm().getString(
                    "stuckThreadDetectionValve.notifyStuckThreadInterrupted",
                    this.getThread().getName(),
                    Long.valueOf(getActiveTimeInMillis()),
                    this.getStartTime(), this.getRequestUri(),
                    Long.valueOf(interruptThreadThreshold),
                    String.valueOf(this.getThread().getId()));
                Throwable th = new Throwable();
                th.setStackTrace(this.getThread().getStackTrace());
                StuckThreadDetectionValve.getLog().warn(msg, th);
            }
            this.thread.interrupt();
        } finally {
            this.interrupted = true;
            this.interruptionSemaphore.release();
        }
        return true;
    }

    public boolean wasInterrupted() {
        return interrupted;
    }
}