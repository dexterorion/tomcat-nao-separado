package org.apache.jasper.compiler;

import org.xml.sax.Attributes;

public abstract class NodeScriptingElement extends Node {

    public NodeScriptingElement(String qName, String localName, String text,
            Mark start, Node parent) {
        super(qName, localName, text, start, parent);
    }

    public NodeScriptingElement(String qName, String localName,
            Attributes nonTaglibXmlnsAttrs, Attributes taglibAttrs,
            Mark start, Node parent) {
        super(qName, localName, null, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);
    }

    /**
     * When this node was created from a JSP page in JSP syntax, its text
     * was stored as a String in the "text" field, whereas when this node
     * was created from a JSP document, its text was stored as one or more
     * TemplateText nodes in its body. This method handles either case.
     * 
     * @return The text string
     */
    @Override
    public String getText() {
        String ret = super.getText();
        if (ret == null) {
            if (getBody() != null) {
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < getBody().size(); i++) {
                    buf.append(getBody().getNode(i).getText());
                }
                ret = buf.toString();
            } else {
                // Nulls cause NPEs further down the line
                ret = "";
            }
        }
        return ret;
    }

    /**
     * For the same reason as above, the source line information in the
     * contained TemplateText node should be used.
     */
    @Override
    public Mark getStart() {
        if (getText() == null && getBody() != null && getBody().size() > 0) {
            return getBody().getNode(0).getStart();
        } else {
            return super.getStart();
        }
    }
}