package org.apache.jasper.compiler;

import javax.servlet.jsp.tagext.TagAttributeInfo;

public class TagFileProcessorTagFileDirectiveVisitorNameEntry {
    private String type;

    private Node node;

    private TagAttributeInfo attr;

    public TagFileProcessorTagFileDirectiveVisitorNameEntry(String type, Node node, TagAttributeInfo attr) {
        this.type = type;
        this.node = node;
        this.attr = attr;
    }

    public String getType() {
        return type;
    }

    public Node getNode() {
        return node;
    }

    public TagAttributeInfo getTagAttributeInfo() {
        return attr;
    }
}