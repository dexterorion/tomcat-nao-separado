package org.apache.catalina.tribes.membership;


public class McastServiceImplSenderThread extends Thread {
	/**
	 * 
	 */
	private final McastServiceImpl mcastServiceImpl;
	private long time;
	private int errorCounter = 0;

	public McastServiceImplSenderThread(McastServiceImpl mcastServiceImpl, long time) {
		this.mcastServiceImpl = mcastServiceImpl;
		this.time = time;
		setName("Tribes-MembershipSender");

	}

	@Override
	public void run() {
		while (this.mcastServiceImpl.isDoRunSender()) {
			try {
				this.mcastServiceImpl.send(true);
				errorCounter = 0;
			} catch (Exception x) {
				if (errorCounter == 0)
					McastServiceImpl.getLog().warn("Unable to send mcast message.", x);
				else
					McastServiceImpl.getLog().debug("Unable to send mcast message.", x);
				if ((++errorCounter) >= this.mcastServiceImpl.getRecoveryCounter()) {
					errorCounter = 0;
					McastServiceImplRecoveryThread.recover(this.mcastServiceImpl);
				}
			}
			try {
				Thread.sleep(time);
			} catch (Exception ignore) {
			}
		}
	}
}