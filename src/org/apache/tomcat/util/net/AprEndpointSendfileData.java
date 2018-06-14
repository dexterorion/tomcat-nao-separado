package org.apache.tomcat.util.net;

/**
 * SendfileData class.
 */
public class AprEndpointSendfileData {
	// File
	private String fileName;
	private long fd;
	private long fdpool;
	// Range information
	private long start;
	private long end;
	// Socket and socket pool
	private long socket;
	// Position
	private long pos;
	// KeepAlive flag
	private boolean keepAlive;

	public long getFd() {
		return fd;
	}

	public void setFd(long fd) {
		this.fd = fd;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public long getFdpool() {
		return fdpool;
	}

	public void setFdpool(long fdpool) {
		this.fdpool = fdpool;
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

	public long getSocket() {
		return socket;
	}

	public void setSocket(long socket) {
		this.socket = socket;
	}

	public long getPos() {
		return pos;
	}

	public void setPos(long pos) {
		this.pos = pos;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}
}