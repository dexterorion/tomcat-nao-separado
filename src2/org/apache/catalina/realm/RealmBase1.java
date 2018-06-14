package org.apache.catalina.realm;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public class RealmBase1 extends RealmBase{
	private Map<String, String> userPass;
	private Map<String, List<String>> userRoles;
	private Map<String, Principal> userPrincipals;
	
	public RealmBase1(Map<String, String> userPass, Map<String, List<String>> userRoles, Map<String, Principal> userPrincipals){
		this.userPass = userPass;
		this.userRoles = userRoles;
		this.userPrincipals = userPrincipals;
	}
	
	@Override
	protected String getName() {
		return "Simple";
	}

	@Override
	protected String getPassword(String username) {
		return userPass.get(username);
	}

	@Override
	protected Principal getPrincipal(String username) {
		Principal p = userPrincipals.get(username);
		if (p == null) {
			String pass = userPass.get(username);
			if (pass != null) {
				p = new GenericPrincipal(username, pass,
						userRoles.get(username));
				userPrincipals.put(username, p);
			}
		}
		return p;
	}
}
