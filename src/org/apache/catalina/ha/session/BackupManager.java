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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.catalina.DistributedManager;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session2;
import org.apache.catalina.ha.ClusterManager;
import org.apache.catalina.ha.ClusterMessage;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.tipis.AbstractReplicatedMapMapOwner;
import org.apache.catalina.tribes.tipis.LazyReplicatedMap;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager3;

/**
 * @author Filip Hanik
 * @version 1.0
 */
public class BackupManager extends ClusterManagerBase implements
		AbstractReplicatedMapMapOwner, DistributedManager {

	private final Log log = LogFactory.getLog(BackupManager.class);

	/**
	 * The string manager for this package.
	 */
	private static final StringManager3 sm = StringManager3.getManager(Constants7
			.getPackage());

	private static long DEFAULT_REPL_TIMEOUT = 15000;// 15 seconds

	/**
	 * Set to true if we don't want the sessions to expire on shutdown
	 * 
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	private boolean mExpireSessionsOnShutdown = true;

	/**
	 * The name of this manager
	 */
	private String name;

	/**
     *
     */
	private int mapSendOptions = Channel.SEND_OPTIONS_SYNCHRONIZED_ACK
			| Channel.SEND_OPTIONS_USE_ACK;

	/**
	 * Timeout for RPC messages.
	 */
	private long rpcTimeout = DEFAULT_REPL_TIMEOUT;

	/**
	 * Flag for whether to terminate this map that failed to start.
	 */
	private boolean terminateOnStartFailure = false;

	/**
	 * Constructor, just calls super()
	 *
	 */
	public BackupManager() {
		super();
	}

	// ******************************************************************************/
	// ClusterManager Interface
	// ******************************************************************************/

	@Override
	public void messageDataReceived(ClusterMessage msg) {
	}

	/**
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	public void setExpireSessionsOnShutdown(boolean expireSessionsOnShutdown) {
		setmExpireSessionsOnShutdownData(expireSessionsOnShutdown);
	}

	/**
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	public boolean getExpireSessionsOnShutdown() {
		return ismExpireSessionsOnShutdownData();
	}

	@Override
	public ClusterMessage requestCompleted(String sessionId) {
		if (!getState().isAvailable())
			return null;
		LazyReplicatedMap<String, Session2> map = (LazyReplicatedMap<String, Session2>) getSessions();
		map.replicate(sessionId, false);
		return null;
	}

	// =========================================================================
	// OVERRIDE THESE METHODS TO IMPLEMENT THE REPLICATION
	// =========================================================================
	@Override
	public void objectMadePrimay(Object key, Object value) {
		if (value != null && value instanceof DeltaSession) {
			DeltaSession session = (DeltaSession) value;
			synchronized (session) {
				session.access();
				session.setPrimarySession(true);
				session.endAccess();
			}
		}
	}

	@Override
	public Session2 createEmptySession() {
		return new DeltaSession(this);
	}

	@Override
	public String getName() {
		return this.getNameData();
	}

	/**
	 * Start this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
	 *
	 * Starts the cluster communication channel, this will connect with the
	 * other nodes in the cluster, and request the current session state to be
	 * transferred to this node.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void startInternal() throws LifecycleException {

		super.startInternal();

		try {
			if (getCluster() == null)
				throw new LifecycleException(sm.getString(
						"backupManager.noCluster", getName()));
			LazyReplicatedMap<String, Session2> map = new LazyReplicatedMap<String, Session2>(
					this, getCluster().getChannel(), getRpcTimeoutData(), getMapName(),
					getClassLoaders(), isTerminateOnStartFailureData());
			map.setChannelSendOptions(getMapSendOptionsData());
			this.setSessions(map);
		} catch (Exception x) {
			log.error(sm.getString("backupManager.startUnable", getName()), x);
			throw new LifecycleException(sm.getString(
					"backupManager.startFailed", getName()), x);
		}
		setState(LifecycleState.STARTING);
	}

	public String getMapName() {
		String name = getCluster().getManagerName(getName(), this) + "-"
				+ "map";
		if (log.isDebugEnabled())
			log.debug("Backup manager, Setting map name to:" + name);
		return name;
	}

	/**
	 * Stop this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
	 *
	 * This will disconnect the cluster communication channel and stop the
	 * listener thread.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void stopInternal() throws LifecycleException {

		if (log.isDebugEnabled())
			log.debug(sm.getString("backupManager.stopped", getName()));

		setState(LifecycleState.STOPPING);

		if (getSessions() instanceof LazyReplicatedMap) {
			LazyReplicatedMap<String, Session2> map = (LazyReplicatedMap<String, Session2>) getSessions();
			map.breakdown();
		}

		super.stopInternal();
	}

	@Override
	public void setDistributable(boolean dist) {
		this.setDistributable(dist);
	}

	@Override
	public void setName(String name) {
		this.setNameData(name);
	}

	public void setMapSendOptions(int mapSendOptions) {
		this.setMapSendOptionsData(mapSendOptions);
	}

	public int getMapSendOptions() {
		return getMapSendOptionsData();
	}

	public void setRpcTimeout(long rpcTimeout) {
		this.setRpcTimeoutData(rpcTimeout);
	}

	public long getRpcTimeout() {
		return getRpcTimeoutData();
	}

	public void setTerminateOnStartFailure(boolean terminateOnStartFailure) {
		this.setTerminateOnStartFailureData(terminateOnStartFailure);
	}

	public boolean isTerminateOnStartFailure() {
		return isTerminateOnStartFailureData();
	}

	@Override
	public String[] getInvalidatedSessions() {
		return new String[0];
	}

	@Override
	public ClusterManager cloneFromTemplate() {
		BackupManager result = new BackupManager();
		clone(result);
		result.setmExpireSessionsOnShutdownData(mExpireSessionsOnShutdown);
		result.setMapSendOptionsData(mapSendOptions);
		result.setRpcTimeoutData(rpcTimeout);
		result.setTerminateOnStartFailureData(terminateOnStartFailure);
		return result;
	}

	@Override
	public int getActiveSessionsFull() {
		LazyReplicatedMap<String, Session2> map = (LazyReplicatedMap<String, Session2>) getSessions();
		return map.sizeFull();
	}

	@Override
	public Set<String> getSessionIdsFull() {
		Set<String> sessionIds = new HashSet<String>();
		LazyReplicatedMap<String, Session2> map = (LazyReplicatedMap<String, Session2>) getSessions();
		Iterator<String> keys = map.keySetFull().iterator();
		while (keys.hasNext()) {
			sessionIds.add(keys.next());
		}
		return sessionIds;
	}

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}

	public int getMapSendOptionsData() {
		return mapSendOptions;
	}

	public void setMapSendOptionsData(int mapSendOptions) {
		this.mapSendOptions = mapSendOptions;
	}

	public long getRpcTimeoutData() {
		return rpcTimeout;
	}

	public void setRpcTimeoutData(long rpcTimeout) {
		this.rpcTimeout = rpcTimeout;
	}

	public boolean isTerminateOnStartFailureData() {
		return terminateOnStartFailure;
	}

	public void setTerminateOnStartFailureData(boolean terminateOnStartFailure) {
		this.terminateOnStartFailure = terminateOnStartFailure;
	}

	public boolean ismExpireSessionsOnShutdownData() {
		return mExpireSessionsOnShutdown;
	}

	public void setmExpireSessionsOnShutdownData(boolean mExpireSessionsOnShutdown) {
		this.mExpireSessionsOnShutdown = mExpireSessionsOnShutdown;
	}

}
