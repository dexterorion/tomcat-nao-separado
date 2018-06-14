package org.apache.jasper.compiler;

import java.util.HashMap;

import org.apache.jasper.JasperException;

public class SmapUtilSmapGenVisitor extends NodeVisitor {

    private SmapStratum smap;
    private boolean breakAtLF;
    private HashMap<String, SmapStratum> innerClassMap;

    public SmapUtilSmapGenVisitor(SmapStratum s, boolean breakAtLF, HashMap<String, SmapStratum> map) {
        this.smap = s;
        this.breakAtLF = breakAtLF;
        this.innerClassMap = map;
    }

    @Override
    public void visitBody(Node n) throws JasperException {
        SmapStratum smapSave = smap;
        String innerClass = n.getInnerClassName();
        if (innerClass != null) {
            this.smap = innerClassMap.get(innerClass);
        }
        super.visitBody(n);
        smap = smapSave;
    }

    @Override
    public void visit(NodeDeclaration n) throws JasperException {
        doSmapText(n);
    }

    @Override
    public void visit(NodeExpression n) throws JasperException {
        doSmapText(n);
    }

    @Override
    public void visit(NodeScriptlet n) throws JasperException {
        doSmapText(n);
    }

    @Override
    public void visit(NodeIncludeAction n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeForwardAction n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeGetProperty n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeSetProperty n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeUseBean n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodePlugIn n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeUninterpretedTag n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeJspElement n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeJspText n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeNamedAttribute n) throws JasperException {
        visitBody(n);
    }

    @Override
    public void visit(NodeJspBody n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeInvokeAction n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeDoBodyAction n) throws JasperException {
        doSmap(n);
        visitBody(n);
    }

    @Override
    public void visit(NodeELExpression n) throws JasperException {
        doSmap(n);
    }

    @Override
    public void visit(NodeTemplateText n) throws JasperException {
        Mark mark = n.getStart();
        if (mark == null) {
            return;
        }

        //Add the file information
        String fileName = mark.getFile();
        smap.addFile(SmapUtil.unqualify(fileName), fileName);

        //Add a LineInfo that corresponds to the beginning of this node
        int iInputStartLine = mark.getLineNumber();
        int iOutputStartLine = n.getBeginJavaLine();
        int iOutputLineIncrement = breakAtLF? 1: 0;
        smap.addLineData(iInputStartLine, fileName, 1, iOutputStartLine, 
                         iOutputLineIncrement);

        // Output additional mappings in the text
        java.util.ArrayList<Integer> extraSmap = n.getExtraSmap();

        if (extraSmap != null) {
            for (int i = 0; i < extraSmap.size(); i++) {
                iOutputStartLine += iOutputLineIncrement;
                smap.addLineData(
                    iInputStartLine+extraSmap.get(i).intValue(),
                    fileName,
                    1,
                    iOutputStartLine,
                    iOutputLineIncrement);
            }
        }
    }

    private void doSmap(
        Node n,
        int inLineCount,
        int outIncrement,
        int skippedLines) {
        Mark mark = n.getStart();
        if (mark == null) {
            return;
        }

        String unqualifiedName = SmapUtil.unqualify(mark.getFile());
        smap.addFile(unqualifiedName, mark.getFile());
        smap.addLineData(
            mark.getLineNumber() + skippedLines,
            mark.getFile(),
            inLineCount - skippedLines,
            n.getBeginJavaLine() + skippedLines,
            outIncrement);
    }

    private void doSmap(Node n) {
        doSmap(n, 1, n.getEndJavaLine() - n.getBeginJavaLine(), 0);
    }

    private void doSmapText(Node n) {
        String text = n.getText();
        int index = 0;
        int next = 0;
        int lineCount = 1;
        int skippedLines = 0;
        boolean slashStarSeen = false;
        boolean beginning = true;

        // Count lines inside text, but skipping comment lines at the
        // beginning of the text.
        while ((next = text.indexOf('\n', index)) > -1) {
            if (beginning) {
                String line = text.substring(index, next).trim();
                if (!slashStarSeen && line.startsWith("/*")) {
                    slashStarSeen = true;
                }
                if (slashStarSeen) {
                    skippedLines++;
                    int endIndex = line.indexOf("*/");
                    if (endIndex >= 0) {
                        // End of /* */ comment
                        slashStarSeen = false;
                        if (endIndex < line.length() - 2) {
                            // Some executable code after comment
                            skippedLines--;
                            beginning = false;
                        }
                    }
                } else if (line.length() == 0 || line.startsWith("//")) {
                    skippedLines++;
                } else {
                    beginning = false;
                }
            }
            lineCount++;
            index = next + 1;
        }

        doSmap(n, lineCount, 1, skippedLines);
    }
}