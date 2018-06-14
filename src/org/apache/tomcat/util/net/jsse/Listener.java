package org.apache.tomcat.util.net.jsse;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

public class Listener implements HandshakeCompletedListener {
    private volatile boolean completed = false;
    @Override
    public void handshakeCompleted(HandshakeCompletedEvent event) {
        setCompleted(true);
    }
    public void reset() {
        setCompleted(false);
    }
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
}