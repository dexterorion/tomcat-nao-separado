package org.apache.jasper.compiler;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jasper.JasperException;

public class Fvisitor extends ELNodeVisitor {
    final ArrayList<ELNodeFunction> funcs = new ArrayList<ELNodeFunction>();
    final HashMap<String, String> keyMap = new HashMap<String, String>();
    @Override
    public void visit(ELNodeFunction n) throws JasperException {
        String key = n.getPrefix() + ":" + n.getName();
        if (! keyMap.containsKey(key)) {
            keyMap.put(key,"");
            funcs.add(n);
        }
    }
}