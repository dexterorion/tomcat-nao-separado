package org.apache.catalina.filters;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;

public class ExpiresFilterXServletOutputStream extends ServletOutputStream {

    /**
	 * 
	 */
	private final ExpiresFilter expiresFilter;

	private final HttpServletRequest request;

    private final ExpiresFilterXHttpServletResponse response;

    private final ServletOutputStream servletOutputStream;

    public ExpiresFilterXServletOutputStream(ExpiresFilter expiresFilter, ServletOutputStream servletOutputStream,
            HttpServletRequest request, ExpiresFilterXHttpServletResponse response) {
        super();
		this.expiresFilter = expiresFilter;
        this.servletOutputStream = servletOutputStream;
        this.response = response;
        this.request = request;
    }

    @Override
    public void close() throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.close();
    }

    private void fireOnBeforeWriteResponseBodyEvent() {
        if (!this.response.isWriteResponseBodyStarted()) {
            this.response.setWriteResponseBodyStarted(true);
            this.expiresFilter.onBeforeWriteResponseBody(request, response);
        }
    }

    @Override
    public void flush() throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.flush();
    }

    @Override
    public void print(boolean b) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(b);
    }

    @Override
    public void print(char c) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(c);
    }

    @Override
    public void print(double d) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(d);
    }

    @Override
    public void print(float f) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(f);
    }

    @Override
    public void print(int i) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(i);
    }

    @Override
    public void print(long l) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(l);
    }

    @Override
    public void print(String s) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.print(s);
    }

    @Override
    public void println() throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println();
    }

    @Override
    public void println(boolean b) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(b);
    }

    @Override
    public void println(char c) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(c);
    }

    @Override
    public void println(double d) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(d);
    }

    @Override
    public void println(float f) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(f);
    }

    @Override
    public void println(int i) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(i);
    }

    @Override
    public void println(long l) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(l);
    }

    @Override
    public void println(String s) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.println(s);
    }

    @Override
    public void write(byte[] b) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        fireOnBeforeWriteResponseBodyEvent();
        servletOutputStream.write(b);
    }

}