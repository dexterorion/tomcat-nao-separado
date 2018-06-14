package org.apache.jasper.compiler;

public class NodeChildInfo {
    private boolean scriptless; // true if the tag and its body

    // contain no scripting elements.
    private boolean hasNodeUseBean;

    private boolean hasNodeIncludeAction;

    private boolean hasNodeParamAction;

    private boolean hasNodeSetProperty;

    private boolean hasScriptingVars;

    public void setScriptless(boolean s) {
        scriptless = s;
    }

    public boolean isScriptless() {
        return scriptless;
    }

    public void setHasNodeUseBean(boolean u) {
        hasNodeUseBean = u;
    }

    public boolean hasNodeUseBean() {
        return hasNodeUseBean;
    }

    public void setHasNodeIncludeAction(boolean i) {
        hasNodeIncludeAction = i;
    }

    public boolean hasNodeIncludeAction() {
        return hasNodeIncludeAction;
    }

    public void setHasNodeParamAction(boolean i) {
        hasNodeParamAction = i;
    }

    public boolean hasNodeParamAction() {
        return hasNodeParamAction;
    }

    public void setHasNodeSetProperty(boolean s) {
        hasNodeSetProperty = s;
    }

    public boolean hasNodeSetProperty() {
        return hasNodeSetProperty;
    }

    public void setHasScriptingVars(boolean s) {
        hasScriptingVars = s;
    }

    public boolean hasScriptingVars() {
        return hasScriptingVars;
    }
}