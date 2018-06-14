package org.apache.tomcat.util.net;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.File;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.util.ExceptionUtils2;

public class AprEndpointSendfile implements Runnable {

	/**
 * 
 */
	private final AprEndpoint aprEndpoint;

	/**
	 * @param aprEndpoint
	 */
	public AprEndpointSendfile(AprEndpoint aprEndpoint) {
		this.aprEndpoint = aprEndpoint;
	}

	private long sendfilePollset = 0;
	private long pool = 0;
	private long[] desc;
	private HashMap<Long, AprEndpointSendfileData> sendfileData;

	private int sendfileCount;

	public int getSendfileCount() {
		return sendfileCount;
	}

	private ArrayList<AprEndpointSendfileData> addS;

	private volatile boolean sendfileRunning = true;

	/**
	 * Create the sendfile poller. With some versions of APR, the maximum poller
	 * size will be 62 (recompiling APR is necessary to remove this limitation).
	 */
	protected void init() {
		pool = Pool.create(this.aprEndpoint.getServerSockPool());
		int size = this.aprEndpoint.getSendfileSize();
		if (size <= 0) {
			size = (OS.isWin32() || OS.isWin64()) ? (1 * 1024) : (16 * 1024);
		}
		sendfilePollset = this.aprEndpoint.allocatePoller(size, pool,
				this.aprEndpoint.getSoTimeout());
		if (sendfilePollset == 0 && size > 1024) {
			size = 1024;
			sendfilePollset = this.aprEndpoint.allocatePoller(size, pool,
					this.aprEndpoint.getSoTimeout());
		}
		if (sendfilePollset == 0) {
			size = 62;
			sendfilePollset = this.aprEndpoint.allocatePoller(size, pool,
					this.aprEndpoint.getSoTimeout());
		}
		desc = new long[size * 2];
		sendfileData = new HashMap<Long, AprEndpointSendfileData>(size);
		addS = new ArrayList<AprEndpointSendfileData>();
	}

	/**
	 * Destroy the poller.
	 */
	protected void destroy() {
		sendfileRunning = false;
		// Wait for polltime before doing anything, so that the poller threads
		// exit, otherwise parallel destruction of sockets which are still
		// in the poller can cause problems
		try {
			synchronized (this) {
				this.notify();
				this.wait(this.aprEndpoint.getPollTime() / 1000);
			}
		} catch (InterruptedException e) {
			// Ignore
		}
		// Close any socket remaining in the add queue
		for (int i = (addS.size() - 1); i >= 0; i--) {
			AprEndpointSendfileData data = addS.get(i);
			this.aprEndpoint.closeSocket(data.getSocket());
		}
		// Close all sockets still in the poller
		int rv = Poll.pollset(sendfilePollset, desc);
		if (rv > 0) {
			for (int n = 0; n < rv; n++) {
				this.aprEndpoint.closeSocket(desc[n * 2 + 1]);
			}
		}
		Pool.destroy(pool);
		sendfileData.clear();
	}

	/**
	 * Add the sendfile data to the sendfile poller. Note that in most cases,
	 * the initial non blocking calls to sendfile will return right away, and
	 * will be handled asynchronously inside the kernel. As a result, the poller
	 * will never be used.
	 *
	 * @param data
	 *            containing the reference to the data which should be snet
	 * @return true if all the data has been sent right away, and false
	 *         otherwise
	 */
	public boolean add(AprEndpointSendfileData data) {
		// Initialize fd from data given
		try {
			data.setFdpool(Socket.pool(data.getSocket()));
			data.setFd(File.open(data.getFileName(), File.getAprFopenRead()
					| File.getAprFopenSendfileEnabled() | File.getAprFopenBinary(),
					0, data.getFdpool()));
			data.setPos(data.getStart());
			// Set the socket to nonblocking mode
			Socket.timeoutSet(data.getSocket(), 0);
			while (true) {
				long nw = Socket.sendfilen(data.getSocket(), data.getFd(), data.getPos(),
						data.getEnd() - data.getPos(), 0);
				if (nw < 0) {
					if (!(-nw == Status.getEagain())) {
						Pool.destroy(data.getFdpool());
						data.setSocket(0);
						return false;
					} else {
						// Break the loop and add the socket to poller.
						break;
					}
				} else {
					data.setPos(data.getPos() + nw);
					if (data.getPos() >= data.getEnd()) {
						// Entire file has been sent
						Pool.destroy(data.getFdpool());
						// Set back socket to blocking mode
						Socket.timeoutSet(data.getSocket(),
								this.aprEndpoint.getSoTimeout() * 1000);
						return true;
					}
				}
			}
		} catch (Exception e) {
			aprEndpoint.getLog()
					.warn(AprEndpoint.getSm().getString(
							"endpoint.sendfile.error"), e);
			return false;
		}
		// Add socket to the list. Newly added sockets will wait
		// at most for pollTime before being polled
		synchronized (this) {
			addS.add(data);
			this.notify();
		}
		return false;
	}

	/**
	 * Remove socket from the poller.
	 *
	 * @param data
	 *            the sendfile data which should be removed
	 */
	protected void remove(AprEndpointSendfileData data) {
		int rv = Poll.remove(sendfilePollset, data.getSocket());
		if (rv == Status.getAprSuccess()) {
			sendfileCount--;
		}
		sendfileData.remove(new Long(data.getSocket()));
	}

	/**
	 * The background thread that listens for incoming TCP/IP connections and
	 * hands them off to an appropriate processor.
	 */
	@Override
	public void run() {

		long maintainTime = 0;
		// Loop until we receive a shutdown command
		while (sendfileRunning) {

			// Loop if endpoint is paused
			while (sendfileRunning && this.aprEndpoint.isPaused()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Ignore
				}
			}
			// Loop if poller is empty
			while (sendfileRunning && sendfileCount < 1 && addS.size() < 1) {
				// Reset maintain time.
				maintainTime = 0;
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (InterruptedException e) {
					// Ignore
				}
			}

			// Don't add or poll if the poller has been stopped
			if (!sendfileRunning) {
				break;
			}

			try {
				// Add socket to the poller
				if (addS.size() > 0) {
					synchronized (this) {
						for (int i = (addS.size() - 1); i >= 0; i--) {
							AprEndpointSendfileData data = addS.get(i);
							int rv = Poll.add(sendfilePollset, data.getSocket(),
									Poll.getAprPollout());
							if (rv == Status.getAprSuccess()) {
								sendfileData.put(new Long(data.getSocket()), data);
								sendfileCount++;
							} else {
								this.aprEndpoint.getLog().warn(
										AprEndpoint.getSm().getString(
												"endpoint.sendfile.addfail",
												Integer.valueOf(rv),
												Error.strerror(rv)));
								// Can't do anything: close the socket right
								// away
								this.aprEndpoint.closeSocket(data.getSocket());
							}
						}
						addS.clear();
					}
				}

				maintainTime += this.aprEndpoint.getPollTime();
				// Pool for the specified interval
				int rv = Poll.poll(sendfilePollset,
						this.aprEndpoint.getPollTime(), desc, false);
				if (rv > 0) {
					for (int n = 0; n < rv; n++) {
						// Get the sendfile state
						AprEndpointSendfileData state = sendfileData
								.get(new Long(desc[n * 2 + 1]));
						// Problem events
						if (((desc[n * 2] & Poll.getAprPollhup()) == Poll.getAprPollhup())
								|| ((desc[n * 2] & Poll.getAprPollerr()) == Poll.getAprPollerr())) {
							// Close socket and clear pool
							remove(state);
							// Destroy file descriptor pool, which should close
							// the file
							// Close the socket, as the response would be
							// incomplete
							this.aprEndpoint.closeSocket(state.getSocket());
							continue;
						}
						// Write some data using sendfile
						long nw = Socket.sendfilen(state.getSocket(), state.getFd(),
								state.getPos(), state.getEnd() - state.getPos(), 0);
						if (nw < 0) {
							// Close socket and clear pool
							remove(state);
							// Close the socket, as the response would be
							// incomplete
							// This will close the file too.
							this.aprEndpoint.closeSocket(state.getSocket());
							continue;
						}

						state.setPos(state.getPos() + nw);
						if (state.getPos() >= state.getEnd()) {
							remove(state);
							if (state.isKeepAlive()) {
								// Destroy file descriptor pool, which should
								// close the file
								Pool.destroy(state.getFdpool());
								Socket.timeoutSet(state.getSocket(),
										this.aprEndpoint.getSoTimeout() * 1000);
								// If all done put the socket back in the
								// poller for processing of further requests
								this.aprEndpoint.getPoller().add(state.getSocket(),
										this.aprEndpoint.getKeepAliveTimeout(),
										true, false);
							} else {
								// Close the socket since this is
								// the end of not keep-alive request.
								this.aprEndpoint.closeSocket(state.getSocket());
							}
						}
					}
				} else if (rv < 0) {
					int errn = -rv;
					/* Any non timeup or interrupted error is critical */
					if ((errn != Status.getTimeup()) && (errn != Status.getEintr())) {
						if (errn > Status.getAprOsStartUsererr()) {
							errn -= Status.getAprOsStartUsererr();
						}
						this.aprEndpoint.getLog().error(
								AprEndpoint.getSm().getString(
										"Unexpected poller error",
										Integer.valueOf(errn),
										Error.strerror(errn)));
						// Handle poll critical failure
						synchronized (this) {
							destroy();
							init();
						}
						continue;
					}
				}
				// Call maintain for the sendfile poller
				if (this.aprEndpoint.getSoTimeout() > 0
						&& maintainTime > 1000000L && sendfileRunning) {
					rv = Poll.maintain(sendfilePollset, desc, false);
					maintainTime = 0;
					if (rv > 0) {
						for (int n = 0; n < rv; n++) {
							// Get the sendfile state
							AprEndpointSendfileData state = sendfileData
									.get(new Long(desc[n]));
							// Close socket and clear pool
							remove(state);
							// Destroy file descriptor pool, which should close
							// the file
							// Close the socket, as the response would be
							// incomplete
							this.aprEndpoint.closeSocket(state.getSocket());
						}
					}
				}
			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
				this.aprEndpoint.getLog()
						.error(AprEndpoint.getSm().getString(
								"endpoint.poll.error"), t);
			}
		}

		synchronized (this) {
			this.notifyAll();
		}

	}

}