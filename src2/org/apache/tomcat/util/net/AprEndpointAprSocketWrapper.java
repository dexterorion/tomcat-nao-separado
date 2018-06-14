package org.apache.tomcat.util.net;

public class AprEndpointAprSocketWrapper extends SocketWrapper<Long> {

    // This field should only be used by Poller#run()
    private int pollerFlags = 0;

    public AprEndpointAprSocketWrapper(Long socket) {
        super(socket);
    }

	public int getPollerFlags() {
		return pollerFlags;
	}

	public void setPollerFlags(int pollerFlags) {
		this.pollerFlags = pollerFlags;
	}
}