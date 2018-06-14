package org.apache.catalina.valves;

public class ThreadLocal3 extends ThreadLocal<ExtendedAccessLogValveElementTimestampStruct>{
	@Override
    protected ExtendedAccessLogValveElementTimestampStruct initialValue() {
        return new ExtendedAccessLogValveElementTimestampStruct("yyyy-MM-dd");
    }
}
