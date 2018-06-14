package org.apache.catalina.tribes.group.interceptors;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderInterceptorCounter {
    private AtomicInteger value = new AtomicInteger(0);
    
    public int getCounter() {
        return value.get();
    }
    
    public void setCounter(int counter) {
        this.value.set(counter);
    }
    
    public int inc() {
        return value.addAndGet(1);
    }
}