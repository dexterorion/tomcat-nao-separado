package org.apache.jasper.compiler;

import java.util.HashMap;

import org.apache.jasper.compiler.tagplugin.TagPluginContext;

public class TagPluginContextImpl1 implements TagPluginContext {
    private final NodeCustomTag node;
    private NodeNodes curNodes;
    private PageInfo pageInfo;
    private HashMap<String, Object> pluginAttributes;

    public TagPluginContextImpl1(NodeCustomTag n, PageInfo pageInfo) {
        this.node = n;
        this.pageInfo = pageInfo;
        curNodes = new NodeNodes();
        n.setAtETag(curNodes);
        curNodes = new NodeNodes();
        n.setAtSTag(curNodes);
        n.setUseTagPlugin(true);
        pluginAttributes = new HashMap<String, Object>();
    }

    @Override
    public TagPluginContext getParentContext() {
        Node parent = node.getParent();
        if (! (parent instanceof NodeCustomTag)) {
            return null;
        }
        return ((NodeCustomTag) parent).getTagPluginContext();
    }

    @Override
    public void setPluginAttribute(String key, Object value) {
        pluginAttributes.put(key, value);
    }

    @Override
    public Object getPluginAttribute(String key) {
        return pluginAttributes.get(key);
    }

    @Override
    public boolean isScriptless() {
        return node.getNodeChildInfo().isScriptless();
    }

    @Override
    public boolean isConstantAttribute(String attribute) {
        NodeJspAttribute attr = getNodeAttribute(attribute);
        if (attr == null)
            return false;
        return attr.isLiteral();
    }

    @Override
    public String getConstantAttribute(String attribute) {
        NodeJspAttribute attr = getNodeAttribute(attribute);
        if (attr == null)
            return null;
        return attr.getValue();
    }

    @Override
    public boolean isAttributeSpecified(String attribute) {
        return getNodeAttribute(attribute) != null;
    }

    @Override
    public String getTemporaryVariableName() {
        return node.getRoot().nextTemporaryVariableName();
    }

    @Override
    public void generateImport(String imp) {
        pageInfo.addImport(imp);
    }

    @Override
    public void generateDeclaration(String id, String text) {
        if (pageInfo.isPluginDeclared(id)) {
            return;
        }
        curNodes.add(new NodeDeclaration(text, node.getStart(), null));
    }

    @Override
    public void generateJavaSource(String sourceCode) {
        curNodes.add(new NodeScriptlet(sourceCode, node.getStart(),
                                        null));
    }

    @Override
    public void generateAttribute(String attributeName) {
        curNodes.add(new NodeAttributeGenerator(node.getStart(),
                                                 attributeName,
                                                 node));
    }

    @Override
    public void dontUseTagPlugin() {
        node.setUseTagPlugin(false);
    }

    @Override
    public void generateBody() {
        // Since we'll generate the body anyway, this is really a nop,
        // except for the fact that it lets us put the Java sources the
        // plugins produce in the correct order (w.r.t the body).
        curNodes = node.getAtETag();
    }

    @Override
    public boolean isTagFile() {
        return pageInfo.isTagFile();
    }

    private NodeJspAttribute getNodeAttribute(String attribute) {
        NodeJspAttribute[] attrs = node.getJspAttributes();
        for (int i=0; attrs != null && i < attrs.length; i++) {
            if (attrs[i].getName().equals(attribute)) {
                return attrs[i];
            }
        }
        return null;
    }
}