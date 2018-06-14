package org.apache.juli;

import java.util.concurrent.TimeUnit;

public class AsyncFileHandlerLoggerThread extends Thread {
    private boolean run = true;
    public AsyncFileHandlerLoggerThread() {
        this.setDaemon(true);
        this.setName("AsyncFileHandlerWriter-"+System.identityHashCode(this));
    }
    
    @Override
    public void run() {
        while (run) {
            try {
            	AsyncFileHandlerLogEntry entry = AsyncFileHandler.getQueue().poll(AsyncFileHandler.getLoggerSleepTime(), TimeUnit.MILLISECONDS);
                if (entry!=null) entry.flush();
            }catch (InterruptedException x) {
                Thread.interrupted();
            }catch (Exception x) {
                x.printStackTrace();
            }
        }//while
    }
}