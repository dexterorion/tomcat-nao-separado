package org.apache.catalina.tribes.group.interceptors;

import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.io.XByteBuffer;

public class FragmentationInterceptorFragCollection {
    private long received = System.currentTimeMillis();
    private ChannelMessage msg;
    private XByteBuffer[] frags;
    public FragmentationInterceptorFragCollection(ChannelMessage msg) {
        //get the total messages
        int count = XByteBuffer.toInt(msg.getMessage().getBytesDirect(),msg.getMessage().getLength()-4);
        frags = new XByteBuffer[count];
        this.msg = msg;
    }
    
    public void addMessage(ChannelMessage msg) {
        //remove the total messages
        msg.getMessage().trim(4);
        //get the msg nr
        int nr = XByteBuffer.toInt(msg.getMessage().getBytesDirect(),msg.getMessage().getLength()-4);
        //remove the msg nr
        msg.getMessage().trim(4);
        frags[nr] = msg.getMessage();
        
    }
    
    public boolean complete() {
        boolean result = true;
        for ( int i=0; (i<frags.length) && (result); i++ ) result = (frags[i] != null);
        return result;
    }
    
    public ChannelMessage assemble() {
        if ( !complete() ) throw new IllegalStateException("Fragments are missing.");
        int buffersize = 0;
        for (int i=0; i<frags.length; i++ ) buffersize += frags[i].getLength();
        XByteBuffer buf = new XByteBuffer(buffersize,false);
        msg.setMessage(buf);
        for ( int i=0; i<frags.length; i++ ) {
            msg.getMessage().append(frags[i].getBytesDirect(),0,frags[i].getLength());
        }
        return msg;
    }
    
    public boolean expired(long expire) {
        return (System.currentTimeMillis()-received)>expire;
    }


}