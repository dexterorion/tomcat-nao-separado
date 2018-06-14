package org.apache.tomcat.util.buf;

import java.io.CharConversionException;

public class UDecoderDecodeException extends CharConversionException {
    private static final long serialVersionUID = 1L;
    public UDecoderDecodeException(String s) {
        super(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // This class does not provide a stack trace
        return this;
    }
}