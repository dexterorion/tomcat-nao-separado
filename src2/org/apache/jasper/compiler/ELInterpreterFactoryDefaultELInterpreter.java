package org.apache.jasper.compiler;

import org.apache.jasper.JspCompilationContext;

public class ELInterpreterFactoryDefaultELInterpreter implements ELInterpreter {

    @Override
    public String interpreterCall(JspCompilationContext context,
            boolean isTagFile, String expression,
            Class<?> expectedType, String fnmapvar, boolean xmlEscape) {
        return JspUtil.interpreterCall(isTagFile, expression, expectedType,
                fnmapvar, xmlEscape);
    }
}