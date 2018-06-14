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

package org.apache.catalina.tribes.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.tribes.Member;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public abstract class AbstractSender implements DataSender {

    private boolean connected = false;
    private int rxBufSize = 25188;
    private int txBufSize = 43800;
    private int udpRxBufSize = 25188;
    private int udpTxBufSize = 43800;
    private boolean directBuffer = false;
    private int keepAliveCount = -1;
    private int requestCount = 0;
    private long connectTime;
    private long keepAliveTime = -1;
    private long timeout = 3000;
    private Member destination;
    private InetAddress address;
    private int port;
    private int maxRetryAttempts = 1;//1 resends
    private int attempt;
    private boolean tcpNoDelay = true;
    private boolean soKeepAlive = false;
    private boolean ooBInline = true;
    private boolean soReuseAddress = true;
    private boolean soLingerOn = false;
    private int soLingerTime = 3;
    private int soTrafficClass = 0x04 | 0x08 | 0x010;
    private boolean throwOnFailedAck = true;
    private boolean udpBased = false;
    private int udpPort = -1;

    /**
     * transfers sender properties from one sender to another
     * @param from AbstractSender
     * @param to AbstractSender
     */
    public static void transferProperties(AbstractSender from, AbstractSender to) {
        to.setRxBufSizeData(from.getRxBufSizeData());
        to.setTxBufSizeData(from.getTxBufSizeData());
        to.setDirectBufferData(from.isDirectBufferData());
        to.setKeepAliveCountData(from.getKeepAliveCountData());
        to.setKeepAliveTimeData(from.getKeepAliveTimeData());
        to.setTimeoutData(from.getTimeoutData());
        to.setDestinationData(from.getDestinationData());
        to.setAddressData(from.getAddressData());
        to.setPortData(from.getPortData());
        to.setMaxRetryAttemptsData(from.getMaxRetryAttemptsData());
        to.setTcpNoDelayData(from.isTcpNoDelayData());
        to.setSoKeepAliveData(from.isSoKeepAliveData());
        to.setOoBInlineData(from.isOoBInlineData());
        to.setSoReuseAddressData(from.isSoReuseAddressData());
        to.setSoLingerOnData(from.isSoLingerOnData());
        to.setSoLingerTimeData(from.getSoLingerTimeData());
        to.setSoTrafficClassData(from.getSoTrafficClassData());
        to.setThrowOnFailedAckData(from.isThrowOnFailedAckData());
        to.setUdpBasedData(from.isUdpBasedData());
        to.setUdpPortData(from.getUdpPortData());
    }


    public AbstractSender() {

    }

    /**
     * connect
     *
     * @throws IOException
     * TODO Implement this org.apache.catalina.tribes.transport.DataSender method
     */
    @Override
    public abstract void connect() throws IOException;

    /**
     * disconnect
     *
     * TODO Implement this org.apache.catalina.tribes.transport.DataSender method
     */
    @Override
    public abstract void disconnect();

    /**
     * keepalive
     *
     * @return boolean
     * TODO Implement this org.apache.catalina.tribes.transport.DataSender method
     */
    @Override
    public boolean keepalive() {
        boolean disconnect = false;
        if (isUdpBased()) disconnect = true; //always disconnect UDP, TODO optimize the keepalive handling
        else if ( getKeepAliveCountData() >= 0 && getRequestCountData()>getKeepAliveCountData() ) disconnect = true;
        else if ( getKeepAliveTimeData() >= 0 && (System.currentTimeMillis()-getConnectTimeData())>getKeepAliveTimeData() ) disconnect = true;
        if ( disconnect ) disconnect();
        return disconnect;
    }

    protected void setConnected(boolean connected){
        this.setConnectedData(connected);
    }

    @Override
    public boolean isConnected() {
        return isConnectedData();
    }

    @Override
    public long getConnectTime() {
        return getConnectTimeData();
    }

    public Member getDestination() {
        return getDestinationData();
    }


    public int getKeepAliveCount() {
        return getKeepAliveCountData();
    }

    public long getKeepAliveTime() {
        return getKeepAliveTimeData();
    }

    @Override
    public int getRequestCount() {
        return getRequestCountData();
    }

    public int getRxBufSize() {
        return getRxBufSizeData();
    }

    public long getTimeout() {
        return getTimeoutData();
    }

    public int getTxBufSize() {
        return getTxBufSizeData();
    }

    public InetAddress getAddress() {
        return getAddressData();
    }

    public int getPort() {
        return getPortData();
    }

    public int getMaxRetryAttempts() {
        return getMaxRetryAttemptsData();
    }

    public void setDirect(boolean direct) {
        setDirectBuffer(direct);
    }

    public void setDirectBuffer(boolean directBuffer) {
        this.setDirectBufferData(directBuffer);
    }

    public boolean getDirect() {
        return getDirectBuffer();
    }

    public boolean getDirectBuffer() {
        return this.isDirectBufferData();
    }

    public int getAttempt() {
        return getAttemptData();
    }

    public boolean getTcpNoDelay() {
        return isTcpNoDelayData();
    }

    public boolean getSoKeepAlive() {
        return isSoKeepAliveData();
    }

    public boolean getOoBInline() {
        return isOoBInlineData();
    }

    public boolean getSoReuseAddress() {
        return isSoReuseAddressData();
    }

    public boolean getSoLingerOn() {
        return isSoLingerOnData();
    }

    public int getSoLingerTime() {
        return getSoLingerTimeData();
    }

    public int getSoTrafficClass() {
        return getSoTrafficClassData();
    }

    public boolean getThrowOnFailedAck() {
        return isThrowOnFailedAckData();
    }

    @Override
    public void setKeepAliveCount(int keepAliveCount) {
        this.setKeepAliveCountData(keepAliveCount);
    }

    @Override
    public void setKeepAliveTime(long keepAliveTime) {
        this.setKeepAliveTimeData(keepAliveTime);
    }

    public void setRequestCount(int requestCount) {
        this.setRequestCountData(requestCount);
    }

    @Override
    public void setRxBufSize(int rxBufSize) {
        this.setRxBufSizeData(rxBufSize);
    }

    @Override
    public void setTimeout(long timeout) {
        this.setTimeoutData(timeout);
    }

    @Override
    public void setTxBufSize(int txBufSize) {
        this.setTxBufSizeData(txBufSize);
    }

    public void setConnectTime(long connectTime) {
        this.setConnectTimeData(connectTime);
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.setMaxRetryAttemptsData(maxRetryAttempts);
    }

    public void setAttempt(int attempt) {
        this.setAttemptData(attempt);
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.setTcpNoDelayData(tcpNoDelay);
    }

    public void setSoKeepAlive(boolean soKeepAlive) {
        this.setSoKeepAliveData(soKeepAlive);
    }

    public void setOoBInline(boolean ooBInline) {
        this.setOoBInlineData(ooBInline);
    }

    public void setSoReuseAddress(boolean soReuseAddress) {
        this.setSoReuseAddressData(soReuseAddress);
    }

    public void setSoLingerOn(boolean soLingerOn) {
        this.setSoLingerOnData(soLingerOn);
    }

    public void setSoLingerTime(int soLingerTime) {
        this.setSoLingerTimeData(soLingerTime);
    }

    public void setSoTrafficClass(int soTrafficClass) {
        this.setSoTrafficClassData(soTrafficClass);
    }

    public void setThrowOnFailedAck(boolean throwOnFailedAck) {
        this.setThrowOnFailedAckData(throwOnFailedAck);
    }

    public void setDestination(Member destination) throws UnknownHostException {
        this.setDestinationData(destination);
        this.setAddressData(InetAddress.getByAddress(destination.getHost()));
        this.setPortData(destination.getPort());
        this.setUdpPortData(destination.getUdpPort());

    }

    public void setPort(int port) {
        this.setPortData(port);
    }

    public void setAddress(InetAddress address) {
        this.setAddressData(address);
    }


    public boolean isUdpBased() {
        return isUdpBasedData();
    }


    public void setUdpBased(boolean udpBased) {
        this.setUdpBasedData(udpBased);
    }


    public int getUdpPort() {
        return getUdpPortData();
    }


    public void setUdpPort(int udpPort) {
        this.setUdpPortData(udpPort);
    }


    public int getUdpRxBufSize() {
        return getUdpRxBufSizeData();
    }


    public void setUdpRxBufSize(int udpRxBufSize) {
        this.setUdpRxBufSizeData(udpRxBufSize);
    }


    public int getUdpTxBufSize() {
        return getUdpTxBufSizeData();
    }


    public void setUdpTxBufSize(int udpTxBufSize) {
        this.setUdpTxBufSizeData(udpTxBufSize);
    }


	public boolean isConnectedData() {
		return connected;
	}


	public void setConnectedData(boolean connected) {
		this.connected = connected;
	}


	public int getRxBufSizeData() {
		return rxBufSize;
	}


	public void setRxBufSizeData(int rxBufSize) {
		this.rxBufSize = rxBufSize;
	}


	public int getTxBufSizeData() {
		return txBufSize;
	}


	public void setTxBufSizeData(int txBufSize) {
		this.txBufSize = txBufSize;
	}


	public int getUdpRxBufSizeData() {
		return udpRxBufSize;
	}


	public void setUdpRxBufSizeData(int udpRxBufSize) {
		this.udpRxBufSize = udpRxBufSize;
	}


	public int getUdpTxBufSizeData() {
		return udpTxBufSize;
	}


	public void setUdpTxBufSizeData(int udpTxBufSize) {
		this.udpTxBufSize = udpTxBufSize;
	}


	private boolean isDirectBufferData() {
		return directBuffer;
	}


	private void setDirectBufferData(boolean directBuffer) {
		this.directBuffer = directBuffer;
	}


	private int getKeepAliveCountData() {
		return keepAliveCount;
	}


	private void setKeepAliveCountData(int keepAliveCount) {
		this.keepAliveCount = keepAliveCount;
	}


	private int getRequestCountData() {
		return requestCount;
	}


	private void setRequestCountData(int requestCount) {
		this.requestCount = requestCount;
	}


	public long getConnectTimeData() {
		return connectTime;
	}


	public void setConnectTimeData(long connectTime) {
		this.connectTime = connectTime;
	}


	public long getKeepAliveTimeData() {
		return keepAliveTime;
	}


	public void setKeepAliveTimeData(long keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}


	public long getTimeoutData() {
		return timeout;
	}


	public void setTimeoutData(long timeout) {
		this.timeout = timeout;
	}


	public Member getDestinationData() {
		return destination;
	}


	public void setDestinationData(Member destination) {
		this.destination = destination;
	}


	public InetAddress getAddressData() {
		return address;
	}


	public void setAddressData(InetAddress address) {
		this.address = address;
	}


	public int getPortData() {
		return port;
	}


	public void setPortData(int port) {
		this.port = port;
	}


	public int getMaxRetryAttemptsData() {
		return maxRetryAttempts;
	}


	public void setMaxRetryAttemptsData(int maxRetryAttempts) {
		this.maxRetryAttempts = maxRetryAttempts;
	}


	public int getAttemptData() {
		return attempt;
	}


	public void setAttemptData(int attempt) {
		this.attempt = attempt;
	}


	public boolean isTcpNoDelayData() {
		return tcpNoDelay;
	}


	public void setTcpNoDelayData(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}


	public boolean isSoKeepAliveData() {
		return soKeepAlive;
	}


	public void setSoKeepAliveData(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}


	public boolean isOoBInlineData() {
		return ooBInline;
	}


	public void setOoBInlineData(boolean ooBInline) {
		this.ooBInline = ooBInline;
	}


	public boolean isSoReuseAddressData() {
		return soReuseAddress;
	}


	public void setSoReuseAddressData(boolean soReuseAddress) {
		this.soReuseAddress = soReuseAddress;
	}


	public boolean isSoLingerOnData() {
		return soLingerOn;
	}


	public void setSoLingerOnData(boolean soLingerOn) {
		this.soLingerOn = soLingerOn;
	}


	public int getSoLingerTimeData() {
		return soLingerTime;
	}


	public void setSoLingerTimeData(int soLingerTime) {
		this.soLingerTime = soLingerTime;
	}


	public int getSoTrafficClassData() {
		return soTrafficClass;
	}


	public void setSoTrafficClassData(int soTrafficClass) {
		this.soTrafficClass = soTrafficClass;
	}


	public boolean isThrowOnFailedAckData() {
		return throwOnFailedAck;
	}


	public void setThrowOnFailedAckData(boolean throwOnFailedAck) {
		this.throwOnFailedAck = throwOnFailedAck;
	}


	public boolean isUdpBasedData() {
		return udpBased;
	}


	public void setUdpBasedData(boolean udpBased) {
		this.udpBased = udpBased;
	}


	public int getUdpPortData() {
		return udpPort;
	}


	public void setUdpPortData(int udpPort) {
		this.udpPort = udpPort;
	}

}