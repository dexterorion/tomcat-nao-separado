package org.apache.catalina.valves;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AccessLogValveDateFormatCacheCache {

    /**
	 * 
	 */
	private final AccessLogValveDateFormatCache accessLogValveDateFormatCache;

	/* CLF log format */
    private static final String cLFFormat = "dd/MMM/yyyy:HH:mm:ss Z";

    /* Second used to retrieve CLF format in most recent invocation */
    private long previousSeconds = Long.MIN_VALUE;
    /* Value of CLF format retrieved in most recent invocation */
    private String previousFormat = "";

    /* First second contained in cache */
    private long first = Long.MIN_VALUE;
    /* Last second contained in cache */
    private long last = Long.MIN_VALUE;
    /* Index of "first" in the cyclic cache */
    private int offset = 0;
    /* Helper object to be able to call SimpleDateFormat.format(). */
    private final Date currentDate = new Date();

    private final String cache[];
    private SimpleDateFormat formatter;
    private boolean isCLF = false;

    private AccessLogValveDateFormatCacheCache parent = null;

    public AccessLogValveDateFormatCacheCache(AccessLogValveDateFormatCache accessLogValveDateFormatCache, AccessLogValveDateFormatCacheCache parent) {
        this(accessLogValveDateFormatCache, null, parent);
    }

    private AccessLogValveDateFormatCacheCache(AccessLogValveDateFormatCache accessLogValveDateFormatCache, String format, AccessLogValveDateFormatCacheCache parent) {
        this(accessLogValveDateFormatCache, format, null, parent);
    }

    public AccessLogValveDateFormatCacheCache(AccessLogValveDateFormatCache accessLogValveDateFormatCache, String format, Locale loc, AccessLogValveDateFormatCacheCache parent) {
        this.accessLogValveDateFormatCache = accessLogValveDateFormatCache;
		cache = new String[this.accessLogValveDateFormatCache.getCacheSize()];
        for (int i = 0; i < this.accessLogValveDateFormatCache.getCacheSize(); i++) {
            cache[i] = null;
        }
        if (loc == null) {
            loc = this.accessLogValveDateFormatCache.getCacheDefaultLocale();
        }
        if (format == null) {
            isCLF = true;
            format = cLFFormat;
            formatter = new SimpleDateFormat(format, Locale.US);
        } else {
            formatter = new SimpleDateFormat(format, loc);
        }
        formatter.setTimeZone(TimeZone.getDefault());
        this.parent = parent;
    }

    public String getFormatInternal(long time) {

        long seconds = time / 1000;

        /* First step: if we have seen this timestamp
           during the previous call, and we need CLF, return the previous value. */
        if (seconds == previousSeconds) {
            return previousFormat;
        }

        /* Second step: Try to locate in cache */
        previousSeconds = seconds;
        int index = (offset + (int)(seconds - first)) % this.accessLogValveDateFormatCache.getCacheSize();
        if (index < 0) {
            index += this.accessLogValveDateFormatCache.getCacheSize();
        }
        if (seconds >= first && seconds <= last) {
            if (cache[index] != null) {
                /* Found, so remember for next call and return.*/
                previousFormat = cache[index];
                return previousFormat;
            }

        /* Third step: not found in cache, adjust cache and add item */
        } else if (seconds >= last + this.accessLogValveDateFormatCache.getCacheSize() || seconds <= first - this.accessLogValveDateFormatCache.getCacheSize()) {
            first = seconds;
            last = first + this.accessLogValveDateFormatCache.getCacheSize() - 1;
            index = 0;
            offset = 0;
            for (int i = 1; i < this.accessLogValveDateFormatCache.getCacheSize(); i++) {
                cache[i] = null;
            }
        } else if (seconds > last) {
            for (int i = 1; i < seconds - last; i++) {
                cache[(index + this.accessLogValveDateFormatCache.getCacheSize() - i) % this.accessLogValveDateFormatCache.getCacheSize()] = null;
            }
            first = seconds - (this.accessLogValveDateFormatCache.getCacheSize() - 1);
            last = seconds;
            offset = (index + 1) % this.accessLogValveDateFormatCache.getCacheSize();
        } else if (seconds < first) {
            for (int i = 1; i < first - seconds; i++) {
                cache[(index + i) % this.accessLogValveDateFormatCache.getCacheSize()] = null;
            }
            first = seconds;
            last = seconds + (this.accessLogValveDateFormatCache.getCacheSize() - 1);
            offset = index;
        }

        /* Last step: format new timestamp either using
         * parent cache or locally. */
        if (parent != null) {
            synchronized(parent) {
                previousFormat = parent.getFormatInternal(time);
            }
        } else {
            currentDate.setTime(time);
            previousFormat = formatter.format(currentDate);
            if (isCLF) {
                StringBuilder current = new StringBuilder(32);
                current.append('[');
                current.append(previousFormat);
                current.append(']');
                previousFormat = current.toString();
            }
        }
        cache[index] = previousFormat;
        return previousFormat;
    }
}