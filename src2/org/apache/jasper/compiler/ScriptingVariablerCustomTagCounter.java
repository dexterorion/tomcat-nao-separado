package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/*
 * Assigns an identifier (of type integer) to every custom tag, in order
 * to help identify, for every custom tag, the scripting variables that it
 * needs to declare.
 */
public class ScriptingVariablerCustomTagCounter extends NodeVisitor {

    private int count;
    private NodeCustomTag parent;

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        n.setNodeCustomTagParent(parent);
        NodeCustomTag tmpParent = parent;
        parent = n;
        visitBody(n);
        parent = tmpParent;
        n.setNumCount(new Integer(count++));
    }
}