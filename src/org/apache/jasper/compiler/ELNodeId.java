package org.apache.jasper.compiler;

/*
 * Represents an ID token in EL
 */
public class ELNodeId extends ELNodeToken {
    private String id;

    public ELNodeId(String whiteSpace, String id) {
        super(whiteSpace);
        this.id = id;
    }

    @Override
    public String toString() {
        return getWhiteSpace() + id;
    }

    @Override
    public String toTrimmedString() {
        return id;
    }
}