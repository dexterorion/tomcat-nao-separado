package org.apache.catalina.core;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.tomcat.util.security.PrivilegedGetTccl;
import org.apache.tomcat.util.security.PrivilegedSetTccl;

public class AsyncContextImplRunnableWrapper implements Runnable {

    private Runnable wrapped = null;
    private Context context = null;

    public AsyncContextImplRunnableWrapper(Runnable wrapped, Context ctxt) {
        this.wrapped = wrapped;
        this.context = ctxt;
    }

    @Override
    public void run() {
        ClassLoader oldCL;
        if (Globals.isSecurityEnabled()) {
            PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
            oldCL = AccessController.doPrivileged(pa);
        } else {
            oldCL = Thread.currentThread().getContextClassLoader();
        }

        try {
            if (Globals.isSecurityEnabled()) {
                PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                        context.getLoader().getClassLoader());
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader
                        (context.getLoader().getClassLoader());
            }
            wrapped.run();
        } finally {
            if (Globals.isSecurityEnabled()) {
                PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                        oldCL);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(oldCL);
            }
        }
    }

}