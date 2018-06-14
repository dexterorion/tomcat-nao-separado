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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelInterfaceptorInterceptorEvent;
import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.UniqueId;
import org.apache.catalina.tribes.group.AbsoluteOrder;
import org.apache.catalina.tribes.group.ChannelInterceptorBase;
import org.apache.catalina.tribes.group.InterceptorPayload;
import org.apache.catalina.tribes.io.ChannelData;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.util.Arrays;
import org.apache.catalina.tribes.util.UUIDGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * <p>
 * Title: Auto merging leader election algorithm
 * </p>
 *
 * <p>
 * Description: Implementation of a simple coordinator algorithm that not only
 * selects a coordinator, it also merges groups automatically when members are
 * discovered that werent part of the
 * </p>
 * <p>
 * This algorithm is non blocking meaning it allows for transactions while the
 * coordination phase is going on
 * </p>
 * <p>
 * This implementation is based on a home brewed algorithm that uses the
 * AbsoluteOrder of a membership to pass a token ring of the current membership.
 * <br>
 * This is not the same as just using AbsoluteOrder! Consider the following
 * scenario:<br>
 * Nodes, A,B,C,D,E on a network, in that priority. AbsoluteOrder will only work
 * if all nodes are receiving pings from all the other nodes. meaning, that
 * node{i} receives pings from node{all}-node{i}<br>
 * but the following could happen if a multicast problem occurs. A has members
 * {B,C,D}<br>
 * B has members {A,C}<br>
 * C has members {D,E}<br>
 * D has members {A,B,C,E}<br>
 * E has members {A,C,D}<br>
 * Because the default Tribes membership implementation, relies on the multicast
 * packets to arrive at all nodes correctly, there is nothing guaranteeing that
 * it will.<br>
 * <br>
 * To best explain how this algorithm works, lets take the above example: For
 * simplicity we assume that a send operation is O(1) for all nodes, although
 * this algorithm will work where messages overlap, as they all depend on
 * absolute order<br>
 * Scenario 1: A,B,C,D,E all come online at the same time Eval phase, A thinks
 * of itself as leader, B thinks of A as leader, C thinks of itself as leader,
 * D,E think of A as leader<br>
 * Token phase:<br>
 * (1) A sends out a message X{A-ldr, A-src, mbrs-A,B,C,D} to B where X is the
 * id for the message(and the view)<br>
 * (1) C sends out a message Y{C-ldr, C-src, mbrs-C,D,E} to D where Y is the id
 * for the message(and the view)<br>
 * (2) B receives X{A-ldr, A-src, mbrs-A,B,C,D}, sends X{A-ldr, A-src,
 * mbrs-A,B,C,D} to C <br>
 * (2) D receives Y{C-ldr, C-src, mbrs-C,D,E} D is aware of A,B, sends Y{A-ldr,
 * C-src, mbrs-A,B,C,D,E} to E<br>
 * (3) C receives X{A-ldr, A-src, mbrs-A,B,C,D}, sends X{A-ldr, A-src,
 * mbrs-A,B,C,D,E} to D<br>
 * (3) E receives Y{A-ldr, C-src, mbrs-A,B,C,D,E} sends Y{A-ldr, C-src,
 * mbrs-A,B,C,D,E} to A<br>
 * (4) D receives X{A-ldr, A-src, mbrs-A,B,C,D,E} sends sends X{A-ldr, A-src,
 * mbrs-A,B,C,D,E} to A<br>
 * (4) A receives Y{A-ldr, C-src, mbrs-A,B,C,D,E}, holds the message, add E to
 * its list of members<br>
 * (5) A receives X{A-ldr, A-src, mbrs-A,B,C,D,E} <br>
 * At this point, the state looks like<br>
 * A - {A-ldr, mbrs-A,B,C,D,E, id=X}<br>
 * B - {A-ldr, mbrs-A,B,C,D, id=X}<br>
 * C - {A-ldr, mbrs-A,B,C,D,E, id=X}<br>
 * D - {A-ldr, mbrs-A,B,C,D,E, id=X}<br>
 * E - {A-ldr, mbrs-A,B,C,D,E, id=Y}<br>
 * <br>
 * A message doesn't stop until it reaches its original sender, unless its
 * dropped by a higher leader. As you can see, E still thinks the viewId=Y,
 * which is not correct. But at this point we have arrived at the same
 * membership and all nodes are informed of each other.<br>
 * To synchronize the rest we simply perform the following check at A when A
 * receives X:<br>
 * Original X{A-ldr, A-src, mbrs-A,B,C,D} == Arrived X{A-ldr, A-src,
 * mbrs-A,B,C,D,E}<br>
 * Since the condition is false, A, will resend the token, and A sends X{A-ldr,
 * A-src, mbrs-A,B,C,D,E} to B When A receives X again, the token is complete. <br>
 * Optionally, A can send a message X{A-ldr, A-src, mbrs-A,B,C,D,E confirmed} to
 * A,B,C,D,E who then install and accept the view.
 * </p>
 * <p>
 * Lets assume that C1 arrives, C1 has lower priority than C, but higher
 * priority than D.<br>
 * Lets also assume that C1 sees the following view {B,D,E}<br>
 * C1 waits for a token to arrive. When the token arrives, the same scenario as
 * above will happen.<br>
 * In the scenario where C1 sees {D,E} and A,B,C can not see C1, no token will
 * ever arrive.<br>
 * In this case, C1 sends a Z{C1-ldr, C1-src, mbrs-C1,D,E} to D<br>
 * D receives Z{C1-ldr, C1-src, mbrs-C1,D,E} and sends Z{A-ldr, C1-src,
 * mbrs-A,B,C,C1,D,E} to E<br>
 * E receives Z{A-ldr, C1-src, mbrs-A,B,C,C1,D,E} and sends it to A<br>
 * A sends Z{A-ldr, A-src, mbrs-A,B,C,C1,D,E} to B and the chain continues until
 * A receives the token again. At that time A optionally sends out Z{A-ldr,
 * A-src, mbrs-A,B,C,C1,D,E, confirmed} to A,B,C,C1,D,E
 * </p>
 * <p>
 * To ensure that the view gets implemented at all nodes at the same time, A
 * will send out a VIEW_CONF message, this is the 'confirmed' message that is
 * optional above.
 * <p>
 * Ideally, the interceptor below this one would be the TcpFailureDetector to
 * ensure correct memberships
 * </p>
 *
 * <p>
 * The example above, of course can be simplified with a finite statemachine:<br>
 * But I suck at writing state machines, my head gets all confused. One day I
 * will document this algorithm though.<br>
 * Maybe I'll do a state diagram :)
 * </p>
 * <h2>State Diagrams</h2> <a href=
 * "http://people.apache.org/~fhanik/tribes/docs/leader-election-initiate-election.jpg"
 * >Initiate an election</a><br>
 * <br>
 * <a href=
 * "http://people.apache.org/~fhanik/tribes/docs/leader-election-message-arrives.jpg"
 * >Receive an election message</a><br>
 * <br>
 * 
 * @author Filip Hanik
 * @version 1.0
 * 
 * 
 * 
 */
public class NonBlockingCoordinator extends ChannelInterceptorBase {

	private static final Log log = LogFactory
			.getLog(NonBlockingCoordinator.class);

	/**
	 * header for a coordination message
	 */
	private static final byte[] COORD_HEADER = new byte[] { -86, 38, -34, -29,
			-98, 90, 65, 63, -81, -122, -6, -110, 99, -54, 13, 63 };
	/**
	 * Coordination request
	 */
	private static final byte[] COORD_REQUEST = new byte[] { 104, -95, -92,
			-42, 114, -36, 71, -19, -79, 20, 122, 101, -1, -48, -49, 30 };
	/**
	 * Coordination confirmation, for blocking installations
	 */
	private static final byte[] COORD_CONF = new byte[] { 67, 88, 107, -86, 69,
			23, 76, -70, -91, -23, -87, -25, -125, 86, 75, 20 };

	/**
	 * Alive message
	 */
	private static final byte[] COORD_ALIVE = new byte[] { 79, -121, -25, -15,
			-59, 5, 64, 94, -77, 113, -119, -88, 52, 114, -56, -46, -18, 102,
			10, 34, -127, -9, 71, 115, -70, 72, -101, 88, 72, -124, 127, 111,
			74, 76, -116, 50, 111, 103, 65, 3, -77, 51, -35, 0, 119, 117, 9,
			-26, 119, 50, -75, -105, -102, 36, 79, 37, -68, -84, -123, 15, -22,
			-109, 106, -55 };
	/**
	 * Time to wait for coordination timeout
	 */
	private long waitForCoordMsgTimeout = 15000;
	/**
	 * Our current view
	 */
	private Membership view = null;
	/**
	 * Out current viewId
	 */
	private UniqueId viewId;

	/**
	 * Our nonblocking membership
	 */
	private Membership membership = null;

	/**
	 * indicates that we are running an election and this is the one we are
	 * running
	 */
	private UniqueId suggestedviewId;
	private Membership suggestedView;

	private boolean started = false;
	private final int startsvc = 0xFFFF;

	private Object electionMutex = new Object();

	private AtomicBoolean coordMsgReceived = new AtomicBoolean(false);

	public NonBlockingCoordinator() {
		super();
	}

	// ============================================================================================================
	// COORDINATION HANDLING
	// ============================================================================================================

	public void startElection(boolean force) throws ChannelException {
		synchronized (electionMutex) {
			MemberImpl local = (MemberImpl) getLocalMember(false);
			MemberImpl[] others = membership.getMembers();
			fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
					NonBlockingCoordinatorCoordinationEvent.getEvtStartElect(),
					this, "Election initated"));
			if (others.length == 0) {
				this.viewId = new UniqueId(UUIDGenerator.randomUUID(false));
				this.view = new Membership(local, AbsoluteOrder.getComp(), true);
				this.handleViewConf(
						this.createElectionMsg(local, others, local), local,
						view);
				return; // the only member, no need for an election
			}
			if (suggestedviewId != null) {

				if (view != null
						&& Arrays.diff(view, suggestedView, local).length == 0
						&& Arrays.diff(suggestedView, view, local).length == 0) {
					suggestedviewId = null;
					suggestedView = null;
					fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
							NonBlockingCoordinatorCoordinationEvent
									.getEvtElectAbandoned(),
							this,
							"Election abandoned, running election matches view"));
				} else {
					fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
							NonBlockingCoordinatorCoordinationEvent
									.getEvtElectAbandoned(),
							this, "Election abandoned, election running"));
				}
				return; // election already running, I'm not allowed to have two
						// of them
			}
			if (view != null
					&& Arrays.diff(view, membership, local).length == 0
					&& Arrays.diff(membership, view, local).length == 0) {
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent
								.getEvtElectAbandoned(),
						this, "Election abandoned, view matches membership"));
				return; // already have this view installed
			}
			int prio = AbsoluteOrder.getComp().compare(local, others[0]);
			MemberImpl leader = (prio < 0) ? local : others[0];// am I the
																// leader in my
																// view?
			if (local.equals(leader) || force) {
				NonBlockingCoordinatorCoordinationMessage msg = createElectionMsg(
						local, others, leader);
				suggestedviewId = msg.getId();
				suggestedView = new Membership(local, AbsoluteOrder.getComp(),
						true);
				Arrays.fill(suggestedView, msg.getMembers());
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent
								.getEvtProcessElect(),
						this, "Election, sending request"));
				sendElectionMsg(local, others[0], msg);
			} else {
				try {
					coordMsgReceived.set(false);
					fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
							NonBlockingCoordinatorCoordinationEvent
									.getEvtWaitForMsg(),
							this, "Election, waiting for request"));
					electionMutex.wait(waitForCoordMsgTimeout);
				} catch (InterruptedException x) {
					Thread.interrupted();
				}
				if (suggestedviewId == null && (!coordMsgReceived.get())) {
					// no message arrived, send the coord msg
					// fireInterceptorEvent(new
					// NonBlockingCoordinatorCoordinationEvent(NonBlockingCoordinatorCoordinationEvent.EVT_WAIT_FOR_MSG,this,"Election, waiting timed out."));
					// startElection(true);
					fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
							NonBlockingCoordinatorCoordinationEvent
									.getEvtElectAbandoned(),
							this, "Election abandoned, waiting timed out."));
				} else {
					fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
							NonBlockingCoordinatorCoordinationEvent
									.getEvtElectAbandoned(),
							this, "Election abandoned, received a message"));
				}
			}// end if

		}
	}

	private NonBlockingCoordinatorCoordinationMessage createElectionMsg(
			MemberImpl local, MemberImpl[] others, MemberImpl leader) {
		Membership m = new Membership(local, AbsoluteOrder.getComp(), true);
		Arrays.fill(m, others);
		MemberImpl[] mbrs = m.getMembers();
		m.reset();
		NonBlockingCoordinatorCoordinationMessage msg = new NonBlockingCoordinatorCoordinationMessage(
				leader, local, mbrs, new UniqueId(
						UUIDGenerator.randomUUID(true)), COORD_REQUEST);
		return msg;
	}

	protected void sendElectionMsg(MemberImpl local, MemberImpl next,
			NonBlockingCoordinatorCoordinationMessage msg)
			throws ChannelException {
		fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
				NonBlockingCoordinatorCoordinationEvent.getEvtSendMsg(), this,
				"Sending election message to(" + next.getName() + ")"));
		super.sendMessage(new Member[] { next }, createData(msg, local), null);
	}

	protected void sendElectionMsgToNextInline(MemberImpl local,
			NonBlockingCoordinatorCoordinationMessage msg)
			throws ChannelException {
		int next = Arrays.nextIndex(local, msg.getMembers());
		int current = next;
		msg.setLeader(msg.getMembers()[0]);
		boolean sent = false;
		while (!sent && current >= 0) {
			try {
				sendElectionMsg(local, msg.getMembers()[current], msg);
				sent = true;
			} catch (ChannelException x) {
				log.warn("Unable to send election message to:"
						+ msg.getMembers()[current]);
				current = Arrays.nextIndex(msg.getMembers()[current],
						msg.getMembers());
				if (current == next)
					throw x;
			}
		}
	}

	public Member getNextInLine(MemberImpl local, MemberImpl[] others) {
		MemberImpl result = null;
		for (int i = 0; i < others.length; i++) {

		}
		return result;
	}

	public ChannelData createData(
			NonBlockingCoordinatorCoordinationMessage msg, MemberImpl local) {
		msg.write();
		ChannelData data = new ChannelData(true);
		data.setAddress(local);
		data.setMessage(msg.getBuffer());
		data.setOptions(Channel.SEND_OPTIONS_USE_ACK);
		data.setTimestamp(System.currentTimeMillis());
		return data;
	}

	protected void viewChange(UniqueId viewId, Member[] view) {
		// invoke any listeners
	}

	protected boolean alive(Member mbr) {
		return TcpFailureDetector
				.memberAlive(mbr, COORD_ALIVE, false, false,
						waitForCoordMsgTimeout, waitForCoordMsgTimeout,
						getOptionFlag());
	}

	protected Membership mergeOnArrive(
			NonBlockingCoordinatorCoordinationMessage msg, Member sender) {
		fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
				NonBlockingCoordinatorCoordinationEvent.getEvtPreMerge(), this,
				"Pre merge"));
		MemberImpl local = (MemberImpl) getLocalMember(false);
		Membership merged = new Membership(local, AbsoluteOrder.getComp(), true);
		Arrays.fill(merged, msg.getMembers());
		Arrays.fill(merged, getMembers());
		Member[] diff = Arrays.diff(merged, membership, local);
		for (int i = 0; i < diff.length; i++) {
			if (!alive(diff[i]))
				merged.removeMember((MemberImpl) diff[i]);
			else
				memberAdded(diff[i], false);
		}
		fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
				NonBlockingCoordinatorCoordinationEvent.getEvtPostMerge(),
				this, "Post merge"));
		return merged;
	}

	protected void processCoordMessage(
			NonBlockingCoordinatorCoordinationMessage msg, Member sender)
			throws ChannelException {
		if (!coordMsgReceived.get()) {
			coordMsgReceived.set(true);
			synchronized (electionMutex) {
				electionMutex.notifyAll();
			}
		}
		msg.setTimestamp(System.currentTimeMillis());
		Membership merged = mergeOnArrive(msg, sender);
		if (isViewConf(msg))
			handleViewConf(msg, sender, merged);
		else
			handleToken(msg, sender, merged);
	}

	protected void handleToken(NonBlockingCoordinatorCoordinationMessage msg,
			Member sender, Membership merged) throws ChannelException {
		MemberImpl local = (MemberImpl) getLocalMember(false);
		if (local.equals(msg.getSource())) {
			// my message msg.src=local
			handleMyToken(local, msg, sender, merged);
		} else {
			handleOtherToken(local, msg, sender, merged);
		}
	}

	protected void handleMyToken(MemberImpl local,
			NonBlockingCoordinatorCoordinationMessage msg, Member sender,
			Membership merged) throws ChannelException {
		if (local.equals(msg.getLeader())) {
			// no leadership change
			if (Arrays.sameMembers(msg.getMembers(), merged.getMembers())) {
				msg.setType(COORD_CONF);
				super.sendMessage(Arrays.remove(msg.getMembers(), local),
						createData(msg, local), null);
				handleViewConf(msg, local, merged);
			} else {
				// membership change
				suggestedView = new Membership(local, AbsoluteOrder.getComp(),
						true);
				suggestedviewId = msg.getId();
				Arrays.fill(suggestedView, merged.getMembers());
				msg.setView(merged.getMembers());
				sendElectionMsgToNextInline(local, msg);
			}
		} else {
			// leadership change
			suggestedView = null;
			suggestedviewId = null;
			msg.setView(merged.getMembers());
			sendElectionMsgToNextInline(local, msg);
		}
	}

	protected void handleOtherToken(MemberImpl local,
			NonBlockingCoordinatorCoordinationMessage msg, Member sender,
			Membership merged) throws ChannelException {
		if (local.equals(msg.getLeader())) {
			// I am the new leader
			// startElection(false);
		} else {
			msg.setView(merged.getMembers());
			sendElectionMsgToNextInline(local, msg);
		}
	}

	protected void handleViewConf(
			NonBlockingCoordinatorCoordinationMessage msg, Member sender,
			Membership merged) throws ChannelException {
		if (viewId != null && msg.getId().equals(viewId))
			return;// we already have this view
		view = new Membership((MemberImpl) getLocalMember(false),
				AbsoluteOrder.getComp(), true);
		Arrays.fill(view, msg.getMembers());
		viewId = msg.getId();

		if (viewId.equals(suggestedviewId)) {
			suggestedView = null;
			suggestedviewId = null;
		}

		if (suggestedView != null
				&& AbsoluteOrder.getComp().compare(
						suggestedView.getMembers()[0], merged.getMembers()[0]) < 0) {
			suggestedView = null;
			suggestedviewId = null;
		}

		viewChange(viewId, view.getMembers());
		fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
				NonBlockingCoordinatorCoordinationEvent.getEvtConfRx(), this,
				"Accepted View"));

		if (suggestedviewId == null
				&& hasHigherPriority(merged.getMembers(),
						membership.getMembers())) {
			startElection(false);
		}
	}

	protected boolean isViewConf(NonBlockingCoordinatorCoordinationMessage msg) {
		return Arrays.contains(msg.getType(), 0, COORD_CONF, 0,
				COORD_CONF.length);
	}

	protected boolean hasHigherPriority(Member[] complete, Member[] local) {
		if (local == null || local.length == 0)
			return false;
		if (complete == null || complete.length == 0)
			return true;
		AbsoluteOrder.absoluteOrder(complete);
		AbsoluteOrder.absoluteOrder(local);
		return (AbsoluteOrder.getComp().compare(complete[0], local[0]) > 0);

	}

	/**
	 * Returns coordinator if one is available
	 * 
	 * @return Member
	 */
	public Member getCoordinator() {
		return (view != null && view.hasMembers()) ? view.getMembers()[0]
				: null;
	}

	public Member[] getView() {
		return (view != null && view.hasMembers()) ? view.getMembers()
				: new Member[0];
	}

	public UniqueId getViewId() {
		return viewId;
	}

	/**
	 * Block in/out messages while a election is going on
	 */
	protected void halt() {

	}

	/**
	 * Release lock for in/out messages election is completed
	 */
	protected void release() {

	}

	/**
	 * Wait for an election to end
	 */
	protected void waitForRelease() {

	}

	// ============================================================================================================
	// OVERRIDDEN METHODS FROM CHANNEL INTERCEPTOR BASE
	// ============================================================================================================
	@Override
	public void start(int svc) throws ChannelException {
		if (membership == null)
			setupMembership();
		if (started)
			return;
		fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
				NonBlockingCoordinatorCoordinationEvent.getEvtStart(), this,
				"Before start"));
		super.start(startsvc);
		started = true;
		if (view == null)
			view = new Membership((MemberImpl) super.getLocalMember(true),
					AbsoluteOrder.getComp(), true);
		fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
				NonBlockingCoordinatorCoordinationEvent.getEvtStart(), this,
				"After start"));
		startElection(false);
	}

	@Override
	public void stop(int svc) throws ChannelException {
		try {
			halt();
			synchronized (electionMutex) {
				if (!started)
					return;
				started = false;
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent.getEvtStop(),
						this, "Before stop"));
				super.stop(startsvc);
				this.view = null;
				this.viewId = null;
				this.suggestedView = null;
				this.suggestedviewId = null;
				this.membership.reset();
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent.getEvtStop(),
						this, "After stop"));
			}
		} finally {
			release();
		}
	}

	@Override
	public void sendMessage(Member[] destination, ChannelMessage msg,
			InterceptorPayload payload) throws ChannelException {
		waitForRelease();
		super.sendMessage(destination, msg, payload);
	}

	@Override
	public void messageReceived(ChannelMessage msg) {
		if (Arrays.contains(msg.getMessage().getBytesDirect(), 0, COORD_ALIVE,
				0, COORD_ALIVE.length)) {
			// ignore message, its an alive message
			fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
					NonBlockingCoordinatorCoordinationEvent.getEvtMsgArrive(),
					this, "Alive Message"));

		} else if (Arrays.contains(msg.getMessage().getBytesDirect(), 0,
				COORD_HEADER, 0, COORD_HEADER.length)) {
			try {
				NonBlockingCoordinatorCoordinationMessage cmsg = new NonBlockingCoordinatorCoordinationMessage(
						msg.getMessage());
				Member[] cmbr = cmsg.getMembers();
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent
								.getEvtMsgArrive(),
						this, "Coord Msg Arrived(" + Arrays.toNameString(cmbr)
								+ ")"));
				processCoordMessage(cmsg, msg.getAddress());
			} catch (ChannelException x) {
				log.error(
						"Error processing coordination message. Could be fatal.",
						x);
			}
		} else {
			super.messageReceived(msg);
		}
	}

	@Override
	public void memberAdded(Member member) {
		memberAdded(member, true);
	}

	public void memberAdded(Member member, boolean elect) {
		try {
			if (membership == null)
				setupMembership();
			if (membership.memberAlive((MemberImpl) member))
				super.memberAdded(member);
			try {
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent.getEvtMbrAdd(),
						this, "Member add(" + member.getName() + ")"));
				if (started && elect)
					startElection(false);
			} catch (ChannelException x) {
				log.error("Unable to start election when member was added.", x);
			}
		} finally {
		}

	}

	@Override
	public void memberDisappeared(Member member) {
		try {

			membership.removeMember((MemberImpl) member);
			super.memberDisappeared(member);
			try {
				fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
						NonBlockingCoordinatorCoordinationEvent.getEvtMbrDel(),
						this, "Member remove(" + member.getName() + ")"));
				if (started && (isCoordinator() || isHighest()))
					startElection(true); // to do, if a member disappears, only
											// the coordinator can start
			} catch (ChannelException x) {
				log.error("Unable to start election when member was removed.",
						x);
			}
		} finally {
		}
	}

	public boolean isHighest() {
		Member local = getLocalMember(false);
		if (membership.getMembers().length == 0)
			return true;
		else
			return AbsoluteOrder.getComp().compare(local,
					membership.getMembers()[0]) <= 0;
	}

	public boolean isCoordinator() {
		Member coord = getCoordinator();
		return coord != null && getLocalMember(false).equals(coord);
	}

	@Override
	public void heartbeat() {
		try {
			MemberImpl local = (MemberImpl) getLocalMember(false);
			if (view != null
					&& (Arrays.diff(view, membership, local).length != 0 || Arrays
							.diff(membership, view, local).length != 0)) {
				if (isHighest()) {
					fireInterceptorEvent(new NonBlockingCoordinatorCoordinationEvent(
							NonBlockingCoordinatorCoordinationEvent
									.getEvtStartElect(),
							this,
							"Heartbeat found inconsistency, restart election"));
					startElection(true);
				}
			}
		} catch (Exception x) {
			log.error("Unable to perform heartbeat.", x);
		} finally {
			super.heartbeat();
		}
	}

	/**
	 * has members
	 */
	@Override
	public boolean hasMembers() {

		return membership.hasMembers();
	}

	/**
	 * Get all current cluster members
	 * 
	 * @return all members or empty array
	 */
	@Override
	public Member[] getMembers() {

		return membership.getMembers();
	}

	/**
	 *
	 * @param mbr
	 *            Member
	 * @return Member
	 */
	@Override
	public Member getMember(Member mbr) {

		return membership.getMember(mbr);
	}

	/**
	 * Return the member that represents this node.
	 *
	 * @return Member
	 */
	@Override
	public Member getLocalMember(boolean incAlive) {
		Member local = super.getLocalMember(incAlive);
		if (view == null && (local != null))
			setupMembership();
		return local;
	}

	protected synchronized void setupMembership() {
		if (membership == null) {
			membership = new Membership(
					(MemberImpl) super.getLocalMember(true),
					AbsoluteOrder.getComp(), false);
		}
	}

	// ============================================================================================================
	// HELPER CLASSES FOR COORDINATION
	// ============================================================================================================

	@Override
	public void fireInterceptorEvent(ChannelInterfaceptorInterceptorEvent event) {
		if (event instanceof NonBlockingCoordinatorCoordinationEvent
				&& ((NonBlockingCoordinatorCoordinationEvent) event).getType() == NonBlockingCoordinatorCoordinationEvent
						.getEvtConfRx())
			log.info(event);
	}

	public long getWaitForCoordMsgTimeout() {
		return waitForCoordMsgTimeout;
	}

	public void setWaitForCoordMsgTimeout(long waitForCoordMsgTimeout) {
		this.waitForCoordMsgTimeout = waitForCoordMsgTimeout;
	}

	public Membership getMembership() {
		return membership;
	}

	public void setMembership(Membership membership) {
		this.membership = membership;
	}

	public UniqueId getSuggestedviewId() {
		return suggestedviewId;
	}

	public void setSuggestedviewId(UniqueId suggestedviewId) {
		this.suggestedviewId = suggestedviewId;
	}

	public Membership getSuggestedView() {
		return suggestedView;
	}

	public void setSuggestedView(Membership suggestedView) {
		this.suggestedView = suggestedView;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public Object getElectionMutex() {
		return electionMutex;
	}

	public void setElectionMutex(Object electionMutex) {
		this.electionMutex = electionMutex;
	}

	public AtomicBoolean getCoordMsgReceived() {
		return coordMsgReceived;
	}

	public void setCoordMsgReceived(AtomicBoolean coordMsgReceived) {
		this.coordMsgReceived = coordMsgReceived;
	}

	public static Log getLog() {
		return log;
	}

	public static byte[] getCoordHeader() {
		return COORD_HEADER;
	}

	public static byte[] getCoordRequest() {
		return COORD_REQUEST;
	}

	public static byte[] getCoordConf() {
		return COORD_CONF;
	}

	public static byte[] getCoordAlive() {
		return COORD_ALIVE;
	}

	public int getStartsvc() {
		return startsvc;
	}

	public void setView(Membership view) {
		this.view = view;
	}
	
	public Membership getViewVariable(){
		return this.view;
	}

	public void setViewId(UniqueId viewId) {
		this.viewId = viewId;
	}

}