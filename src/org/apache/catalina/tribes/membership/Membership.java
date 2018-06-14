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

package org.apache.catalina.tribes.membership;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.catalina.tribes.Member;

/**
 * A <b>membership</b> implementation using simple multicast.
 * This is the representation of a multicast membership.
 * This class is responsible for maintaining a list of active cluster nodes in the cluster.
 * If a node fails to send out a heartbeat, the node will be dismissed.
 *
 * @author Filip Hanik
 * @author Peter Rossbach
 */
public class Membership implements Cloneable {

    private static final MemberImpl[] EMPTY_MEMBERS = new MemberImpl[0];
    
    private final Object membersLock = new Object();
    
    /**
     * The name of this membership, has to be the same as the name for the local
     * member
     */
    private MemberImpl local;
    
    /**
     * A map of all the members in the cluster.
     */
    private HashMap<MemberImpl, MembershipMbrEntry> map = new HashMap<MemberImpl, MembershipMbrEntry>();
    
    /**
     * A list of all the members in the cluster.
     */
    private MemberImpl[] members = EMPTY_MEMBERS;
    
    /**
      * sort members by alive time
      */
    private Comparator<Member> memberComparator = new MembershipMemberComparator();

    @Override
    public Object clone() {
        synchronized (membersLock) {
            Membership clone = new Membership(local, memberComparator);
            @SuppressWarnings("unchecked") // map is correct type already
            final HashMap<MemberImpl, MembershipMbrEntry> tmpclone = (HashMap<MemberImpl, MembershipMbrEntry>) getMapData().clone();
            clone.setMapData(tmpclone);
            clone.setMembersData(new MemberImpl[getMembersData().length]);
            System.arraycopy(getMembersData(),0,clone.getMembersData(),0,getMembersData().length);
            return clone;
        }
    }

    /**
     * Constructs a new membership
     * @param local - has to be the name of the local member. Used to filter the local member from the cluster membership
     * @param includeLocal - TBA
     */
    public Membership(MemberImpl local, boolean includeLocal) {
        this.local = local;
        if ( includeLocal ) addMember(local);
    }

    public Membership(MemberImpl local) {
        this(local,false);
    }

    public Membership(MemberImpl local, Comparator<Member> comp) {
        this(local,comp,false);
    }

    public Membership(MemberImpl local, Comparator<Member> comp, boolean includeLocal) {
        this(local,includeLocal);
        this.memberComparator = comp;
    }
    /**
     * Reset the membership and start over fresh.
     * Ie, delete all the members and wait for them to ping again and join this membership
     */
    public synchronized void reset() {
        getMapData().clear();
        setMembersData(EMPTY_MEMBERS) ;
    }

    /**
     * Notify the membership that this member has announced itself.
     *
     * @param member - the member that just pinged us
     * @return - true if this member is new to the cluster, false otherwise.<br/>
     * - false if this member is the local member or updated.
     */
    public synchronized boolean memberAlive(MemberImpl member) {
        boolean result = false;
        //ignore ourselves
        if (  member.equals(local) ) return result;

        //return true if the membership has changed
        MembershipMbrEntry entry = getMapData().get(member);
        if ( entry == null ) {
            entry = addMember(member);
            result = true;
       } else {
            //update the member alive time
            MemberImpl updateMember = entry.getMember() ;
            if(updateMember.getMemberAliveTime() != member.getMemberAliveTime()) {
                //update fields that can change
                updateMember.setMemberAliveTime(member.getMemberAliveTime());
                updateMember.setPayload(member.getPayload());
                updateMember.setCommand(member.getCommand());
                Arrays.sort(getMembersData(), memberComparator);
            }
        }
        entry.accessed();
        return result;
    }

    /**
     * Add a member to this component and sort array with memberComparator
     * @param member The member to add
     */
    public synchronized MembershipMbrEntry addMember(MemberImpl member) {
      synchronized (membersLock) {
          MembershipMbrEntry entry = new MembershipMbrEntry(member);
          if (!getMapData().containsKey(member) ) {
              getMapData().put(member, entry);
              MemberImpl results[] = new MemberImpl[getMembersData().length + 1];
              for (int i = 0; i < getMembersData().length; i++) results[i] = getMembersData()[i];
              results[getMembersData().length] = member;
              setMembersData(results);
              Arrays.sort(getMembersData(), memberComparator);
          }
          return entry;
      }
    }
    
    /**
     * Remove a member from this component.
     * 
     * @param member The member to remove
     */
    public void removeMember(MemberImpl member) {
        getMapData().remove(member);
        synchronized (membersLock) {
            int n = -1;
            for (int i = 0; i < getMembersData().length; i++) {
                if (getMembersData()[i] == member || getMembersData()[i].equals(member)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) return;
            MemberImpl results[] = new MemberImpl[getMembersData().length - 1];
            int j = 0;
            for (int i = 0; i < getMembersData().length; i++) {
                if (i != n)
                    results[j++] = getMembersData()[i];
            }
            setMembersData(results);
        }
    }
    
    /**
     * Runs a refresh cycle and returns a list of members that has expired.
     * This also removes the members from the membership, in such a way that
     * getMembers() = getMembers() - expire()
     * @param maxtime - the max time a member can remain unannounced before it is considered dead.
     * @return the list of expired members
     */
    public synchronized MemberImpl[] expire(long maxtime) {
        if(!hasMembers() )
           return EMPTY_MEMBERS;
       
        ArrayList<MemberImpl> list = null;
        Iterator<MembershipMbrEntry> i = getMapData().values().iterator();
        while(i.hasNext()) {
            MembershipMbrEntry entry = i.next();
            if( entry.hasExpired(maxtime) ) {
                if(list == null) // only need a list when members are expired (smaller gc)
                    list = new java.util.ArrayList<MemberImpl>();
                list.add(entry.getMember());
            }
        }
        
        if(list != null) {
            MemberImpl[] result = new MemberImpl[list.size()];
            list.toArray(result);
            for( int j=0; j<result.length; j++) {
                removeMember(result[j]);
            }
            return result;
        } else {
            return EMPTY_MEMBERS ;
        }
    }

    /**
     * Returning that service has members or not
     */
    public boolean hasMembers() {
        return getMembersData().length > 0 ;
    }
    
    
    public MemberImpl getMember(Member mbr) {
        if(hasMembers()) {
            MemberImpl result = null;
            for ( int i=0; i<this.getMembersData().length && result==null; i++ ) {
                if ( getMembersData()[i].equals(mbr) ) result = getMembersData()[i];
            }//for
            return result;
        } else {
            return null;
        }
    }
    
    public boolean contains(Member mbr) { 
        return getMember(mbr)!=null;
    }
 
    /**
     * Returning a list of all the members in the membership
     * We not need a copy: add and remove generate new arrays.
     */
    public MemberImpl[] getMembers() {
        if(hasMembers()) {
            return getMembersData();
        } else {
            return EMPTY_MEMBERS;
        }
    }

    /**
     * get a copy from all member entries
     */
    protected synchronized MembershipMbrEntry[] getMemberEntries()
    {
        MembershipMbrEntry[] result = new MembershipMbrEntry[getMapData().size()];
        Iterator<Map.Entry<MemberImpl,MembershipMbrEntry>> i = getMapData().entrySet().iterator();
        int pos = 0;
        while ( i.hasNext() )
            result[pos++] = i.next().getValue();
        return result;
    }

	public MemberImpl[] getMembersData() {
		return members;
	}

	public void setMembersData(MemberImpl[] members) {
		this.members = members;
	}

	public HashMap<MemberImpl, MembershipMbrEntry> getMapData() {
		return map;
	}

	public void setMapData(HashMap<MemberImpl, MembershipMbrEntry> map) {
		this.map = map;
	}
}
