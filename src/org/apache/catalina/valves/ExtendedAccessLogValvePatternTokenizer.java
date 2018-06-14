package org.apache.catalina.valves;

import java.io.IOException;
import java.io.StringReader;

public class ExtendedAccessLogValvePatternTokenizer {
    private StringReader sr = null;
    private StringBuilder buf = new StringBuilder();
    private boolean ended = false;
    private boolean subToken;
    private boolean parameter;

    public ExtendedAccessLogValvePatternTokenizer(String str) {
        sr = new StringReader(str);
    }

    public boolean hasSubToken() {
        return subToken;
    }

    public boolean hasParameter() {
        return parameter;
    }

    public String getToken() throws IOException {
        if(ended) {
            return null ;
        }

        String result = null;
        subToken = false;
        parameter = false;

        int c = sr.read();
        while (c != -1) {
            switch (c) {
            case ' ':
                result = buf.toString();
                buf = new StringBuilder();
                buf.append((char) c);
                return result;
            case '-':
                result = buf.toString();
                buf = new StringBuilder();
                subToken = true;
                return result;
            case '(':
                result = buf.toString();
                buf = new StringBuilder();
                parameter = true;
                return result;
            case ')':
                result = buf.toString();
                buf = new StringBuilder();
                break;
            default:
                buf.append((char) c);
            }
            c = sr.read();
        }
        ended = true;
        if (buf.length() != 0) {
            return buf.toString();
        } else {
            return null;
        }
    }

    public String getParameter()throws IOException {
        String result;
        if (!parameter) {
            return null;
        }
        parameter = false;
        int c = sr.read();
        while (c != -1) {
            if (c == ')') {
                result = buf.toString();
                buf = new StringBuilder();
                return result;
            }
            buf.append((char) c);
            c = sr.read();
        }
        return null;
    }

    public String getWhiteSpaces() throws IOException {
        if(isEnded()) {
            return "" ;
        }
        StringBuilder whiteSpaces = new StringBuilder();
        if (buf.length() > 0) {
            whiteSpaces.append(buf);
            buf = new StringBuilder();
        }
        int c = sr.read();
        while (Character.isWhitespace((char) c)) {
            whiteSpaces.append((char) c);
            c = sr.read();
        }
        if (c == -1) {
            ended = true;
        } else {
            buf.append((char) c);
        }
        return whiteSpaces.toString();
    }

    public boolean isEnded() {
        return ended;
    }

    public String getRemains() throws IOException {
        StringBuilder remains = new StringBuilder();
        for(int c = sr.read(); c != -1; c = sr.read()) {
            remains.append((char) c);
        }
        return remains.toString();
    }

}