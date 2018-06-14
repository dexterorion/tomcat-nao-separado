package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

public class PersistentManagerBasePrivilegedStoreRemove implements PrivilegedExceptionAction<Void> {

    /**
	 * 
	 */
	private final PersistentManagerBase persistentManagerBase;
	private String id;    
        
    public PersistentManagerBasePrivilegedStoreRemove(PersistentManagerBase persistentManagerBase, String id) {     
        this.persistentManagerBase = persistentManagerBase;
		this.id = id;
    }

    @Override
    public Void run() throws Exception{
       this.persistentManagerBase.getStore().remove(id);
       return null;
    }                       
}