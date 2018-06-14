package org.apache.jasper.compiler;

import java.security.CodeSource;
import java.security.PermissionCollection;

// Helper class to allow initSecurity() to return two items
public class JspRuntimeContextSecurityHolder{
    private final CodeSource cs;
    private final PermissionCollection pc;
    public JspRuntimeContextSecurityHolder(CodeSource cs, PermissionCollection pc){
        this.cs = cs;
        this.pc = pc;
    }
	public CodeSource getCs() {
		return cs;
	}
	public PermissionCollection getPc() {
		return pc;
	}
}