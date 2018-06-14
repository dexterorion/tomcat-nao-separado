package org.apache.tomcat.util.net;

import java.nio.channels.FileChannel;

// ----------------------------------------------- SendfileData Inner Class
/**
 * SendfileData class.
 */
public class NioEndpointSendfileData {
    // File
    private String fileName;
    private FileChannel fchannel;
    private long pos;
    private long length;
    // KeepAlive flag
    private boolean keepAlive;
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public FileChannel getFchannel() {
		return fchannel;
	}
	public void setFchannel(FileChannel fchannel) {
		this.fchannel = fchannel;
	}
	public long getPos() {
		return pos;
	}
	public void setPos(long pos) {
		this.pos = pos;
	}
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public boolean isKeepAlive() {
		return keepAlive;
	}
	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}
}