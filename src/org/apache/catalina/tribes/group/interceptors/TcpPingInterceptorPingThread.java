package org.apache.catalina.tribes.group.interceptors;

public class TcpPingInterceptorPingThread extends Thread {
    /**
	 * 
	 */
	private final TcpPingInterceptor tcpPingInterceptor;

	/**
	 * @param tcpPingInterceptor
	 */
	public TcpPingInterceptorPingThread(TcpPingInterceptor tcpPingInterceptor) {
		this.tcpPingInterceptor = tcpPingInterceptor;
	}

	@Override
    public void run() {
        while (this.tcpPingInterceptor.isRunning()) {
            try {
                sleep(this.tcpPingInterceptor.getInterval());
                this.tcpPingInterceptor.sendPing();
            }catch ( InterruptedException ix ) {
                interrupted();
            }catch ( Exception x )  {
                TcpPingInterceptor.getLog().warn("Unable to send ping from TCP ping thread.",x);
            }
        }
    }
}