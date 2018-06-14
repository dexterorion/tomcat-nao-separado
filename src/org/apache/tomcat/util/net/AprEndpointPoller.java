package org.apache.tomcat.util.net;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.net.AprEndpointAprSocketWrapper;

public class AprEndpointPoller implements Runnable {

	/**
 * 
 */
	private final AprEndpoint aprEndpoint;

	/**
	 * @param aprEndpoint
	 */
	public AprEndpointPoller(AprEndpoint aprEndpoint) {
		this.aprEndpoint = aprEndpoint;
	}

	/**
	 * Pointers to the pollers.
	 */
	private long[] pollers = null;

	/**
	 * Actual poller size.
	 */
	private int actualPollerSize = 0;

	/**
	 * Amount of spots left in the poller.
	 */
	private int[] pollerSpace = null;

	/**
	 * Amount of low level pollers in use by this poller.
	 */
	private int pollerCount;

	/**
	 * Timeout value for the poll call.
	 */
	private int pollerTime;

	/**
	 * Variable poller timeout that adjusts depending on how many poll sets are
	 * in use so that the total poll time across all poll sets remains equal to
	 * pollTime.
	 */
	private int nextPollerTime;

	/**
	 * Root pool.
	 */
	private long pool = 0;

	/**
	 * Socket descriptors.
	 */
	private long[] desc;

	/**
	 * List of sockets to be added to the poller.
	 */
	private AprEndpointSocketList addList = null;

	/**
	 * List of sockets to be closed.
	 */
	private AprEndpointSocketList closeList = null;

	/**
	 * Structure used for storing timeouts.
	 */
	private AprEndpointSocketTimeouts timeouts = null;

	/**
	 * Last run of maintain. Maintain will run usually every 5s.
	 */
	private long lastMaintain = System.currentTimeMillis();

	/**
	 * The number of connections currently inside this Poller. The correct
	 * operation of the Poller depends on this figure being correct. If it is
	 * not, it is possible that the Poller will enter a wait loop where it waits
	 * for the next connection to be added to the Poller before it calls poll
	 * when it should still be polling existing connections. Although not
	 * necessary at the time of writing this comment, it has been implemented as
	 * an AtomicInteger to ensure that it remains thread-safe.
	 */
	private AtomicInteger connectionCount = new AtomicInteger(0);

	public int getConnectionCount() {
		return connectionCount.get();
	}

	private volatile boolean pollerRunning = true;

	/**
	 * Create the poller. With some versions of APR, the maximum poller size
	 * will be 62 (recompiling APR is necessary to remove this limitation).
	 */
	protected void init() {

		pool = Pool.create(this.aprEndpoint.getServerSockPool());

		// Single poller by default
		int defaultPollerSize = this.aprEndpoint.getMaxConnections();

		if ((OS.isWin32() || OS.isWin64()) && (defaultPollerSize > 1024)) {
			// The maximum per poller to get reasonable performance is 1024
			// Adjust poller size so that it won't reach the limit. This is
			// a limitation of XP / Server 2003 that has been fixed in
			// Vista / Server 2008 onwards.
			actualPollerSize = 1024;
		} else {
			actualPollerSize = defaultPollerSize;
		}

		timeouts = new AprEndpointSocketTimeouts(defaultPollerSize);

		// At the moment, setting the timeout is useless, but it could get
		// used again as the normal poller could be faster using maintain.
		// It might not be worth bothering though.
		long pollset = this.aprEndpoint.allocatePoller(actualPollerSize, pool,
				-1);
		if (pollset == 0 && actualPollerSize > 1024) {
			actualPollerSize = 1024;
			pollset = this.aprEndpoint.allocatePoller(actualPollerSize, pool,
					-1);
		}
		if (pollset == 0) {
			actualPollerSize = 62;
			pollset = this.aprEndpoint.allocatePoller(actualPollerSize, pool,
					-1);
		}

		pollerCount = defaultPollerSize / actualPollerSize;
		pollerTime = this.aprEndpoint.getPollTime() / pollerCount;
		nextPollerTime = pollerTime;

		pollers = new long[pollerCount];
		pollers[0] = pollset;
		for (int i = 1; i < pollerCount; i++) {
			pollers[i] = this.aprEndpoint.allocatePoller(actualPollerSize,
					pool, -1);
		}

		pollerSpace = new int[pollerCount];
		for (int i = 0; i < pollerCount; i++) {
			pollerSpace[i] = actualPollerSize;
		}

		desc = new long[actualPollerSize * 2];
		connectionCount.set(0);
		addList = new AprEndpointSocketList(defaultPollerSize);
		closeList = new AprEndpointSocketList(defaultPollerSize);
	}

	/*
	 * This method is synchronized so that it is not possible for a socket to be
	 * added to the Poller's addList once this method has completed.
	 */
	protected synchronized void stop() {
		pollerRunning = false;
	}

	/**
	 * Destroy the poller.
	 */
	protected void destroy() {
		// Wait for pollerTime before doing anything, so that the poller
		// threads exit, otherwise parallel destruction of sockets which are
		// still in the poller can cause problems
		try {
			synchronized (this) {
				this.notify();
				this.wait(this.aprEndpoint.getPollTime() / 1000);
			}
		} catch (InterruptedException e) {
			// Ignore
		}
		// Close all sockets in the add queue
		AprEndpointSocketInfo info = addList.get();
		while (info != null) {
			boolean comet = this.aprEndpoint.getConnections()
					.get(Long.valueOf(info.getSocket())).isComet();
			if (!comet
					|| (comet && !this.aprEndpoint.processSocket(info.getSocket(),
							SocketStatus.STOP))) {
				// Poller isn't running at this point so use destroySocket()
				// directly
				this.aprEndpoint.destroySocket(info.getSocket());
			}
			info = addList.get();
		}
		addList.clear();
		// Close all sockets still in the poller
		for (int i = 0; i < pollerCount; i++) {
			int rv = Poll.pollset(pollers[i], desc);
			if (rv > 0) {
				for (int n = 0; n < rv; n++) {
					boolean comet = this.aprEndpoint.getConnections()
							.get(Long.valueOf(desc[n * 2 + 1])).isComet();
					if (!comet
							|| (comet && !this.aprEndpoint.processSocket(
									desc[n * 2 + 1], SocketStatus.STOP))) {
						this.aprEndpoint.destroySocket(desc[n * 2 + 1]);
					}
				}
			}
		}
		Pool.destroy(pool);
		connectionCount.set(0);
	}

	/**
	 * Add specified socket and associated pool to the poller. The socket will
	 * be added to a temporary array, and polled first after a maximum amount of
	 * time equal to pollTime (in most cases, latency will be much lower,
	 * however). Note: If both read and write are false, the socket will only be
	 * checked for timeout; if the socket was already present in the poller, a
	 * callback event will be generated and the socket will be removed from the
	 * poller.
	 *
	 * @param socket
	 *            to add to the poller
	 * @param timeout
	 *            to use for this connection
	 * @param read
	 *            to do read polling
	 * @param write
	 *            to do write polling
	 */
	public void add(long socket, int timeout, boolean read, boolean write) {
		add(socket, timeout, (read ? Poll.getAprPollin() : 0)
				| (write ? Poll.getAprPollout() : 0));
	}

	private void add(long socket, int timeout, int flags) {
		if (aprEndpoint.getLog().isDebugEnabled()) {
			String msg = AprEndpoint.getSm().getString(
					"endpoint.debug.pollerAdd", Long.valueOf(socket),
					Integer.valueOf(timeout), Integer.valueOf(flags));
			if (aprEndpoint.getLog().isTraceEnabled()) {
				aprEndpoint.getLog().trace(msg, new Exception());
			} else {
				aprEndpoint.getLog().debug(msg);
			}
		}
		if (timeout <= 0) {
			// Always put a timeout in
			timeout = Integer.MAX_VALUE;
		}
		boolean ok = false;
		synchronized (this) {
			// Add socket to the list. Newly added sockets will wait
			// at most for pollTime before being polled. Don't add the
			// socket once the poller has stopped but destroy it straight
			// away
			if (pollerRunning && addList.add(socket, timeout, flags)) {
				ok = true;
				this.notify();
			}
		}
		if (!ok) {
			// Can't do anything: close the socket right away
			boolean comet = this.aprEndpoint.getConnections()
					.get(Long.valueOf(socket)).isComet();
			if (!comet
					|| (comet && !this.aprEndpoint.processSocket(socket,
							SocketStatus.ERROR))) {
				this.aprEndpoint.closeSocket(socket);
			}
		}
	}

	/**
	 * Add specified socket to one of the pollers. Must only be called from
	 * {@link Poller#run()}.
	 */
	protected boolean addToPoller(long socket, int events) {
		int rv = -1;
		for (int i = 0; i < pollers.length; i++) {
			if (pollerSpace[i] > 0) {
				rv = Poll.add(pollers[i], socket, events);
				if (rv == Status.getAprSuccess()) {
					pollerSpace[i]--;
					connectionCount.incrementAndGet();
					return true;
				}
			}
		}
		return false;
	}

	protected boolean close(long socket) {
		if (!pollerRunning) {
			return false;
		}
		synchronized (this) {
			if (!pollerRunning) {
				return false;
			}
			closeList.add(socket, 0, 0);
			this.notify();
			return true;
		}
	}

	/**
	 * Remove specified socket from the pollers. Must only be called from
	 * {@link Poller#run()}.
	 */
	private boolean removeFromPoller(long socket) {
		if (aprEndpoint.getLog().isDebugEnabled()) {
			aprEndpoint.getLog().debug(
					AprEndpoint.getSm()
							.getString("endpoint.debug.pollerRemove",
									Long.valueOf(socket)));
		}
		int rv = -1;
		for (int i = 0; i < pollers.length; i++) {
			if (pollerSpace[i] < actualPollerSize) {
				rv = Poll.remove(pollers[i], socket);
				if (rv != Status.getAprNotfound()) {
					pollerSpace[i]++;
					connectionCount.decrementAndGet();
					if (aprEndpoint.getLog().isDebugEnabled()) {
						aprEndpoint.getLog().debug(
								AprEndpoint.getSm().getString(
										"endpoint.debug.pollerRemoved",
										Long.valueOf(socket)));
					}
					break;
				}
			}
		}
		timeouts.remove(socket);
		return (rv == Status.getAprSuccess());
	}

	/**
	 * Timeout checks.
	 */
	protected void maintain() {

		long date = System.currentTimeMillis();
		// Maintain runs at most once every 5s, although it will likely get
		// called more
		if ((date - lastMaintain) < 5000L) {
			return;
		} else {
			lastMaintain = date;
		}
		long socket = timeouts.check(date);
		while (socket != 0) {
			if (aprEndpoint.getLog().isDebugEnabled()) {
				aprEndpoint.getLog().debug(
						AprEndpoint.getSm().getString(
								"endpoint.debug.socketTimeout",
								Long.valueOf(socket)));
			}
			removeFromPoller(socket);
			boolean comet = this.aprEndpoint.getConnections()
					.get(Long.valueOf(socket)).isComet();
			if (!comet
					|| (comet && !this.aprEndpoint.processSocket(socket,
							SocketStatus.TIMEOUT))) {
				this.aprEndpoint.destroySocket(socket);
			}
			socket = timeouts.check(date);
		}

	}

	/**
	 * Displays the list of sockets in the pollers.
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Poller");
		long[] res = new long[actualPollerSize * 2];
		for (int i = 0; i < pollers.length; i++) {
			int count = Poll.pollset(pollers[i], res);
			buf.append(" [ ");
			for (int j = 0; j < count; j++) {
				buf.append(desc[2 * j + 1]).append(" ");
			}
			buf.append("]");
		}
		return buf.toString();
	}

	/**
	 * The background thread that listens for incoming TCP/IP connections and
	 * hands them off to an appropriate processor.
	 */
	@Override
	public void run() {

		int maintain = 0;
		AprEndpointSocketList localAddList = new AprEndpointSocketList(
				this.aprEndpoint.getMaxConnections());
		AprEndpointSocketList localCloseList = new AprEndpointSocketList(
				this.aprEndpoint.getMaxConnections());

		// Loop until we receive a shutdown command
		while (pollerRunning) {

			// Loop if endpoint is paused
			while (pollerRunning && this.aprEndpoint.isPaused()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Ignore
				}
			}
			// Check timeouts if the poller is empty
			while (pollerRunning && connectionCount.get() < 1
					&& addList.size() < 1 && closeList.size() < 1) {
				// Reset maintain time.
				try {
					if (this.aprEndpoint.getSoTimeout() > 0 && pollerRunning) {
						maintain();
					}
					synchronized (this) {
						this.wait(10000);
					}
				} catch (InterruptedException e) {
					// Ignore
				} catch (Throwable t) {
					ExceptionUtils2.handleThrowable(t);
					this.aprEndpoint.getLog().warn(
							AprEndpoint.getSm().getString(
									"endpoint.timeout.err"));
				}
			}

			// Don't add or poll if the poller has been stopped
			if (!pollerRunning) {
				break;
			}

			try {
				// Duplicate the add and remove lists so that the syncs are
				// minimised
				if (closeList.size() > 0) {
					synchronized (this) {
						// Duplicate to another list, so that the syncing is
						// minimal
						closeList.duplicate(localCloseList);
						closeList.clear();
					}
				} else {
					localCloseList.clear();
				}
				if (addList.size() > 0) {
					synchronized (this) {
						// Duplicate to another list, so that the syncing is
						// minimal
						addList.duplicate(localAddList);
						addList.clear();
					}
				} else {
					localAddList.clear();
				}

				// Remove sockets
				if (localCloseList.size() > 0) {
					AprEndpointSocketInfo info = localCloseList.get();
					while (info != null) {
						localAddList.remove(info.getSocket());
						removeFromPoller(info.getSocket());
						this.aprEndpoint.destroySocket(info.getSocket());
						info = localCloseList.get();
					}
				}

				// Add sockets which are waiting to the poller
				if (localAddList.size() > 0) {
					AprEndpointSocketInfo info = localAddList.get();
					while (info != null) {
						if (aprEndpoint.getLog().isDebugEnabled()) {
							aprEndpoint.getLog().debug(
									AprEndpoint.getSm().getString(
											"endpoint.debug.pollerAddDo",
											Long.valueOf(info.getSocket())));
						}
						timeouts.remove(info.getSocket());
						AprEndpointAprSocketWrapper wrapper = this.aprEndpoint
								.getConnections()
								.get(Long.valueOf(info.getSocket()));
						if (wrapper == null) {
							continue;
						}
						if (info.read() || info.write()) {
							boolean comet = wrapper.isComet();
							if (comet || wrapper.getPollerFlags() != 0) {
								removeFromPoller(info.getSocket());
							}
							wrapper.setPollerFlags(wrapper.getPollerFlags()
									| (info.read() ? Poll.getAprPollin() : 0)
									| (info.write() ? Poll.getAprPollout() : 0));
							if (!addToPoller(info.getSocket(),
									wrapper.getPollerFlags())) {
								// Can't do anything: close the socket right
								// away
								if (!comet
										|| (comet && !this.aprEndpoint
												.processSocket(info.getSocket(),
														SocketStatus.ERROR))) {
									this.aprEndpoint.closeSocket(info.getSocket());
								}
							} else {
								timeouts.add(info.getSocket(),
										System.currentTimeMillis()
												+ info.getTimeout());
							}
						} else {
							// Should never happen.
							this.aprEndpoint.closeSocket(info.getSocket());
							this.aprEndpoint
									.getLog()
									.warn(AprEndpoint
											.getSm()
											.getString(
													"endpoint.apr.pollAddInvalid",
													info));
						}
						info = localAddList.get();
					}
				}

				// Poll for the specified interval
				for (int i = 0; i < pollers.length; i++) {

					// Flags to ask to reallocate the pool
					boolean reset = false;
					// ArrayList<Long> skip = null;

					int rv = 0;
					// Iterate on each pollers, but no need to poll empty
					// pollers
					if (pollerSpace[i] < actualPollerSize) {
						rv = Poll.poll(pollers[i], nextPollerTime, desc, true);
						// Reset the nextPollerTime
						nextPollerTime = pollerTime;
					} else {
						// Skipping an empty poll set means skipping a wait
						// time of pollerTime microseconds. If most of the
						// poll sets are skipped then this loop will be
						// tighter than expected which could lead to higher
						// than expected CPU usage. Extending the
						// nextPollerTime ensures that this loop always
						// takes about the same time to execute.
						nextPollerTime += pollerTime;
					}
					if (rv > 0) {
						pollerSpace[i] += rv;
						connectionCount.addAndGet(-rv);
						for (int n = 0; n < rv; n++) {
							long timeout = timeouts.remove(desc[n * 2 + 1]);
							AprEndpointAprSocketWrapper wrapper = this.aprEndpoint
									.getConnections().get(
											Long.valueOf(desc[n * 2 + 1]));
							if (this.aprEndpoint.getLog().isDebugEnabled()) {
								aprEndpoint.getLog().debug(
										AprEndpoint.getSm().getString(
												"endpoint.debug.pollerProcess",
												Long.valueOf(desc[n * 2 + 1]),
												Long.valueOf(desc[n * 2])));
							}
							wrapper.setPollerFlags(wrapper.getPollerFlags()
									& ~((int) desc[n * 2]));
							// Check for failed sockets and hand this socket off
							// to a worker
							if (wrapper.isComet()) {
								// Event processes either a read or a write
								// depending on what the poller returns
								if (((desc[n * 2] & Poll.getAprPollhup()) == Poll.getAprPollhup())
										|| ((desc[n * 2] & Poll.getAprPollerr()) == Poll.getAprPollerr())
										|| ((desc[n * 2] & Poll.getAprPollnval()) == Poll.getAprPollnval())) {
									if (!this.aprEndpoint
											.processSocket(desc[n * 2 + 1],
													SocketStatus.ERROR)) {
										// Close socket and clear pool
										this.aprEndpoint
												.closeSocket(desc[n * 2 + 1]);
									}
								} else if ((desc[n * 2] & Poll.getAprPollin()) == Poll.getAprPollin()) {
									if (wrapper.getPollerFlags() != 0) {
										add(desc[n * 2 + 1], 1,
												wrapper.getPollerFlags());
									}
									if (!this.aprEndpoint.processSocket(
											desc[n * 2 + 1],
											SocketStatus.OPEN_READ)) {
										// Close socket and clear pool
										this.aprEndpoint
												.closeSocket(desc[n * 2 + 1]);
									}
								} else if ((desc[n * 2] & Poll.getAprPollout()) == Poll.getAprPollout()) {
									if (wrapper.getPollerFlags() != 0) {
										add(desc[n * 2 + 1], 1,
												wrapper.getPollerFlags());
									}
									if (!this.aprEndpoint.processSocket(
											desc[n * 2 + 1],
											SocketStatus.OPEN_WRITE)) {
										// Close socket and clear pool
										this.aprEndpoint
												.closeSocket(desc[n * 2 + 1]);
									}
								} else {
									// Unknown event
									this.aprEndpoint
											.getLog()
											.warn(AprEndpoint
													.getSm()
													.getString(
															"endpoint.apr.pollUnknownEvent",
															Long.valueOf(desc[n * 2])));
									if (!this.aprEndpoint
											.processSocket(desc[n * 2 + 1],
													SocketStatus.ERROR)) {
										// Close socket and clear pool
										this.aprEndpoint
												.closeSocket(desc[n * 2 + 1]);
									}
								}
							} else if (((desc[n * 2] & Poll.getAprPollhup()) == Poll.getAprPollhup())
									|| ((desc[n * 2] & Poll.getAprPollerr()) == Poll.getAprPollerr())
									|| ((desc[n * 2] & Poll.getAprPollnval()) == Poll.getAprPollnval())) {
								if (wrapper.isUpgraded()) {
									// Using non-blocking IO. Need to trigger
									// error handling.
									// Poller may return error codes plus the
									// flags it was
									// waiting for or it may just return an
									// error code. By
									// signalling read/write is possible, a
									// read/write will be
									// attempted, fail and that will trigger an
									// exception the
									// application will see.
									// Check the return flags first, followed by
									// what the socket
									// was registered for
									if ((desc[n * 2] & Poll.getAprPollin()) == Poll.getAprPollin()) {
										// Error probably occurred during a
										// non-blocking read
										if (!this.aprEndpoint.processSocket(
												desc[n * 2 + 1],
												SocketStatus.OPEN_READ)) {
											// Close socket and clear pool
											this.aprEndpoint
													.closeSocket(desc[n * 2 + 1]);
										}
									} else if ((desc[n * 2] & Poll.getAprPollout()) == Poll.getAprPollout()) {
										// Error probably occurred during a
										// non-blocking write
										if (!this.aprEndpoint.processSocket(
												desc[n * 2 + 1],
												SocketStatus.OPEN_WRITE)) {
											// Close socket and clear pool
											this.aprEndpoint
													.closeSocket(desc[n * 2 + 1]);
										}
									} else if ((wrapper.getPollerFlags() & Poll.getAprPollin()) == Poll.getAprPollin()) {
										// Can't tell what was happening when
										// the error occurred but the
										// socket is registered for non-blocking
										// read so use that
										if (!this.aprEndpoint.processSocket(
												desc[n * 2 + 1],
												SocketStatus.OPEN_READ)) {
											// Close socket and clear pool
											this.aprEndpoint
													.closeSocket(desc[n * 2 + 1]);
										}
									} else if ((wrapper.getPollerFlags() & Poll.getAprPollout()) == Poll.getAprPollout()) {
										// Can't tell what was happening when
										// the error occurred but the
										// socket is registered for non-blocking
										// write so use that
										if (!this.aprEndpoint.processSocket(
												desc[n * 2 + 1],
												SocketStatus.OPEN_WRITE)) {
											// Close socket and clear pool
											this.aprEndpoint
													.closeSocket(desc[n * 2 + 1]);
										}
									} else {
										// Close socket and clear pool
										this.aprEndpoint
												.closeSocket(desc[n * 2 + 1]);
									}
								} else {
									// Close socket and clear pool
									this.aprEndpoint
											.closeSocket(desc[n * 2 + 1]);
								}
							} else if (((desc[n * 2] & Poll.getAprPollin()) == Poll.getAprPollin())
									|| ((desc[n * 2] & Poll.getAprPollout()) == Poll.getAprPollout())) {
								boolean error = false;
								if (((desc[n * 2] & Poll.getAprPollin()) == Poll.getAprPollin())
										&& !this.aprEndpoint.processSocket(
												desc[n * 2 + 1],
												SocketStatus.OPEN_READ)) {
									error = true;
									// Close socket and clear pool
									this.aprEndpoint
											.closeSocket(desc[n * 2 + 1]);
								}
								if (!error
										&& ((desc[n * 2] & Poll.getAprPollout()) == Poll.getAprPollout())
										&& !this.aprEndpoint.processSocket(
												desc[n * 2 + 1],
												SocketStatus.OPEN_WRITE)) {
									// Close socket and clear pool
									error = true;
									this.aprEndpoint
											.closeSocket(desc[n * 2 + 1]);
								}
								if (!error && wrapper.getPollerFlags() != 0) {
									// If socket was registered for multiple
									// events but
									// only some of the occurred, re-register
									// for the
									// remaining events.
									// timeout is the value of
									// System.currentTimeMillis() that
									// was set as the point that the socket will
									// timeout. When
									// adding to the poller, the timeout from
									// now in
									// milliseconds is required.
									// So first, subtract the current timestamp
									if (timeout > 0) {
										timeout = timeout
												- System.currentTimeMillis();
									}
									// If the socket should have already expired
									// by now,
									// re-add it with a very short timeout
									if (timeout <= 0) {
										timeout = 1;
									}
									// Should be impossible but just in case
									// since timeout will
									// be cast to an int.
									if (timeout > Integer.MAX_VALUE) {
										timeout = Integer.MAX_VALUE;
									}
									add(desc[n * 2 + 1], (int) timeout,
											wrapper.getPollerFlags());
								}
							} else {
								// Unknown event
								this.aprEndpoint
										.getLog()
										.warn(AprEndpoint
												.getSm()
												.getString(
														"endpoint.apr.pollUnknownEvent",
														Long.valueOf(desc[n * 2])));
								// Close socket and clear pool
								this.aprEndpoint.closeSocket(desc[n * 2 + 1]);
							}
						}
					} else if (rv < 0) {
						int errn = -rv;
						// Any non timeup or interrupted error is critical
						if ((errn != Status.getTimeup()) && (errn != Status.getEintr())) {
							if (errn > Status.getAprOsStartUsererr()) {
								errn -= Status.getAprOsStartUsererr();
							}
							this.aprEndpoint.getLog().error(
									AprEndpoint.getSm().getString(
											"endpoint.apr.pollError",
											Integer.valueOf(errn),
											Error.strerror(errn)));
							// Destroy and reallocate the poller
							reset = true;
						}
					}

					if (reset) {
						// Reallocate the current poller
						int count = Poll.pollset(pollers[i], desc);
						long newPoller = this.aprEndpoint.allocatePoller(
								actualPollerSize, pool, -1);
						// Don't restore connections for now, since I have not
						// tested it
						pollerSpace[i] = actualPollerSize;
						connectionCount.addAndGet(-count);
						Poll.destroy(pollers[i]);
						pollers[i] = newPoller;
					}

				}

				// Process socket timeouts
				if (this.aprEndpoint.getSoTimeout() > 0 && maintain++ > 1000
						&& pollerRunning) {
					// This works and uses only one timeout mechanism for
					// everything, but the
					// non event poller might be a bit faster by using the old
					// maintain.
					maintain = 0;
					maintain();
				}

			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
				if (maintain == 0) {
					this.aprEndpoint.getLog().warn(
							AprEndpoint.getSm().getString(
									"endpoint.timeout.error"), t);
				} else {
					this.aprEndpoint.getLog().warn(
							AprEndpoint.getSm()
									.getString("endpoint.poll.error"), t);
				}
			}

		}

		synchronized (this) {
			this.notifyAll();
		}
	}
}