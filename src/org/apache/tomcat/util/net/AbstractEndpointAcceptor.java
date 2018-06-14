package org.apache.tomcat.util.net;

public abstract class AbstractEndpointAcceptor implements Runnable {
    private volatile AcceptorState state = AcceptorState.NEW;
    public final AcceptorState getState() {
        return state;
    }
    

    public void setState(AcceptorState state) {
		this.state = state;
	}


	private String threadName;
    protected final void setThreadName(final String threadName) {
        this.threadName = threadName;
    }
    protected final String getThreadName() {
        return threadName;
    }
}