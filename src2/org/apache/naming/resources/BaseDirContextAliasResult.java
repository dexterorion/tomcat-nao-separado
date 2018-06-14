package org.apache.naming.resources;

public class BaseDirContextAliasResult {
    private BaseDirContext dirContext;
    private String aliasName;
	public BaseDirContext getDirContext() {
		return dirContext;
	}
	public void setDirContext(BaseDirContext dirContext) {
		this.dirContext = dirContext;
	}
	public String getAliasName() {
		return aliasName;
	}
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
}