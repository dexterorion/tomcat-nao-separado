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

package org.apache.catalina.tribes.group.interceptors;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelInterceptor;
import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.ChannelInterceptorBase;
import org.apache.catalina.tribes.io.ChannelData;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * 
 * Sends a ping to all members. Configure this interceptor with the
 * TcpFailureDetector below it, and the TcpFailureDetector will act as the
 * membership guide.
 * 
 * @author Filip Hanik
 * @version 1.0
 */

public class TcpPingInterceptor extends ChannelInterceptorBase {

	private static final Log log = LogFactory.getLog(TcpPingInterceptor.class);

	private static byte[] TCP_PING_DATA = new byte[] { 79, -89, 115, 72, 121,
			-33, 67, -55, -97, 111, -119, -128, -95, 91, 7, 20, 125, -39, 82,
			91, -21, -33, 67, -102, -73, 126, -66, -113, -127, 103, 30, -74,
			55, 21, -66, -121, 69, 33, 76, -88, -65, 10, 77, 19, 83, 56, 21,
			50, 85, -10, -108, -73, 58, -33, 33, 120, -111, 4, 125, -41, 114,
			-124, -64, -43 };

	private long interval = 1000; // 1 second

	private boolean useThread = false;
	private boolean staticOnly = false;
	private volatile boolean running = true;
	private TcpPingInterceptorPingThread thread = null;
	private static AtomicInteger cnt = new AtomicInteger(0);

	private WeakReference<TcpFailureDetector> failureDetector = null;
	private WeakReference<StaticMembershipInterceptor> staticMembers = null;

	@Override
	public synchronized void start(int svc) throws ChannelException {
		super.start(svc);
		running = true;
		if (thread == null && useThread) {
			thread = new TcpPingInterceptorPingThread(this);
			thread.setDaemon(true);
			thread.setName("TcpPingInterceptor.TcpPingInterceptorPingThread-"
					+ cnt.addAndGet(1));
			thread.start();
		}

		// acquire the interceptors to invoke on send ping events
		ChannelInterceptor next = getNext();
		while (next != null) {
			if (next instanceof TcpFailureDetector)
				failureDetector = new WeakReference<TcpFailureDetector>(
						(TcpFailureDetector) next);
			if (next instanceof StaticMembershipInterceptor)
				staticMembers = new WeakReference<StaticMembershipInterceptor>(
						(StaticMembershipInterceptor) next);
			next = next.getNext();
		}

	}

	@Override
	public void stop(int svc) throws ChannelException {
		running = false;
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
		super.stop(svc);
	}

	@Override
	public void heartbeat() {
		super.heartbeat();
		if (!getUseThread())
			sendPing();
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public void setUseThread(boolean useThread) {
		this.useThread = useThread;
	}

	public void setStaticOnly(boolean staticOnly) {
		this.staticOnly = staticOnly;
	}

	public boolean getUseThread() {
		return useThread;
	}

	public boolean getStaticOnly() {
		return staticOnly;
	}

	protected void sendPing() {
		TcpFailureDetector tcpFailureDetector = failureDetector != null ? failureDetector
				.get() : null;
		if (tcpFailureDetector != null) {
			// We have a reference to the failure detector
			// Piggy back on it
			tcpFailureDetector.checkMembers(true);
		} else {
			StaticMembershipInterceptor smi = staticOnly
					&& staticMembers != null ? staticMembers.get() : null;
			if (smi != null) {
				sendPingMessage(smi.getMembers());
			} else {
				sendPingMessage(getMembers());
			}
		}
	}

	protected void sendPingMessage(Member[] members) {
		if (members == null || members.length == 0)
			return;
		ChannelData data = new ChannelData(true);// generates a unique Id
		data.setAddress(getLocalMember(false));
		data.setTimestamp(System.currentTimeMillis());
		data.setOptions(getOptionFlag());
		data.setMessage(new XByteBuffer(TCP_PING_DATA, false));
		try {
			super.sendMessage(members, data, null);
		} catch (ChannelException x) {
			log.warn("Unable to send TCP ping.", x);
		}
	}

	@Override
	public void messageReceived(ChannelMessage msg) {
		// catch incoming
		boolean process = true;
		if (okToProcess(msg.getOptions())) {
			// check to see if it is a ping message, if so, process = false
			process = ((msg.getMessage().getLength() != TCP_PING_DATA.length) || (!Arrays
					.equals(TCP_PING_DATA, msg.getMessage().getBytes())));
		}// end if

		// ignore the message, it doesnt have the flag set
		if (process)
			super.messageReceived(msg);
		else if (log.isDebugEnabled())
			log.debug("Received a TCP ping packet:" + msg);
	}

	public static byte[] getTCP_PING_DATA() {
		return TCP_PING_DATA;
	}

	public static void setTCP_PING_DATA(byte[] tCP_PING_DATA) {
		TCP_PING_DATA = tCP_PING_DATA;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public TcpPingInterceptorPingThread getThread() {
		return thread;
	}

	public void setThread(TcpPingInterceptorPingThread thread) {
		this.thread = thread;
	}

	public static AtomicInteger getCnt() {
		return cnt;
	}

	public static void setCnt(AtomicInteger cnt) {
		TcpPingInterceptor.cnt = cnt;
	}

	public WeakReference<TcpFailureDetector> getFailureDetector() {
		return failureDetector;
	}

	public void setFailureDetector(
			WeakReference<TcpFailureDetector> failureDetector) {
		this.failureDetector = failureDetector;
	}

	public WeakReference<StaticMembershipInterceptor> getStaticMembers() {
		return staticMembers;
	}

	public void setStaticMembers(
			WeakReference<StaticMembershipInterceptor> staticMembers) {
		this.staticMembers = staticMembers;
	}

	public static Log getLog() {
		return log;
	}

}