package org.apache.juli;

public class ThreadLocal2 extends ThreadLocal<DateFormatCache>{
	@Override
    protected DateFormatCache initialValue() {
        return new DateFormatCache(OneLineFormatter.getLocalcachesize(), OneLineFormatter.getTimeformat(), OneLineFormatter.getGlobaldatecache());
    }
}
