package javax.servlet.http;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.servlet.ServletOutputStream;

public class NoBodyOutputStream extends ServletOutputStream {

    private static final String LSTRING_FILE =
        "javax.servlet.http.LocalStrings";
    private static ResourceBundle lStrings =
        ResourceBundle.getBundle(LSTRING_FILE);

    private int                contentLength = 0;

    // file private
    public NoBodyOutputStream() {
        // NOOP
    }

    // file private
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public void write(int b) {
        contentLength++;
    }

    @Override
    public void write(byte buf[], int offset, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException(
                    lStrings.getString("err.io.nullArray"));
        }

        if (offset < 0 || len < 0 || offset+len > buf.length) {
            String msg = lStrings.getString("err.io.indexOutOfBounds");
            Object[] msgArgs = new Object[3];
            msgArgs[0] = Integer.valueOf(offset);
            msgArgs[1] = Integer.valueOf(len);
            msgArgs[2] = Integer.valueOf(buf.length);
            msg = MessageFormat.format(msg, msgArgs);
            throw new IndexOutOfBoundsException(msg);
        }

        contentLength += len;
    }
}