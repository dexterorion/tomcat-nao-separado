package org.apache.jasper.compiler;

import java.util.List;
import java.util.Vector;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeTagDirective extends Node {
    private Vector<String> imports;

    public NodeTagDirective(Attributes attrs, Mark start, Node parent) {
        this(JSP_TAG_DIRECTIVE_ACTION, attrs, null, null, start, parent);
    }

    public NodeTagDirective(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, TAG_DIRECTIVE_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
        imports = new Vector<String>();
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    /**
     * Parses the comma-separated list of class or package names in the
     * given attribute value and adds each component to this NodePageDirective's
     * vector of imported classes and packages.
     * 
     * @param value
     *            A comma-separated string of imports.
     */
    public void addImport(String value) {
        int start = 0;
        int index;
        while ((index = value.indexOf(',', start)) != -1) {
            imports.add(value.substring(start, index).trim());
            start = index + 1;
        }
        if (start == 0) {
            // No comma found
            imports.add(value.trim());
        } else {
            imports.add(value.substring(start).trim());
        }
    }

    public List<String> getImports() {
        return imports;
    }
}