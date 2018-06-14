package org.apache.catalina.tribes.membership;

public class MembershipMbrEntry
{

    private MemberImpl mbr;
    private long lastHeardFrom;

    public MembershipMbrEntry(MemberImpl mbr) {
       this.mbr = mbr;
    }

    /**
     * Indicate that this member has been accessed.
     */
    public void accessed(){
       lastHeardFrom = System.currentTimeMillis();
    }

    /**
     * Return the actual Member object
     */
    public MemberImpl getMember() {
        return mbr;
    }

    /**
     * Check if this dude has expired
     * @param maxtime The time threshold
     */
    public boolean hasExpired(long maxtime) {
        long delta = System.currentTimeMillis() - lastHeardFrom;
        return delta > maxtime;
    }
}