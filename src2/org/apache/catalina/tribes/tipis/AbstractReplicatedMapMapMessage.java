package org.apache.catalina.tribes.tipis;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.catalina.tribes.membership.MemberImpl;

public class AbstractReplicatedMapMapMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int MSG_BACKUP = 1;
	private static final int MSG_RETRIEVE_BACKUP = 2;
	private static final int MSG_PROXY = 3;
	private static final int MSG_REMOVE = 4;
	private static final int MSG_STATE = 5;
	private static final int MSG_START = 6;
	private static final int MSG_STOP = 7;
	private static final int MSG_INIT = 8;
	private static final int MSG_COPY = 9;
	private static final int MSG_STATE_COPY = 10;
	private static final int MSG_ACCESS = 11;

	private byte[] mapId;
	private int msgtype;
	private boolean diff;
	private transient Serializable key;
	private transient Serializable value;
	private byte[] valuedata;
	private byte[] keydata;
	private byte[] diffvalue;
	private Member[] nodes;
	private Member primary;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(
				"AbstractReplicateMapMapMessage[context=");
		buf.append(new String(mapId));
		buf.append("; type=");
		buf.append(getTypeDesc());
		buf.append("; key=");
		buf.append(key);
		buf.append("; value=");
		buf.append(value);
		return buf.toString();
	}

	public String getTypeDesc() {
		switch (msgtype) {
		case MSG_BACKUP:
			return "MSG_BACKUP";
		case MSG_RETRIEVE_BACKUP:
			return "MSG_RETRIEVE_BACKUP";
		case MSG_PROXY:
			return "MSG_PROXY";
		case MSG_REMOVE:
			return "MSG_REMOVE";
		case MSG_STATE:
			return "MSG_STATE";
		case MSG_START:
			return "MSG_START";
		case MSG_STOP:
			return "MSG_STOP";
		case MSG_INIT:
			return "MSG_INIT";
		case MSG_STATE_COPY:
			return "MSG_STATE_COPY";
		case MSG_COPY:
			return "MSG_COPY";
		case MSG_ACCESS:
			return "MSG_ACCESS";
		default:
			return "UNKNOWN";
		}
	}

	/**
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	public AbstractReplicatedMapMapMessage() {
	}

	public AbstractReplicatedMapMapMessage(byte[] mapId, int msgtype,
			boolean diff, Serializable key, Serializable value,
			byte[] diffvalue, Member primary, Member[] nodes) {
		this.mapId = mapId;
		this.msgtype = msgtype;
		this.diff = diff;
		this.key = key;
		this.value = value;
		this.diffvalue = diffvalue;
		this.nodes = nodes;
		this.primary = primary;
		setValue(value);
		setKey(key);
	}

	public void deserialize(ClassLoader[] cls) throws IOException,
			ClassNotFoundException {
		key(cls);
		value(cls);
	}

	public int getMsgType() {
		return msgtype;
	}

	public boolean isDiff() {
		return diff;
	}

	public Serializable getKey() {
		try {
			return key(null);
		} catch (Exception x) {
			throw new RuntimeException(
					"Deserialization error of the AbstractReplicateMapMapMessage.key",
					x);
		}
	}

	public Serializable key(ClassLoader[] cls) throws IOException,
			ClassNotFoundException {
		if (key != null)
			return key;
		if (getKeydataData() == null || getKeydataData().length == 0)
			return null;
		key = XByteBuffer.deserialize(getKeydataData(), 0, getKeydataData().length, cls);
		setKeydataData(null);
		return key;
	}

	public byte[] getKeyData() {
		return getKeydataData();
	}

	public Serializable getValue() {
		try {
			return value(null);
		} catch (Exception x) {
			throw new RuntimeException(
					"Deserialization error of the AbstractReplicateMapMapMessage.value",
					x);
		}
	}

	public Serializable value(ClassLoader[] cls) throws IOException,
			ClassNotFoundException {
		if (value != null)
			return value;
		if (getValuedataData() == null || getValuedataData().length == 0)
			return null;
		value = XByteBuffer.deserialize(getValuedataData(), 0, getValuedataData().length, cls);
		setValuedataData(null);
		return value;
	}

	public byte[] getValueData() {
		return getValuedataData();
	}

	public byte[] getDiffValue() {
		return diffvalue;
	}

	public Member[] getBackupNodes() {
		return nodes;
	}

	public Member getPrimary() {
		return primary;
	}

	void setPrimary(Member m) {
		primary = m;
	}

	public byte[] getMapId() {
		return mapId;
	}

	public void setValue(Serializable value) {
		try {
			if (value != null)
				setValuedataData(XByteBuffer.serialize(value));
			this.value = value;
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	public void setKey(Serializable key) {
		try {
			if (key != null)
				setKeydataData(XByteBuffer.serialize(key));
			this.key = key;
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	protected Member[] readMembers(ObjectInput in) throws IOException {
		int nodecount = in.readInt();
		Member[] members = new Member[nodecount];
		for (int i = 0; i < members.length; i++) {
			byte[] d = new byte[in.readInt()];
			in.readFully(d);
			if (d.length > 0)
				members[i] = MemberImpl.getMember(d);
		}
		return members;
	}

	/**
	 * @deprecated Unused - will be removed in 8.0.x
	 */
	@Deprecated
	protected void writeMembers(ObjectOutput out, Member[] members)
			throws IOException {
		if (members == null)
			members = new Member[0];
		out.writeInt(members.length);
		for (int i = 0; i < members.length; i++) {
			if (members[i] != null) {
				byte[] d = members[i] != null ? ((MemberImpl) members[i])
						.getData(false) : new byte[0];
				out.writeInt(d.length);
				out.write(d);
			}
		}
	}

	/**
	 * shallow clone
	 * 
	 * @return Object
	 */
	@Override
	public Object clone() {
		AbstractReplicatedMapMapMessage msg = new AbstractReplicatedMapMapMessage(
				this.mapId, this.msgtype, this.diff, this.key, this.value,
				this.diffvalue, this.primary, this.nodes);
		msg.setKeydataData(this.getKeydataData());
		msg.setValuedataData(this.getValuedataData());
		return msg;
	}

	public int getMsgtype() {
		return msgtype;
	}

	public void setMsgtype(int msgtype) {
		this.msgtype = msgtype;
	}

	public byte[] getValuedata() {
		return getValuedataData();
	}

	public void setValuedata(byte[] valuedata) {
		this.setValuedataData(valuedata);
	}

	public byte[] getKeydata() {
		return getKeydataData();
	}

	public void setKeydata(byte[] keydata) {
		this.setKeydataData(keydata);
	}

	public byte[] getDiffvalue() {
		return diffvalue;
	}

	public void setDiffvalue(byte[] diffvalue) {
		this.diffvalue = diffvalue;
	}

	public Member[] getNodes() {
		return nodes;
	}

	public void setNodes(Member[] nodes) {
		this.nodes = nodes;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public static int getMsgBackup() {
		return MSG_BACKUP;
	}

	public static int getMsgRetrieveBackup() {
		return MSG_RETRIEVE_BACKUP;
	}

	public static int getMsgProxy() {
		return MSG_PROXY;
	}

	public static int getMsgRemove() {
		return MSG_REMOVE;
	}

	public static int getMsgState() {
		return MSG_STATE;
	}

	public static int getMsgStart() {
		return MSG_START;
	}

	public static int getMsgStop() {
		return MSG_STOP;
	}

	public static int getMsgInit() {
		return MSG_INIT;
	}

	public static int getMsgCopy() {
		return MSG_COPY;
	}

	public static int getMsgStateCopy() {
		return MSG_STATE_COPY;
	}

	public static int getMsgAccess() {
		return MSG_ACCESS;
	}

	public void setMapId(byte[] mapId) {
		this.mapId = mapId;
	}

	public void setDiff(boolean diff) {
		this.diff = diff;
	}

	public byte[] getKeydataData() {
		return keydata;
	}

	public void setKeydataData(byte[] keydata) {
		this.keydata = keydata;
	}

	public byte[] getValuedataData() {
		return valuedata;
	}

	public void setValuedataData(byte[] valuedata) {
		this.valuedata = valuedata;
	}

}