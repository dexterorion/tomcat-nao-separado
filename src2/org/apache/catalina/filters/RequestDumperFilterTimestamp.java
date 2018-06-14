package org.apache.catalina.filters;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class RequestDumperFilterTimestamp {
	private final Date date = new Date(0);
	private final SimpleDateFormat format = new SimpleDateFormat(
			"dd-MMM-yyyy HH:mm:ss");
	private String dateString = format.format(date);

	public void update() {
		setDateString(format.format(date));
	}

	public String getDateString() {
		return dateString;
	}

	public void setDateString(String dateString) {
		this.dateString = dateString;
	}

	public Date getDate() {
		return date;
	}

	public SimpleDateFormat getFormat() {
		return format;
	}

}