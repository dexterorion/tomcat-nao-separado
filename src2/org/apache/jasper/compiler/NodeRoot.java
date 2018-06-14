package org.apache.jasper.compiler;

import org.apache.jasper.Constants28;
import org.apache.jasper.JasperException;

/**
 * Represents the root of a Jsp page or Jsp document
 */
public class NodeRoot extends Node {

    private NodeRoot parentRoot;

    private boolean isXmlSyntax;

    // Source encoding of the page containing this Root
    private String pageEnc;

    // Page encoding specified in JSP config element
    private String jspConfigPageEnc;

    /*
     * Flag indicating if the default page encoding is being used (only
     * applicable with standard syntax).
     * 
     * True if the page does not provide a page directive with a
     * 'contentType' attribute (or the 'contentType' attribute doesn't have
     * a CHARSET value), the page does not provide a page directive with a
     * 'pageEncoding' attribute, and there is no JSP configuration element
     * page-encoding whose URL pattern matches the page.
     */
    private boolean isDefaultPageEncoding;

    /*
     * Indicates whether an encoding has been explicitly specified in the
     * page's XML prolog (only used for pages in XML syntax). This
     * information is used to decide whether a translation error must be
     * reported for encoding conflicts.
     */
    private boolean isEncodingSpecifiedInProlog;

    /*
     * Indicates whether an encoding has been explicitly specified in the
     * page's dom.
     */
    private boolean isBomPresent;

    /*
     * Sequence number for temporary variables.
     */
    private int tempSequenceNumber = 0;

    /*
     * Constructor.
     */
    public NodeRoot(Mark start, Node parent, boolean isXmlSyntax) {
        super(start, parent);
        this.isXmlSyntax = isXmlSyntax;
        this.setqName(JSP_ROOT_ACTION);
        this.setLocalName(ROOT_ACTION);

        // Figure out and set the parent root
        Node r = parent;
        while ((r != null) && !(r instanceof NodeRoot))
            r = r.getParent();
        parentRoot = (NodeRoot) r;
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    public boolean isXmlSyntax() {
        return isXmlSyntax;
    }

    /*
     * Sets the encoding specified in the JSP config element whose URL
     * pattern matches the page containing this Root.
     */
    public void setJspConfigPageEncoding(String enc) {
        jspConfigPageEnc = enc;
    }

    /*
     * Gets the encoding specified in the JSP config element whose URL
     * pattern matches the page containing this Root.
     */
    public String getJspConfigPageEncoding() {
        return jspConfigPageEnc;
    }

    public void setPageEncoding(String enc) {
        pageEnc = enc;
    }

    public String getPageEncoding() {
        return pageEnc;
    }

    public void setIsDefaultPageEncoding(boolean isDefault) {
        isDefaultPageEncoding = isDefault;
    }

    public boolean isDefaultPageEncoding() {
        return isDefaultPageEncoding;
    }

    public void setIsEncodingSpecifiedInProlog(boolean isSpecified) {
        isEncodingSpecifiedInProlog = isSpecified;
    }

    public boolean isEncodingSpecifiedInProlog() {
        return isEncodingSpecifiedInProlog;
    }

    public void setIsBomPresent(boolean isBom) {
        isBomPresent = isBom;
    }

    public boolean isBomPresent() {
        return isBomPresent;
    }

    /**
     * @return The enclosing root to this Root. Usually represents the page
     *         that includes this one.
     */
    public NodeRoot getParentRoot() {
        return parentRoot;
    }
    
    /**
     * Generates a new temporary variable name.
     */
    public String nextTemporaryVariableName() {
        if (parentRoot == null) {
            return Constants28.getTempVariableNamePrefix() + (tempSequenceNumber++);
        } else {
            return parentRoot.nextTemporaryVariableName();
        }
        
    }
}