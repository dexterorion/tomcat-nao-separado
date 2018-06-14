package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

import org.apache.catalina.Session2;

public class PersistentManagerBasePrivilegedStoreSave implements PrivilegedExceptionAction<Void> {

    /**
	 * 
	 */
	private final PersistentManagerBase persistentManagerBase;
	private Session2 session;    
        
    public PersistentManagerBasePrivilegedStoreSave(PersistentManagerBase persistentManagerBase, Session2 session) {     
        this.persistentManagerBase = persistentManagerBase;
		this.session = session;
    }

    @Override
    public Void run() throws Exception{
       this.persistentManagerBase.getStore().save(session);
       return null;
    }                       
}