package org.apache.jasper.el;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

public final class ELContextImplVariableMapperImpl extends VariableMapper {

    private Map<String, ValueExpression> vars;

    @Override
    public ValueExpression resolveVariable(String variable) {
        if (vars == null) {
            return null;
        }
        return vars.get(variable);
    }

    @Override
    public ValueExpression setVariable(String variable,
            ValueExpression expression) {
        if (vars == null)
            vars = new HashMap<String, ValueExpression>();
        return vars.put(variable, expression);
    }

}