package org.apache.jasper.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class AntCompilerSystemLogHandler extends PrintStream {
    /**
     * Construct the handler to capture the output of the given steam.
     */
    public AntCompilerSystemLogHandler(PrintStream wrapped) {
        super(wrapped);
        this.wrapped = wrapped;
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Wrapped PrintStream.
     */
    private PrintStream wrapped = null;


    /**
     * Thread <-> PrintStream associations.
     */
    private static ThreadLocal<PrintStream> streams =
        new ThreadLocal<PrintStream>();


    /**
     * Thread <-> ByteArrayOutputStream associations.
     */
    private static ThreadLocal<ByteArrayOutputStream> data =
        new ThreadLocal<ByteArrayOutputStream>();


    // --------------------------------------------------------- Public Methods


    /**
     * @deprecated Unused. Will be removed in Tomcat 8.0.x.
     */
    @Deprecated
    public PrintStream getWrapped() {
      return wrapped;
    }

    /**
     * Start capturing thread's output.
     */
    public static void setThread() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        data.set(baos);
        streams.set(new PrintStream(baos));
    }


    /**
     * Stop capturing thread's output and return captured data as a String.
     */
    public static String unsetThread() {
        ByteArrayOutputStream baos = data.get();
        if (baos == null) {
            return null;
        }
        streams.set(null);
        data.set(null);
        return baos.toString();
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Find PrintStream to which the output must be written to.
     */
    protected PrintStream findStream() {
        PrintStream ps = streams.get();
        if (ps == null) {
            ps = wrapped;
        }
        return ps;
    }


    // ---------------------------------------------------- PrintStream Methods


    @Override
    public void flush() {
        findStream().flush();
    }

    @Override
    public void close() {
        findStream().close();
    }

    @Override
    public boolean checkError() {
        return findStream().checkError();
    }

    @Override
    protected void setError() {
        //findStream().setError();
    }

    @Override
    public void write(int b) {
        findStream().write(b);
    }

    @Override
    public void write(byte[] b)
        throws IOException {
        findStream().write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        findStream().write(buf, off, len);
    }

    @Override
    public void print(boolean b) {
        findStream().print(b);
    }

    @Override
    public void print(char c) {
        findStream().print(c);
    }

    @Override
    public void print(int i) {
        findStream().print(i);
    }

    @Override
    public void print(long l) {
        findStream().print(l);
    }

    @Override
    public void print(float f) {
        findStream().print(f);
    }

    @Override
    public void print(double d) {
        findStream().print(d);
    }

    @Override
    public void print(char[] s) {
        findStream().print(s);
    }

    @Override
    public void print(String s) {
        findStream().print(s);
    }

    @Override
    public void print(Object obj) {
        findStream().print(obj);
    }

    @Override
    public void println() {
        findStream().println();
    }

    @Override
    public void println(boolean x) {
        findStream().println(x);
    }

    @Override
    public void println(char x) {
        findStream().println(x);
    }

    @Override
    public void println(int x) {
        findStream().println(x);
    }

    @Override
    public void println(long x) {
        findStream().println(x);
    }

    @Override
    public void println(float x) {
        findStream().println(x);
    }

    @Override
    public void println(double x) {
        findStream().println(x);
    }

    @Override
    public void println(char[] x) {
        findStream().println(x);
    }

    @Override
    public void println(String x) {
        findStream().println(x);
    }

    @Override
    public void println(Object x) {
        findStream().println(x);
    }

}