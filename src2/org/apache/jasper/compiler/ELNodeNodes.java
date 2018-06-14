package org.apache.jasper.compiler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jasper.JasperException;

public class ELNodeNodes {

    /* Name used for creating a map for the functions in this
       EL expression, for communication to Generator.
     */
    private String mapName = null;        // The function map associated this EL
    private final List<ELNode> list;

    public ELNodeNodes() {
        list = new ArrayList<ELNode>();
    }

    public void add(ELNode en) {
        list.add(en);
    }

    /**
     * Visit the nodes in the list with the supplied visitor
     * @param v The visitor used
     */
    public void visit(ELNodeVisitor v) throws JasperException {
        Iterator<ELNode> iter = list.iterator();
        while (iter.hasNext()) {
            ELNode n = iter.next();
            n.accept(v);
        }
    }

    public Iterator<ELNode> iterator() {
        return list.iterator();
    }

    public boolean isEmpty() {
        return list.size() == 0;
    }

    /**
     * @return true if the expression contains a ${...}
     */
    public boolean containsEL() {
        Iterator<ELNode> iter = list.iterator();
        while (iter.hasNext()) {
            ELNode n = iter.next();
            if (n instanceof ELNodeRoot) {
                return true;
            }
        }
        return false;
    }

    public void setMapName(String name) {
        this.mapName = name;
    }

    public String getMapName() {
        return mapName;
    }

}