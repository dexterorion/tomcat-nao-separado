package org.apache.jasper.tagplugins.jstl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/** Wraps responses to allow us to retrieve results as Strings. 
 * mainly taken from org.apache.taglibs.standard.tag.common.core.importSupport 
 */
public class UtilImportResponseWrapper extends HttpServletResponseWrapper{
    
    private StringWriter sw = new StringWriter();
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private ServletOutputStream sos = new ServletOutputStream() {
        @Override
        public void write(int b) throws IOException {
            bos.write(b);
        }
    };
    private boolean isWriterUsed;
    private boolean isStreamUsed;
    private int status = 200;
    private String charEncoding;
    
    public UtilImportResponseWrapper(HttpServletResponse arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public PrintWriter getWriter() {
        if (isStreamUsed)
            throw new IllegalStateException("Unexpected internal error during &lt;import&gt: " +
            "Target servlet called getWriter(), then getOutputStream()");
        isWriterUsed = true;
        return new PrintWriter(sw);
    }
    
    @Override
    public ServletOutputStream getOutputStream() {
        if (isWriterUsed)
            throw new IllegalStateException("Unexpected internal error during &lt;import&gt: " +
            "Target servlet called getOutputStream(), then getWriter()");
        isStreamUsed = true;
        return sos;
    }
    
    /** Has no effect. */
    @Override
    public void setContentType(String x) {
        // ignore
    }
    
    /** Has no effect. */
    @Override
    public void setLocale(Locale x) {
        // ignore
    }
    
    @Override
    public void setStatus(int status) {
        this.status = status;
    }
    
    @Override
    public int getStatus() {
        return status;
    }
    
    public String getCharEncoding(){
        return this.charEncoding;
    }
    
    public void setCharEncoding(String ce){
        this.charEncoding = ce;
    }
    
    public String getString() throws UnsupportedEncodingException {
        if (isWriterUsed)
            return sw.toString();
        else if (isStreamUsed) {
            if (this.charEncoding != null && !this.charEncoding.equals(""))
                return bos.toString(charEncoding);
            else
                return bos.toString("ISO-8859-1");
        } else
            return ""; // target didn't write anything
    }
}