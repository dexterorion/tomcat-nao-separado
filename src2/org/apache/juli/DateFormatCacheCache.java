package org.apache.juli;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatCacheCache {

    /**
	 * 
	 */
	private final DateFormatCache dateFormatCache;
	/* Second formatted in most recent invocation */
    private long previousSeconds = Long.MIN_VALUE;
    /* Formatted timestamp generated in most recent invocation */
    private String previousFormat = "";

    /* First second contained in cache */
    private long first = Long.MIN_VALUE;
    /* Last second contained in cache */
    private long last = Long.MIN_VALUE;
    /* Index of "first" in the cyclic cache */
    private int offset = 0;
    /* Helper object to be able to call SimpleDateFormat.format(). */
    private final Date currentDate = new Date();

    private String cache[];
    private SimpleDateFormat formatter;

    private DateFormatCacheCache parent = null;

    public DateFormatCacheCache(DateFormatCache dateFormatCache, DateFormatCacheCache parent) {
        this.dateFormatCache = dateFormatCache;
		cache = new String[this.dateFormatCache.getCacheSize()];
        for (int i = 0; i < this.dateFormatCache.getCacheSize(); i++) {
            cache[i] = null;
        }
        formatter = new SimpleDateFormat(this.dateFormatCache.getFormat(), Locale.US);
        formatter.setTimeZone(TimeZone.getDefault());
        this.parent = parent;
    }

    public String getFormat(long time) {

        long seconds = time / 1000;

        /* First step: if we have seen this timestamp
           during the previous call, return the previous value. */
        if (seconds == previousSeconds) {
            return previousFormat;
        }

        /* Second step: Try to locate in cache */
        previousSeconds = seconds;
        int index = (offset + (int)(seconds - first)) % this.dateFormatCache.getCacheSize();
        if (index < 0) {
            index += this.dateFormatCache.getCacheSize();
        }
        if (seconds >= first && seconds <= last) {
            if (cache[index] != null) {
                /* Found, so remember for next call and return.*/
                previousFormat = cache[index];
                return previousFormat;
            }

        /* Third step: not found in cache, adjust cache and add item */
        } else if (seconds >= last + this.dateFormatCache.getCacheSize() || seconds <= first - this.dateFormatCache.getCacheSize()) {
            first = seconds;
            last = first + this.dateFormatCache.getCacheSize() - 1;
            index = 0;
            offset = 0;
            for (int i = 1; i < this.dateFormatCache.getCacheSize(); i++) {
                cache[i] = null;
            }
        } else if (seconds > last) {
            for (int i = 1; i < seconds - last; i++) {
                cache[(index + this.dateFormatCache.getCacheSize() - i) % this.dateFormatCache.getCacheSize()] = null;
            }
            first = seconds - (this.dateFormatCache.getCacheSize() - 1);
            last = seconds;
            offset = (index + 1) % this.dateFormatCache.getCacheSize();
        } else if (seconds < first) {
            for (int i = 1; i < first - seconds; i++) {
                cache[(index + i) % this.dateFormatCache.getCacheSize()] = null;
            }
            first = seconds;
            last = seconds + (this.dateFormatCache.getCacheSize() - 1);
            offset = index;
        }

        /* Last step: format new timestamp either using
         * parent cache or locally. */
        if (parent != null) {
            synchronized(parent) {
                previousFormat = parent.getFormat(time);
            }
        } else {
            currentDate.setTime(time);
            previousFormat = formatter.format(currentDate);
        }
        cache[index] = previousFormat;
        return previousFormat;
    }
}