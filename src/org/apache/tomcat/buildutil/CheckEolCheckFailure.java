package org.apache.tomcat.buildutil;

import java.io.File;

public class CheckEolCheckFailure {
    private final File file;
    private final int line;
    private final String value;

    public CheckEolCheckFailure(File file, int line, String value) {
        this.file = file;
        this.line = line;
        this.value = value;
    }

    @Override
    public String toString() {
        return CheckEol.getEoln() + file + ": uses " + value + " on line " + line;
    }
}