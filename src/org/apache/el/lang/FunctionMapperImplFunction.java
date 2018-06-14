package org.apache.el.lang;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

import org.apache.el.util.ReflectionUtil;

public class FunctionMapperImplFunction implements Externalizable {

    private transient Method m;
    private String owner;
    private String name;
    private String[] types;
    private String prefix;
    private String localName;

    /**
     * 
     */
    public FunctionMapperImplFunction(String prefix, String localName, Method m) {
        if (localName == null) {
            throw new NullPointerException("LocalName cannot be null");
        }
        if (m == null) {
            throw new NullPointerException("Method cannot be null");
        }
        this.prefix = prefix;
        this.localName = localName;
        this.m = m;
    }

    public FunctionMapperImplFunction() {
        // for serialization
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF((this.prefix != null) ? this.prefix : "");
        out.writeUTF(this.localName);
        // make sure m isn't null
        getMethod();
        out.writeUTF((this.owner != null) ?
                 this.owner :
                 this.m.getDeclaringClass().getName());
        out.writeUTF((this.name != null) ?
                 this.name :
                 this.m.getName());
        out.writeObject((this.types != null) ?
                 this.types :
                 ReflectionUtil.toTypeNameArray(this.m.getParameterTypes()));

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {

        this.prefix = in.readUTF();
        if ("".equals(this.prefix)) this.prefix = null;
        this.localName = in.readUTF();
        this.owner = in.readUTF();
        this.name = in.readUTF();
        this.types = (String[]) in.readObject();
    }

    public Method getMethod() {
        if (this.m == null) {
            try {
                Class<?> t = ReflectionUtil.forName(this.owner);
                Class<?>[] p = ReflectionUtil.toTypeArray(this.types);
                this.m = t.getMethod(this.name, p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this.m;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FunctionMapperImplFunction) {
            return this.hashCode() == obj.hashCode();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (this.prefix + this.localName).hashCode();
    }
}