package org.apache.catalina.filters;

public class ThreadLocal5 extends ThreadLocal<RequestDumperFilterTimestamp>{
	@Override
    protected RequestDumperFilterTimestamp initialValue() {
        return new RequestDumperFilterTimestamp();
    }
}
