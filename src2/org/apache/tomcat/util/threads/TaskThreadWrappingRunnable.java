package org.apache.tomcat.util.threads;

/**
 * Wraps a {@link Runnable} to swallow any {@link StopPooledThreadException}
 * instead of letting it go and potentially trigger a break in a debugger.
 */
public class TaskThreadWrappingRunnable implements Runnable {
    private Runnable wrappedRunnable;
    public TaskThreadWrappingRunnable(Runnable wrappedRunnable) {
        this.wrappedRunnable = wrappedRunnable;
    }
    @Override
    public void run() {
        try {
            wrappedRunnable.run();
        } catch(StopPooledThreadException exc) {
            //expected : we just swallow the exception to avoid disturbing
            //debuggers like eclipse's
            TaskThread.getLog().debug("Thread exiting on purpose", exc);
        }
    }

}