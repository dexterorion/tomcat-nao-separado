package org.apache.catalina.realm;

import java.util.concurrent.atomic.AtomicInteger;

public class LockOutRealmLockRecord {
    private AtomicInteger failures = new AtomicInteger(0);
    private long lastFailureTime = 0;
    
    public int getFailures() {
        return failures.get();
    }
    
    public void setFailures(int theFailures) {
        failures.set(theFailures);
    }

    public long getLastFailureTime() {
        return lastFailureTime;
    }
    
    public void registerFailure() {
        failures.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
    }
}