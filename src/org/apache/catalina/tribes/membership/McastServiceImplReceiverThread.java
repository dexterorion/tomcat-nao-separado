package org.apache.catalina.tribes.membership;


public class McastServiceImplReceiverThread extends Thread {
	/**
	 * 
	 */
	private final McastServiceImpl mcastServiceImpl;
	private int errorCounter = 0;

	public McastServiceImplReceiverThread(McastServiceImpl mcastServiceImpl) {
		super();
		this.mcastServiceImpl = mcastServiceImpl;
		setName("Tribes-MembershipReceiver");
	}

	@Override
	public void run() {
		while (this.mcastServiceImpl.isDoRunReceiver()) {
			try {
				this.mcastServiceImpl.receive();
				errorCounter = 0;
			} catch (ArrayIndexOutOfBoundsException ax) {
				// we can ignore this, as it means we have an invalid
				// package
				// but we will log it to debug
				if (McastServiceImpl.getLog().isDebugEnabled())
					McastServiceImpl.getLog().debug("Invalid member mcast package.", ax);
			} catch (Exception x) {
				if (x instanceof InterruptedException)
					interrupted();
				else {
					if (errorCounter == 0 && this.mcastServiceImpl.isDoRunReceiver())
						McastServiceImpl.getLog().warn(
								"Error receiving mcast package. Sleeping 500ms",
								x);
					else if (McastServiceImpl.getLog().isDebugEnabled())
						McastServiceImpl.getLog().debug(
								"Error receiving mcast package"
										+ (this.mcastServiceImpl.isDoRunReceiver() ? ". Sleeping 500ms"
												: "."), x);
					if (this.mcastServiceImpl.isDoRunReceiver()) {
						try {
							Thread.sleep(500);
						} catch (Exception ignore) {
						}
						if ((++errorCounter) >= this.mcastServiceImpl.getRecoveryCounter()) {
							errorCounter = 0;
							McastServiceImplRecoveryThread.recover(this.mcastServiceImpl);
						}
					}
				}
			}
		}
	}
}