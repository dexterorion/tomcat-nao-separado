package org.apache.catalina.authenticator;

import java.security.PrivilegedExceptionAction;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * This class gets a gss credential via a privileged action.
 */
public class SpnegoAuthenticatorAcceptAction implements PrivilegedExceptionAction<byte[]> {

    private GSSContext gssContext;

    private byte[] decoded;

    public SpnegoAuthenticatorAcceptAction(GSSContext context, byte[] decodedToken) {
        this.gssContext = context;
        this.decoded = decodedToken;
    }

    @Override
    public byte[] run() throws GSSException {
        return gssContext.acceptSecContext(decoded,
                0, decoded.length);
    }
}