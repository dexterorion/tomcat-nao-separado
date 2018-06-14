package org.apache.catalina.session;

import java.security.PrivilegedExceptionAction;

// ---------------------------------------------------- Security Classes
public class StandardManagerPrivilegedDoLoad implements PrivilegedExceptionAction<Void> {

	/**
	 * 
	 */
	private final StandardManager standardManager;

	public StandardManagerPrivilegedDoLoad(StandardManager standardManager) {
		this.standardManager = standardManager;
        // NOOP
    }

    @Override
    public Void run() throws Exception{
       this.standardManager.doLoad();
       return null;
    }
}