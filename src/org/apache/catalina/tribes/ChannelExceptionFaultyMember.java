package org.apache.catalina.tribes;

/**
 * 
 * <p>Title: FaultyMember class</p> 
 * 
 * <p>Description: Represent a failure to a specific member when a message was sent
 * to more than one member</p> 
 * 
 * @author Filip Hanik
 * @version 1.0
 */
public class ChannelExceptionFaultyMember {
    private Exception cause;
    private Member member;
    public ChannelExceptionFaultyMember(Member mbr, Exception x) { 
        this.setMemberData(mbr);
        this.cause = x;
    }
    
    public Member getMember() {
        return getMemberData();
    }
    
    public Exception getCause() {
        return cause;
    }
    
    @Override
    public String toString() {
        return "FaultyMember:"+getMemberData().toString();
    }
    
    @Override
    public int hashCode() {
        return (getMemberData()!=null)?getMemberData().hashCode():0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (getMemberData()==null || (!(o instanceof ChannelExceptionFaultyMember)) || (((ChannelExceptionFaultyMember)o).getMemberData()==null)) return false;
        return getMemberData().equals(((ChannelExceptionFaultyMember)o).getMemberData());
    }

	public Member getMemberData() {
		return member;
	}

	public void setMemberData(Member member) {
		this.member = member;
	}
}