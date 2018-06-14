package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

import org.apache.catalina.Session2;

public class PersistentManagerBasePrivilegedStoreLoad implements PrivilegedExceptionAction<Session2> {

    /**
	 * 
	 */
	private final PersistentManagerBase persistentManagerBase;
	private String id;    
        
    public PersistentManagerBasePrivilegedStoreLoad(PersistentManagerBase persistentManagerBase, String id) {     
        this.persistentManagerBase = persistentManagerBase;
		this.id = id;
    }

    @Override
    public Session2 run() throws Exception{
       return this.persistentManagerBase.getStore().load(id);
    }                       
}