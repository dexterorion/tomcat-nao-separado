package org.apache.catalina.startup;

import org.apache.catalina.Container;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

/**
 * Rule that sets the parent class loader for the top object on the stack,
 * which must be a <code>Container</code>.
 */

public final class CatalinaSetParentClassLoaderRule extends Rule {

    public CatalinaSetParentClassLoaderRule(ClassLoader parentClassLoader) {

        this.parentClassLoader = parentClassLoader;

    }

    private ClassLoader parentClassLoader = null;

    @Override
    public void begin(String namespace, String name, Attributes attributes)
        throws Exception {

        if (getDigester().getLogger().isDebugEnabled()) {
            getDigester().getLogger().debug("Setting parent class loader");
        }

        Container top = (Container) getDigester().peek();
        top.setParentClassLoader(parentClassLoader);

    }


}