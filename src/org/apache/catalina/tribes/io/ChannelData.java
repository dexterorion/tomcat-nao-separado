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
package org.apache.catalina.tribes.io;

import java.sql.Timestamp;
import java.util.Arrays;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.catalina.tribes.util.UUIDGenerator;

/**
 * The <code>ChannelData</code> object is used to transfer a message through the 
 * channel interceptor stack and eventually out on a transport to be sent 
 * to another node. While the message is being processed by the different 
 * interceptors, the message data can be manipulated as each interceptor seems appropriate.
 * @author Peter Rossbach
 * @author Filip Hanik
 */
public class ChannelData implements ChannelMessage {
    private static final long serialVersionUID = 1L;

    private static final ChannelData[] EMPTY_DATA_ARRAY = new ChannelData[0];
    
    private static volatile boolean USE_SECURE_RANDOM_FOR_UUID = false;
    
    /**
     * The options this message was sent with
     */
    private int options = 0 ;
    /**
     * The message data, stored in a dynamic buffer
     */
    private XByteBuffer message ;
    /**
     * The timestamp that goes with this message
     */
    private long timestamp ;
    /**
     * A unique message id
     */
    private byte[] uniqueId ;
    /**
     * The source or reply-to address for this message
     */
    private Member address;

    /**
     * Creates an empty channel data with a new unique Id
     * @see #ChannelData(boolean)
     */
    public ChannelData() {
        this(true);
    }
    
    /**
     * Create an empty channel data object
     * @param generateUUID boolean - if true, a unique Id will be generated
     */
    public ChannelData(boolean generateUUID) {
        if ( generateUUID ) generateUUID();
    }


    /**
     * Creates a new channel data object with data
     * @param uniqueId - unique message id
     * @param message - message data
     * @param timestamp - message timestamp
     */
    public ChannelData(byte[] uniqueId, XByteBuffer message, long timestamp) {
        this.setUniqueIdData(uniqueId);
        this.setMessageData(message);
        this.setTimestampData(timestamp);
    }
    
    /**
     * @return Returns the message byte buffer
     */
    @Override
    public XByteBuffer getMessage() {
        return getMessageData();
    }
    /**
     * @param message The message to send.
     */
    @Override
    public void setMessage(XByteBuffer message) {
        this.setMessageData(message);
    }
    /**
     * @return Returns the timestamp.
     */
    @Override
    public long getTimestamp() {
        return getTimestampData();
    }
    /**
     * @param timestamp The timestamp to send
     */
    @Override
    public void setTimestamp(long timestamp) {
        this.setTimestampData(timestamp);
    }
    /**
     * @return Returns the uniqueId.
     */
    @Override
    public byte[] getUniqueId() {
        return getUniqueIdData();
    }
    /**
     * @param uniqueId The uniqueId to send.
     */
    public void setUniqueId(byte[] uniqueId) {
        this.setUniqueIdData(uniqueId);
    }
    /**
     * @return returns the message options 
     * see org.apache.catalina.tribes.Channel#sendMessage(org.apache.catalina.tribes.Member[], java.io.Serializable, int)
     *                                                 
     */
    @Override
    public int getOptions() {
        return getOptionsData();
    }
    /**
     * Sets the message options.
     * 
     * @param options the message options
     */
    @Override
    public void setOptions(int options) {
        this.setOptionsData(options);
    }
    
    /**
     * Returns the source or reply-to address
     * @return Member
     */
    @Override
    public Member getAddress() {
        return getAddressData();
    }

    /**
     * Sets the source or reply-to address
     * @param address Member
     */
    @Override
    public void setAddress(Member address) {
        this.setAddressData(address);
    }
    
    /**
     * Generates a UUID and invokes setUniqueId
     */
    public void generateUUID() {
        byte[] data = new byte[16];
        UUIDGenerator.randomUUID(isUSE_SECURE_RANDOM_FOR_UUIDData(),data,0);
        setUniqueId(data);
    }

    public int getDataPackageLength() {
        int length = 
            4 + //options
            8 + //timestamp  off=4
            4 + //unique id length off=12
            getUniqueIdData().length+ //id data off=12+uniqueId.length
            4 + //addr length off=12+uniqueId.length+4
            ((MemberImpl)getAddressData()).getDataLength()+ //member data off=12+uniqueId.length+4+add.length
            4 + //message length off=12+uniqueId.length+4+add.length+4
            getMessageData().getLength();
        return length;

    }
    
    /**
     * Serializes the ChannelData object into a byte[] array
     * @return byte[]
     */
    public byte[] getDataPackage()  {
        int length = getDataPackageLength();
        byte[] data = new byte[length];
        int offset = 0;
        return getDataPackage(data,offset);
    }

    public byte[] getDataPackage(byte[] data, int offset)  {
        byte[] addr = ((MemberImpl)getAddressData()).getData(false);
        XByteBuffer.toBytes(getOptionsData(),data,offset);
        offset += 4; //options
        XByteBuffer.toBytes(getTimestampData(),data,offset);
        offset += 8; //timestamp
        XByteBuffer.toBytes(getUniqueIdData().length,data,offset);
        offset += 4; //uniqueId.length
        System.arraycopy(getUniqueIdData(),0,data,offset,getUniqueIdData().length);
        offset += getUniqueIdData().length; //uniqueId data
        XByteBuffer.toBytes(addr.length,data,offset);
        offset += 4; //addr.length
        System.arraycopy(addr,0,data,offset,addr.length);
        offset += addr.length; //addr data
        XByteBuffer.toBytes(getMessageData().getLength(),data,offset);
        offset += 4; //message.length
        System.arraycopy(getMessageData().getBytesDirect(),0,data,offset,getMessageData().getLength());
        offset += getMessageData().getLength(); //message data
        return data;
    }
    
    /**
     * Deserializes a ChannelData object from a byte array
     * @param xbuf byte[]
     * @return ChannelData
     */
    public static ChannelData getDataFromPackage(XByteBuffer xbuf)  {
        ChannelData data = new ChannelData(false);
        int offset = 0;
        data.setOptions(XByteBuffer.toInt(xbuf.getBytesDirect(),offset));
        offset += 4; //options
        data.setTimestamp(XByteBuffer.toLong(xbuf.getBytesDirect(),offset));
        offset += 8; //timestamp
        data.setUniqueIdData(new byte[XByteBuffer.toInt(xbuf.getBytesDirect(),offset)]);
        offset += 4; //uniqueId length
        System.arraycopy(xbuf.getBytesDirect(),offset,data.getUniqueIdData(),0,data.getUniqueIdData().length);
        offset += data.getUniqueIdData().length; //uniqueId data
        //byte[] addr = new byte[XByteBuffer.toInt(xbuf.getBytesDirect(),offset)];
        int addrlen = XByteBuffer.toInt(xbuf.getBytesDirect(),offset);
        offset += 4; //addr length
        //System.arraycopy(xbuf.getBytesDirect(),offset,addr,0,addr.length);
        data.setAddress(MemberImpl.getMember(xbuf.getBytesDirect(),offset,addrlen));
        //offset += addr.length; //addr data
        offset += addrlen;
        int xsize = XByteBuffer.toInt(xbuf.getBytesDirect(),offset);
        offset += 4; //xsize length
        System.arraycopy(xbuf.getBytesDirect(),offset,xbuf.getBytesDirect(),0,xsize);
        xbuf.setLength(xsize);
        data.setMessageData(xbuf);
        return data;

    }

    public static ChannelData getDataFromPackage(byte[] b)  {
        ChannelData data = new ChannelData(false);
        int offset = 0;
        data.setOptions(XByteBuffer.toInt(b,offset));
        offset += 4; //options
        data.setTimestamp(XByteBuffer.toLong(b,offset));
        offset += 8; //timestamp
        data.setUniqueIdData(new byte[XByteBuffer.toInt(b,offset)]);
        offset += 4; //uniqueId length
        System.arraycopy(b,offset,data.getUniqueIdData(),0,data.getUniqueIdData().length);
        offset += data.getUniqueIdData().length; //uniqueId data
        byte[] addr = new byte[XByteBuffer.toInt(b,offset)];
        offset += 4; //addr length
        System.arraycopy(b,offset,addr,0,addr.length);
        data.setAddress(MemberImpl.getMember(addr));
        offset += addr.length; //addr data
        int xsize = XByteBuffer.toInt(b,offset);
        //data.message = new XByteBuffer(new byte[xsize],false);
        data.setMessageData(BufferPool.getBufferPool().getBuffer(xsize,false));
        offset += 4; //message length
        System.arraycopy(b,offset,data.getMessageData().getBytesDirect(),0,xsize);
        data.getMessageData().append(b,offset,xsize);
        offset += xsize; //message data
        return data;
    }
    
    @Override
    public int hashCode() {
        return XByteBuffer.toInt(getUniqueId(),0);
    }
    
    /**
     * Compares to ChannelData objects, only compares on getUniqueId().equals(o.getUniqueId())
     * @param o Object
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if ( o instanceof ChannelData ) {
            return Arrays.equals(getUniqueId(),((ChannelData)o).getUniqueId());
        } else return false;
    }
    
    /**
     * Create a shallow clone, only the data gets recreated
     * @return ClusterData
     */
    @Override
    public Object clone() {
//        byte[] d = this.getDataPackage();
//        return ClusterData.getDataFromPackage(d);
        ChannelData clone = new ChannelData(false);
        clone.setOptionsData(this.getOptionsData());
        clone.setMessageData(new XByteBuffer(this.getMessageData().getBytesDirect(),false));
        clone.setTimestampData(this.getTimestampData());
        clone.setUniqueIdData(this.getUniqueIdData());
        clone.setAddressData(this.getAddressData());
        return clone;
    }
    
    /**
     * Complete clone
     * @return ClusterData
     */
    @Override
    public Object deepclone() {
        byte[] d = this.getDataPackage();
        return ChannelData.getDataFromPackage(d);
    }
    
    /**
     * Utility method, returns true if the options flag indicates that an ack
     * is to be sent after the message has been received and processed
     * @param options int - the options for the message
     * @return boolean 
     * @see org.apache.catalina.tribes.Channel#SEND_OPTIONS_USE_ACK
     * @see org.apache.catalina.tribes.Channel#SEND_OPTIONS_SYNCHRONIZED_ACK
     */
    public static boolean sendAckSync(int options) {
        return ( (Channel.SEND_OPTIONS_USE_ACK & options) == Channel.SEND_OPTIONS_USE_ACK) &&
            ( (Channel.SEND_OPTIONS_SYNCHRONIZED_ACK & options) == Channel.SEND_OPTIONS_SYNCHRONIZED_ACK);
    }


    /**
     * Utility method, returns true if the options flag indicates that an ack
     * is to be sent after the message has been received but not yet processed
     * @param options int - the options for the message
     * @return boolean 
     * @see org.apache.catalina.tribes.Channel#SEND_OPTIONS_USE_ACK
     * @see org.apache.catalina.tribes.Channel#SEND_OPTIONS_SYNCHRONIZED_ACK
     */
    public static boolean sendAckAsync(int options) {
        return ( (Channel.SEND_OPTIONS_USE_ACK & options) == Channel.SEND_OPTIONS_USE_ACK) &&
            ( (Channel.SEND_OPTIONS_SYNCHRONIZED_ACK & options) != Channel.SEND_OPTIONS_SYNCHRONIZED_ACK);
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("ClusterData[src=");
        buf.append(getAddress()).append("; id=");
        buf.append(bToS(getUniqueId())).append("; sent=");
        buf.append(new Timestamp(this.getTimestamp()).toString()).append("]");
        return buf.toString();
    }
    
    public static String bToS(byte[] data) {
        StringBuilder buf = new StringBuilder(4*16);
        buf.append("{");
        for (int i=0; data!=null && i<data.length; i++ ) buf.append(String.valueOf(data[i])).append(" ");
        buf.append("}");
        return buf.toString();
    }

	public static ChannelData[] getEmptyDataArray() {
		return EMPTY_DATA_ARRAY;
	}

	public static boolean isUSE_SECURE_RANDOM_FOR_UUIDData() {
		return USE_SECURE_RANDOM_FOR_UUID;
	}

	public static void setUSE_SECURE_RANDOM_FOR_UUIDData(
			boolean uSE_SECURE_RANDOM_FOR_UUID) {
		USE_SECURE_RANDOM_FOR_UUID = uSE_SECURE_RANDOM_FOR_UUID;
	}

	public int getOptionsData() {
		return options;
	}

	public void setOptionsData(int options) {
		this.options = options;
	}

	public XByteBuffer getMessageData() {
		return message;
	}

	public void setMessageData(XByteBuffer message) {
		this.message = message;
	}

	public long getTimestampData() {
		return timestamp;
	}

	public void setTimestampData(long timestamp) {
		this.timestamp = timestamp;
	}

	public byte[] getUniqueIdData() {
		return uniqueId;
	}

	public void setUniqueIdData(byte[] uniqueId) {
		this.uniqueId = uniqueId;
	}

	public Member getAddressData() {
		return address;
	}

	public void setAddressData(Member address) {
		this.address = address;
	}

    
}
