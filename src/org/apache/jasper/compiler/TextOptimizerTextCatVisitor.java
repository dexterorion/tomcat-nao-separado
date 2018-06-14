package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.apache.jasper.Options;

/**
 * A visitor to concatenate contiguous template texts.
 */
public class TextOptimizerTextCatVisitor extends NodeVisitor {

    private static final String EMPTY_TEXT = "";

    private Options options;
    private PageInfo pageInfo;
    private int textNodeCount = 0;
    private NodeTemplateText firstTextNode = null;
    private StringBuilder textBuffer;

    public TextOptimizerTextCatVisitor(Compiler2 compiler) {
        options = compiler.getCompilationContext().getOptions();
        pageInfo = compiler.getPageInfo();
    }

    @Override
    public void doVisit(Node n) throws JasperException {
        collectText();
    }

    /*
     * The following directives are ignored in text concatenation
     */

    @Override
    public void visit(NodePageDirective n) throws JasperException {
    }

    @Override
    public void visit(NodeTagDirective n) throws JasperException {
    }

    @Override
    public void visit(NodeTaglibDirective n) throws JasperException {
    }

    @Override
    public void visit(NodeAttributeDirective n) throws JasperException {
    }

    @Override
    public void visit(NodeVariableDirective n) throws JasperException {
    }

    /*
     * Don't concatenate text across body boundaries
     */
    @Override
    public void visitBody(Node n) throws JasperException {
        super.visitBody(n);
        collectText();
    }

    @Override
    public void visit(NodeTemplateText n) throws JasperException {
        if ((options.getTrimSpaces() || pageInfo.isTrimDirectiveWhitespaces()) 
                && n.isAllSpace()) {
            n.setText(EMPTY_TEXT);
            return;
        }

        if (textNodeCount++ == 0) {
            firstTextNode = n;
            textBuffer = new StringBuilder(n.getText());
        } else {
            // Append text to text buffer
            textBuffer.append(n.getText());
            n.setText(EMPTY_TEXT);
        }
    }

    /**
     * This method breaks concatenation mode.  As a side effect it copies
     * the concatenated string to the first text node 
     */
    public void collectText() {

        if (textNodeCount > 1) {
            // Copy the text in buffer into the first template text node.
            firstTextNode.setText(textBuffer.toString());
        }
        textNodeCount = 0;
    }

}