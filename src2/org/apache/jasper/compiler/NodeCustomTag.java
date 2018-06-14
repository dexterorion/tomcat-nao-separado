package org.apache.jasper.compiler;

import java.util.List;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.JspIdConsumer;
import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.TryCatchFinally;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.tagplugin.TagPluginContext;
import org.xml.sax.Attributes;

public class NodeCustomTag extends Node {

    private String uri;

    private String prefix;

    private NodeJspAttribute[] jspAttrs;

    private TagData tagData;

    private String tagHandlerPoolName;

    private TagInfo tagInfo;

    private TagFileInfo tagFileInfo;

    private Class<?> tagHandlerClass;

    private VariableInfo[] varInfos;

    private int customNestingLevel;

    private NodeChildInfo childInfo;

    private boolean implementsIterationTag;

    private boolean implementsBodyTag;

    private boolean implementsTryCatchFinally;

    private boolean implementsJspIdConsumer;

    private boolean implementsSimpleTag;

    private boolean implementsDynamicAttributes;

    private List<Object> atBeginScriptingVars;

    private List<Object> atEndScriptingVars;

    private List<Object> nestedScriptingVars;

    private NodeCustomTag customTagParent;

    private Integer numCount;

    private boolean useTagPlugin;

    private TagPluginContext tagPluginContext;

    /**
     * The following two fields are used for holding the Java scriptlets
     * that the tag plugins may generate. Meaningful only if useTagPlugin is
     * true; Could move them into TagPluginContextImpl, but we'll need to
     * cast tagPluginContext to TagPluginContextImpl all the time...
     */
    private NodeNodes atSTag;

    private NodeNodes atETag;

    /*
     * Constructor for custom action implemented by tag handler.
     */
    public NodeCustomTag(String qName, String prefix, String localName,
            String uri, Attributes attrs, Mark start, Node parent,
            TagInfo tagInfo, Class<?> tagHandlerClass) {
        this(qName, prefix, localName, uri, attrs, null, null, start,
                parent, tagInfo, tagHandlerClass);
    }

    /*
     * Constructor for custom action implemented by tag handler.
     */
    public NodeCustomTag(String qName, String prefix, String localName,
            String uri, Attributes attrs, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent,
            TagInfo tagInfo, Class<?> tagHandlerClass) {
        super(qName, localName, attrs, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);

        this.uri = uri;
        this.prefix = prefix;
        this.tagInfo = tagInfo;
        this.tagHandlerClass = tagHandlerClass;
        this.customNestingLevel = makeCustomNestingLevel();
        this.childInfo = new NodeChildInfo();

        this.implementsIterationTag = IterationTag.class
                .isAssignableFrom(tagHandlerClass);
        this.implementsBodyTag = BodyTag.class
                .isAssignableFrom(tagHandlerClass);
        this.implementsTryCatchFinally = TryCatchFinally.class
                .isAssignableFrom(tagHandlerClass);
        this.implementsSimpleTag = SimpleTag.class
                .isAssignableFrom(tagHandlerClass);
        this.implementsDynamicAttributes = DynamicAttributes.class
                .isAssignableFrom(tagHandlerClass);
        this.implementsJspIdConsumer = JspIdConsumer.class
                .isAssignableFrom(tagHandlerClass);
    }

    /*
     * Constructor for custom action implemented by tag file.
     */
    public NodeCustomTag(String qName, String prefix, String localName,
            String uri, Attributes attrs, Mark start, Node parent,
            TagFileInfo tagFileInfo) {
        this(qName, prefix, localName, uri, attrs, null, null, start,
                parent, tagFileInfo);
    }

    /*
     * Constructor for custom action implemented by tag file.
     */
    public NodeCustomTag(String qName, String prefix, String localName,
            String uri, Attributes attrs, Attributes nonTaglibXmlnsAttrs,
            Attributes taglibAttrs, Mark start, Node parent,
            TagFileInfo tagFileInfo) {

        super(qName, localName, attrs, nonTaglibXmlnsAttrs, taglibAttrs,
                start, parent);

        this.uri = uri;
        this.prefix = prefix;
        this.tagFileInfo = tagFileInfo;
        this.tagInfo = tagFileInfo.getTagInfo();
        this.customNestingLevel = makeCustomNestingLevel();
        this.childInfo = new NodeChildInfo();

        this.implementsIterationTag = false;
        this.implementsBodyTag = false;
        this.implementsTryCatchFinally = false;
        this.implementsSimpleTag = true;
        this.implementsJspIdConsumer = false;
        this.implementsDynamicAttributes = tagInfo.hasDynamicAttributes();
    }

    @Override
    public void accept(NodeVisitor v) throws JasperException {
        v.visit(this);
    }

    /**
     * @return The URI namespace that this custom action belongs to
     */
    public String getURI() {
        return this.uri;
    }

    /**
     * @return The tag prefix
     */
    public String getPrefix() {
        return prefix;
    }

    public void setJspAttributes(NodeJspAttribute[] jspAttrs) {
        this.jspAttrs = jspAttrs;
    }

    public NodeJspAttribute[] getJspAttributes() {
        return jspAttrs;
    }

    public NodeChildInfo getNodeChildInfo() {
        return childInfo;
    }

    public void setTagData(TagData tagData) {
        this.tagData = tagData;
        this.varInfos = tagInfo.getVariableInfo(tagData);
        if (this.varInfos == null) {
            this.varInfos = getZeroVariableInfo();
        }
    }

    public TagData getTagData() {
        return tagData;
    }

    public void setTagHandlerPoolName(String s) {
        tagHandlerPoolName = s;
    }

    public String getTagHandlerPoolName() {
        return tagHandlerPoolName;
    }

    public TagInfo getTagInfo() {
        return tagInfo;
    }

    public TagFileInfo getTagFileInfo() {
        return tagFileInfo;
    }

    /*
     * @return true if this custom action is supported by a tag file, false
     * otherwise
     */
    public boolean isTagFile() {
        return tagFileInfo != null;
    }

    public Class<?> getTagHandlerClass() {
        return tagHandlerClass;
    }

    public void setTagHandlerClass(Class<?> hc) {
        tagHandlerClass = hc;
    }

    public boolean implementsIterationTag() {
        return implementsIterationTag;
    }

    public boolean implementsBodyTag() {
        return implementsBodyTag;
    }

    public boolean implementsTryCatchFinally() {
        return implementsTryCatchFinally;
    }

    public boolean implementsJspIdConsumer() {
        return implementsJspIdConsumer;
    }

    public boolean implementsSimpleTag() {
        return implementsSimpleTag;
    }

    public boolean implementsDynamicAttributes() {
        return implementsDynamicAttributes;
    }

    public TagVariableInfo[] getTagVariableInfos() {
        return tagInfo.getTagVariableInfos();
    }

    public VariableInfo[] getVariableInfos() {
        return varInfos;
    }

    public void setNodeCustomTagParent(NodeCustomTag n) {
        this.customTagParent = n;
    }

    public NodeCustomTag getNodeCustomTagParent() {
        return this.customTagParent;
    }

    public void setNumCount(Integer count) {
        this.numCount = count;
    }

    public Integer getNumCount() {
        return this.numCount;
    }

    public void setScriptingVars(List<Object> vec, int scope) {
        switch (scope) {
        case 0:
            this.atBeginScriptingVars = vec;
            break;
        case 1:
            this.atEndScriptingVars = vec;
            break;
        case 2:
            this.nestedScriptingVars = vec;
            break;
        }
    }

    /*
     * Gets the scripting variables for the given scope that need to be
     * declared.
     */
    public List<Object> getScriptingVars(int scope) {
        List<Object> vec = null;

        switch (scope) {
        case 0:
            vec = this.atBeginScriptingVars;
            break;
        case 1:
            vec = this.atEndScriptingVars;
            break;
        case 2:
            vec = this.nestedScriptingVars;
            break;
        }

        return vec;
    }

    /*
     * Gets this custom tag's custom nesting level, which is given as the
     * number of times this custom tag is nested inside itself.
     */
    public int getCustomNestingLevel() {
        return customNestingLevel;
    }

    /**
     * Checks to see if the attribute of the given name is of type
     * JspFragment.
     */
    public boolean checkIfAttributeIsJspFragment(String name) {
        boolean result = false;

        TagAttributeInfo[] attributes = tagInfo.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getName().equals(name)
                    && attributes[i].isFragment()) {
                result = true;
                break;
            }
        }

        return result;
    }

    public void setUseTagPlugin(boolean use) {
        useTagPlugin = use;
    }

    public boolean useTagPlugin() {
        return useTagPlugin;
    }

    public void setTagPluginContext(TagPluginContext tagPluginContext) {
        this.tagPluginContext = tagPluginContext;
    }

    public TagPluginContext getTagPluginContext() {
        return tagPluginContext;
    }

    public void setAtSTag(NodeNodes sTag) {
        atSTag = sTag;
    }

    public NodeNodes getAtSTag() {
        return atSTag;
    }

    public void setAtETag(NodeNodes eTag) {
        atETag = eTag;
    }

    public NodeNodes getAtETag() {
        return atETag;
    }

    /*
     * Computes this custom tag's custom nesting level, which corresponds to
     * the number of times this custom tag is nested inside itself.
     * 
     * Example:
     * 
     * <g:h> <a:b> -- nesting level 0 <c:d> <e:f> <a:b> -- nesting level 1
     * <a:b> -- nesting level 2 </a:b> </a:b> <a:b> -- nesting level 1
     * </a:b> </e:f> </c:d> </a:b> </g:h>
     * 
     * @return Custom tag's nesting level
     */
    private int makeCustomNestingLevel() {
        int n = 0;
        Node p = getParent();
        while (p != null) {
            if ((p instanceof NodeCustomTag)
                    && getqName().equals(((NodeCustomTag) p).getqName())) {
                n++;
            }
            p = p.getParent();
        }
        return n;
    }

    /**
     * Returns true if this custom action has an empty body, and false
     * otherwise.
     * 
     * A custom action is considered to have an empty body if the following
     * holds true: - getBody() returns null, or - all immediate children are
     * jsp:attribute actions, or - the action's jsp:body is empty.
     */
    public boolean hasEmptyBody() {
        boolean hasEmptyBody = true;
        NodeNodes nodes = getBody();
        if (nodes != null) {
            int numChildNodes = nodes.size();
            for (int i = 0; i < numChildNodes; i++) {
                Node n = nodes.getNode(i);
                if (!(n instanceof NodeNamedAttribute)) {
                    if (n instanceof NodeJspBody) {
                        hasEmptyBody = (n.getBody() == null);
                    } else {
                        hasEmptyBody = false;
                    }
                    break;
                }
            }
        }

        return hasEmptyBody;
    }
}