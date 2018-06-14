package org.apache.catalina.tribes.group;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RcpMessageNoRpcChannelReply extends RpcMessage {
	public RcpMessageNoRpcChannelReply() {

	}

	public RcpMessageNoRpcChannelReply(byte[] rpcid, byte[] uuid) {
		super(rpcid, uuid, null);
		setReply(true);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		setReply(true);
		int length = in.readInt();
		setUuid(new byte[length]);
		in.readFully(getUuid());
		length = in.readInt();
		setRpcId(new byte[length]);
		in.readFully(getRpcId());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(getUuid().length);
		out.write(getUuid(), 0, getUuid().length);
		out.writeInt(getRpcId().length);
		out.write(getRpcId(), 0, getRpcId().length);
	}
}