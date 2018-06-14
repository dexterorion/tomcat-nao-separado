package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

public class PersistentManagerBasePrivilegedStoreKeys implements
		PrivilegedExceptionAction<String[]> {

	/**
	 * 
	 */
	private final PersistentManagerBase persistentManagerBase;

	public PersistentManagerBasePrivilegedStoreKeys(
			PersistentManagerBase persistentManagerBase) {
		this.persistentManagerBase = persistentManagerBase;
		// NOOP
	}

	@Override
	public String[] run() throws Exception {
		return this.persistentManagerBase.getStore().keys();
	}
}