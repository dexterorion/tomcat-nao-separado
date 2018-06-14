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

/**
 * This class is used to track the series of actions that happens when
 * a request is executed. These actions will then translate into invocations of methods 
 * on the actual session.
 * This class is NOT thread safe. One DeltaRequest per session
 * @author <a href="mailto:fhanik@apache.org">Filip Hanik</a>
 * @version 1.0
 */

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.LinkedList;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.tomcat.util.res.StringManager3;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class DeltaRequest implements Externalizable {

	private static final Log log = LogFactory.getLog(DeltaRequest.class);

	/**
	 * The string manager for this package.
	 */
	private static final StringManager3 sm = StringManager3.getManager(Constants7
			.getPackage());

	private static final int TYPE_ATTRIBUTE = 0;
	private static final int TYPE_PRINCIPAL = 1;
	private static final int TYPE_ISNEW = 2;
	private static final int TYPE_MAXINTERVAL = 3;
	private static final int TYPE_AUTHTYPE = 4;

	private static final int ACTION_SET = 0;
	private static final int ACTION_REMOVE = 1;

	private static final String NAME_PRINCIPAL = "__SET__PRINCIPAL__";
	private static final String NAME_MAXINTERVAL = "__SET__MAXINTERVAL__";
	private static final String NAME_ISNEW = "__SET__ISNEW__";
	private static final String NAME_AUTHTYPE = "__SET__AUTHTYPE__";

	private String sessionId;
	private LinkedList<DeltaRequestAttributeInfo> actions = new LinkedList<DeltaRequestAttributeInfo>();
	private LinkedList<DeltaRequestAttributeInfo> actionPool = new LinkedList<DeltaRequestAttributeInfo>();

	private boolean recordAllActions = false;

	public DeltaRequest() {

	}

	public DeltaRequest(String sessionId, boolean recordAllActions) {
		this.recordAllActions = recordAllActions;
		if (sessionId != null)
			setSessionId(sessionId);
	}

	public void setAttribute(String name, Object value) {
		int action = (value == null) ? ACTION_REMOVE : ACTION_SET;
		addAction(TYPE_ATTRIBUTE, action, name, value);
	}

	public void removeAttribute(String name) {
		int action = ACTION_REMOVE;
		addAction(TYPE_ATTRIBUTE, action, name, null);
	}

	public void setMaxInactiveInterval(int interval) {
		int action = ACTION_SET;
		addAction(TYPE_MAXINTERVAL, action, NAME_MAXINTERVAL,
				Integer.valueOf(interval));
	}

	/**
	 * convert principal at SerializablePrincipal for backup nodes. Only support
	 * principals from type {@link GenericPrincipal GenericPrincipal}
	 * 
	 * @param p
	 *            Session principal
	 * @see GenericPrincipal
	 */
	public void setPrincipal(Principal p) {
		int action = (p == null) ? ACTION_REMOVE : ACTION_SET;
		SerializablePrincipal sp = null;
		if (p != null) {
			if (p instanceof GenericPrincipal) {
				sp = SerializablePrincipal
						.createPrincipal((GenericPrincipal) p);
				if (log.isDebugEnabled())
					log.debug(sm.getString("deltaRequest.showPrincipal",
							p.getName(), getSessionId()));
			} else
				log.error(sm.getString("deltaRequest.wrongPrincipalClass", p
						.getClass().getName()));
		}
		addAction(TYPE_PRINCIPAL, action, NAME_PRINCIPAL, sp);
	}

	public void setNew(boolean n) {
		int action = ACTION_SET;
		addAction(TYPE_ISNEW, action, NAME_ISNEW, Boolean.valueOf(n));
	}

	public void setAuthType(String authType) {
		int action = (authType == null) ? ACTION_REMOVE : ACTION_SET;
		addAction(TYPE_AUTHTYPE, action, NAME_AUTHTYPE, authType);
	}

	protected void addAction(int type, int action, String name, Object value) {
		DeltaRequestAttributeInfo info = null;
		if (this.actionPool.size() > 0) {
			try {
				info = actionPool.removeFirst();
			} catch (Exception x) {
				log.error("Unable to remove element:", x);
				info = new DeltaRequestAttributeInfo(type, action, name, value);
			}
			info.init(type, action, name, value);
		} else {
			info = new DeltaRequestAttributeInfo(type, action, name, value);
		}
		// if we have already done something to this attribute, make sure
		// we don't send multiple actions across the wire
		if (!recordAllActions) {
			try {
				actions.remove(info);
			} catch (java.util.NoSuchElementException x) {
				// do nothing, we wanted to remove it anyway
			}
		}
		// add the action
		actions.addLast(info);
	}

	public void execute(DeltaSession session, boolean notifyListeners) {
		if (!this.sessionId.equals(session.getId()))
			throw new java.lang.IllegalArgumentException(
					"Session id mismatch, not executing the delta request");
		session.access();
		for (int i = 0; i < actions.size(); i++) {
			DeltaRequestAttributeInfo info = actions.get(i);
			switch (info.getType()) {
			case TYPE_ATTRIBUTE: {
				if (info.getAction() == ACTION_SET) {
					if (log.isTraceEnabled())
						log.trace("Session.setAttribute('" + info.getName()
								+ "', '" + info.getValue() + "')");
					session.setAttribute(info.getName(), info.getValue(),
							notifyListeners, false);
				} else {
					if (log.isTraceEnabled())
						log.trace("Session.removeAttribute('" + info.getName()
								+ "')");
					session.removeAttribute(info.getName(), notifyListeners,
							false);
				}

				break;
			}// case
			case TYPE_ISNEW: {
				if (log.isTraceEnabled())
					log.trace("Session.setNew('" + info.getValue() + "')");
				session.setNew(((Boolean) info.getValue()).booleanValue(),
						false);
				break;
			}// case
			case TYPE_MAXINTERVAL: {
				if (log.isTraceEnabled())
					log.trace("Session.setMaxInactiveInterval('"
							+ info.getValue() + "')");
				session.setMaxInactiveInterval(
						((Integer) info.getValue()).intValue(), false);
				break;
			}// case
			case TYPE_PRINCIPAL: {
				Principal p = null;
				if (info.getAction() == ACTION_SET) {
					SerializablePrincipal sp = (SerializablePrincipal) info
							.getValue();
					p = sp.getPrincipal();
				}
				session.setPrincipal(p, false);
				break;
			}// case
			case TYPE_AUTHTYPE: {
				String authType = null;
				if (info.getAction() == ACTION_SET) {
					authType = (String) info.getValue();
				}
				session.setAuthType(authType, false);
				break;
			}// case
			default:
				throw new java.lang.IllegalArgumentException(
						"Invalid attribute info type=" + info);
			}// switch
		}// for
		session.endAccess();
		reset();
	}

	public void reset() {
		while (actions.size() > 0) {
			try {
				DeltaRequestAttributeInfo info = actions.removeFirst();
				info.recycle();
				actionPool.addLast(info);
			} catch (Exception x) {
				log.error("Unable to remove element", x);
			}
		}
		actions.clear();
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
		if (sessionId == null) {
			new Exception("Session Id is null for setSessionId")
					.fillInStackTrace().printStackTrace();
		}
	}

	public int getSize() {
		return actions.size();
	}

	public void clear() {
		actions.clear();
		actionPool.clear();
	}

	@Override
	public void readExternal(java.io.ObjectInput in) throws IOException,
			ClassNotFoundException {
		// sessionId - String
		// recordAll - boolean
		// size - int
		// AttributeInfo - in an array
		reset();
		sessionId = in.readUTF();
		recordAllActions = in.readBoolean();
		int cnt = in.readInt();
		if (actions == null)
			actions = new LinkedList<DeltaRequestAttributeInfo>();
		else
			actions.clear();
		for (int i = 0; i < cnt; i++) {
			DeltaRequestAttributeInfo info = null;
			if (this.actionPool.size() > 0) {
				try {
					info = actionPool.removeFirst();
				} catch (Exception x) {
					log.error("Unable to remove element", x);
					info = new DeltaRequestAttributeInfo();
				}
			} else {
				info = new DeltaRequestAttributeInfo();
			}
			info.readExternal(in);
			actions.addLast(info);
		}// for
	}

	@Override
	public void writeExternal(java.io.ObjectOutput out)
			throws java.io.IOException {
		// sessionId - String
		// recordAll - boolean
		// size - int
		// AttributeInfo - in an array
		out.writeUTF(getSessionId());
		out.writeBoolean(recordAllActions);
		out.writeInt(getSize());
		for (int i = 0; i < getSize(); i++) {
			DeltaRequestAttributeInfo info = actions.get(i);
			info.writeExternal(out);
		}
	}

	/**
	 * serialize DeltaRequest
	 * 
	 * @see DeltaRequest#writeExternal(java.io.ObjectOutput)
	 * 
	 * @return serialized delta request
	 * @throws IOException
	 */
	protected byte[] serialize() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		writeExternal(oos);
		oos.flush();
		oos.close();
		return bos.toByteArray();
	}
}
