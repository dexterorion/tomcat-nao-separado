package org.apache.naming.factory;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class MailSessionFactoryAuthenticator extends Authenticator{
	private PasswordAuthentication pa;
	
	public MailSessionFactoryAuthenticator(PasswordAuthentication pa){
		this.pa = pa;
	}
	
	@Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return pa;
    }

}
