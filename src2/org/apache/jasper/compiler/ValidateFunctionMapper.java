package org.apache.jasper.compiler;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.el.FunctionMapper2;

public class ValidateFunctionMapper extends FunctionMapper2 {

    private HashMap<String, Method> fnmap = new HashMap<String, Method>();

    public void mapFunction(String fnQName, Method method) {
        fnmap.put(fnQName, method);
    }

    @Override
    public Method resolveFunction(String prefix, String localName) {
        return this.fnmap.get(prefix + ":" + localName);
    }
}