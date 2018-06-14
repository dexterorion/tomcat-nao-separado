package org.apache.jasper.compiler;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.servlet.jsp.tagext.TagAttributeInfo;

public class NodeJspAttribute {

    private String qName;

    private String uri;

    private String localName;

    private String value;

    private boolean expression;

    private boolean dynamic;

    private final ELNodeNodes el;

    private final TagAttributeInfo tai;

    // If true, this NodeJspAttribute represents a <jsp:attribute>
    private boolean namedAttribute;

    // The node in the parse tree for the NodeNamedAttribute
    private NodeNamedAttribute namedAttributeNode;

    public NodeJspAttribute(TagAttributeInfo tai, String qName, String uri,
            String localName, String value, boolean expr, ELNodeNodes el,
            boolean dyn) {
        this.qName = qName;
        this.uri = uri;
        this.localName = localName;
        this.value = value;
        this.namedAttributeNode = null;
        this.expression = expr;
        this.el = el;
        this.dynamic = dyn;
        this.namedAttribute = false;
        this.tai = tai;
    }

    /**
     * Allow node to validate itself
     * 
     * @param ef
     * @param ctx
     * @throws ELException
     */
    public void validateEL(ExpressionFactory ef, ELContext ctx)
            throws ELException {
        if (this.el != null) {
            // determine exact type
            ef.createValueExpression(ctx, this.value, String.class);
        }
    }

    /**
     * Use this constructor if the NodeJspAttribute represents a named
     * attribute. In this case, we have to store the nodes of the body of
     * the attribute.
     */
    public NodeJspAttribute(NodeNamedAttribute na, TagAttributeInfo tai, boolean dyn) {
        this.qName = na.getName();
        this.localName = na.getLocalName();
        this.value = null;
        this.namedAttributeNode = na;
        this.expression = false;
        this.el = null;
        this.dynamic = dyn;
        this.namedAttribute = true;
        this.tai = null;
    }

    /**
     * @return The name of the attribute
     */
    public String getName() {
        return qName;
    }

    /**
     * @return The local name of the attribute
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * @return The namespace of the attribute, or null if in the default
     *         namespace
     */
    public String getURI() {
        return uri;
    }

    public TagAttributeInfo getTagAttributeInfo() {
        return this.tai;
    }

    /**
     * 
     * @return return true if there's TagAttributeInfo meaning we need to
     *         assign a ValueExpression
     */
    public boolean isDeferredInput() {
        return (this.tai != null) ? this.tai.isDeferredValue() : false;
    }

    /**
     * 
     * @return return true if there's TagAttributeInfo meaning we need to
     *         assign a MethodExpression
     */
    public boolean isDeferredMethodInput() {
        return (this.tai != null) ? this.tai.isDeferredMethod() : false;
    }

    public String getExpectedTypeName() {
        if (this.tai != null) {
            if (this.isDeferredInput()) {
                return this.tai.getExpectedTypeName();
            } else if (this.isDeferredMethodInput()) {
                String m = this.tai.getMethodSignature();
                if (m != null) {
                    int rti = m.trim().indexOf(' ');
                    if (rti > 0) {
                        return m.substring(0, rti).trim();
                    }
                }
            }
        }
        return "java.lang.Object";
    }
    
    public String[] getParameterTypeNames() {
        if (this.tai != null) {
            if (this.isDeferredMethodInput()) {
                String m = this.tai.getMethodSignature();
                if (m != null) {
                    m = m.trim();
                    m = m.substring(m.indexOf('(') + 1);
                    m = m.substring(0, m.length() - 1);
                    if (m.trim().length() > 0) {
                        String[] p = m.split(",");
                        for (int i = 0; i < p.length; i++) {
                            p[i] = p[i].trim();
                        }
                        return p;
                    }
                }
            }
        }
        return new String[0];
    }

    /**
     * Only makes sense if namedAttribute is false.
     * 
     * @return the value for the attribute, or the expression string
     *         (stripped of "<%=", "%>", "%=", or "%" but containing "${"
     *         and "}" for EL expressions)
     */
    public String getValue() {
        return value;
    }

    /**
     * Only makes sense if namedAttribute is true.
     * 
     * @return the nodes that evaluate to the body of this attribute.
     */
    public NodeNamedAttribute getNodeNamedAttributeNode() {
        return namedAttributeNode;
    }

    /**
     * @return true if the value represents a traditional rtexprvalue
     */
    public boolean isExpression() {
        return expression;
    }

    /**
     * @return true if the value represents a NodeNamedAttribute value.
     */
    public boolean isNodeNamedAttribute() {
        return namedAttribute;
    }

    /**
     * @return true if the value represents an expression that should be fed
     *         to the expression interpreter
     *         false for string literals or rtexprvalues that should not be
     *         interpreted or reevaluated
     */
    public boolean isELInterpreterInput() {
        return el != null || this.isDeferredInput()
                || this.isDeferredMethodInput();
    }

    /**
     * @return true if the value is a string literal known at translation
     *         time.
     */
    public boolean isLiteral() {
        return !expression && (el == null) && !namedAttribute;
    }

    /**
     * <code>true</code> if the attribute is a "dynamic" attribute of a
     * custom tag that implements DynamicAttributes interface. That is,
     * a random extra attribute that is not declared by the tag.
     */
    public boolean isDynamic() {
        return dynamic;
    }

    public ELNodeNodes getEL() {
        return el;
    }
}