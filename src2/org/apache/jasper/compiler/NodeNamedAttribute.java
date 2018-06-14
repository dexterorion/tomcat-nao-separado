package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class NodeNamedAttribute extends Node {

    // A unique temporary variable name suitable for code generation
    private String temporaryVariableName;

    // True if this node is to be trimmed, or false otherwise
    private boolean trim = true;

    // True if this attribute should be omitted from the output if
    // used with a <jsp:element>, otherwise false
    private NodeJspAttribute omit;

    private NodeChildInfo childInfo;

    private String name;

    private String localName;

    private String prefix;

    public NodeNamedAttribute(Attributes attrs, Mark start, Node parent) {
        this(JSP_ATTRIBUTE_ACTION, attrs, null, null, start, parent);
    }

    public NodeNamedAttribute(String qName, Attributes attrs,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {

        super(qName, ATTRIBUTE_ACTION, attrs, nonTaglibXmlnsAttrs,
                taglibAttrs, start, parent);
        if ("false".equals(this.getAttributeValue("trim"))) {
            // (if null or true, leave default of true)
            trim = false;
        }
        childInfo = new NodeChildInfo();
        name = this.getAttributeValue("name");
        if (name != null) {
            // Mandatory attribute "name" will be checked in Validator
            localName = name;
            int index = name.indexOf(':');
            if (index != -1) {
                prefix = name.substring(0, index);
                localName = name.substring(index + 1);
            }
        }
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public NodeChildInfo getNodeChildInfo() {
        return this.childInfo;
    }

    public boolean isTrim() {
        return trim;
    }

    public void setOmit(NodeJspAttribute omit) {
        this.omit = omit;
    }
    
    public NodeJspAttribute getOmit() {
        return omit;
    }

    /**
     * @return A unique temporary variable name to store the result in.
     *         (this probably could go elsewhere, but it's convenient here)
     */
    public String getTemporaryVariableName() {
        if (temporaryVariableName == null) {
            temporaryVariableName = getRoot().nextTemporaryVariableName();
        }
        return temporaryVariableName;
    }

    /*
     * Get the attribute value from this named attribute (<jsp:attribute>).
     * Since this method is only for attributes that are not rtexpr, we can
     * assume the body of the jsp:attribute is a template text.
     */
    @Override
    public String getText() {
    	// According to JSP 2.0, if the body of the <jsp:attribute>
        // action is empty, it is equivalent of specifying "" as the value
        // of the attribute.
        String text = "";
        if (getBody() != null) {
            AttributeVisitor attributeVisitor = new AttributeVisitor();
            try {
                getBody().visit(attributeVisitor);
            } catch (JasperException e) {
            }
            text = attributeVisitor.getAttrValue();
        }

        return text;
    }
}