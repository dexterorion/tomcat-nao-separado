package org.apache.catalina.startup;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;

import org.apache.tomcat.util.bcel.classfile.JavaClass;

public class ContextConfigJavaClassCacheEntry {
    private final String superclassName;

    private final String[] interfaceNames;

    private Set<ServletContainerInitializer> sciSet = null;

    public ContextConfigJavaClassCacheEntry(JavaClass javaClass) {
        superclassName = javaClass.getSuperclassName();
        interfaceNames = javaClass.getInterfaceNames();
    }

    public String getSuperclassName() {
        return superclassName;
    }

    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    public Set<ServletContainerInitializer> getSciSet() {
        return sciSet;
    }

    public void setSciSet(Set<ServletContainerInitializer> sciSet) {
        this.sciSet = sciSet;
    }
}