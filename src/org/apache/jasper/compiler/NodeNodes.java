package org.apache.jasper.compiler;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.jasper.JasperException;

public class NodeNodes {

    private List<Node> list;

    private NodeRoot root; // null if this is not a page

    private boolean generatedInBuffer;

    public NodeNodes() {
        list = new Vector<Node>();
    }

    public NodeNodes(NodeRoot root) {
        this.root = root;
        list = new Vector<Node>();
        list.add(root);
    }

    /**
     * Appends a node to the list
     * 
     * @param n
     *            The node to add
     */
    public void add(Node n) {
        list.add(n);
        root = null;
    }

    /**
     * Removes the given node from the list.
     * 
     * @param n
     *            The node to be removed
     */
    public void remove(Node n) {
        list.remove(n);
    }

    /**
     * Visit the nodes in the list with the supplied visitor
     * 
     * @param v
     *            The visitor used
     */
    public void visit(NodeVisitor v) throws JasperException {
        Iterator<Node> iter = list.iterator();
        while (iter.hasNext()) {
            Node n = iter.next();
            n.accept(v);
        }
    }

    public int size() {
        return list.size();
    }

    public Node getNode(int index) {
        Node n = null;
        try {
            n = list.get(index);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        return n;
    }

    public NodeRoot getRoot() {
        return root;
    }

    public boolean isGeneratedInBuffer() {
        return generatedInBuffer;
    }

    public void setGeneratedInBuffer(boolean g) {
        generatedInBuffer = g;
    }
}