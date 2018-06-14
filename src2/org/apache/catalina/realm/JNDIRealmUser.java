package org.apache.catalina.realm;

import java.util.Collections;
import java.util.List;

public class JNDIRealmUser {

	private final String username;
	private final String dn;
	private final String password;
	private final List<String> roles;
	private final String userRoleId;

	public JNDIRealmUser(String username, String dn, String password,
			List<String> roles, String userRoleId) {
		this.username = username;
		this.dn = dn;
		this.password = password;
		if (roles == null) {
			this.roles = Collections.emptyList();
		} else {
			this.roles = Collections.unmodifiableList(roles);
		}
		this.userRoleId = userRoleId;
	}

	public String getUserName() {
		return username;
	}

	public String getDN() {
		return dn;
	}

	public String getPassword() {
		return password;
	}

	public List<String> getRoles() {
		return roles;
	}

	public String getUserRoleId() {
		return userRoleId;
	}

}