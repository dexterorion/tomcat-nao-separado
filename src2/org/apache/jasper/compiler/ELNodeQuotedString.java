package org.apache.jasper.compiler;

public class ELNodeQuotedString extends ELNodeToken {

    private String value;

    public ELNodeQuotedString(String whiteSpace, String v) {
        super(whiteSpace);
        this.value = v;
    }

    @Override
    public String toString() {
        return getWhiteSpace() + value;
    }

    @Override
    public String toTrimmedString() {
        return value;
    }
}