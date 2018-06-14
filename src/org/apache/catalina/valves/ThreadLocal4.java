package org.apache.catalina.valves;

public class ThreadLocal4 extends ThreadLocal<ExtendedAccessLogValveElementTimestampStruct>{
	@Override
    protected ExtendedAccessLogValveElementTimestampStruct initialValue() {
        return new ExtendedAccessLogValveElementTimestampStruct("HH:mm:ss");
    }
}
