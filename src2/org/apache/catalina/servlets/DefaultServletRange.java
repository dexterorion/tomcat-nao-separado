package org.apache.catalina.servlets;

public class DefaultServletRange {

	private long start;
	private long end;
	private long length;

	/**
	 * Validate range.
	 */
	public boolean validate() {
		if (end >= length)
			end = length - 1;
		return (start >= 0) && (end >= 0) && (start <= end) && (length > 0);
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}
	
	
}