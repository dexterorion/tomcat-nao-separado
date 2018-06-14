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

package org.apache.catalina.tribes.group;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.apache.catalina.tribes.util.Arrays;

/**
 * <p>
 * Title:
 * </p>
 *
 * <p>
 * Description:
 * </p>
 *
 * <p>
 * Company:
 * </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class RpcMessage implements Externalizable {

	private Serializable message;
	private byte[] uuid;
	private byte[] rpcId;
	private boolean reply = false;

	public RpcMessage() {
		// for serialization
	}

	public RpcMessage(byte[] rpcId, byte[] uuid, Serializable message) {
		this.rpcId = rpcId;
		this.uuid = uuid;
		this.message = message;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		reply = in.readBoolean();
		int length = in.readInt();
		uuid = new byte[length];
		in.readFully(uuid);
		length = in.readInt();
		rpcId = new byte[length];
		in.readFully(rpcId);
		message = (Serializable) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeBoolean(reply);
		out.writeInt(uuid.length);
		out.write(uuid, 0, uuid.length);
		out.writeInt(rpcId.length);
		out.write(rpcId, 0, rpcId.length);
		out.writeObject(message);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("RpcMessage[");
		buf.append(super.toString());
		buf.append("] rpcId=");
		buf.append(Arrays.toString(rpcId));
		buf.append("; uuid=");
		buf.append(Arrays.toString(uuid));
		buf.append("; msg=");
		buf.append(message);
		return buf.toString();
	}

	public Serializable getMessage() {
		return message;
	}

	public void setMessage(Serializable message) {
		this.message = message;
	}

	public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}

	public byte[] getRpcId() {
		return rpcId;
	}

	public void setRpcId(byte[] rpcId) {
		this.rpcId = rpcId;
	}

	public boolean isReply() {
		return reply;
	}

	public void setReply(boolean reply) {
		this.reply = reply;
	}
}
