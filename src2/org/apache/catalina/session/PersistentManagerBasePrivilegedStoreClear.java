package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

public class PersistentManagerBasePrivilegedStoreClear
    implements PrivilegedExceptionAction<Void> {

	/**
	 * 
	 */
	private final PersistentManagerBase persistentManagerBase;

	public PersistentManagerBasePrivilegedStoreClear(PersistentManagerBase persistentManagerBase) {
		this.persistentManagerBase = persistentManagerBase;
        // NOOP
    }

    @Override
    public Void run() throws Exception{
       this.persistentManagerBase.getStore().clear();
       return null;
    }                       
}