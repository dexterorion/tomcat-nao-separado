package org.apache.catalina.filters;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ExpiresFilterXHttpServletResponse extends HttpServletResponseWrapper {

    /**
	 * 
	 */
	private final ExpiresFilter expiresFilter;

	/**
     * Value of the <tt>Cache-Control/tt> http response header if it has
     * been set.
     */
    private String cacheControlHeader;

    /**
     * Value of the <tt>Last-Modified</tt> http response header if it has
     * been set.
     */
    private long lastModifiedHeader;

    private boolean lastModifiedHeaderSet;

    private PrintWriter printWriter;

    private final HttpServletRequest request;

    private ServletOutputStream servletOutputStream;

    /**
     * Indicates whether calls to write methods (<tt>write(...)</tt>,
     * <tt>print(...)</tt>, etc) of the response body have been called or
     * not.
     */
    private boolean writeResponseBodyStarted;

    public ExpiresFilterXHttpServletResponse(ExpiresFilter expiresFilter, HttpServletRequest request,
            HttpServletResponse response) {
        super(response);
		this.expiresFilter = expiresFilter;
        this.request = request;
    }

    @Override
    public void addDateHeader(String name, long date) {
        super.addDateHeader(name, date);
        if (!lastModifiedHeaderSet) {
            this.lastModifiedHeader = date;
            this.lastModifiedHeaderSet = true;
        }
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        if (ExpiresFilter.getHeaderCacheControl().equalsIgnoreCase(name) &&
                cacheControlHeader == null) {
            cacheControlHeader = value;
        }
    }

    public String getCacheControlHeader() {
        return cacheControlHeader;
    }

    public long getLastModifiedHeader() {
        return lastModifiedHeader;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (servletOutputStream == null) {
            servletOutputStream = new ExpiresFilterXServletOutputStream(
                    this.expiresFilter, super.getOutputStream(), request, this);
        }
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (printWriter == null) {
            printWriter = new ExpiresFilterXPrintWriter(this.expiresFilter, super.getWriter(), request, this);
        }
        return printWriter;
    }

    public boolean isLastModifiedHeaderSet() {
        return lastModifiedHeaderSet;
    }

    public boolean isWriteResponseBodyStarted() {
        return writeResponseBodyStarted;
    }

    @Override
    public void reset() {
        super.reset();
        this.lastModifiedHeader = 0;
        this.lastModifiedHeaderSet = false;
        this.cacheControlHeader = null;
    }

    @Override
    public void setDateHeader(String name, long date) {
        super.setDateHeader(name, date);
        if (ExpiresFilter.getHeaderLastModified().equalsIgnoreCase(name)) {
            this.lastModifiedHeader = date;
            this.lastModifiedHeaderSet = true;
        }
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        if (ExpiresFilter.getHeaderCacheControl().equalsIgnoreCase(name)) {
            this.cacheControlHeader = value;
        }
    }

    public void setWriteResponseBodyStarted(boolean writeResponseBodyStarted) {
        this.writeResponseBodyStarted = writeResponseBodyStarted;
    }
}