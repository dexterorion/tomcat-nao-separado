package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/*
 * Visitor responsible for mapping a line number in the generated servlet
 * source code to the corresponding JSP node.
 */
public class ErrorDispatcherErrorVisitor extends NodeVisitor {

    // Java source line number to be mapped
    private int lineNum;

    /*
     * JSP node whose Java source code range in the generated servlet
     * contains the Java source line number to be mapped
     */
    private Node found;

    /*
     * Constructor.
     *
     * @param lineNum Source line number in the generated servlet code
     */
    public ErrorDispatcherErrorVisitor(int lineNum) {
        this.lineNum = lineNum;
    }

    @Override
    public void doVisit(Node n) throws JasperException {
        if ((lineNum >= n.getBeginJavaLine())
                && (lineNum < n.getEndJavaLine())) {
            found = n;
        }
    }

    /*
     * Gets the JSP node to which the source line number in the generated
     * servlet code was mapped.
     *
     * @return JSP node to which the source line number in the generated
     * servlet code was mapped
     */
    public Node getJspSourceNode() {
        return found;
    }
}