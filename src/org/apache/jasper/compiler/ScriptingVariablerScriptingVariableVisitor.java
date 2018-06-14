package org.apache.jasper.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.JasperException;

public class ScriptingVariablerScriptingVariableVisitor extends NodeVisitor {

    private ErrorDispatcher err;
    private Map<String, Integer> scriptVars;

    public ScriptingVariablerScriptingVariableVisitor(ErrorDispatcher err) {
        this.err = err;
        scriptVars = new HashMap<String,Integer>();
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        setScriptingVars(n, VariableInfo.getAtBegin());
        setScriptingVars(n, VariableInfo.getNested());
        visitBody(n);
        setScriptingVars(n, VariableInfo.getAtEnd());
    }

    private void setScriptingVars(NodeCustomTag n, int scope)
            throws JasperException {

        TagVariableInfo[] tagVarInfos = n.getTagVariableInfos();
        VariableInfo[] varInfos = n.getVariableInfos();
        if (tagVarInfos.length == 0 && varInfos.length == 0) {
            return;
        }

        List<Object> vec = new ArrayList<Object>();

        Integer ownRange = null;
        NodeCustomTag parent = n.getNodeCustomTagParent();
        if (scope == VariableInfo.getAtBegin()
                || scope == VariableInfo.getAtEnd()) {
            if (parent == null)
                ownRange = ScriptingVariabler.getMaxScope();
            else
                ownRange = parent.getNumCount();
        } else {
            // NESTED
            ownRange = n.getNumCount();
        }

        if (varInfos.length > 0) {
            for (int i=0; i<varInfos.length; i++) {
                if (varInfos[i].getScope() != scope
                        || !varInfos[i].getDeclare()) {
                    continue;
                }
                String varName = varInfos[i].getVarName();
                
                Integer currentRange = scriptVars.get(varName);
                if (currentRange == null ||
                        ownRange.compareTo(currentRange) > 0) {
                    scriptVars.put(varName, ownRange);
                    vec.add(varInfos[i]);
                }
            }
        } else {
            for (int i=0; i<tagVarInfos.length; i++) {
                if (tagVarInfos[i].getScope() != scope
                        || !tagVarInfos[i].getDeclare()) {
                    continue;
                }
                String varName = tagVarInfos[i].getNameGiven();
                if (varName == null) {
                    varName = n.getTagData().getAttributeString(
                                    tagVarInfos[i].getNameFromAttribute());
                    if (varName == null) {
                        err.jspError(n,
                                "jsp.error.scripting.variable.missing_name",
                                tagVarInfos[i].getNameFromAttribute());
                    }
                }

                Integer currentRange = scriptVars.get(varName);
                if (currentRange == null ||
                        ownRange.compareTo(currentRange) > 0) {
                    scriptVars.put(varName, ownRange);
                    vec.add(tagVarInfos[i]);
                }
            }
        }

        n.setScriptingVars(vec, scope);
    }
}