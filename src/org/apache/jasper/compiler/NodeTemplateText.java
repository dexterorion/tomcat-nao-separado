package org.apache.jasper.compiler;

import java.util.ArrayList;

import org.apache.jasper.JasperException;

public class NodeTemplateText extends Node {

    private ArrayList<Integer> extraSmap = null;

    public NodeTemplateText(String text, Mark start, Node parent) {
        super(null, null, text, start, parent);
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    /**
     * Trim all whitespace from the left of the template text
     */
    public void ltrim() {
        int index = 0;
        while ((index < getText().length()) && (getText().charAt(index) <= ' ')) {
            index++;
        }
        setText(getText().substring(index));
    }

    public void setText(String text) {
        super.setText(text);
    }

    /**
     * Trim all whitespace from the right of the template text
     */
    public void rtrim() {
        int index = getText().length();
        while ((index > 0) && (getText().charAt(index - 1) <= ' ')) {
            index--;
        }
        setText(getText().substring(0, index));
    }

    /**
     * Returns true if this template text contains whitespace only.
     */
    public boolean isAllSpace() {
        boolean isAllSpace = true;
        for (int i = 0; i < getText().length(); i++) {
            if (!Character.isWhitespace(getText().charAt(i))) {
                isAllSpace = false;
                break;
            }
        }
        return isAllSpace;
    }

    /**
     * Add a source to Java line mapping
     * 
     * @param srcLine
     *            The position of the source line, relative to the line at
     *            the start of this node. The corresponding java line is
     *            assumed to be consecutive, i.e. one more than the last.
     */
    public void addSmap(int srcLine) {
        if (extraSmap == null) {
            extraSmap = new ArrayList<Integer>();
        }
        extraSmap.add(new Integer(srcLine));
    }

    public ArrayList<Integer> getExtraSmap() {
        return extraSmap;
    }
}