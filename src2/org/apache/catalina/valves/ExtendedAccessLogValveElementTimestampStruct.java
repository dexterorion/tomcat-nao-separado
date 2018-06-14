package org.apache.catalina.valves;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ExtendedAccessLogValveElementTimestampStruct {
    private final Date currentTimestamp = new Date(0);
    private final SimpleDateFormat currentTimestampFormat;
    private String currentTimestampString;

    public ExtendedAccessLogValveElementTimestampStruct(String format) {
        currentTimestampFormat = new SimpleDateFormat(format, Locale.US);
        currentTimestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

	public Date getCurrentTimestamp() {
		return currentTimestamp;
	}

	public String getCurrentTimestampString() {
		return currentTimestampString;
	}

	public void setCurrentTimestampString(String currentTimestampString) {
		this.currentTimestampString = currentTimestampString;
	}

	public SimpleDateFormat getCurrentTimestampFormat() {
		return currentTimestampFormat;
	}
	
	
}