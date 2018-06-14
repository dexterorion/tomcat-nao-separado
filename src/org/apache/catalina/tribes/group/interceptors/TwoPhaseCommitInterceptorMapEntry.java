package org.apache.catalina.tribes.group.interceptors;

import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.UniqueId;

public class TwoPhaseCommitInterceptorMapEntry {
    private ChannelMessage msg;
    private UniqueId id;
    private long timestamp;

    public TwoPhaseCommitInterceptorMapEntry(ChannelMessage msg, UniqueId id, long timestamp) {
        this.setMsg(msg);
        this.setId(id);
        this.timestamp = timestamp;
    }
    public boolean expired(long now, long expiration) {
        return (now - timestamp ) > expiration;
    }
	public ChannelMessage getMsg() {
		return msg;
	}
	public void setMsg(ChannelMessage msg) {
		this.msg = msg;
	}
	public UniqueId getId() {
		return id;
	}
	public void setId(UniqueId id) {
		this.id = id;
	}

}