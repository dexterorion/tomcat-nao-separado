package org.apache.tomcat.util.net;

public class AprEndpointSocketList {
    private int size;
    private int pos;

    private long[] sockets;
    private int[] timeouts;
    private int[] flags;

    private AprEndpointSocketInfo info = new AprEndpointSocketInfo();

    public AprEndpointSocketList(int size) {
        this.setSizeData(0);
        setPosData(0);
        setSocketsData(new long[size]);
        setTimeoutsData(new int[size]);
        setFlagsData(new int[size]);
    }

    public int size() {
        return this.getSizeData();
    }

    public AprEndpointSocketInfo get() {
        if (getPosData() == getSizeData()) {
            return null;
        } else {
            getInfoData().setSocket(getSocketsData()[getPosData()]);
            getInfoData().setTimeout(getTimeoutsData()[getPosData()]);
            getInfoData().setFlags(getFlagsData()[getPosData()]);
            setPosData(getPosData() + 1);
            return getInfoData();
        }
    }

    public void clear() {
        setSizeData(0);
        setPosData(0);
    }

    public boolean add(long socket, int timeout, int flag) {
        if (getSizeData() == getSocketsData().length) {
            return false;
        } else {
            for (int i = 0; i < getSizeData(); i++) {
                if (getSocketsData()[i] == socket) {
                    getFlagsData()[i] = AprEndpointSocketInfo.merge(getFlagsData()[i], flag);
                    return true;
                }
            }
            getSocketsData()[getSizeData()] = socket;
            getTimeoutsData()[getSizeData()] = timeout;
            getFlagsData()[getSizeData()] = flag;
            setSizeData(getSizeData() + 1);
            return true;
        }
    }

    public boolean remove(long socket) {
        for (int i = 0; i < getSizeData(); i++) {
            if (getSocketsData()[i] == socket) {
                getSocketsData()[i] = getSocketsData()[getSizeData() - 1];
                getTimeoutsData()[i] = getTimeoutsData()[getSizeData() - 1];
                getFlagsData()[getSizeData()] = getFlagsData()[getSizeData() -1];
                setSizeData(getSizeData() - 1);
                return true;
            }
        }
        return false;
    }

    public void duplicate(AprEndpointSocketList copy) {
        copy.setSizeData(size);
        copy.setPosData(pos);
        System.arraycopy(getSocketsData(), 0, copy.getSocketsData(), 0, getSizeData());
        System.arraycopy(getTimeoutsData(), 0, copy.getTimeoutsData(), 0, getSizeData());
        System.arraycopy(getFlagsData(), 0, copy.getFlagsData(), 0, getSizeData());
    }

	public int getSizeData() {
		return size;
	}

	public void setSizeData(int size) {
		this.size = size;
	}

	public int getPosData() {
		return pos;
	}

	public void setPosData(int pos) {
		this.pos = pos;
	}

	public long[] getSocketsData() {
		return sockets;
	}

	public void setSocketsData(long[] sockets) {
		this.sockets = sockets;
	}

	public int[] getTimeoutsData() {
		return timeouts;
	}

	public void setTimeoutsData(int[] timeouts) {
		this.timeouts = timeouts;
	}

	public int[] getFlagsData() {
		return flags;
	}

	public void setFlagsData(int[] flags) {
		this.flags = flags;
	}

	public AprEndpointSocketInfo getInfoData() {
		return info;
	}

	public void setInfoData(AprEndpointSocketInfo info) {
		this.info = info;
	}

}