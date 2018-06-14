package org.apache.jasper.compiler;

/*
 * Represents a token in EL expression string
 */
public class ELNodeToken {

    private final String whiteSpace;

    public ELNodeToken(String whiteSpace) {
        this.whiteSpace = whiteSpace;
    }

    public char toChar() {
        return 0;
    }

    @Override
    public String toString() {
        return whiteSpace;
    }

    public String toTrimmedString() {
        return "";
    }

    public String getWhiteSpace() {
        return whiteSpace;
    }
}