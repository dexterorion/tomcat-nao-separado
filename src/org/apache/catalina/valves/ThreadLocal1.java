package org.apache.catalina.valves;

import java.util.Locale;

public class ThreadLocal1 extends ThreadLocal<AccessLogValveDateFormatCache>{
	@Override
	protected AccessLogValveDateFormatCache initialValue() {
		return new AccessLogValveDateFormatCache(AccessLogValve.getLocalcachesize(),
				Locale.getDefault(), AccessLogValve.getGlobaldatecache());
	}
}
