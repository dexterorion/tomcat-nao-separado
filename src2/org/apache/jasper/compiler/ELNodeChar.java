package org.apache.jasper.compiler;

public class ELNodeChar extends ELNodeToken {

    private char ch;

    public ELNodeChar(String whiteSpace, char ch) {
        super(whiteSpace);
        this.ch = ch;
    }

    @Override
    public char toChar() {
        return ch;
    }

    @Override
    public String toString() {
        return getWhiteSpace() + ch;
    }

    @Override
    public String toTrimmedString() {
        return "" + ch;
    }
}