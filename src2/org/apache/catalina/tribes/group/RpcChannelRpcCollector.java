package org.apache.catalina.tribes.group;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.catalina.tribes.Member;

/**
 * 
 * Class that holds all response.
 * 
 * @author not attributable
 * @version 1.0
 */
public class RpcChannelRpcCollector {
	private ArrayList<Response2> responses = new ArrayList<Response2>();
	private RpcChannelRpcCollectorKey key;
	private int options;
	private int destcnt;
	/**
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	public long timeout;

	/**
	 * @deprecated Use
	 *             {@link RpcChannel.RpcCollector#RpcChannel.RpcCollector(RpcChannel.RpcCollectorKey, int, int)}
	 */
	@Deprecated
	public RpcChannelRpcCollector(RpcChannelRpcCollectorKey key, int options,
			int destcnt, long timeout) {
		this.setKeyData(key);
		this.options = options;
		this.destcnt = destcnt;
		this.timeout = timeout;
	}

	public RpcChannelRpcCollector(RpcChannelRpcCollectorKey key, int options,
			int destcnt) {
		this(key, options, destcnt, 0);
	}

	public void addResponse(Serializable message, Member sender) {
		Response2 resp = new Response2(sender, message);
		responses.add(resp);
	}

	public boolean isComplete() {
		if (destcnt <= 0)
			return true;
		switch (options) {
		case 3:
			return destcnt == responses.size();
		case 2: {
			float perc = ((float) responses.size()) / ((float) destcnt);
			return perc >= 0.50f;
		}
		case 1:
			return responses.size() > 0;
		default:
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getKeyData().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RpcChannelRpcCollector) {
			RpcChannelRpcCollector r = (RpcChannelRpcCollector) o;
			return r.getKeyData().equals(this.getKeyData());
		} else
			return false;
	}

	public Response2[] getResponses() {
		return responses.toArray(new Response2[responses.size()]);
	}

	public RpcChannelRpcCollectorKey getKey() {
		return getKeyData();
	}

	public void setKey(RpcChannelRpcCollectorKey key) {
		this.setKeyData(key);
	}

	public int getOptions() {
		return options;
	}

	public void setOptions(int options) {
		this.options = options;
	}

	public int getDestcnt() {
		return destcnt;
	}

	public void setDestcnt(int destcnt) {
		this.destcnt = destcnt;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setResponses(ArrayList<Response2> responses) {
		this.responses = responses;
	}

	public RpcChannelRpcCollectorKey getKeyData() {
		return key;
	}

	public void setKeyData(RpcChannelRpcCollectorKey key) {
		this.key = key;
	}

}