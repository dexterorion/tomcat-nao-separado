package org.apache.catalina.filters;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Wrapper that adds a character set for text media types if no character
 * set is specified.
 */
public class AddDefaultCharsetFilterResponseWrapper extends HttpServletResponseWrapper {

    private String encoding;
    
    public AddDefaultCharsetFilterResponseWrapper(HttpServletResponse response, String encoding) {
        super(response);
        this.encoding = encoding;
    }

    @Override
    public void setContentType(String ct) {
        
        if (ct != null && ct.startsWith("text/")) {
            if (ct.indexOf("charset=") < 0) {
                super.setContentType(ct + ";charset=" + encoding);
            } else {
                super.setContentType(ct);
                encoding = getCharacterEncoding();
            }
        } else {
            super.setContentType(ct);
        }

    }

    @Override
    public void setCharacterEncoding(String charset) {
        super.setCharacterEncoding(charset);
        encoding = charset;
    }
}