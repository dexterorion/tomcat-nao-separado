package org.apache.catalina.authenticator;

import java.security.Principal;
import java.security.PrivilegedAction;

import org.apache.catalina.Realm;
import org.ietf.jgss.GSSContext;

public class SpnegoAuthenticatorAuthenticateAction implements PrivilegedAction<Principal> {

    private final Realm realm;
    private final GSSContext gssContext;
    private final boolean storeDelegatedCredential;

    public SpnegoAuthenticatorAuthenticateAction(Realm realm, GSSContext gssContext,
            boolean storeDelegatedCredential) {
        this.realm = realm;
        this.gssContext = gssContext;
        this.storeDelegatedCredential = storeDelegatedCredential;
    }

    @Override
    public Principal run() {
        return realm.authenticate(gssContext, storeDelegatedCredential);
    }
}