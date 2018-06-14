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

package org.apache.catalina.ha.session;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session2;
import org.apache.catalina.ha.ClusterManager;
import org.apache.catalina.ha.ClusterMessage;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.io.ReplicationStream;
import org.apache.tomcat.util.ExceptionUtils2;
import org.apache.tomcat.util.res.StringManager3;
import org.apache.juli.logging.LogFactory;
import org.apache.juli.logging.Log;

/**
 * The DeltaManager manages replicated sessions by only replicating the deltas
 * in data. For applications written to handle this, the DeltaManager is the
 * optimal way of replicating data.
 * 
 * This code is almost identical to StandardManager with a difference in how it
 * persists sessions and some modifications to it.
 * 
 * <b>IMPLEMENTATION NOTE </b>: Correct behavior of session storing and
 * reloading depends upon external calls to the <code>start()</code> and
 * <code>stop()</code> methods of this class at the correct times.
 * 
 * @author Filip Hanik
 * @author Craig R. McClanahan
 * @author Jean-Francois Arcand
 * @author Peter Rossbach
 */
public class DeltaManager extends ClusterManagerBase {

	// ---------------------------------------------------- Security Classes
	private final Log log = LogFactory.getLog(DeltaManager.class);

	/**
	 * The string manager for this package.
	 */
	private static final StringManager3 sm = StringManager3.getManager(Constants7
			.getPackage());

	// ----------------------------------------------------- Instance Variables

	/**
	 * The descriptive information about this implementation.
	 */
	private static final String info = "DeltaManager/2.1";

	/**
	 * The descriptive name of this Manager implementation (for logging).
	 */
	private static String managerName = "DeltaManager";
	private String name = null;

	private boolean expireSessionsOnShutdown = false;
	private boolean notifySessionListenersOnReplication = true;
	private boolean notifyContainerListenersOnReplication = true;
	private volatile boolean stateTransfered = false;
	private volatile boolean noContextManagerReceived = false;
	private int stateTransferTimeout = 60;
	private boolean sendAllSessions = true;
	private int sendAllSessionsSize = 1000;

	/**
	 * wait time between send session block (default 2 sec)
	 */
	private int sendAllSessionsWaitTime = 2 * 1000;
	private ArrayList<SessionMessage> receivedMessageQueue = new ArrayList<SessionMessage>();
	private boolean receiverQueue = false;
	private boolean stateTimestampDrop = true;
	private long stateTransferCreateSendTime;

	// ------------------------------------------------------------------ stats
	// attributes

	private long sessionReplaceCounter = 0;
	private long counterReceive_EVT_GET_ALL_SESSIONS = 0;
	private long counterReceive_EVT_ALL_SESSION_DATA = 0;
	private long counterReceive_EVT_SESSION_CREATED = 0;
	private long counterReceive_EVT_SESSION_EXPIRED = 0;
	private long counterReceive_EVT_SESSION_ACCESSED = 0;
	private long counterReceive_EVT_SESSION_DELTA = 0;
	private int counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE = 0;
	private long counterReceive_EVT_CHANGE_SESSION_ID = 0;
	private long counterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER = 0;
	private long counterSend_EVT_GET_ALL_SESSIONS = 0;
	private long counterSend_EVT_ALL_SESSION_DATA = 0;
	private long counterSend_EVT_SESSION_CREATED = 0;
	private long counterSend_EVT_SESSION_DELTA = 0;
	private long counterSend_EVT_SESSION_ACCESSED = 0;
	private long counterSend_EVT_SESSION_EXPIRED = 0;
	private int counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE = 0;
	private long counterSend_EVT_CHANGE_SESSION_ID = 0;
	private int counterNoStateTransfered = 0;

	// ------------------------------------------------------------- Constructor
	public DeltaManager() {
		super();
	}

	// ------------------------------------------------------------- Properties

	/**
	 * Return descriptive information about this Manager implementation and the
	 * corresponding version number, in the format
	 * <code>&lt;description&gt;/&lt;version&gt;</code>.
	 */
	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public void setName(String name) {
		this.setNameData(name);
	}

	/**
	 * Return the descriptive short name of this Manager implementation.
	 */
	@Override
	public String getName() {
		return getNameData();
	}

	/**
	 * @return Returns the counterSend_EVT_GET_ALL_SESSIONS.
	 */
	public long getCounterSend_EVT_GET_ALL_SESSIONS() {
		return getCounterSend_EVT_GET_ALL_SESSIONSData();
	}

	/**
	 * @return Returns the counterSend_EVT_SESSION_ACCESSED.
	 */
	public long getCounterSend_EVT_SESSION_ACCESSED() {
		return getCounterSend_EVT_SESSION_ACCESSEDData();
	}

	/**
	 * @return Returns the counterSend_EVT_SESSION_CREATED.
	 */
	public long getCounterSend_EVT_SESSION_CREATED() {
		return getCounterSend_EVT_SESSION_CREATEDData();
	}

	/**
	 * @return Returns the counterSend_EVT_SESSION_DELTA.
	 */
	public long getCounterSend_EVT_SESSION_DELTA() {
		return getCounterSend_EVT_SESSION_DELTAData();
	}

	/**
	 * @return Returns the counterSend_EVT_SESSION_EXPIRED.
	 */
	public long getCounterSend_EVT_SESSION_EXPIRED() {
		return getCounterSend_EVT_SESSION_EXPIREDData();
	}

	/**
	 * @return Returns the counterSend_EVT_ALL_SESSION_DATA.
	 */
	public long getCounterSend_EVT_ALL_SESSION_DATA() {
		return getCounterSend_EVT_ALL_SESSION_DATAData();
	}

	/**
	 * @return Returns the counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE.
	 */
	public int getCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE() {
		return getCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETEData();
	}

	/**
	 * @return Returns the counterSend_EVT_CHANGE_SESSION_ID.
	 */
	public long getCounterSend_EVT_CHANGE_SESSION_ID() {
		return getCounterSend_EVT_CHANGE_SESSION_IDData();
	}

	/**
	 * @return Returns the counterReceive_EVT_ALL_SESSION_DATA.
	 */
	public long getCounterReceive_EVT_ALL_SESSION_DATA() {
		return getCounterReceive_EVT_ALL_SESSION_DATAData();
	}

	/**
	 * @return Returns the counterReceive_EVT_GET_ALL_SESSIONS.
	 */
	public long getCounterReceive_EVT_GET_ALL_SESSIONS() {
		return getCounterReceive_EVT_GET_ALL_SESSIONSData();
	}

	/**
	 * @return Returns the counterReceive_EVT_SESSION_ACCESSED.
	 */
	public long getCounterReceive_EVT_SESSION_ACCESSED() {
		return getCounterReceive_EVT_SESSION_ACCESSEDData();
	}

	/**
	 * @return Returns the counterReceive_EVT_SESSION_CREATED.
	 */
	public long getCounterReceive_EVT_SESSION_CREATED() {
		return getCounterReceive_EVT_SESSION_CREATEDData();
	}

	/**
	 * @return Returns the counterReceive_EVT_SESSION_DELTA.
	 */
	public long getCounterReceive_EVT_SESSION_DELTA() {
		return getCounterReceive_EVT_SESSION_DELTAData();
	}

	/**
	 * @return Returns the counterReceive_EVT_SESSION_EXPIRED.
	 */
	public long getCounterReceive_EVT_SESSION_EXPIRED() {
		return getCounterReceive_EVT_SESSION_EXPIREDData();
	}

	/**
	 * @return Returns the counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE.
	 */
	public int getCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE() {
		return getCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETEData();
	}

	/**
	 * @return Returns the counterReceive_EVT_CHANGE_SESSION_ID.
	 */
	public long getCounterReceive_EVT_CHANGE_SESSION_ID() {
		return getCounterReceive_EVT_CHANGE_SESSION_IDData();
	}

	/**
	 * @return Returns the counterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER.
	 */
	public long getCounterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER() {
		return getCounterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGERData();
	}

	/**
	 * @return Returns the processingTime.
	 */
	@Override
	public long getProcessingTime() {
		return super.getProcessingTime();
	}

	/**
	 * @return Returns the sessionReplaceCounter.
	 */
	public long getSessionReplaceCounter() {
		return getSessionReplaceCounterData();
	}

	/**
	 * @return Returns the counterNoStateTransfered.
	 */
	public int getCounterNoStateTransfered() {
		return getCounterNoStateTransferedData();
	}

	public int getReceivedQueueSize() {
		return getReceivedMessageQueueData().size();
	}

	/**
	 * @return Returns the stateTransferTimeout.
	 */
	public int getStateTransferTimeout() {
		return getStateTransferTimeoutData();
	}

	/**
	 * @param timeoutAllSession
	 *            The timeout
	 */
	public void setStateTransferTimeout(int timeoutAllSession) {
		this.setStateTransferTimeoutData(timeoutAllSession);
	}

	/**
	 * is session state transfered complete?
	 * 
	 */
	public boolean getStateTransfered() {
		return isStateTransferedData();
	}

	/**
	 * set that state ist complete transfered
	 * 
	 * @param stateTransfered
	 */
	public void setStateTransfered(boolean stateTransfered) {
		this.setStateTransferedData(stateTransfered);
	}

	public boolean isNoContextManagerReceived() {
		return isNoContextManagerReceivedData();
	}

	public void setNoContextManagerReceived(boolean noContextManagerReceived) {
		this.setNoContextManagerReceivedData(noContextManagerReceived);
	}

	/**
	 * @return Returns the sendAllSessionsWaitTime in msec
	 */
	public int getSendAllSessionsWaitTime() {
		return getSendAllSessionsWaitTimeData();
	}

	/**
	 * @param sendAllSessionsWaitTime
	 *            The sendAllSessionsWaitTime to set at msec.
	 */
	public void setSendAllSessionsWaitTime(int sendAllSessionsWaitTime) {
		this.setSendAllSessionsWaitTimeData(sendAllSessionsWaitTime);
	}

	/**
	 * @return Returns the stateTimestampDrop.
	 */
	public boolean isStateTimestampDrop() {
		return isStateTimestampDropData();
	}

	/**
	 * @param isTimestampDrop
	 *            The new flag value
	 */
	public void setStateTimestampDrop(boolean isTimestampDrop) {
		this.setStateTimestampDropData(isTimestampDrop);
	}

	/**
	 * 
	 * @return Returns the sendAllSessions.
	 */
	public boolean isSendAllSessions() {
		return isSendAllSessionsData();
	}

	/**
	 * @param sendAllSessions
	 *            The sendAllSessions to set.
	 */
	public void setSendAllSessions(boolean sendAllSessions) {
		this.setSendAllSessionsData(sendAllSessions);
	}

	/**
	 * @return Returns the sendAllSessionsSize.
	 */
	public int getSendAllSessionsSize() {
		return getSendAllSessionsSizeData();
	}

	/**
	 * @param sendAllSessionsSize
	 *            The sendAllSessionsSize to set.
	 */
	public void setSendAllSessionsSize(int sendAllSessionsSize) {
		this.setSendAllSessionsSizeData(sendAllSessionsSize);
	}

	/**
	 * @return Returns the notifySessionListenersOnReplication.
	 */
	public boolean isNotifySessionListenersOnReplication() {
		return isNotifySessionListenersOnReplicationData();
	}

	/**
	 * @param notifyListenersCreateSessionOnReplication
	 *            The notifySessionListenersOnReplication to set.
	 */
	public void setNotifySessionListenersOnReplication(
			boolean notifyListenersCreateSessionOnReplication) {
		this.setNotifySessionListenersOnReplicationData(notifyListenersCreateSessionOnReplication);
	}

	public boolean isExpireSessionsOnShutdown() {
		return isExpireSessionsOnShutdownData();
	}

	public void setExpireSessionsOnShutdown(boolean expireSessionsOnShutdown) {
		this.setExpireSessionsOnShutdownData(expireSessionsOnShutdown);
	}

	public boolean isNotifyContainerListenersOnReplication() {
		return isNotifyContainerListenersOnReplicationData();
	}

	public void setNotifyContainerListenersOnReplication(
			boolean notifyContainerListenersOnReplication) {
		this.setNotifyContainerListenersOnReplicationData(notifyContainerListenersOnReplication);
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Construct and return a new session object, based on the default settings
	 * specified by this Manager's properties. The session id will be assigned
	 * by this method, and available via the getId() method of the returned
	 * session. If a new session cannot be created for any reason, return
	 * <code>null</code>.
	 * 
	 * @exception IllegalStateException
	 *                if a new session cannot be instantiated for any reason
	 * 
	 *                Construct and return a new session object, based on the
	 *                default settings specified by this Manager's properties.
	 *                The session id will be assigned by this method, and
	 *                available via the getId() method of the returned session.
	 *                If a new session cannot be created for any reason, return
	 *                <code>null</code>.
	 * 
	 * @exception IllegalStateException
	 *                if a new session cannot be instantiated for any reason
	 */
	@Override
	public Session2 createSession(String sessionId) {
		return createSession(sessionId, true);
	}

	/**
	 * create new session with check maxActiveSessions and send session creation
	 * to other cluster nodes.
	 * 
	 * @param distribute
	 * @return The session
	 */
	public Session2 createSession(String sessionId, boolean distribute) {
		DeltaSession session = (DeltaSession) super.createSession(sessionId);
		if (distribute) {
			sendCreateSession(session.getId(), session);
		}
		if (log.isDebugEnabled())
			log.debug(sm.getString("deltaManager.createSession.newSession",
					session.getId(), Integer.valueOf(getSessions().size())));
		return (session);
	}

	/**
	 * Send create session evt to all backup node
	 * 
	 * @param sessionId
	 * @param session
	 */
	protected void sendCreateSession(String sessionId, DeltaSession session) {
		if (getCluster().getMembers().length > 0) {
			SessionMessage msg = new SessionMessageImpl(getName(),
					SessionMessage.EVT_SESSION_CREATED, null, sessionId,
					sessionId + "-" + System.currentTimeMillis());
			if (log.isDebugEnabled())
				log.debug(sm.getString("deltaManager.sendMessage.newSession",
						getNameData(), sessionId));
			msg.setTimestamp(session.getCreationTime());
			setCounterSend_EVT_SESSION_CREATEDData(getCounterSend_EVT_SESSION_CREATEDData() + 1);
			send(msg);
		}
	}

	/**
	 * Send messages to other backup member (domain or all)
	 * 
	 * @param msg
	 *            Session message
	 */
	protected void send(SessionMessage msg) {
		if (getCluster() != null) {
			getCluster().send(msg);
		}
	}

	/**
	 * Create DeltaSession
	 * 
	 * @see org.apache.catalina.Manager#createEmptySession()
	 */
	@Override
	public Session2 createEmptySession() {
		return getNewDeltaSession();
	}

	/**
	 * Get new session class to be used in the doLoad() method.
	 */
	protected DeltaSession getNewDeltaSession() {
		return new DeltaSession(this);
	}

	/**
	 * Change the session ID of the current session to a new randomly generated
	 * session ID.
	 * 
	 * @param session
	 *            The session to change the session ID for
	 */
	@Override
	public void changeSessionId(Session2 session) {
		changeSessionId(session, true);
	}

	public void changeSessionId(Session2 session, boolean notify) {
		// original sessionID
		String orgSessionID = session.getId();
		super.changeSessionId(session);
		if (notify && getCluster().getMembers().length > 0) {
			// changed sessionID
			String newSessionID = session.getId();
			try {
				// serialize sessionID
				byte[] data = serializeSessionId(newSessionID);
				// notify change sessionID
				SessionMessage msg = new SessionMessageImpl(getName(),
						SessionMessage.EVT_CHANGE_SESSION_ID, data,
						orgSessionID, orgSessionID + "-"
								+ System.currentTimeMillis());
				msg.setTimestamp(System.currentTimeMillis());
				setCounterSend_EVT_CHANGE_SESSION_IDData(getCounterSend_EVT_CHANGE_SESSION_IDData() + 1);
				send(msg);
			} catch (IOException e) {
				log.error(sm.getString("deltaManager.unableSerializeSessionID",
						newSessionID), e);
			}
		}
	}

	/**
	 * serialize sessionID
	 * 
	 * @throws IOException
	 *             if an input/output error occurs
	 */
	protected byte[] serializeSessionId(String sessionId) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeUTF(sessionId);
		oos.flush();
		oos.close();
		return bos.toByteArray();
	}

	/**
	 * Load sessionID
	 * 
	 * @throws IOException
	 *             if an input/output error occurs
	 */
	protected String deserializeSessionId(byte[] data) throws IOException {
		ReplicationStream ois = getReplicationStream(data);
		String sessionId = ois.readUTF();
		ois.close();
		return sessionId;
	}

	/**
	 * Load Deltarequest from external node Load the Class at container
	 * classloader
	 * 
	 * @see DeltaRequest#readExternal(java.io.ObjectInput)
	 * @param session
	 * @param data
	 *            message data
	 * @return The request
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected DeltaRequest deserializeDeltaRequest(DeltaSession session,
			byte[] data) throws ClassNotFoundException, IOException {
		try {
			session.lock();
			ReplicationStream ois = getReplicationStream(data);
			session.getDeltaRequest().readExternal(ois);
			ois.close();
			return session.getDeltaRequest();
		} finally {
			session.unlock();
		}
	}

	/**
	 * serialize DeltaRequest
	 * 
	 * @see DeltaRequest#writeExternal(java.io.ObjectOutput)
	 * 
	 * @param deltaRequest
	 * @return serialized delta request
	 * @throws IOException
	 */
	protected byte[] serializeDeltaRequest(DeltaSession session,
			DeltaRequest deltaRequest) throws IOException {
		try {
			session.lock();
			return deltaRequest.serialize();
		} finally {
			session.unlock();
		}
	}

	/**
	 * Load sessions from other cluster node. FIXME replace currently sessions
	 * with same id without notification. FIXME SSO handling is not really
	 * correct with the session replacement!
	 * 
	 * @exception ClassNotFoundException
	 *                if a serialized class cannot be found during the reload
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void deserializeSessions(byte[] data)
			throws ClassNotFoundException, IOException {

		// Initialize our internal data structures
		// sessions.clear(); //should not do this
		// Open an input stream to the specified pathname, if any
		ClassLoader originalLoader = Thread.currentThread()
				.getContextClassLoader();
		ObjectInputStream ois = null;
		// Load the previously unloaded active sessions
		try {
			ois = getReplicationStream(data);
			Integer count = (Integer) ois.readObject();
			int n = count.intValue();
			for (int i = 0; i < n; i++) {
				DeltaSession session = (DeltaSession) createEmptySession();
				session.readObjectData(ois);
				session.setManager(this);
				session.setValid(true);
				session.setPrimarySession(false);
				// in case the nodes in the cluster are out of
				// time synch, this will make sure that we have the
				// correct timestamp, isValid returns true, cause
				// accessCount=1
				session.access();
				// make sure that the session gets ready to expire if
				// needed
				session.setAccessCount(0);
				session.resetDeltaRequest();
				// FIXME How inform other session id cache like SingleSignOn
				// increment sessionCounter to correct stats report
				if (findSession(session.getIdInternal()) == null) {
					setSessionCounter(getSessionCounter() + 1);
				} else {
					setSessionReplaceCounterData(getSessionReplaceCounterData() + 1);
					// FIXME better is to grap this sessions again !
					if (log.isWarnEnabled())
						log.warn(sm.getString(
								"deltaManager.loading.existing.session",
								session.getIdInternal()));
				}
				add(session);
				if (isNotifySessionListenersOnReplicationData()) {
					session.tellNew();
				}
			}
		} catch (ClassNotFoundException e) {
			log.error(sm.getString("deltaManager.loading.cnfe", e), e);
			throw e;
		} catch (IOException e) {
			log.error(sm.getString("deltaManager.loading.ioe", e), e);
			throw e;
		} finally {
			// Close the input stream
			try {
				if (ois != null)
					ois.close();
			} catch (IOException f) {
				// ignored
			}
			ois = null;
			if (originalLoader != null)
				Thread.currentThread().setContextClassLoader(originalLoader);
		}

	}

	/**
	 * Save any currently active sessions in the appropriate persistence
	 * mechanism, if any. If persistence is not supported, this method returns
	 * without doing anything.
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected byte[] serializeSessions(Session2[] currentSessions)
			throws IOException {

		// Open an output stream to the specified pathname, if any
		ByteArrayOutputStream fos = null;
		ObjectOutputStream oos = null;

		try {
			fos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(new BufferedOutputStream(fos));
			oos.writeObject(Integer.valueOf(currentSessions.length));
			for (int i = 0; i < currentSessions.length; i++) {
				((DeltaSession) currentSessions[i]).writeObjectData(oos);
			}
			// Flush and close the output stream
			oos.flush();
		} catch (IOException e) {
			log.error(sm.getString("deltaManager.unloading.ioe", e), e);
			throw e;
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException f) {
					// Ignore
				}
				oos = null;
			}
		}
		// send object data as byte[]
		return fos.toByteArray();
	}

	/**
	 * Start this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void startInternal() throws LifecycleException {

		super.startInternal();

		// Load unloaded sessions, if any
		try {
			if (getCluster() == null) {
				log.error(sm.getString("deltaManager.noCluster", getName()));
				return;
			} else {
				if (log.isInfoEnabled()) {
					String type = "unknown";
					if (getCluster().getContainer() instanceof Host) {
						type = "Host";
					} else if (getCluster().getContainer() instanceof Engine) {
						type = "Engine";
					}
					log.info(sm.getString("deltaManager.registerCluster",
							getName(), type, getCluster().getClusterName()));
				}
			}
			if (log.isInfoEnabled())
				log.info(sm
						.getString("deltaManager.startClustering", getName()));

			getAllClusterSessions();

		} catch (Throwable t) {
			ExceptionUtils2.handleThrowable(t);
			log.error(sm.getString("deltaManager.managerLoad"), t);
		}

		setState(LifecycleState.STARTING);
	}

	/**
	 * get from first session master the backup from all clustered sessions
	 * 
	 * @see #findSessionMasterMember()
	 */
	public synchronized void getAllClusterSessions() {
		if (getCluster() != null && getCluster().getMembers().length > 0) {
			long beforeSendTime = System.currentTimeMillis();
			Member mbr = findSessionMasterMember();
			if (mbr == null) { // No domain member found
				return;
			}
			SessionMessage msg = new SessionMessageImpl(this.getName(),
					SessionMessage.EVT_GET_ALL_SESSIONS, null, "GET-ALL",
					"GET-ALL-" + getName());
			msg.setTimestamp(beforeSendTime);
			// set reference time
			setStateTransferCreateSendTimeData(beforeSendTime);
			// request session state
			setCounterSend_EVT_GET_ALL_SESSIONSData(getCounterSend_EVT_GET_ALL_SESSIONSData() + 1);
			setStateTransferedData(false);
			// FIXME This send call block the deploy thread, when sender
			// waitForAck is enabled
			try {
				synchronized (getReceivedMessageQueueData()) {
					setReceiverQueueData(true);
				}
				getCluster().send(msg, mbr);
				if (log.isInfoEnabled())
					log.info(sm.getString("deltaManager.waitForSessionState",
							getName(), mbr,
							Integer.valueOf(getStateTransferTimeout())));
				// FIXME At sender ack mode this method check only the state
				// transfer and resend is a problem!
				waitForSendAllSessions(beforeSendTime);
			} finally {
				synchronized (getReceivedMessageQueueData()) {
					for (Iterator<SessionMessage> iter = getReceivedMessageQueueData()
							.iterator(); iter.hasNext();) {
						SessionMessage smsg = iter.next();
						if (!isStateTimestampDropData()) {
							messageReceived(
									smsg,
									smsg.getAddress() != null ? (Member) smsg
											.getAddress() : null);
						} else {
							if (smsg.getEventType() != SessionMessage.EVT_GET_ALL_SESSIONS
									&& smsg.getTimestamp() >= getStateTransferCreateSendTimeData()) {
								// FIXME handle EVT_GET_ALL_SESSIONS later
								messageReceived(
										smsg,
										smsg.getAddress() != null ? (Member) smsg
												.getAddress() : null);
							} else {
								if (log.isWarnEnabled()) {
									log.warn(sm.getString(
											"deltaManager.dropMessage",
											getName(),
											smsg.getEventTypeString(),
											new Date(
													getStateTransferCreateSendTimeData()),
											new Date(smsg.getTimestamp())));
								}
							}
						}
					}
					getReceivedMessageQueueData().clear();
					setReceiverQueueData(false);
				}
			}
		} else {
			if (log.isInfoEnabled())
				log.info(sm.getString("deltaManager.noMembers", getName()));
		}
	}

	/**
	 * Find the master of the session state
	 * 
	 * @return master member of sessions
	 */
	protected Member findSessionMasterMember() {
		Member mbr = null;
		Member mbrs[] = getCluster().getMembers();
		if (mbrs.length != 0)
			mbr = mbrs[0];
		if (mbr == null && log.isWarnEnabled())
			log.warn(sm.getString("deltaManager.noMasterMember", getName(), ""));
		if (mbr != null && log.isDebugEnabled())
			log.warn(sm.getString("deltaManager.foundMasterMember", getName(),
					mbr));
		return mbr;
	}

	/**
	 * Wait that cluster session state is transfer or timeout after 60 Sec With
	 * stateTransferTimeout == -1 wait that backup is transfered (forever mode)
	 */
	protected void waitForSendAllSessions(long beforeSendTime) {
		long reqStart = System.currentTimeMillis();
		long reqNow = reqStart;
		boolean isTimeout = false;
		if (getStateTransferTimeout() > 0) {
			// wait that state is transfered with timeout check
			do {
				try {
					Thread.sleep(100);
				} catch (Exception sleep) {
					//
				}
				reqNow = System.currentTimeMillis();
				isTimeout = ((reqNow - reqStart) > (1000L * getStateTransferTimeout()));
			} while ((!getStateTransfered()) && (!isTimeout)
					&& (!isNoContextManagerReceived()));
		} else {
			if (getStateTransferTimeout() == -1) {
				// wait that state is transfered
				do {
					try {
						Thread.sleep(100);
					} catch (Exception sleep) {
					}
				} while ((!getStateTransfered())
						&& (!isNoContextManagerReceived()));
				reqNow = System.currentTimeMillis();
			}
		}
		if (isTimeout) {
			setCounterNoStateTransferedData(getCounterNoStateTransferedData() + 1);
			log.error(sm.getString("deltaManager.noSessionState", getName(),
					new Date(beforeSendTime),
					Long.valueOf(reqNow - beforeSendTime)));
		} else if (isNoContextManagerReceived()) {
			if (log.isWarnEnabled())
				log.warn(sm.getString("deltaManager.noContextManager",
						getName(), new Date(beforeSendTime),
						Long.valueOf(reqNow - beforeSendTime)));
		} else {
			if (log.isInfoEnabled())
				log.info(sm.getString("deltaManager.sessionReceived",
						getName(), new Date(beforeSendTime),
						Long.valueOf(reqNow - beforeSendTime)));
		}
	}

	/**
	 * Stop this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void stopInternal() throws LifecycleException {

		if (log.isDebugEnabled())
			log.debug(sm.getString("deltaManager.stopped", getName()));

		setState(LifecycleState.STOPPING);

		// Expire all active sessions
		if (log.isInfoEnabled())
			log.info(sm.getString("deltaManager.expireSessions", getName()));
		Session2 sessions[] = findSessions();
		for (int i = 0; i < sessions.length; i++) {
			DeltaSession session = (DeltaSession) sessions[i];
			if (!session.isValid())
				continue;
			try {
				session.expire(true, isExpireSessionsOnShutdown());
			} catch (Throwable t) {
				ExceptionUtils2.handleThrowable(t);
			}
		}

		// Require a new random number generator if we are restarted
		super.stopInternal();
	}

	// -------------------------------------------------------- Replication
	// Methods

	/**
	 * A message was received from another node, this is the callback method to
	 * implement if you are interested in receiving replication messages.
	 * 
	 * @param cmsg
	 *            - the message received.
	 */
	@Override
	public void messageDataReceived(ClusterMessage cmsg) {
		if (cmsg != null && cmsg instanceof SessionMessage) {
			SessionMessage msg = (SessionMessage) cmsg;
			switch (msg.getEventType()) {
			case SessionMessage.EVT_GET_ALL_SESSIONS:
			case SessionMessage.EVT_SESSION_CREATED:
			case SessionMessage.EVT_SESSION_EXPIRED:
			case SessionMessage.EVT_SESSION_ACCESSED:
			case SessionMessage.EVT_SESSION_DELTA:
			case SessionMessage.EVT_CHANGE_SESSION_ID: {
				synchronized (getReceivedMessageQueueData()) {
					if (isReceiverQueueData()) {
						getReceivedMessageQueueData().add(msg);
						return;
					}
				}
				break;
			}
			default: {
				// we didn't queue, do nothing
				break;
			}
			}

			messageReceived(msg,
					msg.getAddress() != null ? (Member) msg.getAddress() : null);
		}
	}

	/**
	 * When the request has been completed, the replication valve will notify
	 * the manager, and the manager will decide whether any replication is
	 * needed or not. If there is a need for replication, the manager will
	 * create a session message and that will be replicated. The cluster
	 * determines where it gets sent.
	 * 
	 * @param sessionId
	 *            - the sessionId that just completed.
	 * @return a SessionMessage to be sent,
	 */
	@Override
	public ClusterMessage requestCompleted(String sessionId) {
		return requestCompleted(sessionId, false);
	}

	/**
	 * When the request has been completed, the replication valve will notify
	 * the manager, and the manager will decide whether any replication is
	 * needed or not. If there is a need for replication, the manager will
	 * create a session message and that will be replicated. The cluster
	 * determines where it gets sent.
	 * 
	 * Session expiration also calls this method, but with expires == true.
	 * 
	 * @param sessionId
	 *            - the sessionId that just completed.
	 * @param expires
	 *            - whether this method has been called during session
	 *            expiration
	 * @return a SessionMessage to be sent,
	 */
	public ClusterMessage requestCompleted(String sessionId, boolean expires) {
		DeltaSession session = null;
		try {
			session = (DeltaSession) findSession(sessionId);
			if (session == null) {
				// A parallel request has called session.invalidate() which has
				// removed the session from the Manager.
				return null;
			}
			DeltaRequest deltaRequest = session.getDeltaRequest();
			session.lock();
			SessionMessage msg = null;
			boolean isDeltaRequest = false;
			synchronized (deltaRequest) {
				isDeltaRequest = deltaRequest.getSize() > 0;
				if (isDeltaRequest) {
					setCounterSend_EVT_SESSION_DELTAData(getCounterSend_EVT_SESSION_DELTAData() + 1);
					byte[] data = serializeDeltaRequest(session, deltaRequest);
					msg = new SessionMessageImpl(getName(),
							SessionMessage.EVT_SESSION_DELTA, data, sessionId,
							sessionId + "-" + System.currentTimeMillis());
					session.resetDeltaRequest();
				}
			}
			if (!isDeltaRequest) {
				if (!expires && !session.isPrimarySession()) {
					setCounterSend_EVT_SESSION_ACCESSEDData(getCounterSend_EVT_SESSION_ACCESSEDData() + 1);
					msg = new SessionMessageImpl(getName(),
							SessionMessage.EVT_SESSION_ACCESSED, null,
							sessionId, sessionId + "-"
									+ System.currentTimeMillis());
					if (log.isDebugEnabled()) {
						log.debug(sm
								.getString(
										"deltaManager.createMessage.accessChangePrimary",
										getName(), sessionId));
					}
				}
			} else { // log only outside synch block!
				if (log.isDebugEnabled()) {
					log.debug(sm.getString("deltaManager.createMessage.delta",
							getName(), sessionId));
				}
			}
			if (!expires)
				session.setPrimarySession(true);
			// check to see if we need to send out an access message
			if (!expires && (msg == null)) {
				long replDelta = System.currentTimeMillis()
						- session.getLastTimeReplicated();
				if (session.getMaxInactiveInterval() >= 0
						&& replDelta > (session.getMaxInactiveInterval() * 1000L)) {
					setCounterSend_EVT_SESSION_ACCESSEDData(getCounterSend_EVT_SESSION_ACCESSEDData() + 1);
					msg = new SessionMessageImpl(getName(),
							SessionMessage.EVT_SESSION_ACCESSED, null,
							sessionId, sessionId + "-"
									+ System.currentTimeMillis());
					if (log.isDebugEnabled()) {
						log.debug(sm.getString(
								"deltaManager.createMessage.access", getName(),
								sessionId));
					}
				}

			}

			// update last replicated time
			if (msg != null) {
				session.setLastTimeReplicated(System.currentTimeMillis());
				msg.setTimestamp(session.getLastTimeReplicated());
			}
			return msg;
		} catch (IOException x) {
			log.error(sm.getString(
					"deltaManager.createMessage.unableCreateDeltaRequest",
					sessionId), x);
			return null;
		} finally {
			if (session != null)
				session.unlock();
		}

	}

	/**
	 * Reset manager statistics
	 */
	public synchronized void resetStatistics() {
		setProcessingTime(0);
		getExpiredSessionsVariable().set(0);
		synchronized (getSessionCreationTiming()) {
			getSessionCreationTiming().clear();
			while (getSessionCreationTiming().size() < ManagerBase
					.getTimingStatsCacheSize()) {
				getSessionCreationTiming().add(null);
			}
		}
		synchronized (getSessionExpirationTiming()) {
			getSessionExpirationTiming().clear();
			while (getSessionExpirationTiming().size() < ManagerBase
					.getTimingStatsCacheSize()) {
				getSessionExpirationTiming().add(null);
			}
		}
		setRejectedSessions(0);
		setSessionReplaceCounterData(0);
		setCounterNoStateTransferedData(0);
		setMaxActive(getActiveSessions());
		setSessionCounter(getActiveSessions());
		setCounterReceive_EVT_ALL_SESSION_DATAData(0);
		setCounterReceive_EVT_GET_ALL_SESSIONSData(0);
		setCounterReceive_EVT_SESSION_ACCESSEDData(0);
		setCounterReceive_EVT_SESSION_CREATEDData(0);
		setCounterReceive_EVT_SESSION_DELTAData(0);
		setCounterReceive_EVT_SESSION_EXPIREDData(0);
		setCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETEData(0);
		setCounterReceive_EVT_CHANGE_SESSION_IDData(0);
		setCounterSend_EVT_ALL_SESSION_DATAData(0);
		setCounterSend_EVT_GET_ALL_SESSIONSData(0);
		setCounterSend_EVT_SESSION_ACCESSEDData(0);
		setCounterSend_EVT_SESSION_CREATEDData(0);
		setCounterSend_EVT_SESSION_DELTAData(0);
		setCounterSend_EVT_SESSION_EXPIREDData(0);
		setCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETEData(0);
		setCounterSend_EVT_CHANGE_SESSION_IDData(0);

	}

	// -------------------------------------------------------- expire

	/**
	 * send session expired to other cluster nodes
	 * 
	 * @param id
	 *            session id
	 */
	protected void sessionExpired(String id) {
		if (getCluster().getMembers().length > 0) {
			setCounterSend_EVT_SESSION_EXPIREDData(getCounterSend_EVT_SESSION_EXPIREDData() + 1);
			SessionMessage msg = new SessionMessageImpl(getName(),
					SessionMessage.EVT_SESSION_EXPIRED, null, id, id
							+ "-EXPIRED-MSG");
			msg.setTimestamp(System.currentTimeMillis());
			if (log.isDebugEnabled())
				log.debug(sm.getString("deltaManager.createMessage.expire",
						getName(), id));
			send(msg);
		}
	}

	/**
	 * Expire all find sessions.
	 */
	public void expireAllLocalSessions() {
		long timeNow = System.currentTimeMillis();
		Session2 sessions[] = findSessions();
		int expireDirect = 0;
		int expireIndirect = 0;

		if (log.isDebugEnabled())
			log.debug("Start expire all sessions " + getName() + " at "
					+ timeNow + " sessioncount " + sessions.length);
		for (int i = 0; i < sessions.length; i++) {
			if (sessions[i] instanceof DeltaSession) {
				DeltaSession session = (DeltaSession) sessions[i];
				if (session.isPrimarySession()) {
					if (session.isValid()) {
						session.expire();
						expireDirect++;
					} else {
						expireIndirect++;
					}// end if
				}// end if
			}// end if
		}// for
		long timeEnd = System.currentTimeMillis();
		if (log.isDebugEnabled())
			log.debug("End expire sessions " + getName()
					+ " expire processingTime " + (timeEnd - timeNow)
					+ " expired direct sessions: " + expireDirect
					+ " expired direct sessions: " + expireIndirect);

	}

	/**
	 * When the manager expires session not tied to a request. The cluster will
	 * periodically ask for a list of sessions that should expire and that
	 * should be sent across the wire.
	 * 
	 * @return The invalidated sessions array
	 */
	@Override
	public String[] getInvalidatedSessions() {
		return new String[0];
	}

	// -------------------------------------------------------- message receive

	/**
	 * Test that sender and local domain is the same
	 */
	protected boolean checkSenderDomain(SessionMessage msg, Member sender) {
		boolean sameDomain = true;
		if (!sameDomain && log.isWarnEnabled()) {
			log.warn(sm.getString(
					"deltaManager.receiveMessage.fromWrongDomain",
					new Object[] { getName(), msg.getEventTypeString(), sender,
							"", "" }));
		}
		return sameDomain;
	}

	/**
	 * This method is called by the received thread when a SessionMessage has
	 * been received from one of the other nodes in the getCluster().
	 * 
	 * @param msg
	 *            - the message received
	 * @param sender
	 *            - the sender of the message, this is used if we receive a
	 *            EVT_GET_ALL_SESSION message, so that we only reply to the
	 *            requesting node
	 */
	protected void messageReceived(SessionMessage msg, Member sender) {
		if (!checkSenderDomain(msg, sender)) {
			return;
		}
		ClassLoader contextLoader = Thread.currentThread()
				.getContextClassLoader();
		try {

			ClassLoader[] loaders = getClassLoaders();
			if (loaders != null && loaders.length > 0)
				Thread.currentThread().setContextClassLoader(loaders[0]);
			if (log.isDebugEnabled())
				log.debug(sm.getString("deltaManager.receiveMessage.eventType",
						getName(), msg.getEventTypeString(), sender));

			switch (msg.getEventType()) {
			case SessionMessage.EVT_GET_ALL_SESSIONS: {
				handleGET_ALL_SESSIONS(msg, sender);
				break;
			}
			case SessionMessage.EVT_ALL_SESSION_DATA: {
				handleALL_SESSION_DATA(msg, sender);
				break;
			}
			case SessionMessage.EVT_ALL_SESSION_TRANSFERCOMPLETE: {
				handleALL_SESSION_TRANSFERCOMPLETE(msg, sender);
				break;
			}
			case SessionMessage.EVT_SESSION_CREATED: {
				handleSESSION_CREATED(msg, sender);
				break;
			}
			case SessionMessage.EVT_SESSION_EXPIRED: {
				handleSESSION_EXPIRED(msg, sender);
				break;
			}
			case SessionMessage.EVT_SESSION_ACCESSED: {
				handleSESSION_ACCESSED(msg, sender);
				break;
			}
			case SessionMessage.EVT_SESSION_DELTA: {
				handleSESSION_DELTA(msg, sender);
				break;
			}
			case SessionMessage.EVT_CHANGE_SESSION_ID: {
				handleCHANGE_SESSION_ID(msg, sender);
				break;
			}
			case SessionMessage.EVT_ALL_SESSION_NOCONTEXTMANAGER: {
				handleALL_SESSION_NOCONTEXTMANAGER(msg, sender);
				break;
			}
			default: {
				// we didn't recognize the message type, do nothing
				break;
			}
			}
		} catch (Exception x) {
			log.error(sm.getString("deltaManager.receiveMessage.error",
					getName()), x);
		} finally {
			Thread.currentThread().setContextClassLoader(contextLoader);
		}
	}

	// -------------------------------------------------------- message receiver
	// handler

	/**
	 * handle receive session state is complete transfered
	 * 
	 * @param msg
	 * @param sender
	 */
	protected void handleALL_SESSION_TRANSFERCOMPLETE(SessionMessage msg,
			Member sender) {
		setCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETEData(getCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETEData() + 1);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.transfercomplete", getName(),
					sender.getHost(), Integer.valueOf(sender.getPort())));
		setStateTransferCreateSendTimeData(msg.getTimestamp());
		setStateTransferedData(true);
	}

	/**
	 * handle receive session delta
	 * 
	 * @param msg
	 * @param sender
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected void handleSESSION_DELTA(SessionMessage msg, Member sender)
			throws IOException, ClassNotFoundException {
		setCounterReceive_EVT_SESSION_DELTAData(getCounterReceive_EVT_SESSION_DELTAData() + 1);
		byte[] delta = msg.getSession();
		DeltaSession session = (DeltaSession) findSession(msg.getSessionID());
		if (session != null) {
			if (log.isDebugEnabled())
				log.debug(sm.getString("deltaManager.receiveMessage.delta",
						getName(), msg.getSessionID()));
			try {
				session.lock();
				DeltaRequest dreq = deserializeDeltaRequest(session, delta);
				dreq.execute(session, isNotifyListenersOnReplication());
				session.setPrimarySession(false);
			} finally {
				session.unlock();
			}
		}
	}

	/**
	 * handle receive session is access at other node ( primary session is now
	 * false)
	 * 
	 * @param msg
	 * @param sender
	 * @throws IOException
	 */
	protected void handleSESSION_ACCESSED(SessionMessage msg, Member sender)
			throws IOException {
		setCounterReceive_EVT_SESSION_ACCESSEDData(getCounterReceive_EVT_SESSION_ACCESSEDData() + 1);
		DeltaSession session = (DeltaSession) findSession(msg.getSessionID());
		if (session != null) {
			if (log.isDebugEnabled())
				log.debug(sm.getString("deltaManager.receiveMessage.accessed",
						getName(), msg.getSessionID()));
			session.access();
			session.setPrimarySession(false);
			session.endAccess();
		}
	}

	/**
	 * handle receive session is expire at other node ( expire session also
	 * here)
	 * 
	 * @param msg
	 * @param sender
	 * @throws IOException
	 */
	protected void handleSESSION_EXPIRED(SessionMessage msg, Member sender)
			throws IOException {
		setCounterReceive_EVT_SESSION_EXPIREDData(getCounterReceive_EVT_SESSION_EXPIREDData() + 1);
		DeltaSession session = (DeltaSession) findSession(msg.getSessionID());
		if (session != null) {
			if (log.isDebugEnabled())
				log.debug(sm.getString("deltaManager.receiveMessage.expired",
						getName(), msg.getSessionID()));
			session.expire(isNotifySessionListenersOnReplicationData(), false);
		}
	}

	/**
	 * handle receive new session is created at other node (create backup -
	 * primary false)
	 * 
	 * @param msg
	 * @param sender
	 */
	protected void handleSESSION_CREATED(SessionMessage msg, Member sender) {
		setCounterReceive_EVT_SESSION_CREATEDData(getCounterReceive_EVT_SESSION_CREATEDData() + 1);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.createNewSession", getName(),
					msg.getSessionID()));
		DeltaSession session = (DeltaSession) createEmptySession();
		session.setManager(this);
		session.setValid(true);
		session.setPrimarySession(false);
		session.setCreationTime(msg.getTimestamp());
		// use container maxInactiveInterval so that session will expire
		// correctly in case of primary transfer
		session.setMaxInactiveInterval(getMaxInactiveInterval(), false);
		session.access();
		session.setId(msg.getSessionID(), isNotifySessionListenersOnReplicationData());
		session.resetDeltaRequest();
		session.endAccess();

	}

	/**
	 * handle receive sessions from other not ( restart )
	 * 
	 * @param msg
	 * @param sender
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected void handleALL_SESSION_DATA(SessionMessage msg, Member sender)
			throws ClassNotFoundException, IOException {
		setCounterReceive_EVT_ALL_SESSION_DATAData(getCounterReceive_EVT_ALL_SESSION_DATAData() + 1);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.allSessionDataBegin",
					getName()));
		byte[] data = msg.getSession();
		deserializeSessions(data);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.allSessionDataAfter",
					getName()));
		// stateTransferred = true;
	}

	/**
	 * handle receive that other node want all sessions ( restart ) a) send all
	 * sessions with one message b) send session at blocks After sending send
	 * state is complete transfered
	 * 
	 * @param msg
	 * @param sender
	 * @throws IOException
	 */
	protected void handleGET_ALL_SESSIONS(SessionMessage msg, Member sender)
			throws IOException {
		setCounterReceive_EVT_GET_ALL_SESSIONSData(getCounterReceive_EVT_GET_ALL_SESSIONSData() + 1);
		// get a list of all the session from this manager
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.unloadingBegin", getName()));
		// Write the number of active sessions, followed by the details
		// get all sessions and serialize without sync
		Session2[] currentSessions = findSessions();
		long findSessionTimestamp = System.currentTimeMillis();
		if (isSendAllSessions()) {
			sendSessions(sender, currentSessions, findSessionTimestamp);
		} else {
			// send session at blocks
			int remain = currentSessions.length;
			for (int i = 0; i < currentSessions.length; i += getSendAllSessionsSize()) {
				int len = i + getSendAllSessionsSize() > currentSessions.length ? currentSessions.length
						- i
						: getSendAllSessionsSize();
				Session2[] sendSessions = new Session2[len];
				System.arraycopy(currentSessions, i, sendSessions, 0, len);
				sendSessions(sender, sendSessions, findSessionTimestamp);
				remain = remain - len;
				if (getSendAllSessionsWaitTime() > 0 && remain > 0) {
					try {
						Thread.sleep(getSendAllSessionsWaitTime());
					} catch (Exception sleep) {
					}
				}// end if
			}// for
		}// end if

		SessionMessage newmsg = new SessionMessageImpl(getNameData(),
				SessionMessage.EVT_ALL_SESSION_TRANSFERCOMPLETE, null,
				"SESSION-STATE-TRANSFERED", "SESSION-STATE-TRANSFERED"
						+ getName());
		newmsg.setTimestamp(findSessionTimestamp);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.createMessage.allSessionTransfered",
					getName()));
		setCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETEData(getCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETEData() + 1);
		getCluster().send(newmsg, sender);
	}

	/**
	 * handle receive change sessionID at other node
	 * 
	 * @param msg
	 * @param sender
	 * @throws IOException
	 */
	protected void handleCHANGE_SESSION_ID(SessionMessage msg, Member sender)
			throws IOException {
		setCounterReceive_EVT_CHANGE_SESSION_IDData(getCounterReceive_EVT_CHANGE_SESSION_IDData() + 1);
		DeltaSession session = (DeltaSession) findSession(msg.getSessionID());
		if (session != null) {
			String newSessionID = deserializeSessionId(msg.getSession());
			session.setPrimarySession(false);
			session.setId(newSessionID, false);
			if (isNotifyContainerListenersOnReplicationData()) {
				getContainer().fireContainerEvent(
						Context.CHANGE_SESSION_ID_EVENT,
						new String[] { msg.getSessionID(), newSessionID });
			}
		}
	}

	/**
	 * handle receive no context manager.
	 * 
	 * @param msg
	 * @param sender
	 */
	protected void handleALL_SESSION_NOCONTEXTMANAGER(SessionMessage msg,
			Member sender) {
		setCounterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGERData(getCounterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGERData() + 1);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.noContextManager", getName(),
					sender.getHost(), Integer.valueOf(sender.getPort())));
		setNoContextManagerReceivedData(true);
	}

	/**
	 * send a block of session to sender
	 * 
	 * @param sender
	 * @param currentSessions
	 * @param sendTimestamp
	 * @throws IOException
	 */
	protected void sendSessions(Member sender, Session2[] currentSessions,
			long sendTimestamp) throws IOException {
		byte[] data = serializeSessions(currentSessions);
		if (log.isDebugEnabled())
			log.debug(sm.getString(
					"deltaManager.receiveMessage.unloadingAfter", getName()));
		SessionMessage newmsg = new SessionMessageImpl(getNameData(),
				SessionMessage.EVT_ALL_SESSION_DATA, data, "SESSION-STATE",
				"SESSION-STATE-" + getName());
		newmsg.setTimestamp(sendTimestamp);
		if (log.isDebugEnabled())
			log.debug(sm.getString("deltaManager.createMessage.allSessionData",
					getName()));
		setCounterSend_EVT_ALL_SESSION_DATAData(getCounterSend_EVT_ALL_SESSION_DATAData() + 1);
		getCluster().send(newmsg, sender);
	}

	@Override
	public ClusterManager cloneFromTemplate() {
		DeltaManager result = new DeltaManager();
		clone(result);
		result.setExpireSessionsOnShutdownData(expireSessionsOnShutdown);
		result.setNotifySessionListenersOnReplicationData(notifySessionListenersOnReplication);
		result.setNotifyContainerListenersOnReplicationData(notifyContainerListenersOnReplication);
		result.setStateTransferTimeoutData(stateTransferTimeout);
		result.setSendAllSessionsData(sendAllSessions);
		result.setSendAllSessionsSizeData(sendAllSessionsSize);
		result.setSendAllSessionsWaitTimeData(sendAllSessionsWaitTime);
		result.setStateTimestampDropData(stateTimestampDrop);
		return result;
	}

	public static String getManagerNameData() {
		return managerName;
	}

	public static void setManagerNameData(String managerName) {
		DeltaManager.managerName = managerName;
	}

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}

	public boolean isExpireSessionsOnShutdownData() {
		return expireSessionsOnShutdown;
	}

	public void setExpireSessionsOnShutdownData(boolean expireSessionsOnShutdown) {
		this.expireSessionsOnShutdown = expireSessionsOnShutdown;
	}

	public boolean isNotifySessionListenersOnReplicationData() {
		return notifySessionListenersOnReplication;
	}

	public void setNotifySessionListenersOnReplicationData(
			boolean notifySessionListenersOnReplication) {
		this.notifySessionListenersOnReplication = notifySessionListenersOnReplication;
	}

	public boolean isNotifyContainerListenersOnReplicationData() {
		return notifyContainerListenersOnReplication;
	}

	public void setNotifyContainerListenersOnReplicationData(
			boolean notifyContainerListenersOnReplication) {
		this.notifyContainerListenersOnReplication = notifyContainerListenersOnReplication;
	}

	public boolean isStateTransferedData() {
		return stateTransfered;
	}

	public void setStateTransferedData(boolean stateTransfered) {
		this.stateTransfered = stateTransfered;
	}

	public boolean isNoContextManagerReceivedData() {
		return noContextManagerReceived;
	}

	public void setNoContextManagerReceivedData(boolean noContextManagerReceived) {
		this.noContextManagerReceived = noContextManagerReceived;
	}

	public int getStateTransferTimeoutData() {
		return stateTransferTimeout;
	}

	public void setStateTransferTimeoutData(int stateTransferTimeout) {
		this.stateTransferTimeout = stateTransferTimeout;
	}

	public boolean isSendAllSessionsData() {
		return sendAllSessions;
	}

	public void setSendAllSessionsData(boolean sendAllSessions) {
		this.sendAllSessions = sendAllSessions;
	}

	public int getSendAllSessionsSizeData() {
		return sendAllSessionsSize;
	}

	public void setSendAllSessionsSizeData(int sendAllSessionsSize) {
		this.sendAllSessionsSize = sendAllSessionsSize;
	}

	public int getSendAllSessionsWaitTimeData() {
		return sendAllSessionsWaitTime;
	}

	public void setSendAllSessionsWaitTimeData(int sendAllSessionsWaitTime) {
		this.sendAllSessionsWaitTime = sendAllSessionsWaitTime;
	}

	public ArrayList<SessionMessage> getReceivedMessageQueueData() {
		return receivedMessageQueue;
	}

	public void setReceivedMessageQueueData(ArrayList<SessionMessage> receivedMessageQueue) {
		this.receivedMessageQueue = receivedMessageQueue;
	}

	public boolean isReceiverQueueData() {
		return receiverQueue;
	}

	public void setReceiverQueueData(boolean receiverQueue) {
		this.receiverQueue = receiverQueue;
	}

	public boolean isStateTimestampDropData() {
		return stateTimestampDrop;
	}

	public void setStateTimestampDropData(boolean stateTimestampDrop) {
		this.stateTimestampDrop = stateTimestampDrop;
	}

	public long getStateTransferCreateSendTimeData() {
		return stateTransferCreateSendTime;
	}

	public void setStateTransferCreateSendTimeData(
			long stateTransferCreateSendTime) {
		this.stateTransferCreateSendTime = stateTransferCreateSendTime;
	}

	public long getSessionReplaceCounterData() {
		return sessionReplaceCounter;
	}

	public void setSessionReplaceCounterData(long sessionReplaceCounter) {
		this.sessionReplaceCounter = sessionReplaceCounter;
	}

	public long getCounterReceive_EVT_GET_ALL_SESSIONSData() {
		return counterReceive_EVT_GET_ALL_SESSIONS;
	}

	public void setCounterReceive_EVT_GET_ALL_SESSIONSData(
			long counterReceive_EVT_GET_ALL_SESSIONS) {
		this.counterReceive_EVT_GET_ALL_SESSIONS = counterReceive_EVT_GET_ALL_SESSIONS;
	}

	public long getCounterReceive_EVT_ALL_SESSION_DATAData() {
		return counterReceive_EVT_ALL_SESSION_DATA;
	}

	public void setCounterReceive_EVT_ALL_SESSION_DATAData(
			long counterReceive_EVT_ALL_SESSION_DATA) {
		this.counterReceive_EVT_ALL_SESSION_DATA = counterReceive_EVT_ALL_SESSION_DATA;
	}

	public long getCounterReceive_EVT_SESSION_CREATEDData() {
		return counterReceive_EVT_SESSION_CREATED;
	}

	public void setCounterReceive_EVT_SESSION_CREATEDData(
			long counterReceive_EVT_SESSION_CREATED) {
		this.counterReceive_EVT_SESSION_CREATED = counterReceive_EVT_SESSION_CREATED;
	}

	public long getCounterReceive_EVT_SESSION_EXPIREDData() {
		return counterReceive_EVT_SESSION_EXPIRED;
	}

	public void setCounterReceive_EVT_SESSION_EXPIREDData(
			long counterReceive_EVT_SESSION_EXPIRED) {
		this.counterReceive_EVT_SESSION_EXPIRED = counterReceive_EVT_SESSION_EXPIRED;
	}

	public long getCounterReceive_EVT_SESSION_ACCESSEDData() {
		return counterReceive_EVT_SESSION_ACCESSED;
	}

	public void setCounterReceive_EVT_SESSION_ACCESSEDData(
			long counterReceive_EVT_SESSION_ACCESSED) {
		this.counterReceive_EVT_SESSION_ACCESSED = counterReceive_EVT_SESSION_ACCESSED;
	}

	public long getCounterReceive_EVT_SESSION_DELTAData() {
		return counterReceive_EVT_SESSION_DELTA;
	}

	public void setCounterReceive_EVT_SESSION_DELTAData(
			long counterReceive_EVT_SESSION_DELTA) {
		this.counterReceive_EVT_SESSION_DELTA = counterReceive_EVT_SESSION_DELTA;
	}

	public int getCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETEData() {
		return counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE;
	}

	public void setCounterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETEData(
			int counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE) {
		this.counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE = counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE;
	}

	public long getCounterReceive_EVT_CHANGE_SESSION_IDData() {
		return counterReceive_EVT_CHANGE_SESSION_ID;
	}

	public void setCounterReceive_EVT_CHANGE_SESSION_IDData(
			long counterReceive_EVT_CHANGE_SESSION_ID) {
		this.counterReceive_EVT_CHANGE_SESSION_ID = counterReceive_EVT_CHANGE_SESSION_ID;
	}

	public long getCounterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGERData() {
		return counterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER;
	}

	public void setCounterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGERData(
			long counterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER) {
		this.counterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER = counterReceive_EVT_ALL_SESSION_NOCONTEXTMANAGER;
	}

	public long getCounterSend_EVT_GET_ALL_SESSIONSData() {
		return counterSend_EVT_GET_ALL_SESSIONS;
	}

	public void setCounterSend_EVT_GET_ALL_SESSIONSData(
			long counterSend_EVT_GET_ALL_SESSIONS) {
		this.counterSend_EVT_GET_ALL_SESSIONS = counterSend_EVT_GET_ALL_SESSIONS;
	}

	public long getCounterSend_EVT_ALL_SESSION_DATAData() {
		return counterSend_EVT_ALL_SESSION_DATA;
	}

	public void setCounterSend_EVT_ALL_SESSION_DATAData(
			long counterSend_EVT_ALL_SESSION_DATA) {
		this.counterSend_EVT_ALL_SESSION_DATA = counterSend_EVT_ALL_SESSION_DATA;
	}

	public long getCounterSend_EVT_SESSION_CREATEDData() {
		return counterSend_EVT_SESSION_CREATED;
	}

	public void setCounterSend_EVT_SESSION_CREATEDData(
			long counterSend_EVT_SESSION_CREATED) {
		this.counterSend_EVT_SESSION_CREATED = counterSend_EVT_SESSION_CREATED;
	}

	public long getCounterSend_EVT_SESSION_DELTAData() {
		return counterSend_EVT_SESSION_DELTA;
	}

	public void setCounterSend_EVT_SESSION_DELTAData(
			long counterSend_EVT_SESSION_DELTA) {
		this.counterSend_EVT_SESSION_DELTA = counterSend_EVT_SESSION_DELTA;
	}

	public long getCounterSend_EVT_SESSION_ACCESSEDData() {
		return counterSend_EVT_SESSION_ACCESSED;
	}

	public void setCounterSend_EVT_SESSION_ACCESSEDData(
			long counterSend_EVT_SESSION_ACCESSED) {
		this.counterSend_EVT_SESSION_ACCESSED = counterSend_EVT_SESSION_ACCESSED;
	}

	public long getCounterSend_EVT_SESSION_EXPIREDData() {
		return counterSend_EVT_SESSION_EXPIRED;
	}

	public void setCounterSend_EVT_SESSION_EXPIREDData(
			long counterSend_EVT_SESSION_EXPIRED) {
		this.counterSend_EVT_SESSION_EXPIRED = counterSend_EVT_SESSION_EXPIRED;
	}

	public int getCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETEData() {
		return counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE;
	}

	public void setCounterSend_EVT_ALL_SESSION_TRANSFERCOMPLETEData(
			int counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE) {
		this.counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE = counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE;
	}

	public long getCounterSend_EVT_CHANGE_SESSION_IDData() {
		return counterSend_EVT_CHANGE_SESSION_ID;
	}

	public void setCounterSend_EVT_CHANGE_SESSION_IDData(
			long counterSend_EVT_CHANGE_SESSION_ID) {
		this.counterSend_EVT_CHANGE_SESSION_ID = counterSend_EVT_CHANGE_SESSION_ID;
	}

	public int getCounterNoStateTransferedData() {
		return counterNoStateTransfered;
	}

	public void setCounterNoStateTransferedData(int counterNoStateTransfered) {
		this.counterNoStateTransfered = counterNoStateTransfered;
	}
}
