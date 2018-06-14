package org.apache.jasper.compiler;

import java.util.List;
import java.util.Vector;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodePageDirective extends Node {

    private Vector<String> imports;

    public NodePageDirective(Attributes attrs, Mark start, Node parent) {
        this(JSP_PAGE_DIRECTIVE_ACTION, attrs, null, null, start, parent);
    }

    public NodePageDirective(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, PAGE_DIRECTIVE_ACTION, attrs, nonTaglibXmlnsAttrs,
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
            imports.add(validateImport(value.substring(start, index)));
            start = index + 1;
        }
        if (start == 0) {
            // No comma found
            imports.add(validateImport(value));
        } else {
            imports.add(validateImport(value.substring(start)));
        }
    }

    public List<String> getImports() {
        return imports;
    }

    /**
     * Just need enough validation to make sure nothing strange is going on.
     * The compiler will validate this thoroughly when it tries to compile
     * the resulting .java file.
     */
    private String validateImport(String importEntry) {
        // This should either be a fully-qualified class name or a package
        // name with a wildcard
        if (importEntry.indexOf(';') > -1) {
            throw new IllegalArgumentException(
                    Localizer.getMessage("jsp.error.page.invalid.import"));
        }
        return importEntry.trim();
    }
}