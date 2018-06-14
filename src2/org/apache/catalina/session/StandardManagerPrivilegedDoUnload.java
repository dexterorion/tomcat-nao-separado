package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

public class StandardManagerPrivilegedDoUnload implements PrivilegedExceptionAction<Void> {

	/**
	 * 
	 */
	private final StandardManager standardManager;

	public StandardManagerPrivilegedDoUnload(StandardManager standardManager) {
		this.standardManager = standardManager;
        // NOOP
    }

    @Override
    public Void run() throws Exception{
        this.standardManager.doUnload();
        return null;
    }

}