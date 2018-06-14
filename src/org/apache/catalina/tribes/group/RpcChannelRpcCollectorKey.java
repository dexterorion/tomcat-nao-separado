package org.apache.catalina.tribes.group;

import java.util.Arrays;

public class RpcChannelRpcCollectorKey {
	private byte[] id;

	public RpcChannelRpcCollectorKey(byte[] id) {
		this.setIdData(id);
	}

	@Override
	public int hashCode() {
		return getIdData()[0] + getIdData()[1] + getIdData()[2] + getIdData()[3];
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RpcChannelRpcCollectorKey) {
			RpcChannelRpcCollectorKey r = (RpcChannelRpcCollectorKey) o;
			return Arrays.equals(getIdData(), r.getIdData());
		} else
			return false;
	}

	public byte[] getId() {
		return getIdData();
	}

	public void setId(byte[] id) {
		this.setIdData(id);
	}

	public byte[] getIdData() {
		return id;
	}

	public void setIdData(byte[] id) {
		this.id = id;
	}

}