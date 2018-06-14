package org.apache.catalina.filters;

public class ThreadLocal6 extends ThreadLocal<RequestDumperFilterTimestamp>{
	@Override
	protected RequestDumperFilterTimestamp initialValue() {
		return new RequestDumperFilterTimestamp();
	}
}
