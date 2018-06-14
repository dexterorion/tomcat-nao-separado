package javax.servlet.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;

/*
 * A response wrapper for use in (dumb) "HEAD" support.
 * This just swallows that body, counting the bytes in order to set
 * the content length appropriately.  All other methods delegate to the
 * wrapped HTTP Servlet Response object.
 */
// file private
public class NoBodyResponse extends HttpServletResponseWrapper {
    private NoBodyOutputStream                noBody;
    private PrintWriter                        writer;
    private boolean                        didSetContentLength;

    // file private
    public NoBodyResponse(HttpServletResponse r) {
        super(r);
        noBody = new NoBodyOutputStream();
    }

    // file private
    public void setContentLength() {
        if (!didSetContentLength) {
            if (writer != null) {
                writer.flush();
            }
            super.setContentLength(noBody.getContentLength());
        }
    }


    // SERVLET RESPONSE interface methods

    @Override
    public void setContentLength(int len) {
        super.setContentLength(len);
        didSetContentLength = true;
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        checkHeader(name);
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        checkHeader(name);
    }

    @Override
    public void setIntHeader(String name, int value) {
        super.setIntHeader(name, value);
        checkHeader(name);
    }

    @Override
    public void addIntHeader(String name, int value) {
        super.addIntHeader(name, value);
        checkHeader(name);
    }

    private void checkHeader(String name) {
        if ("content-length".equalsIgnoreCase(name)) {
            didSetContentLength = true;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return noBody;
    }

    @Override
    public PrintWriter getWriter() throws UnsupportedEncodingException {

        if (writer == null) {
            OutputStreamWriter w;

            w = new OutputStreamWriter(noBody, getCharacterEncoding());
            writer = new PrintWriter(w);
        }
        return writer;
    }
}