package org.apache.catalina.valves;

// Constants for the AccessLogValve class
public final class ConstantsAccessLog {
    private static final String COMMON_ALIAS = "common";
    private static final String COMMON_PATTERN = "%h %l %u %t \"%r\" %s %b";
    private static final String COMBINED_ALIAS = "combined";
    private static final String COMBINED_PATTERN = "%h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-Agent}i\"";
	public static String getCommonAlias() {
		return COMMON_ALIAS;
	}
	public static String getCommonPattern() {
		return COMMON_PATTERN;
	}
	public static String getCombinedAlias() {
		return COMBINED_ALIAS;
	}
	public static String getCombinedPattern() {
		return COMBINED_PATTERN;
	}
}