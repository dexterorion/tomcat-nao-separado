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

package org.apache.catalina.tribes.tipis;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelExceptionFaultyMember;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Heartbeat;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.Response2;
import org.apache.catalina.tribes.group.RpcCallback;
import org.apache.catalina.tribes.group.RpcChannel;
import org.apache.catalina.tribes.util.Arrays;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 *
 * @version 1.0
 */
public abstract class AbstractReplicatedMap<K, V> implements Map<K, V>,
		Serializable, RpcCallback, ChannelListener, MembershipListener,
		Heartbeat {

	private static final long serialVersionUID = 1L;

	private final Log log = LogFactory.getLog(AbstractReplicatedMap.class);

	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	/**
	 * The load factor used when none specified in constructor.
	 **/
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * Used to identify the map
	 */
	private static final Charset CHARSET_ISO_8859_1 = Charset
			.forName("ISO-8859-1");

	// ------------------------------------------------------------------------------
	// INSTANCE VARIABLES
	// ------------------------------------------------------------------------------
	private final ConcurrentHashMap<K, AbstractReplicatedMapMapEntry<K, V>> innerMap;

	protected abstract int getStateMessageType();

	/**
	 * Timeout for RPC messages, how long we will wait for a reply
	 */
	private transient long rpcTimeout = 5000;
	/**
	 * Reference to the channel for sending messages
	 */
	private transient Channel channel;
	/**
	 * The RpcChannel to send RPC messages through
	 */
	private transient RpcChannel rpcChannel;
	/**
	 * The Map context name makes this map unique, this allows us to have more
	 * than one map shared through one channel
	 */
	private transient byte[] mapContextName;
	/**
	 * Has the state been transferred
	 */
	private transient boolean stateTransferred = false;
	/**
	 * Simple lock object for transfers
	 */
	private final transient Object stateMutex = new Object();
	/**
	 * A list of members in our map
	 */
	private final transient HashMap<Member, Long> mapMembers = new HashMap<Member, Long>();
	/**
	 * Our default send options
	 */
	private transient int channelSendOptions = Channel.SEND_OPTIONS_DEFAULT;
	/**
	 * The owner of this map, ala a SessionManager for example
	 */
	private transient AbstractReplicatedMapMapOwner mapOwner;
	/**
	 * External class loaders if serialization and deserialization is to be
	 * performed successfully.
	 */
	private transient ClassLoader[] externalLoaders;

	/**
	 * The node we are currently backing up data to, this index will rotate on a
	 * round robin basis
	 */
	private transient int currentNode = 0;

	/**
	 * Since the map keeps internal membership this is the timeout for a ping
	 * message to be responded to If a remote map doesn't respond within this
	 * timeframe, its considered dead.
	 */
	private transient long accessTimeout = 5000;

	/**
	 * Readable string of the mapContextName value
	 */
	private transient String mapname = "";

	/**
	 * Creates a new map
	 * 
	 * @param channel
	 *            The channel to use for communication
	 * @param timeout
	 *            long - timeout for RPC messags
	 * @param mapContextName
	 *            String - unique name for this map, to allow multiple maps per
	 *            channel
	 * @param initialCapacity
	 *            int - the size of this map, see HashMap
	 * @param loadFactor
	 *            float - load factor, see HashMap
	 * @param cls
	 *            - a list of classloaders to be used for deserialization of
	 *            objects.
	 * @param terminate
	 *            - Flag for whether to terminate this map that failed to start.
	 */
	public AbstractReplicatedMap(AbstractReplicatedMapMapOwner owner,
			Channel channel, long timeout, String mapContextName,
			int initialCapacity, float loadFactor, int channelSendOptions,
			ClassLoader[] cls, boolean terminate) {
		innerMap = new ConcurrentHashMap<K, AbstractReplicatedMapMapEntry<K, V>>(
				initialCapacity, loadFactor, 15);
		init(owner, channel, mapContextName, timeout, channelSendOptions, cls,
				terminate);

	}

	/**
	 * Helper methods, wraps a single member in an array
	 * 
	 * @param m
	 *            Member
	 * @return Member[]
	 */
	protected Member[] wrap(Member m) {
		if (m == null)
			return new Member[0];
		else
			return new Member[] { m };
	}

	/**
	 * Initializes the map by creating the RPC channel, registering itself as a
	 * channel listener This method is also responsible for initiating the state
	 * transfer
	 * 
	 * @param owner
	 *            Object
	 * @param channel
	 *            Channel
	 * @param mapContextName
	 *            String
	 * @param timeout
	 *            long
	 * @param channelSendOptions
	 *            int
	 * @param cls
	 *            ClassLoader[]
	 * @param terminate
	 *            - Flag for whether to terminate this map that failed to start.
	 */
	protected void init(AbstractReplicatedMapMapOwner owner, Channel channel,
			String mapContextName, long timeout, int channelSendOptions,
			ClassLoader[] cls, boolean terminate) {
		long start = System.currentTimeMillis();
		if (log.isInfoEnabled())
			log.info("Initializing AbstractReplicatedMap with context name:"
					+ mapContextName);
		this.setMapOwnerData(owner);
		this.setExternalLoadersData(cls);
		this.setChannelSendOptionsData(channelSendOptions);
		this.setChannelData(channel);
		this.setRpcTimeoutData(timeout);

		this.setMapnameData(mapContextName);
		// unique context is more efficient if it is stored as bytes
		this.setMapContextNameData(mapContextName.getBytes(CHARSET_ISO_8859_1));
		if (log.isTraceEnabled())
			log.trace("Created Lazy Map with name:" + mapContextName
					+ ", bytes:" + Arrays.toString(this.getMapContextNameData()));

		// create an rpc channel and add the map as a listener
		this.setRpcChannelData(new RpcChannel(this.getMapContextNameData(), channel, this));
		// add this map as a message listener
		this.getChannelData().addChannelListener(this);
		// listen for membership notifications
		this.getChannelData().addMembershipListener(this);

		try {
			// broadcast our map, this just notifies other members of our
			// existence
			broadcast(AbstractReplicatedMapMapMessage.getMsgInit(), true);
			// transfer state from another map
			transferState();
			// state is transferred, we are ready for messaging
			broadcast(AbstractReplicatedMapMapMessage.getMsgStart(), true);
		} catch (ChannelException x) {
			log.warn("Unable to send map start message.");
			if (terminate) {
				breakdown();
				throw new RuntimeException("Unable to start replicated map.", x);
			}
		}
		long complete = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("AbstractReplicatedMap[" + mapContextName
					+ "] initialization was completed in " + complete + " ms.");
	}

	/**
	 * Sends a ping out to all the members in the cluster, not just map members
	 * that this map is alive.
	 * 
	 * @param timeout
	 *            long
	 * @throws ChannelException
	 */
	protected void ping(long timeout) throws ChannelException {
		// send out a map membership message, only wait for the first reply
		AbstractReplicatedMapMapMessage msg = new AbstractReplicatedMapMapMessage(
				this.getMapContextNameData(), AbstractReplicatedMapMapMessage.getMsgInit(),
				false, null, null, null, getChannelData().getLocalMember(false), null);
		if (getChannelData().getMembers().length > 0) {
			try {
				// send a ping, wait for all nodes to reply
				Response2[] resp = getRpcChannelData().send(getChannelData().getMembers(), msg,
						RpcChannel.getAllReply(), (getChannelSendOptionsData()),
						(int) getAccessTimeoutData());
				for (int i = 0; i < resp.length; i++) {
					memberAlive(resp[i].getSource());
				}
			} catch (ChannelException ce) {
				// Handle known failed members
				ChannelExceptionFaultyMember[] faultyMembers = ce
						.getFaultyMembers();
				for (ChannelExceptionFaultyMember faultyMember : faultyMembers) {
					memberDisappeared(faultyMember.getMember());
				}
				throw ce;
			}
		}
		// update our map of members, expire some if we didn't receive a ping
		// back
		synchronized (getMapMembersData()) {
			Member[] members = getMapMembersData().keySet().toArray(
					new Member[getMapMembersData().size()]);
			long now = System.currentTimeMillis();
			for (Member member : members) {
				long access = getMapMembersData().get(member).longValue();
				if ((now - access) > timeout) {
					memberDisappeared(member);
				}
			}
		}// synch
	}

	/**
	 * We have received a member alive notification
	 * 
	 * @param member
	 *            Member
	 */
	protected void memberAlive(Member member) {
		synchronized (getMapMembersData()) {
			if (!getMapMembersData().containsKey(member)) {
				mapMemberAdded(member);
			}
			getMapMembersData().put(member, new Long(System.currentTimeMillis()));
		}
	}

	/**
	 * Helper method to broadcast a message to all members in a channel
	 * 
	 * @param msgtype
	 *            int
	 * @param rpc
	 *            boolean
	 * @throws ChannelException
	 */
	protected void broadcast(int msgtype, boolean rpc) throws ChannelException {
		Member[] members = getChannelData().getMembers();
		// No destination.
		if (members.length == 0)
			return;
		// send out a map membership message, only wait for the first reply
		AbstractReplicatedMapMapMessage msg = new AbstractReplicatedMapMapMessage(
				this.getMapContextNameData(), msgtype, false, null, null, null,
				getChannelData().getLocalMember(false), null);
		if (rpc) {
			Response2[] resp = getRpcChannelData().send(members, msg,
					RpcChannel.getFirstReply(), (getChannelSendOptionsData()),
					getRpcTimeoutData());
			if (resp.length > 0) {
				for (int i = 0; i < resp.length; i++) {
					mapMemberAdded(resp[i].getSource());
					messageReceived(resp[i].getMessage(), resp[i].getSource());
				}
			} else {
				log.warn("broadcast received 0 replies, probably a timeout.");
			}
		} else {
			getChannelData().send(getChannelData().getMembers(), msg, getChannelSendOptionsData());
		}
	}

	public void breakdown() {
		if (this.getRpcChannelData() != null) {
			this.getRpcChannelData().breakdown();
		}
		try {
			broadcast(AbstractReplicatedMapMapMessage.getMsgStop(), false);
		} catch (Exception ignore) {
		}
		// cleanup
		if (this.getChannelData() != null) {
			this.getChannelData().removeChannelListener(this);
			this.getChannelData().removeMembershipListener(this);
		}
		this.setRpcChannelData(null);
		this.setChannelData(null);
		this.getMapMembersData().clear();
		getInnerMapData().clear();
		this.setStateTransferredData(false);
		this.setExternalLoadersData(null);
	}

	@Override
	public void finalize() throws Throwable {
		try {
			breakdown();
		} finally {
			super.finalize();
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.getMapContextNameData());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractReplicatedMap))
			return false;
		if (!(o.getClass().equals(this.getClass())))
			return false;
		@SuppressWarnings("unchecked")
		AbstractReplicatedMap<K, V> other = (AbstractReplicatedMap<K, V>) o;
		return Arrays.equals(getMapContextNameData(), other.getMapContextNameData());
	}

	// ------------------------------------------------------------------------------
	// GROUP COM INTERFACES
	// ------------------------------------------------------------------------------
	public Member[] getMapMembers(HashMap<Member, Long> members) {
		synchronized (members) {
			Member[] result = new Member[members.size()];
			members.keySet().toArray(result);
			return result;
		}
	}

	public Member[] getMapMembers() {
		return getMapMembers(this.getMapMembersData());
	}

	public Member[] getMapMembersExcl(Member[] exclude) {
		synchronized (getMapMembersData()) {
			@SuppressWarnings("unchecked")
			// mapMembers has the correct type
			HashMap<Member, Long> list = (HashMap<Member, Long>) getMapMembersData()
					.clone();
			for (int i = 0; i < exclude.length; i++)
				list.remove(exclude[i]);
			return getMapMembers(list);
		}
	}

	/**
	 * Replicates any changes to the object since the last time The object has
	 * to be primary, ie, if the object is a proxy or a backup, it will not be
	 * replicated<br>
	 * 
	 * @param complete
	 *            - if set to true, the object is replicated to its backup if
	 *            set to false, only objects that implement ReplicatedMapEntry
	 *            and the isDirty() returns true will be replicated
	 */
	public void replicate(Object key, boolean complete) {
		if (log.isTraceEnabled())
			log.trace("Replicate invoked on key:" + key);
		AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(key);
		if (entry == null)
			return;
		if (!entry.isSerializable())
			return;
		if (entry.isPrimary() && entry.getBackupNodes() != null
				&& entry.getBackupNodes().length > 0) {
			// check to see if we need to replicate this object
			// isDirty()||complete || isAccessReplicate()
			ReplicatedMapEntry rentry = null;
			if (entry.getValue() instanceof ReplicatedMapEntry)
				rentry = (ReplicatedMapEntry) entry.getValue();
			boolean isDirty = rentry != null && rentry.isDirty();
			boolean isAccess = rentry != null && rentry.isAccessReplicate();
			boolean repl = complete || isDirty || isAccess;

			if (!repl) {
				if (log.isTraceEnabled())
					log.trace("Not replicating:" + key + ", no change made");

				return;
			}
			// check to see if the message is diffable
			AbstractReplicatedMapMapMessage msg = null;
			if (rentry != null && rentry.isDiffable() && (isDirty || complete)) {
				rentry.lock();
				try {
					// construct a diff message
					msg = new AbstractReplicatedMapMapMessage(getMapContextNameData(),
							AbstractReplicatedMapMapMessage.getMsgBackup(), true,
							(Serializable) entry.getKey(), null,
							rentry.getDiff(), entry.getPrimary(),
							entry.getBackupNodes());
					rentry.resetDiff();
				} catch (IOException x) {
					log.error(
							"Unable to diff object. Will replicate the entire object instead.",
							x);
				} finally {
					rentry.unlock();
				}
			}
			if (msg == null && complete) {
				// construct a complete
				msg = new AbstractReplicatedMapMapMessage(getMapContextNameData(),
						AbstractReplicatedMapMapMessage.getMsgBackup(), false,
						(Serializable) entry.getKey(),
						(Serializable) entry.getValue(), null,
						entry.getPrimary(), entry.getBackupNodes());
			}
			if (msg == null) {
				// construct a access message
				msg = new AbstractReplicatedMapMapMessage(getMapContextNameData(),
						AbstractReplicatedMapMapMessage.getMsgAccess(), false,
						(Serializable) entry.getKey(), null, null,
						entry.getPrimary(), entry.getBackupNodes());
			}
			try {
				if (getChannelData() != null && entry.getBackupNodes() != null
						&& entry.getBackupNodes().length > 0) {
					if (rentry != null)
						rentry.setLastTimeReplicated(System.currentTimeMillis());
					getChannelData().send(entry.getBackupNodes(), msg,
							getChannelSendOptionsData());
				}
			} catch (ChannelException x) {
				log.error("Unable to replicate data.", x);
			}
		}

	}

	/**
	 * This can be invoked by a periodic thread to replicate out any changes.
	 * For maps that don't store objects that implement ReplicatedMapEntry, this
	 * method should be used infrequently to avoid large amounts of data
	 * transfer
	 * 
	 * @param complete
	 *            boolean
	 */
	public void replicate(boolean complete) {
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
				.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<?, ?> e = i.next();
			replicate(e.getKey(), complete);
		}

	}

	public void transferState() {
		try {
			Member[] members = getMapMembers();
			Member backup = members.length > 0 ? (Member) members[0] : null;
			if (backup != null) {
				AbstractReplicatedMapMapMessage msg = new AbstractReplicatedMapMapMessage(
						getMapContextNameData(), getStateMessageType(), false, null,
						null, null, null, null);
				Response2[] resp = getRpcChannelData().send(new Member[] { backup }, msg,
						RpcChannel.getFirstReply(), getChannelSendOptionsData(),
						getRpcTimeoutData());
				if (resp.length > 0) {
					synchronized (getStateMutexData()) {
						msg = (AbstractReplicatedMapMapMessage) resp[0]
								.getMessage();
						msg.deserialize(getExternalLoaders());
						ArrayList<?> list = (ArrayList<?>) msg.getValue();
						for (int i = 0; i < list.size(); i++) {
							messageReceived((Serializable) list.get(i),
									resp[0].getSource());
						}
					}
				} else {
					log.warn("Transfer state, 0 replies, probably a timeout.");
				}
			}
		} catch (ChannelException x) {
			log.error("Unable to transfer LazyReplicatedMap state.", x);
		} catch (IOException x) {
			log.error("Unable to transfer LazyReplicatedMap state.", x);
		} catch (ClassNotFoundException x) {
			log.error("Unable to transfer LazyReplicatedMap state.", x);
		}
		setStateTransferredData(true);
	}

	/**
	 * TODO implement state transfer
	 * 
	 * @param msg
	 *            Serializable
	 * @return Serializable - null if no reply should be sent
	 */
	@Override
	public Serializable replyRequest(Serializable msg, final Member sender) {
		if (!(msg instanceof AbstractReplicatedMapMapMessage))
			return null;
		AbstractReplicatedMapMapMessage mapmsg = (AbstractReplicatedMapMapMessage) msg;

		// map init request
		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgInit()) {
			mapmsg.setPrimary(getChannelData().getLocalMember(false));
			return mapmsg;
		}

		// map start request
		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgStart()) {
			mapmsg.setPrimary(getChannelData().getLocalMember(false));
			mapMemberAdded(sender);
			return mapmsg;
		}

		// backup request
		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgRetrieveBackup()) {
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(mapmsg
					.getKey());
			if (entry == null || (!entry.isSerializable()))
				return null;
			mapmsg.setValue((Serializable) entry.getValue());
			return mapmsg;
		}

		// state transfer request
		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgState()
				|| mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgStateCopy()) {
			synchronized (getStateMutexData()) { // make sure we dont do two things at
										// the same time
				ArrayList<AbstractReplicatedMapMapMessage> list = new ArrayList<AbstractReplicatedMapMapMessage>();
				Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
						.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<?, ?> e = i.next();
					AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(e
							.getKey());
					if (entry != null && entry.isSerializable()) {
						boolean copy = (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgStateCopy());
						AbstractReplicatedMapMapMessage me = new AbstractReplicatedMapMapMessage(
								getMapContextNameData(),
								copy ? AbstractReplicatedMapMapMessage.getMsgCopy()
										: AbstractReplicatedMapMapMessage.getMsgProxy(),
								false, (Serializable) entry.getKey(),
								copy ? (Serializable) entry.getValue() : null,
								null, entry.getPrimary(), entry
										.getBackupNodes());
						list.add(me);
					}
				}
				mapmsg.setValue(list);
				return mapmsg;

			}
		}

		return null;

	}

	/**
	 * If the reply has already been sent to the requesting thread, the rpc
	 * callback can handle any data that comes in after the fact.
	 * 
	 * @param msg
	 *            Serializable
	 * @param sender
	 *            Member
	 */
	@Override
	public void leftOver(Serializable msg, Member sender) {
		// left over membership messages
		if (!(msg instanceof AbstractReplicatedMapMapMessage))
			return;

		AbstractReplicatedMapMapMessage mapmsg = (AbstractReplicatedMapMapMessage) msg;
		try {
			mapmsg.deserialize(getExternalLoaders());
			if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgStart()) {
				mapMemberAdded(mapmsg.getPrimary());
			} else if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgInit()) {
				memberAlive(mapmsg.getPrimary());
			}
		} catch (IOException x) {
			log.error("Unable to deserialize AbstractReplicateMapMapMessage.",
					x);
		} catch (ClassNotFoundException x) {
			log.error("Unable to deserialize AbstractReplicateMapMapMessage.",
					x);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void messageReceived(Serializable msg, Member sender) {
		if (!(msg instanceof AbstractReplicatedMapMapMessage))
			return;

		AbstractReplicatedMapMapMessage mapmsg = (AbstractReplicatedMapMapMessage) msg;
		if (log.isTraceEnabled()) {
			log.trace("Map[" + getMapnameData() + "] received message:" + mapmsg);
		}

		try {
			mapmsg.deserialize(getExternalLoaders());
		} catch (IOException x) {
			log.error("Unable to deserialize AbstractReplicateMapMapMessage.",
					x);
			return;
		} catch (ClassNotFoundException x) {
			log.error("Unable to deserialize AbstractReplicateMapMapMessage.",
					x);
			return;
		}
		if (log.isTraceEnabled())
			log.trace("Map message received from:" + sender.getName() + " msg:"
					+ mapmsg);
		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgStart()) {
			mapMemberAdded(mapmsg.getPrimary());
		}

		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgStop()) {
			memberDisappeared(mapmsg.getPrimary());
		}

		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgProxy()) {
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(mapmsg
					.getKey());
			if (entry == null) {
				entry = new AbstractReplicatedMapMapEntry<K, V>(
						(K) mapmsg.getKey(), (V) mapmsg.getValue());
				AbstractReplicatedMapMapEntry<K, V> old = getInnerMapData().putIfAbsent(
						entry.getKey(), entry);
				if (old != null) {
					entry = old;
				}
			}
			entry.setProxy(true);
			entry.setBackup(false);
			entry.setBackupNodes(mapmsg.getBackupNodes());
			entry.setPrimary(mapmsg.getPrimary());
		}

		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgRemove()) {
			getInnerMapData().remove(mapmsg.getKey());
		}

		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgBackup()
				|| mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgCopy()) {
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(mapmsg
					.getKey());
			if (entry == null) {
				entry = new AbstractReplicatedMapMapEntry<K, V>(
						(K) mapmsg.getKey(), (V) mapmsg.getValue());
				entry.setBackup(mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgBackup());
				entry.setProxy(false);
				entry.setBackupNodes(mapmsg.getBackupNodes());
				entry.setPrimary(mapmsg.getPrimary());
				if (mapmsg.getValue() != null
						&& mapmsg.getValue() instanceof ReplicatedMapEntry) {
					((ReplicatedMapEntry) mapmsg.getValue())
							.setOwner(getAbstractReplicateMapMapOwner());
				}
			} else {
				entry.setBackup(mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgBackup());
				entry.setProxy(false);
				entry.setBackupNodes(mapmsg.getBackupNodes());
				entry.setPrimary(mapmsg.getPrimary());
				if (entry.getValue() instanceof ReplicatedMapEntry) {
					ReplicatedMapEntry diff = (ReplicatedMapEntry) entry
							.getValue();
					if (mapmsg.isDiff()) {
						diff.lock();
						try {
							diff.applyDiff(mapmsg.getDiffValue(), 0,
									mapmsg.getDiffValue().length);
						} catch (Exception x) {
							log.error(
									"Unable to apply diff to key:"
											+ entry.getKey(), x);
						} finally {
							diff.unlock();
						}
					} else {
						if (mapmsg.getValue() != null)
							entry.setValue((V) mapmsg.getValue());
						((ReplicatedMapEntry) entry.getValue())
								.setOwner(getAbstractReplicateMapMapOwner());
					}
				} else if (mapmsg.getValue() instanceof ReplicatedMapEntry) {
					ReplicatedMapEntry re = (ReplicatedMapEntry) mapmsg
							.getValue();
					re.setOwner(getAbstractReplicateMapMapOwner());
					entry.setValue((V) re);
				} else {
					if (mapmsg.getValue() != null)
						entry.setValue((V) mapmsg.getValue());
				}
			}
			getInnerMapData().put(entry.getKey(), entry);
		}

		if (mapmsg.getMsgType() == AbstractReplicatedMapMapMessage.getMsgAccess()) {
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(mapmsg
					.getKey());
			if (entry != null) {
				entry.setBackupNodes(mapmsg.getBackupNodes());
				entry.setPrimary(mapmsg.getPrimary());
				if (entry.getValue() instanceof ReplicatedMapEntry) {
					((ReplicatedMapEntry) entry.getValue()).accessEntry();
				}
			}
		}
	}

	@Override
	public boolean accept(Serializable msg, Member sender) {
		boolean result = false;
		if (msg instanceof AbstractReplicatedMapMapMessage) {
			if (log.isTraceEnabled())
				log.trace("Map[" + getMapnameData() + "] accepting...." + msg);
			result = Arrays.equals(getMapContextNameData(),
					((AbstractReplicatedMapMapMessage) msg).getMapId());
			if (log.isTraceEnabled())
				log.trace("Msg[" + getMapnameData() + "] accepted[" + result + "]...."
						+ msg);
		}
		return result;
	}

	public void mapMemberAdded(Member member) {
		if (member.equals(getChannel().getLocalMember(false)))
			return;
		boolean memberAdded = false;
		// select a backup node if we don't have one
		synchronized (getMapMembersData()) {
			if (!getMapMembersData().containsKey(member)) {
				getMapMembersData().put(member, new Long(System.currentTimeMillis()));
				memberAdded = true;
			}
		}
		if (memberAdded) {
			synchronized (getStateMutexData()) {
				Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
						.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>> e = i
							.next();
					AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(e
							.getKey());
					if (entry == null)
						continue;
					if (entry.isPrimary()
							&& (entry.getBackupNodes() == null || entry
									.getBackupNodes().length == 0)) {
						try {
							Member[] backup = publishEntryInfo(entry.getKey(),
									entry.getValue());
							entry.setBackupNodes(backup);
							entry.setPrimary(getChannelData().getLocalMember(false));
						} catch (ChannelException x) {
							log.error("Unable to select backup node.", x);
						}
					}
				}
			}
		}
	}

	public boolean inSet(Member m, Member[] set) {
		if (set == null)
			return false;
		boolean result = false;
		for (int i = 0; i < set.length && (!result); i++)
			if (m.equals(set[i]))
				result = true;
		return result;
	}

	public Member[] excludeFromSet(Member[] mbrs, Member[] set) {
		ArrayList<Member> result = new ArrayList<Member>();
		for (int i = 0; i < set.length; i++) {
			boolean include = true;
			for (int j = 0; j < mbrs.length && include; j++)
				if (mbrs[j].equals(set[i]))
					include = false;
			if (include)
				result.add(set[i]);
		}
		return result.toArray(new Member[result.size()]);
	}

	@Override
	public void memberAdded(Member member) {
		// do nothing
	}

	@Override
	public void memberDisappeared(Member member) {
		boolean removed = false;
		synchronized (getMapMembersData()) {
			removed = (getMapMembersData().remove(member) != null);
			if (!removed) {
				if (log.isDebugEnabled())
					log.debug("Member[" + member
							+ "] disappeared, but was not present in the map.");
				return; // the member was not part of our map.
			}
		}
		if (log.isInfoEnabled())
			log.info("Member["
					+ member
					+ "] disappeared. Related map entries will be relocated to the new node.");
		long start = System.currentTimeMillis();
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
				.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>> e = i.next();
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(e.getKey());
			if (entry == null)
				continue;
			if (entry.isPrimary() && inSet(member, entry.getBackupNodes())) {
				if (log.isDebugEnabled())
					log.debug("[1] Primary choosing a new backup");
				try {
					Member[] backup = publishEntryInfo(entry.getKey(),
							entry.getValue());
					entry.setBackupNodes(backup);
					entry.setPrimary(getChannelData().getLocalMember(false));
				} catch (ChannelException x) {
					log.error("Unable to relocate[" + entry.getKey()
							+ "] to a new backup node", x);
				}
			} else if (member.equals(entry.getPrimary())) {
				if (log.isDebugEnabled())
					log.debug("[2] Primary disappeared");
				entry.setPrimary(null);
			}

			if (entry.isProxy() && entry.getPrimary() == null
					&& entry.getBackupNodes() != null
					&& entry.getBackupNodes().length == 1
					&& entry.getBackupNodes()[0].equals(member)) {
				// remove proxies that have no backup nor primaries
				if (log.isDebugEnabled())
					log.debug("[3] Removing orphaned proxy");
				i.remove();
			} else if (entry.getPrimary() == null
					&& entry.isBackup()
					&& entry.getBackupNodes() != null
					&& entry.getBackupNodes().length == 1
					&& entry.getBackupNodes()[0].equals(getChannelData()
							.getLocalMember(false))) {
				try {
					if (log.isDebugEnabled())
						log.debug("[4] Backup becoming primary");
					entry.setPrimary(getChannelData().getLocalMember(false));
					entry.setBackup(false);
					entry.setProxy(false);
					Member[] backup = publishEntryInfo(entry.getKey(),
							entry.getValue());
					entry.setBackupNodes(backup);
					if (getMapOwnerData() != null)
						getMapOwnerData().objectMadePrimay(entry.getKey(),
								entry.getValue());

				} catch (ChannelException x) {
					log.error("Unable to relocate[" + entry.getKey()
							+ "] to a new backup node", x);
				}
			}

		}
		long complete = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("Relocation of map entries was complete in " + complete
					+ " ms.");
	}

	public int getNextBackupIndex() {
		int size = getMapMembersData().size();
		if (getMapMembersData().size() == 0)
			return -1;
		int node = getCurrentNodeData();
		setCurrentNodeData(getCurrentNodeData() + 1);
		if (node >= size) {
			node = 0;
			setCurrentNodeData(0);
		}
		return node;
	}

	public Member getNextBackupNode() {
		Member[] members = getMapMembers();
		int node = getNextBackupIndex();
		if (members.length == 0 || node == -1)
			return null;
		if (node >= members.length)
			node = 0;
		return members[node];
	}

	protected abstract Member[] publishEntryInfo(Object key, Object value)
			throws ChannelException;

	@Override
	public void heartbeat() {
		try {
			ping(getAccessTimeoutData());
		} catch (Exception x) {
			log.error("Unable to send AbstractReplicatedMap.ping message", x);
		}
	}

	// ------------------------------------------------------------------------------
	// METHODS TO OVERRIDE
	// ------------------------------------------------------------------------------

	/**
	 * Removes an object from this map, it will also remove it from
	 *
	 * @param key
	 *            Object
	 * @return Object
	 */
	@Override
	public V remove(Object key) {
		return remove(key, true);
	}

	public V remove(Object key, boolean notify) {
		AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().remove(key);

		try {
			if (getMapMembers().length > 0 && notify) {
				AbstractReplicatedMapMapMessage msg = new AbstractReplicatedMapMapMessage(
						getMapContextName(),
						AbstractReplicatedMapMapMessage.getMsgRemove(), false,
						(Serializable) key, null, null, null, null);
				getChannel()
						.send(getMapMembers(), msg, getChannelSendOptions());
			}
		} catch (ChannelException x) {
			log.error(
					"Unable to replicate out data for a LazyReplicatedMap.remove operation",
					x);
		}
		return entry != null ? entry.getValue() : null;
	}

	public AbstractReplicatedMapMapEntry<K, V> getInternal(Object key) {
		return getInnerMapData().get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(key);
		if (log.isTraceEnabled())
			log.trace("Requesting id:" + key + " entry:" + entry);
		if (entry == null)
			return null;
		if (!entry.isPrimary()) {
			// if the message is not primary, we need to retrieve the latest
			// value
			try {
				Member[] backup = null;
				AbstractReplicatedMapMapMessage msg = null;
				if (!entry.isBackup()) {
					// make sure we don't retrieve from ourselves
					msg = new AbstractReplicatedMapMapMessage(
							getMapContextName(),
							AbstractReplicatedMapMapMessage.getMsgRetrieveBackup(),
							false, (Serializable) key, null, null, null, null);
					Response2[] resp = getRpcChannel().send(
							entry.getBackupNodes(), msg,
							RpcChannel.getFirstReply(),
							Channel.SEND_OPTIONS_DEFAULT, getRpcTimeout());
					if (resp == null || resp.length == 0) {
						// no responses
						log.warn("Unable to retrieve remote object for key:"
								+ key);
						return null;
					}
					msg = (AbstractReplicatedMapMapMessage) resp[0].getMessage();
					msg.deserialize(getExternalLoaders());
					backup = entry.getBackupNodes();
					if (entry.getValue() instanceof ReplicatedMapEntry) {
						ReplicatedMapEntry val = (ReplicatedMapEntry) entry
								.getValue();
						val.setOwner(getAbstractReplicateMapMapOwner());
					}
					if (msg.getValue() != null)
						entry.setValue((V) msg.getValue());
				}
				if (entry.isBackup()) {
					// select a new backup node
					backup = publishEntryInfo(key, entry.getValue());
				} else if (entry.isProxy()) {
					// invalidate the previous primary
					msg = new AbstractReplicatedMapMapMessage(
							getMapContextName(),
							AbstractReplicatedMapMapMessage.getMsgProxy(), false,
							(Serializable) key, null, null,
							getChannelData().getLocalMember(false), backup);
					Member[] dest = getMapMembersExcl(backup);
					if (dest != null && dest.length > 0) {
						getChannel().send(dest, msg, getChannelSendOptions());
					}
					if (entry.getValue() != null
							&& entry.getValue() instanceof ReplicatedMapEntry) {
						ReplicatedMapEntry val = (ReplicatedMapEntry) entry
								.getValue();
						val.setOwner(getAbstractReplicateMapMapOwner());
					}
				}
				entry.setPrimary(getChannelData().getLocalMember(false));
				entry.setBackupNodes(backup);
				entry.setBackup(false);
				entry.setProxy(false);
				if (getAbstractReplicateMapMapOwner() != null)
					getAbstractReplicateMapMapOwner().objectMadePrimay(key,
							entry.getValue());

			} catch (Exception x) {
				log.error(
						"Unable to replicate out data for a LazyReplicatedMap.get operation",
						x);
				return null;
			}
		}
		if (log.isTraceEnabled())
			log.trace("Requesting id:" + key + " result:" + entry.getValue());
		return entry.getValue();
	}

	protected void printMap(String header) {
		try {
			System.out.println("\nDEBUG MAP:" + header);
			System.out.println("Map["
					+ new String(getMapContextNameData(), CHARSET_ISO_8859_1)
					+ ", Map Size:" + getInnerMapData().size());
			Member[] mbrs = getMapMembers();
			for (int i = 0; i < mbrs.length; i++) {
				System.out.println("Mbr[" + (i + 1) + "=" + mbrs[i].getName());
			}
			Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
					.entrySet().iterator();
			int cnt = 0;

			while (i.hasNext()) {
				Map.Entry<?, ?> e = i.next();
				System.out.println((++cnt) + ". " + getInnerMapData().get(e.getKey()));
			}
			System.out.println("EndMap]\n\n");
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}

	/**
	 * Returns true if the key has an entry in the map. The entry can be a proxy
	 * or a backup entry, invoking <code>get(key)</code> will make this entry
	 * primary for the group
	 * 
	 * @param key
	 *            Object
	 * @return boolean
	 */
	@Override
	public boolean containsKey(Object key) {
		return getInnerMapData().containsKey(key);
	}

	@Override
	public V put(K key, V value) {
		return put(key, value, true);
	}

	public V put(K key, V value, boolean notify) {
		AbstractReplicatedMapMapEntry<K, V> entry = new AbstractReplicatedMapMapEntry<K, V>(
				key, value);
		entry.setBackup(false);
		entry.setProxy(false);
		entry.setPrimary(getChannelData().getLocalMember(false));

		V old = null;

		// make sure that any old values get removed
		if (containsKey(key))
			old = remove(key);
		try {
			if (notify) {
				Member[] backup = publishEntryInfo(key, value);
				entry.setBackupNodes(backup);
			}
		} catch (ChannelException x) {
			log.error(
					"Unable to replicate out data for a LazyReplicatedMap.put operation",
					x);
		}
		getInnerMapData().put(key, entry);
		return old;
	}

	/**
	 * Copies all values from one map to this instance
	 * 
	 * @param m
	 *            Map
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		Iterator<?> i = m.entrySet().iterator();
		while (i.hasNext()) {
			@SuppressWarnings("unchecked")
			Map.Entry<K, V> entry = (Map.Entry<K, V>) i.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		clear(true);
	}

	public void clear(boolean notify) {
		if (notify) {
			// only delete active keys
			Iterator<K> keys = keySet().iterator();
			while (keys.hasNext())
				remove(keys.next());
		} else {
			getInnerMapData().clear();
		}
	}

	@Override
	public boolean containsValue(Object value) {
		if (value == null) {
			throw new NullPointerException();
		}
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
				.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>> e = i.next();
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(e.getKey());
			if (entry != null && entry.isActive()
					&& value.equals(entry.getValue()))
				return true;
		}
		return false;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException(
				"This operation is not valid on a replicated map");
	}

	/**
	 * Returns the entire contents of the map Map.Entry.getValue() will return a
	 * LazyReplicatedMap.MapEntry object containing all the information about
	 * the object.
	 * 
	 * @return Set
	 */
	public Set<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> entrySetFull() {
		return getInnerMapData().entrySet();
	}

	public Set<K> keySetFull() {
		return getInnerMapData().keySet();
	}

	public int sizeFull() {
		return getInnerMapData().size();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		LinkedHashSet<Map.Entry<K, V>> set = new LinkedHashSet<Map.Entry<K, V>>(
				getInnerMapData().size());
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
				.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<?, ?> e = i.next();
			Object key = e.getKey();
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(key);
			if (entry != null && entry.isActive()) {
				set.add(entry);
			}
		}
		return Collections.unmodifiableSet(set);
	}

	@Override
	public Set<K> keySet() {
		// todo implement
		// should only return keys where this is active.
		LinkedHashSet<K> set = new LinkedHashSet<K>(getInnerMapData().size());
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
				.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>> e = i.next();
			K key = e.getKey();
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(key);
			if (entry != null && entry.isActive())
				set.add(key);
		}
		return Collections.unmodifiableSet(set);

	}

	@Override
	public int size() {
		// todo, implement a counter variable instead
		// only count active members in this node
		int counter = 0;
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> it = getInnerMapData()
				.entrySet().iterator();
		while (it != null && it.hasNext()) {
			Map.Entry<?, ?> e = it.next();
			if (e != null) {
				AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(e
						.getKey());
				if (entry != null && entry.isActive()
						&& entry.getValue() != null)
					counter++;
			}
		}
		return counter;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Collection<V> values() {
		ArrayList<V> values = new ArrayList<V>();
		Iterator<Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>>> i = getInnerMapData()
				.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<K, AbstractReplicatedMapMapEntry<K, V>> e = i.next();
			AbstractReplicatedMapMapEntry<K, V> entry = getInnerMapData().get(e.getKey());
			if (entry != null && entry.isActive() && entry.getValue() != null)
				values.add(entry.getValue());
		}
		return Collections.unmodifiableCollection(values);
	}

	public Channel getChannel() {
		return getChannelData();
	}

	public byte[] getMapContextName() {
		return getMapContextNameData();
	}

	public RpcChannel getRpcChannel() {
		return getRpcChannelData();
	}

	public long getRpcTimeout() {
		return getRpcTimeoutData();
	}

	public Object getStateMutex() {
		return getStateMutexData();
	}

	public boolean isStateTransferred() {
		return isStateTransferredData();
	}

	public AbstractReplicatedMapMapOwner getAbstractReplicateMapMapOwner() {
		return getMapOwnerData();
	}

	public ClassLoader[] getExternalLoaders() {
		return getExternalLoadersData();
	}

	public int getChannelSendOptions() {
		return getChannelSendOptionsData();
	}

	public long getAccessTimeout() {
		return getAccessTimeoutData();
	}

	public void setAbstractReplicateMapMapOwner(
			AbstractReplicatedMapMapOwner mapOwner) {
		this.setMapOwnerData(mapOwner);
	}

	public void setExternalLoaders(ClassLoader[] externalLoaders) {
		this.setExternalLoadersData(externalLoaders);
	}

	public void setChannelSendOptions(int channelSendOptions) {
		this.setChannelSendOptionsData(channelSendOptions);
	}

	public void setAccessTimeout(long accessTimeout) {
		this.setAccessTimeoutData(accessTimeout);
	}

	public static int getDefaultInitialCapacity() {
		return DEFAULT_INITIAL_CAPACITY;
	}

	public static float getDefaultLoadFactor() {
		return DEFAULT_LOAD_FACTOR;
	}

	public AbstractReplicatedMapMapOwner getMapOwner() {
		return getMapOwnerData();
	}

	public void setMapOwner(AbstractReplicatedMapMapOwner mapOwner) {
		this.setMapOwnerData(mapOwner);
	}

	public int getCurrentNode() {
		return getCurrentNodeData();
	}

	public void setCurrentNode(int currentNode) {
		this.setCurrentNodeData(currentNode);
	}

	public String getMapname() {
		return getMapnameData();
	}

	public void setMapname(String mapname) {
		this.setMapnameData(mapname);
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Log getLog() {
		return log;
	}

	public static Charset getCharsetIso88591() {
		return CHARSET_ISO_8859_1;
	}

	public ConcurrentHashMap<K, AbstractReplicatedMapMapEntry<K, V>> getInnerMap() {
		return getInnerMapData();
	}

	public void setRpcTimeout(long rpcTimeout) {
		this.setRpcTimeoutData(rpcTimeout);
	}

	public void setChannel(Channel channel) {
		this.setChannelData(channel);
	}

	public void setRpcChannel(RpcChannel rpcChannel) {
		this.setRpcChannelData(rpcChannel);
	}

	public void setMapContextName(byte[] mapContextName) {
		this.setMapContextNameData(mapContextName);
	}

	public void setStateTransferred(boolean stateTransferred) {
		this.setStateTransferredData(stateTransferred);
	}

	public ConcurrentHashMap<K, AbstractReplicatedMapMapEntry<K, V>> getInnerMapData() {
		return innerMap;
	}

	public long getRpcTimeoutData() {
		return rpcTimeout;
	}

	public void setRpcTimeoutData(long rpcTimeout) {
		this.rpcTimeout = rpcTimeout;
	}

	public Channel getChannelData() {
		return channel;
	}

	public void setChannelData(Channel channel) {
		this.channel = channel;
	}

	public RpcChannel getRpcChannelData() {
		return rpcChannel;
	}

	public void setRpcChannelData(RpcChannel rpcChannel) {
		this.rpcChannel = rpcChannel;
	}

	public byte[] getMapContextNameData() {
		return mapContextName;
	}

	public void setMapContextNameData(byte[] mapContextName) {
		this.mapContextName = mapContextName;
	}

	public boolean isStateTransferredData() {
		return stateTransferred;
	}

	public void setStateTransferredData(boolean stateTransferred) {
		this.stateTransferred = stateTransferred;
	}

	public Object getStateMutexData() {
		return stateMutex;
	}

	public HashMap<Member, Long> getMapMembersData() {
		return mapMembers;
	}

	public int getChannelSendOptionsData() {
		return channelSendOptions;
	}

	public void setChannelSendOptionsData(int channelSendOptions) {
		this.channelSendOptions = channelSendOptions;
	}

	public AbstractReplicatedMapMapOwner getMapOwnerData() {
		return mapOwner;
	}

	public void setMapOwnerData(AbstractReplicatedMapMapOwner mapOwner) {
		this.mapOwner = mapOwner;
	}

	public ClassLoader[] getExternalLoadersData() {
		return externalLoaders;
	}

	public void setExternalLoadersData(ClassLoader[] externalLoaders) {
		this.externalLoaders = externalLoaders;
	}

	public int getCurrentNodeData() {
		return currentNode;
	}

	public void setCurrentNodeData(int currentNode) {
		this.currentNode = currentNode;
	}

	public long getAccessTimeoutData() {
		return accessTimeout;
	}

	public void setAccessTimeoutData(long accessTimeout) {
		this.accessTimeout = accessTimeout;
	}

	public String getMapnameData() {
		return mapname;
	}

	public void setMapnameData(String mapname) {
		this.mapname = mapname;
	}

}