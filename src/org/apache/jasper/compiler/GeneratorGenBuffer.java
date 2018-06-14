package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/**
 * A class for generating codes to a buffer. Included here are some support
 * for tracking source to Java lines mapping.
 */
public class GeneratorGenBuffer {

    /*
     * For a CustomTag, the codes that are generated at the beginning of the
     * tag may not be in the same buffer as those for the body of the tag.
     * Two fields are used here to keep this straight. For codes that do not
     * corresponds to any JSP lines, they should be null.
     */
    private Node node;

    private NodeNodes body;

    private java.io.CharArrayWriter charWriter;

    protected ServletWriter out;

    public GeneratorGenBuffer() {
        this(null, null);
    }

    public GeneratorGenBuffer(Node n, NodeNodes b) {
        node = n;
        body = b;
        if (body != null) {
            body.setGeneratedInBuffer(true);
        }
        charWriter = new java.io.CharArrayWriter();
        out = new ServletWriter(new java.io.PrintWriter(charWriter));
    }

    public ServletWriter getOut() {
        return out;
    }

    @Override
    public String toString() {
        return charWriter.toString();
    }

    /**
     * Adjust the Java Lines. This is necessary because the Java lines
     * stored with the nodes are relative the beginning of this buffer and
     * need to be adjusted when this buffer is inserted into the source.
     */
    public void adjustJavaLines(final int offset) {

        if (node != null) {
            adjustJavaLine(node, offset);
        }

        if (body != null) {
            try {
                body.visit(new NodeVisitor() {

                    @Override
                    public void doVisit(Node n) {
                        adjustJavaLine(n, offset);
                    }

                    @Override
                    public void visit(NodeCustomTag n)
                            throws JasperException {
                        NodeNodes b = n.getBody();
                        if (b != null && !b.isGeneratedInBuffer()) {
                            // Don't adjust lines for the nested tags that
                            // are also generated in buffers, because the
                            // adjustments will be done elsewhere.
                            b.visit(this);
                        }
                    }
                });
            } catch (JasperException ex) {
                // Ignore
            }
        }
    }

    private static void adjustJavaLine(Node n, int offset) {
        if (n.getBeginJavaLine() > 0) {
            n.setBeginJavaLine(n.getBeginJavaLine() + offset);
            n.setEndJavaLine(n.getEndJavaLine() + offset);
        }
    }
}