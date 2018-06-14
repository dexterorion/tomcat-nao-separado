package org.apache.catalina.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

public final class StandardEngineAccessLogListener implements PropertyChangeListener, LifecycleListener, ContainerListener {

    private StandardEngine engine;
    private Host host;
    private Context context;
    private volatile boolean disabled = false;

    public StandardEngineAccessLogListener(StandardEngine engine, Host host,
            Context context) {
        this.engine = engine;
        this.host = host;
        this.context = context;
    }

    public void install() {
        engine.addPropertyChangeListener(this);
        if (host != null) {
            host.addContainerListener(this);
            host.addLifecycleListener(this);
        }
        if (context != null) {
            context.addLifecycleListener(this);
        }
    }

    private void uninstall() {
        disabled = true;
        if (context != null) {
            context.removeLifecycleListener(this);
        }
        if (host != null) {
            host.removeLifecycleListener(this);
            host.removeContainerListener(this);
        }
        engine.removePropertyChangeListener(this);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (disabled) return;

        String type = event.getType();
        if (Lifecycle.AFTER_START_EVENT.equals(type) ||
                Lifecycle.BEFORE_STOP_EVENT.equals(type) ||
                Lifecycle.BEFORE_DESTROY_EVENT.equals(type)) {
            // Container is being started/stopped/removed
            // Force re-calculation and disable listener since it won't
            // be re-used
            engine.getDefaultAccessLog().set(null);
            uninstall();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (disabled) return;
        if ("defaultHost".equals(evt.getPropertyName())) {
            // Force re-calculation and disable listener since it won't
            // be re-used
            engine.getDefaultAccessLog().set(null);
            uninstall();
        }
    }

    @Override
    public void containerEvent(ContainerEvent event) {
        // Only useful for hosts
        if (disabled) return;
        if (Container.ADD_CHILD_EVENT.equals(event.getType())) {
            Context context = (Context) event.getData();
            if ("".equals(context.getPath())) {
                // Force re-calculation and disable listener since it won't
                // be re-used
                engine.getDefaultAccessLog().set(null);
                uninstall();
            }
        }
    }
}