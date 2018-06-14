package org.apache.tomcat.util.net;

import org.apache.tomcat.jni.Poll;

public class AprEndpointSocketInfo {
	private long socket;
	private int timeout;
	private int flags;

	public long getSocket() {
		return socket;
	}

	public void setSocket(long socket) {
		this.socket = socket;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public boolean read() {
		return (flags & Poll.getAprPollin()) == Poll.getAprPollin();
	}

	public boolean write() {
		return (flags & Poll.getAprPollout()) == Poll.getAprPollout();
	}

	public static int merge(int flag1, int flag2) {
		return ((flag1 & Poll.getAprPollin()) | (flag2 & Poll.getAprPollin()))
				| ((flag1 & Poll.getAprPollout()) | (flag2 & Poll.getAprPollout()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Socket: [");
		sb.append(socket);
		sb.append("], timeout: [");
		sb.append(timeout);
		sb.append("], flags: [");
		sb.append(flags);
		return sb.toString();
	}
}