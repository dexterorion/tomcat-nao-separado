/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.juli;



/**
 * <p>Cache structure for SimpleDateFormat formatted timestamps based on
 * seconds.</p>
 *
 * <p>Millisecond formatting using S is not supported. You should add the
 * millisecond information after getting back the second formatting.</p>
 *
 * <p>The cache consists of entries for a consecutive range of
 * seconds. The length of the range is configurable. It is
 * implemented based on a cyclic buffer. New entries shift the range.</p>
 *
 * <p>The cache is not threadsafe. It can be used without synchronization
 * via thread local instances, or with synchronization as a global cache.</p>
 *
 * <p>The cache can be created with a parent cache to build a cache hierarchy.
 * Access to the parent cache is threadsafe.</p>
 */
public class DateFormatCache {

    private static final String msecPattern = "#";

    /* Timestamp format */
    private final String format;

    /* Number of cached entries */
    private int cacheSize = 0;

    private DateFormatCacheCache cache;

    /**
     * Replace the millisecond formatting character 'S' by
     * some dummy characters in order to make the resulting
     * formatted time stamps cacheable. Our consumer might
     * choose to replace the dummy chars with the actual
     * milliseconds because that's relatively cheap.
     */
    private String tidyFormat(String format) {
        boolean escape = false;
        StringBuilder result = new StringBuilder();
        int len = format.length();
        char x;
        for (int i = 0; i < len; i++) {
            x = format.charAt(i);
            if (escape || x != 'S') {
                result.append(x);
            } else {
                result.append(msecPattern);
            }
            if (x == '\'') {
                escape = !escape;
            }
        }
        return result.toString();
    }

    public DateFormatCache(int size, String format, DateFormatCache parent) {
        setCacheSize(size);
        this.format = tidyFormat(format);
        DateFormatCacheCache parentCache = null;
        if (parent != null) {
            synchronized(parent) {
                parentCache = parent.getCacheData();
            }
        }
        setCacheData(new DateFormatCacheCache(this, parentCache));
    }

    public String getFormat(long time) {
        return getCacheData().getFormat(time);
    }

	public int getCacheSize() {
		return getCacheSizeData();
	}

	public void setCacheSize(int cacheSize) {
		this.setCacheSizeData(cacheSize);
	}

	public String getFormat() {
		return getFormatData();
	}

	public String getFormatData() {
		return format;
	}

	public int getCacheSizeData() {
		return cacheSize;
	}

	public void setCacheSizeData(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public DateFormatCacheCache getCacheData() {
		return cache;
	}

	public void setCacheData(DateFormatCacheCache cache) {
		this.cache = cache;
	}
}
