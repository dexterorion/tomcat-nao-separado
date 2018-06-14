package org.apache.catalina.realm;

public class RealmBaseAllRolesMode {
    
    private String name;
    /** Use the strict servlet spec interpretation which requires that the user
     * have one of the web-app/security-role/role-name 
     */
    private static final RealmBaseAllRolesMode STRICT_MODE = new RealmBaseAllRolesMode("strict");
    /** Allow any authenticated user
     */
    private static final RealmBaseAllRolesMode AUTH_ONLY_MODE = new RealmBaseAllRolesMode("authOnly");
    /** Allow any authenticated user only if there are no web-app/security-roles
     */
    private static final RealmBaseAllRolesMode STRICT_AUTH_ONLY_MODE = new RealmBaseAllRolesMode("strictAuthOnly");
    
    static RealmBaseAllRolesMode toMode(String name)
    {
    	RealmBaseAllRolesMode mode;
        if( name.equalsIgnoreCase(STRICT_MODE.getNameData()) )
            mode = STRICT_MODE;
        else if( name.equalsIgnoreCase(AUTH_ONLY_MODE.getNameData()) )
            mode = AUTH_ONLY_MODE;
        else if( name.equalsIgnoreCase(STRICT_AUTH_ONLY_MODE.getNameData()) )
            mode = STRICT_AUTH_ONLY_MODE;
        else
            throw new IllegalStateException("Unknown mode, must be one of: strict, authOnly, strictAuthOnly");
        return mode;
    }
    
    public RealmBaseAllRolesMode(String name)
    {
        this.setNameData(name);
    }
    
    @Override
    public boolean equals(Object o)
    {
        boolean equals = false;
        if( o instanceof RealmBaseAllRolesMode )
        {
        	RealmBaseAllRolesMode mode = (RealmBaseAllRolesMode) o;
            equals = getNameData().equals(mode.getNameData());
        }
        return equals;
    }
    @Override
    public int hashCode()
    {
        return getNameData().hashCode();
    }
    @Override
    public String toString()
    {
        return getNameData();
    }

	public String getName() {
		return getNameData();
	}

	public void setName(String name) {
		this.setNameData(name);
	}

	public static RealmBaseAllRolesMode getStrictMode() {
		return STRICT_MODE;
	}

	public static RealmBaseAllRolesMode getAuthOnlyMode() {
		return AUTH_ONLY_MODE;
	}

	public static RealmBaseAllRolesMode getStrictAuthOnlyMode() {
		return STRICT_AUTH_ONLY_MODE;
	}

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}
}