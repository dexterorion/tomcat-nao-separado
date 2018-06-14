package org.apache.catalina.tribes.group;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.catalina.tribes.Member;

public class AbsoluteOrderAbsoluteComparator implements Comparator<Member>,
        Serializable {
    
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Member m1, Member m2) {
        int result = compareIps(m1,m2);
        if ( result == 0 ) result = comparePorts(m1,m2);
        if ( result == 0 ) result = compareIds(m1,m2);
        return result;
    }
    
    public int compareIps(Member m1, Member m2) {
        return compareBytes(m1.getHost(),m2.getHost());
    }
    
    public int comparePorts(Member m1, Member m2) {
        return compareInts(m1.getPort(),m2.getPort());
    }
    
    public int compareIds(Member m1, Member m2) {
        return compareBytes(m1.getUniqueId(),m2.getUniqueId());
    }
    
    protected int compareBytes(byte[] d1, byte[] d2) {
        int result = 0;
        if ( d1.length == d2.length ) {
            for (int i=0; (result==0) && (i<d1.length); i++) {
                result = compareBytes(d1[i],d2[i]);
            }
        } else if ( d1.length < d2.length) {
            result = -1;
        } else {
            result = 1;
        }
        return result;
    }
    
    protected int compareBytes(byte b1, byte b2) {
        return compareInts(b1,b2);
    }
    
    protected int compareInts(int b1, int b2) {
        int result = 0;
        if ( b1 == b2 ) {

        } else if ( b1 < b2) {
            result = -1;
        } else {
            result = 1;
        }
        return result;
    }
}