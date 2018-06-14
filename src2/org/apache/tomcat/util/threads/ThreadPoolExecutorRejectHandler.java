package org.apache.tomcat.util.threads;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;

public class ThreadPoolExecutorRejectHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r,
            java.util.concurrent.ThreadPoolExecutor executor) {
        throw new RejectedExecutionException();
    }

}