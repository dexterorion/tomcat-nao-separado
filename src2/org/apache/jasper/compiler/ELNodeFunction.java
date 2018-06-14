package org.apache.jasper.compiler;

import javax.servlet.jsp.tagext.FunctionInfo;

import org.apache.jasper.JasperException;

/**
 * Represents a function
 * Currently only include the prefix and function name, but not its
 * arguments.
 */
public class ELNodeFunction extends ELNode {

    private final String prefix;
    private final String name;
    private final String originalText;
    private String uri;
    private FunctionInfo functionInfo;
    private String methodName;
    private String[] parameters;

    public ELNodeFunction(String prefix, String name, String originalText) {
        this.prefix = prefix;
        this.name = name;
        this.originalText = originalText;
    }

    @Override
    public void accept(ELNodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return name;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setFunctionInfo(FunctionInfo f) {
        this.functionInfo = f;
    }

    public FunctionInfo getFunctionInfo() {
        return functionInfo;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public String[] getParameters() {
        return parameters;
    }
}