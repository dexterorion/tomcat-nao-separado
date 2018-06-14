/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.tribes.membership;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.MessageListener;
import org.apache.catalina.tribes.io.ChannelData;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.catalina.tribes.util.ExecutorFactory;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * A <b>membership</b> implementation using simple multicast. This is the
 * representation of a multicast membership service. This class is responsible
 * for maintaining a list of active cluster nodes in the cluster. If a node
 * fails to send out a heartbeat, the node will be dismissed. This is the low
 * level implementation that handles the multicasting sockets. Need to fix this,
 * could use java.nio and only need one thread to send and receive, or just use
 * a timeout on the receive
 * 
 * @author Filip Hanik
 */
public class McastServiceImpl {
	private static final Log log = LogFactory.getLog(McastService.class);

	private static int MAX_PACKET_SIZE = 65535;
	/**
	 * Internal flag used for the listen thread that listens to the multicasting
	 * socket.
	 */
	private volatile boolean doRunSender = false;
	private volatile boolean doRunReceiver = false;
	private int startLevel = 0;
	/**
	 * Socket that we intend to listen to
	 */
	private MulticastSocket socket;
	/**
	 * The local member that we intend to broad cast over and over again
	 */
	private MemberImpl member;
	/**
	 * The multicast address
	 */
	private InetAddress address;
	/**
	 * The multicast port
	 */
	private int port;
	/**
	 * The time it takes for a member to expire.
	 */
	private long timeToExpiration;
	/**
	 * How often to we send out a broadcast saying we are alive, must be smaller
	 * than timeToExpiration
	 */
	private long sendFrequency;
	/**
	 * Reuse the sendPacket, no need to create a new one everytime
	 */
	private DatagramPacket sendPacket;
	/**
	 * Reuse the receivePacket, no need to create a new one everytime
	 */
	private DatagramPacket receivePacket;
	/**
	 * The membership, used so that we calculate memberships when they arrive or
	 * don't arrive
	 */
	private Membership membership;
	/**
	 * The actual listener, for callback when stuff goes down
	 */
	private MembershipListener service;
	/**
	 * The actual listener for broadcast callbacks
	 */
	private MessageListener msgservice;
	/**
	 * Thread to listen for pings
	 */
	private McastServiceImplReceiverThread receiver;
	/**
	 * Thread to send pings
	 */
	private McastServiceImplSenderThread sender;

	/**
	 * Time to live for the multicast packets that are being sent out
	 */
	private int mcastTTL = -1;
	/**
	 * Read timeout on the mcast socket
	 */
	private int mcastSoTimeout = -1;
	/**
	 * bind address
	 */
	private InetAddress mcastBindAddress = null;

	/**
	 * nr of times the system has to fail before a recovery is initiated
	 */
	private int recoveryCounter = 10;

	/**
	 * The time the recovery thread sleeps between recovery attempts
	 */
	private long recoverySleepTime = 5000;

	/**
	 * Add the ability to turn on/off recovery
	 */
	private boolean recoveryEnabled = true;

	/**
	 * Dont interrupt the sender/receiver thread, but pass off to an executor
	 */
	private ExecutorService executor = ExecutorFactory.newThreadPool(0, 2, 2,
			TimeUnit.SECONDS);

	/**
	 * disable/enable local loopback message
	 */
	private boolean localLoopbackDisabled = false;

	/**
	 * Create a new mcast service impl
	 * 
	 * @param member
	 *            - the local member
	 * @param sendFrequency
	 *            - the time (ms) in between pings sent out
	 * @param expireTime
	 *            - the time (ms) for a member to expire
	 * @param port
	 *            - the mcast port
	 * @param bind
	 *            - the bind address (not sure this is used yet)
	 * @param mcastAddress
	 *            - the mcast address
	 * @param service
	 *            - the callback service
	 * @param localLoopbackDisabled
	 *            - disable loopbackMode
	 * @throws IOException
	 */
	public McastServiceImpl(MemberImpl member, long sendFrequency,
			long expireTime, int port, InetAddress bind,
			InetAddress mcastAddress, int ttl, int soTimeout,
			MembershipListener service, MessageListener msgservice,
			boolean localLoopbackDisabled) throws IOException {
		this.member = member;
		this.address = mcastAddress;
		this.port = port;
		this.mcastSoTimeout = soTimeout;
		this.mcastTTL = ttl;
		this.mcastBindAddress = bind;
		this.timeToExpiration = expireTime;
		this.service = service;
		this.msgservice = msgservice;
		this.sendFrequency = sendFrequency;
		this.localLoopbackDisabled = localLoopbackDisabled;
		init();
	}

	public void init() throws IOException {
		setupSocket();
		sendPacket = new DatagramPacket(new byte[MAX_PACKET_SIZE],
				MAX_PACKET_SIZE);
		sendPacket.setAddress(address);
		sendPacket.setPort(port);
		receivePacket = new DatagramPacket(new byte[MAX_PACKET_SIZE],
				MAX_PACKET_SIZE);
		receivePacket.setAddress(address);
		receivePacket.setPort(port);
		member.setCommand(new byte[0]);
		member.getData(true, true);
		if (membership == null)
			membership = new Membership(member);
	}

	protected void setupSocket() throws IOException {
		if (mcastBindAddress != null) {
			try {
				log.info("Attempting to bind the multicast socket to "
						+ address + ":" + port);
				socket = new MulticastSocket(new InetSocketAddress(address,
						port));
			} catch (BindException e) {
				/*
				 * On some platforms (e.g. Linux) it is not possible to bind to
				 * the multicast address. In this case only bind to the port.
				 */
				log.info("Binding to multicast address, failed. Binding to port only.");
				socket = new MulticastSocket(port);
			}
		} else {
			socket = new MulticastSocket(port);
		}
		socket.setLoopbackMode(localLoopbackDisabled); // hint if we want
														// disable loop
														// back(local machine)
														// messages
		if (mcastBindAddress != null) {
			if (log.isInfoEnabled())
				log.info("Setting multihome multicast interface to:"
						+ mcastBindAddress);
			socket.setInterface(mcastBindAddress);
		}
			// force a so timeout so that we don't block forever
		if (mcastSoTimeout <= 0)
			mcastSoTimeout = (int) sendFrequency;
		if (log.isInfoEnabled())
			log.info("Setting cluster mcast soTimeout to " + mcastSoTimeout);
		socket.setSoTimeout(mcastSoTimeout);

		if (mcastTTL >= 0) {
			if (log.isInfoEnabled())
				log.info("Setting cluster mcast TTL to " + mcastTTL);
			socket.setTimeToLive(mcastTTL);
		}
	}

	/**
	 * Start the service
	 * 
	 * @param level
	 *            1 starts the receiver, level 2 starts the sender
	 * @throws IOException
	 *             if the service fails to start
	 * @throws IllegalStateException
	 *             if the service is already started
	 */
	public synchronized void start(int level) throws IOException {
		boolean valid = false;
		if ((level & Channel.MBR_RX_SEQ) == Channel.MBR_RX_SEQ) {
			if (receiver != null)
				throw new IllegalStateException(
						"McastService.receive already running.");
			try {
				if (sender == null)
					socket.joinGroup(address);
			} catch (IOException iox) {
				log.error("Unable to join multicast group, make sure your system has multicasting enabled.");
				throw iox;
			}
			doRunReceiver = true;
			receiver = new McastServiceImplReceiverThread(this);
			receiver.setDaemon(true);
			receiver.start();
			valid = true;
		}
		if ((level & Channel.MBR_TX_SEQ) == Channel.MBR_TX_SEQ) {
			if (sender != null)
				throw new IllegalStateException(
						"McastService.send already running.");
			if (receiver == null)
				socket.joinGroup(address);
			// make sure at least one packet gets out there
			send(false);
			doRunSender = true;
			sender = new McastServiceImplSenderThread(this, sendFrequency);
			sender.setDaemon(true);
			sender.start();
			// we have started the receiver, but not yet waited for membership
			// to establish
			valid = true;
		}
		if (!valid) {
			throw new IllegalArgumentException(
					"Invalid start level. Only acceptable levels are Channel.MBR_RX_SEQ and Channel.MBR_TX_SEQ");
		}
		// pause, once or twice
		waitForMembers(level);
		startLevel = (startLevel | level);
	}

	private void waitForMembers(int level) {
		long memberwait = sendFrequency * 2;
		if (log.isInfoEnabled())
			log.info("Sleeping for "
					+ memberwait
					+ " milliseconds to establish cluster membership, start level:"
					+ level);
		try {
			Thread.sleep(memberwait);
		} catch (InterruptedException ignore) {
		}
		if (log.isInfoEnabled())
			log.info("Done sleeping, membership established, start level:"
					+ level);
	}

	/**
	 * Stops the service
	 * 
	 * @throws IOException
	 *             if the service fails to disconnect from the sockets
	 */
	public synchronized boolean stop(int level) throws IOException {
		boolean valid = false;

		if ((level & Channel.MBR_RX_SEQ) == Channel.MBR_RX_SEQ) {
			valid = true;
			doRunReceiver = false;
			if (receiver != null)
				receiver.interrupt();
			receiver = null;
		}
		if ((level & Channel.MBR_TX_SEQ) == Channel.MBR_TX_SEQ) {
			valid = true;
			doRunSender = false;
			if (sender != null)
				sender.interrupt();
			sender = null;
		}

		if (!valid) {
			throw new IllegalArgumentException(
					"Invalid stop level. Only acceptable levels are Channel.MBR_RX_SEQ and Channel.MBR_TX_SEQ");
		}
		startLevel = (startLevel & (~level));
		// we're shutting down, send a shutdown message and close the socket
		if (startLevel == 0) {
			// send a stop message
			member.setCommand(Member.SHUTDOWN_PAYLOAD);
			member.getData(true, true);
			send(false);
			// leave mcast group
			try {
				socket.leaveGroup(address);
			} catch (Exception ignore) {
			}
			try {
				socket.close();
			} catch (Exception ignore) {
			}
			member.setServiceStartTime(-1);
		}
		return (startLevel == 0);
	}

	/**
	 * Receive a datagram packet, locking wait
	 * 
	 * @throws IOException
	 */
	public void receive() throws IOException {
		boolean checkexpired = true;
		try {

			socket.receive(receivePacket);
			if (receivePacket.getLength() > MAX_PACKET_SIZE) {
				log.error("Multicast packet received was too long, dropping package:"
						+ receivePacket.getLength());
			} else {
				byte[] data = new byte[receivePacket.getLength()];
				System.arraycopy(receivePacket.getData(),
						receivePacket.getOffset(), data, 0, data.length);
				if (XByteBuffer.firstIndexOf(data, 0,
						MemberImpl.getTribesMbrBegin()) == 0) {
					memberDataReceived(data);
				} else {
					memberBroadcastsReceived(data);
				}

			}
		} catch (SocketTimeoutException x) {
			// do nothing, this is normal, we don't want to block forever
			// since the receive thread is the same thread
			// that does membership expiration
		}
		if (checkexpired)
			checkExpired();
	}

	private void memberDataReceived(byte[] data) {
		final MemberImpl m = MemberImpl.getMember(data);
		if (log.isTraceEnabled())
			log.trace("Mcast receive ping from member " + m);
		Runnable t = null;
		if (Arrays.equals(m.getCommand(), Member.SHUTDOWN_PAYLOAD)) {
			if (log.isDebugEnabled())
				log.debug("Member has shutdown:" + m);
			membership.removeMember(m);
			t = new Runnable() {
				@Override
				public void run() {
					String name = Thread.currentThread().getName();
					try {
						Thread.currentThread().setName(
								"Membership-MemberDisappeared.");
						service.memberDisappeared(m);
					} finally {
						Thread.currentThread().setName(name);
					}
				}
			};
		} else if (membership.memberAlive(m)) {
			if (log.isDebugEnabled())
				log.debug("Mcast add member " + m);
			t = new Runnable() {
				@Override
				public void run() {
					String name = Thread.currentThread().getName();
					try {
						Thread.currentThread().setName(
								"Membership-MemberAdded.");
						service.memberAdded(m);
					} finally {
						Thread.currentThread().setName(name);
					}
				}
			};
		}
		if (t != null) {
			executor.execute(t);
		}
	}

	private void memberBroadcastsReceived(final byte[] b) {
		if (log.isTraceEnabled())
			log.trace("Mcast received broadcasts.");
		XByteBuffer buffer = new XByteBuffer(b, true);
		if (buffer.countPackages(true) > 0) {
			int count = buffer.countPackages();
			final ChannelData[] data = new ChannelData[count];
			for (int i = 0; i < count; i++) {
				try {
					data[i] = buffer.extractPackage(true);
				} catch (IllegalStateException ise) {
					log.debug("Unable to decode message.", ise);
				} catch (IOException x) {
					log.debug("Unable to decode message.", x);
				}
			}
			Runnable t = new Runnable() {
				@Override
				public void run() {
					String name = Thread.currentThread().getName();
					try {
						Thread.currentThread().setName(
								"Membership-MemberAdded.");
						for (int i = 0; i < data.length; i++) {
							try {
								if (data[i] != null
										&& !member.equals(data[i].getAddress())) {
									msgservice.messageReceived(data[i]);
								}
							} catch (Throwable t) {
								if (t instanceof ThreadDeath) {
									throw (ThreadDeath) t;
								}
								if (t instanceof VirtualMachineError) {
									throw (VirtualMachineError) t;
								}
								log.error(
										"Unable to receive broadcast message.",
										t);
							}
						}
					} finally {
						Thread.currentThread().setName(name);
					}
				}
			};
			executor.execute(t);
		}
	}

	private final Object expiredMutex = new Object();

	protected void checkExpired() {
		synchronized (expiredMutex) {
			MemberImpl[] expired = membership.expire(timeToExpiration);
			for (int i = 0; i < expired.length; i++) {
				final MemberImpl member = expired[i];
				if (log.isDebugEnabled())
					log.debug("Mcast expire  member " + expired[i]);
				try {
					Runnable t = new Runnable() {
						@Override
						public void run() {
							String name = Thread.currentThread().getName();
							try {
								Thread.currentThread().setName(
										"Membership-MemberExpired.");
								service.memberDisappeared(member);
							} finally {
								Thread.currentThread().setName(name);
							}

						}
					};
					executor.execute(t);
				} catch (Exception x) {
					log.error("Unable to process member disappeared message.",
							x);
				}
			}
		}
	}

	/**
	 * Send a ping
	 * 
	 * @throws IOException
	 */
	public void send(boolean checkexpired) throws IOException {
		send(checkexpired, null);
	}

	private final Object sendLock = new Object();

	public void send(boolean checkexpired, DatagramPacket packet)
			throws IOException {
		checkexpired = (checkexpired && (packet == null));
		// ignore if we haven't started the sender
		// if ( (startLevel&Channel.MBR_TX_SEQ) != Channel.MBR_TX_SEQ ) return;
		if (packet == null) {
			member.inc();
			if (log.isTraceEnabled()) {
				log.trace("Mcast send ping from member " + member);
			}
			byte[] data = member.getData();
			packet = new DatagramPacket(data, data.length);
		} else if (log.isTraceEnabled()) {
			log.trace("Sending message broadcast " + packet.getLength()
					+ " bytes from " + member);
		}
		packet.setAddress(address);
		packet.setPort(port);
		// TODO this operation is not thread safe
		synchronized (sendLock) {
			socket.send(packet);
		}
		if (checkexpired)
			checkExpired();
	}

	public long getServiceStartTime() {
		return (member != null) ? member.getServiceStartTime() : -1l;
	}

	public int getRecoveryCounter() {
		return recoveryCounter;
	}

	public boolean isRecoveryEnabled() {
		return recoveryEnabled;
	}

	public long getRecoverySleepTime() {
		return recoverySleepTime;
	}

	public void setRecoveryCounter(int recoveryCounter) {
		this.recoveryCounter = recoveryCounter;
	}

	public void setRecoveryEnabled(boolean recoveryEnabled) {
		this.recoveryEnabled = recoveryEnabled;
	}

	public void setRecoverySleepTime(long recoverySleepTime) {
		this.recoverySleepTime = recoverySleepTime;
	}

	public static int getMAX_PACKET_SIZE() {
		return MAX_PACKET_SIZE;
	}

	public static void setMAX_PACKET_SIZE(int mAX_PACKET_SIZE) {
		MAX_PACKET_SIZE = mAX_PACKET_SIZE;
	}

	public boolean isDoRunSender() {
		return doRunSender;
	}

	public void setDoRunSender(boolean doRunSender) {
		this.doRunSender = doRunSender;
	}

	public boolean isDoRunReceiver() {
		return doRunReceiver;
	}

	public void setDoRunReceiver(boolean doRunReceiver) {
		this.doRunReceiver = doRunReceiver;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public void setStartLevel(int startLevel) {
		this.startLevel = startLevel;
	}

	public MulticastSocket getSocket() {
		return socket;
	}

	public void setSocket(MulticastSocket socket) {
		this.socket = socket;
	}

	public MemberImpl getMember() {
		return member;
	}

	public void setMember(MemberImpl member) {
		this.member = member;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getTimeToExpiration() {
		return timeToExpiration;
	}

	public void setTimeToExpiration(long timeToExpiration) {
		this.timeToExpiration = timeToExpiration;
	}

	public long getSendFrequency() {
		return sendFrequency;
	}

	public void setSendFrequency(long sendFrequency) {
		this.sendFrequency = sendFrequency;
	}

	public DatagramPacket getSendPacket() {
		return sendPacket;
	}

	public void setSendPacket(DatagramPacket sendPacket) {
		this.sendPacket = sendPacket;
	}

	public DatagramPacket getReceivePacket() {
		return receivePacket;
	}

	public void setReceivePacket(DatagramPacket receivePacket) {
		this.receivePacket = receivePacket;
	}

	public Membership getMembership() {
		return membership;
	}

	public void setMembership(Membership membership) {
		this.membership = membership;
	}

	public MembershipListener getService() {
		return service;
	}

	public void setService(MembershipListener service) {
		this.service = service;
	}

	public MessageListener getMsgservice() {
		return msgservice;
	}

	public void setMsgservice(MessageListener msgservice) {
		this.msgservice = msgservice;
	}

	public McastServiceImplReceiverThread getReceiver() {
		return receiver;
	}

	public void setReceiver(McastServiceImplReceiverThread receiver) {
		this.receiver = receiver;
	}

	public McastServiceImplSenderThread getSender() {
		return sender;
	}

	public void setSender(McastServiceImplSenderThread sender) {
		this.sender = sender;
	}

	public int getMcastTTL() {
		return mcastTTL;
	}

	public void setMcastTTL(int mcastTTL) {
		this.mcastTTL = mcastTTL;
	}

	public int getMcastSoTimeout() {
		return mcastSoTimeout;
	}

	public void setMcastSoTimeout(int mcastSoTimeout) {
		this.mcastSoTimeout = mcastSoTimeout;
	}

	public InetAddress getMcastBindAddress() {
		return mcastBindAddress;
	}

	public void setMcastBindAddress(InetAddress mcastBindAddress) {
		this.mcastBindAddress = mcastBindAddress;
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public boolean isLocalLoopbackDisabled() {
		return localLoopbackDisabled;
	}

	public void setLocalLoopbackDisabled(boolean localLoopbackDisabled) {
		this.localLoopbackDisabled = localLoopbackDisabled;
	}

	public static Log getLog() {
		return log;
	}

	public Object getExpiredMutex() {
		return expiredMutex;
	}

	public Object getSendLock() {
		return sendLock;
	}
	
	
}
