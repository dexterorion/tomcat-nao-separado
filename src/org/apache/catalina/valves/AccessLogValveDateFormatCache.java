package org.apache.catalina.valves;

import java.util.HashMap;
import java.util.Locale;

/**
 * <p>Cache structure for formatted timestamps based on seconds.</p>
 *
 * <p>The cache consists of entries for a consecutive range of
 * seconds. The length of the range is configurable. It is
 * implemented based on a cyclic buffer. New entries shift the range.</p>
 *
 * <p>There is one cache for the CLF format (the access log standard
 * format) and a HashMap of caches for additional formats used by
 * SimpleDateFormat.</p>
 *
 * <p>Although the cache supports specifying a locale when retrieving a
 * formatted timestamp, each format will always use the locale given
 * when the format was first used. New locales can only be used for new formats.
 * The CLF format will always be formatted using the locale
 * <code>en_US</code>.</p>
 *
 * <p>The cache is not threadsafe. It can be used without synchronization
 * via thread local instances, or with synchronization as a global cache.</p>
 *
 * <p>The cache can be created with a parent cache to build a cache hierarchy.
 * Access to the parent cache is threadsafe.</p>
 *
 * <p>This class uses a small thread local first level cache and a bigger
 * synchronized global second level cache.</p>
 */
public class AccessLogValveDateFormatCache {

    /* Number of cached entries */
    private int cacheSize = 0;

    private final Locale cacheDefaultLocale;
    private final AccessLogValveDateFormatCache parent;
    private  final AccessLogValveDateFormatCacheCache cLFCache;
    private final HashMap<String, AccessLogValveDateFormatCacheCache> formatCache = new HashMap<String, AccessLogValveDateFormatCacheCache>();

    public AccessLogValveDateFormatCache(int size, Locale loc, AccessLogValveDateFormatCache parent) {
        setCacheSize(size);
        cacheDefaultLocale = loc;
        this.parent = parent;
        AccessLogValveDateFormatCacheCache parentCache = null;
        if (parent != null) {
            synchronized(parent) {
                parentCache = parent.getCache(null, null);
            }
        }
        cLFCache = new AccessLogValveDateFormatCacheCache(this, parentCache);
    }

    private AccessLogValveDateFormatCacheCache getCache(String format, Locale loc) {
    	AccessLogValveDateFormatCacheCache cache;
        if (format == null) {
            cache = cLFCache;
        } else {
            cache = formatCache.get(format);
            if (cache == null) {
            	AccessLogValveDateFormatCacheCache parentCache = null;
                if (parent != null) {
                    synchronized(parent) {
                        parentCache = parent.getCache(format, loc);
                    }
                }
                cache = new AccessLogValveDateFormatCacheCache(this, format, loc, parentCache);
                formatCache.put(format, cache);
            }
        }
        return cache;
    }

    public String getFormat(long time) {
        return cLFCache.getFormatInternal(time);
    }

    public String getFormat(String format, Locale loc, long time) {
        return getCache(format, loc).getFormatInternal(time);
    }

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public Locale getCacheDefaultLocale() {
		return cacheDefaultLocale;
	}
}