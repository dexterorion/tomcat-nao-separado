package org.apache.catalina.tribes.group.interceptors;

import java.util.Arrays;

import org.apache.catalina.tribes.io.XByteBuffer;

public class FragmentationInterceptorFragKey {
    private byte[] uniqueId;
    private long received = System.currentTimeMillis();
    public FragmentationInterceptorFragKey(byte[] id ) {
        this.setUniqueIdData(id);
    }
    @Override
    public int hashCode() {
        return XByteBuffer.toInt(getUniqueIdData(),0);
    }
    
    @Override
    public boolean equals(Object o ) {
        if ( o instanceof FragmentationInterceptorFragKey ) {
        return Arrays.equals(getUniqueIdData(),((FragmentationInterceptorFragKey)o).getUniqueIdData());
    } else return false;

    }
    
    public boolean expired(long expire) {
        return (System.currentTimeMillis()-getReceivedData())>expire;
    }
	public byte[] getUniqueIdData() {
		return uniqueId;
	}
	public void setUniqueIdData(byte[] uniqueId) {
		this.uniqueId = uniqueId;
	}
	public long getReceivedData() {
		return received;
	}
	public void setReceivedData(long received) {
		this.received = received;
	}

}