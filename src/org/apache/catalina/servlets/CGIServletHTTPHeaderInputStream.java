package org.apache.catalina.servlets;

import java.io.IOException;
import java.io.InputStream;

public class CGIServletHTTPHeaderInputStream extends InputStream {
    private static final int STATE_CHARACTER = 0;
    private static final int STATE_FIRST_CR = 1;
    private static final int STATE_FIRST_LF = 2;
    private static final int STATE_SECOND_CR = 3;
    private static final int STATE_HEADER_END = 4;

    private InputStream input;
    private int state;

    public CGIServletHTTPHeaderInputStream(InputStream theInput) {
        input = theInput;
        state = STATE_CHARACTER;
    }

    /**
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        if (state == STATE_HEADER_END) {
            return -1;
        }

        int i = input.read();

        // Update the state
        // State machine looks like this
        //
        //    -------->--------
        //   |      (CR)       |
        //   |                 |
        //  CR1--->---         |
        //   |        |        |
        //   ^(CR)    |(LF)    |
        //   |        |        |
        // CHAR--->--LF1--->--EOH
        //      (LF)  |  (LF)  |
        //            |(CR)    ^(LF)
        //            |        |
        //          (CR2)-->---

        if (i == 10) {
            // LF
            switch(state) {
                case STATE_CHARACTER:
                    state = STATE_FIRST_LF;
                    break;
                case STATE_FIRST_CR:
                    state = STATE_FIRST_LF;
                    break;
                case STATE_FIRST_LF:
                case STATE_SECOND_CR:
                    state = STATE_HEADER_END;
                    break;
            }

        } else if (i == 13) {
            // CR
            switch(state) {
                case STATE_CHARACTER:
                    state = STATE_FIRST_CR;
                    break;
                case STATE_FIRST_CR:
                    state = STATE_HEADER_END;
                    break;
                case STATE_FIRST_LF:
                    state = STATE_SECOND_CR;
                    break;
            }

        } else {
            state = STATE_CHARACTER;
        }

        return i;
    }
}  // class HTTPHeaderInputStream