package org.apache.jasper.compiler;

import java.io.PrintStream;

import org.apache.jasper.Constants28;
import org.apache.tools.ant.DefaultLogger;

public class AntCompilerJasperAntLogger extends DefaultLogger {
    
    private StringBuilder reportBuf = new StringBuilder();
    
    @Override
    protected void printMessage(final String message,
            final PrintStream stream,
            final int priority) {
    }
    
    @Override
    protected void log(String message) {
        reportBuf.append(message);
        reportBuf.append(Constants28.getNewline());
    }
    
    protected String getReport() {
        String report = reportBuf.toString();
        reportBuf.setLength(0);
        return report;
    }
}