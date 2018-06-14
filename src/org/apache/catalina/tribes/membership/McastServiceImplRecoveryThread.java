package org.apache.catalina.tribes.membership;

import org.apache.catalina.tribes.Channel;

public class McastServiceImplRecoveryThread extends Thread {
	private static volatile boolean running = false;

	public static synchronized void recover(McastServiceImpl parent) {
		if (running)
			return;
		if (!parent.isRecoveryEnabled())
			return;

		running = true;

		Thread t = new McastServiceImplRecoveryThread(parent);

		t.setName("Tribes-MembershipRecovery");
		t.setDaemon(true);
		t.start();
	}

	private McastServiceImpl parent = null;

	public McastServiceImplRecoveryThread(McastServiceImpl parent) {
		this.parent = parent;
	}

	public boolean stopService() {
		try {
			parent.stop(Channel.MBR_RX_SEQ | Channel.MBR_TX_SEQ);
			return true;
		} catch (Exception x) {
			McastServiceImpl.getLog().warn("Recovery thread failed to stop membership service.",
					x);
			return false;
		}
	}

	public boolean startService() {
		try {
			parent.init();
			parent.start(Channel.MBR_RX_SEQ | Channel.MBR_TX_SEQ);
			return true;
		} catch (Exception x) {
			McastServiceImpl.getLog().warn("Recovery thread failed to start membership service.",
					x);
			return false;
		}
	}

	@Override
	public void run() {
		boolean success = false;
		int attempt = 0;
		try {
			while (!success) {
				if (McastServiceImpl.getLog().isInfoEnabled())
					McastServiceImpl.getLog().info("Tribes membership, running recovery thread, multicasting is not functional.");
				if (stopService() & startService()) {
					success = true;
					if (McastServiceImpl.getLog().isInfoEnabled())
						McastServiceImpl.getLog().info("Membership recovery was successful.");
				}
				try {
					if (!success) {
						if (McastServiceImpl.getLog().isInfoEnabled())
							McastServiceImpl.getLog().info("Recovery attempt " + (++attempt)
									+ " failed, trying again in "
									+ parent.getRecoverySleepTime() + " seconds");
						Thread.sleep(parent.getRecoverySleepTime());
					}
				} catch (InterruptedException ignore) {
				}
			}
		} finally {
			running = false;
		}
	}
}