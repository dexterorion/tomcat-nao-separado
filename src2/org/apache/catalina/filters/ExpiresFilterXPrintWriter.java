package org.apache.catalina.filters;

import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

/**
 * Wrapping extension of {@link PrintWriter} to trap the
 * "Start Write Response Body" event.
 */
public class ExpiresFilterXPrintWriter extends PrintWriter {
    /**
	 * 
	 */
	private final ExpiresFilter expiresFilter;

	private final PrintWriter out;

    private final HttpServletRequest request;

    private final ExpiresFilterXHttpServletResponse response;

    public ExpiresFilterXPrintWriter(ExpiresFilter expiresFilter, PrintWriter out, HttpServletRequest request,
    		ExpiresFilterXHttpServletResponse response) {
        super(out);
		this.expiresFilter = expiresFilter;
        this.out = out;
        this.request = request;
        this.response = response;
    }

    @Override
    public PrintWriter append(char c) {
        fireBeforeWriteResponseBodyEvent();
        return out.append(c);
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        fireBeforeWriteResponseBodyEvent();
        return out.append(csq);
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        fireBeforeWriteResponseBodyEvent();
        return out.append(csq, start, end);
    }

    @Override
    public void close() {
        fireBeforeWriteResponseBodyEvent();
        out.close();
    }

    private void fireBeforeWriteResponseBodyEvent() {
        if (!this.response.isWriteResponseBodyStarted()) {
            this.response.setWriteResponseBodyStarted(true);
            this.expiresFilter.onBeforeWriteResponseBody(request, response);
        }
    }

    @Override
    public void flush() {
        fireBeforeWriteResponseBodyEvent();
        out.flush();
    }

    @Override
    public void print(boolean b) {
        fireBeforeWriteResponseBodyEvent();
        out.print(b);
    }

    @Override
    public void print(char c) {
        fireBeforeWriteResponseBodyEvent();
        out.print(c);
    }

    @Override
    public void print(char[] s) {
        fireBeforeWriteResponseBodyEvent();
        out.print(s);
    }

    @Override
    public void print(double d) {
        fireBeforeWriteResponseBodyEvent();
        out.print(d);
    }

    @Override
    public void print(float f) {
        fireBeforeWriteResponseBodyEvent();
        out.print(f);
    }

    @Override
    public void print(int i) {
        fireBeforeWriteResponseBodyEvent();
        out.print(i);
    }

    @Override
    public void print(long l) {
        fireBeforeWriteResponseBodyEvent();
        out.print(l);
    }

    @Override
    public void print(Object obj) {
        fireBeforeWriteResponseBodyEvent();
        out.print(obj);
    }

    @Override
    public void print(String s) {
        fireBeforeWriteResponseBodyEvent();
        out.print(s);
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object... args) {
        fireBeforeWriteResponseBodyEvent();
        return out.printf(l, format, args);
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        fireBeforeWriteResponseBodyEvent();
        return out.printf(format, args);
    }

    @Override
    public void println() {
        fireBeforeWriteResponseBodyEvent();
        out.println();
    }

    @Override
    public void println(boolean x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(char x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(char[] x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(double x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(float x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(int x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(long x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(Object x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void println(String x) {
        fireBeforeWriteResponseBodyEvent();
        out.println(x);
    }

    @Override
    public void write(char[] buf) {
        fireBeforeWriteResponseBodyEvent();
        out.write(buf);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        fireBeforeWriteResponseBodyEvent();
        out.write(buf, off, len);
    }

    @Override
    public void write(int c) {
        fireBeforeWriteResponseBodyEvent();
        out.write(c);
    }

    @Override
    public void write(String s) {
        fireBeforeWriteResponseBodyEvent();
        out.write(s);
    }

    @Override
    public void write(String s, int off, int len) {
        fireBeforeWriteResponseBodyEvent();
        out.write(s, off, len);
    }

}