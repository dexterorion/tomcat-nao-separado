package org.apache.jasper.compiler;

import java.util.HashMap;

public class SmapUtilPreScanVisitor extends NodeVisitor {

    private HashMap<String, SmapStratum> map = new HashMap<String, SmapStratum>();

    @Override
    public void doVisit(Node n) {
        String inner = n.getInnerClassName();
        if (inner != null && !map.containsKey(inner)) {
            map.put(inner, new SmapStratum("JSP"));
        }
    }

    public HashMap<String, SmapStratum> getMap() {
        return map;
    }
}