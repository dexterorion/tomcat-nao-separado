package org.apache.catalina.tribes.membership;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.catalina.tribes.Member;

public class MembershipMemberComparator implements Comparator<Member>,
        Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Member m1, Member m2) {
        //longer alive time, means sort first
        long result = m2.getMemberAliveTime() - m1.getMemberAliveTime();
        if (result < 0)
            return -1;
        else if (result == 0)
            return 0;
        else
            return 1;
    }
}