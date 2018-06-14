package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/*
 * First-pass Visitor for JspRoot nodes (representing jsp:root elements)
 * and TablibDirective nodes, ignoring any other nodes.
 *
 * The purpose of this Visitor is to collect the attributes of the
 * top-level jsp:root and those of the jsp:root elements of any included
 * pages, and add them to the jsp:root element of the XML view.
 * In addition, this Visitor converts any taglib directives into xmlns:
 * attributes and adds them to the jsp:root element of the XML view.
 */
public class PageDataImplFirstPassVisitor
            extends NodeVisitor implements TagConstants {

    private NodeRoot root;
    private AttributesImpl rootAttrs;
    private PageInfo pageInfo;

    // Prefix for the 'id' attribute
    private String jspIdPrefix;

    /*
     * Constructor
     */
    public PageDataImplFirstPassVisitor(NodeRoot root, PageInfo pageInfo) {
        this.root = root;
        this.pageInfo = pageInfo;
        this.rootAttrs = new AttributesImpl();
        this.rootAttrs.addAttribute("", "", "version", "CDATA",
                                    PageDataImpl.getJspVersion());
        this.jspIdPrefix = "jsp";
    }

    @Override
    public void visit(NodeRoot n) throws JasperException {
        visitBody(n);
        if (n == root) {
            /*
             * Top-level page.
             *
             * Add
             *   xmlns:jsp="http://java.sun.com/JSP/Page"
             * attribute only if not already present.
             */
            if (!JSP_URI.equals(rootAttrs.getValue("xmlns:jsp"))) {
                rootAttrs.addAttribute("", "", "xmlns:jsp", "CDATA",
                                       JSP_URI);
            }

            if (pageInfo.isJspPrefixHijacked()) {
                /*
                 * 'jsp' prefix has been hijacked, that is, bound to a
                 * namespace other than the JSP namespace. This means that
                 * when adding an 'id' attribute to each element, we can't
                 * use the 'jsp' prefix. Therefore, create a new prefix 
                 * (one that is unique across the translation unit) for use
                 * by the 'id' attribute, and bind it to the JSP namespace
                 */
                jspIdPrefix += "jsp";
                while (pageInfo.containsPrefix(jspIdPrefix)) {
                    jspIdPrefix += "jsp";
                }
                rootAttrs.addAttribute("", "", "xmlns:" + jspIdPrefix,
                                       "CDATA", JSP_URI);
            }

            root.setAttributes(rootAttrs);
        }
    }

    @Override
    public void visit(NodeJspRoot n) throws JasperException {
        addAttributes(n.getTaglibAttributes());
        addAttributes(n.getNonTaglibXmlnsAttributes());
        addAttributes(n.getAttributes());

        visitBody(n);
    }

    /*
     * Converts taglib directive into "xmlns:..." attribute of jsp:root
     * element.
     */
    @Override
    public void visit(NodeTaglibDirective n) throws JasperException {
        Attributes attrs = n.getAttributes();
        if (attrs != null) {
            String qName = "xmlns:" + attrs.getValue("prefix");
            /*
             * According to javadocs of org.xml.sax.helpers.AttributesImpl,
             * the addAttribute method does not check to see if the
             * specified attribute is already contained in the list: This
             * is the application's responsibility!
             */
            if (rootAttrs.getIndex(qName) == -1) {
                String location = attrs.getValue("uri");
                if (location != null) {
                    if (location.startsWith("/")) {
                        location = URN_JSPTLD + location;
                    }
                    rootAttrs.addAttribute("", "", qName, "CDATA",
                                           location);
                } else {
                    location = attrs.getValue("tagdir");
                    rootAttrs.addAttribute("", "", qName, "CDATA",
                                           URN_JSPTAGDIR + location);
                }
            }
        }
    }

    public String getJspIdPrefix() {
        return jspIdPrefix;
    }

    private void addAttributes(Attributes attrs) {
        if (attrs != null) {
            int len = attrs.getLength();

            for (int i=0; i<len; i++) {
                String qName = attrs.getQName(i);
                if ("version".equals(qName)) {
                    continue;
                }

                // Bugzilla 35252: http://issues.apache.org/bugzilla/show_bug.cgi?id=35252
                if(rootAttrs.getIndex(qName) == -1) {
                    rootAttrs.addAttribute(attrs.getURI(i),
                                           attrs.getLocalName(i),
                                           qName,
                                           attrs.getType(i),
                                           attrs.getValue(i));
                }
            }
        }
    }
}