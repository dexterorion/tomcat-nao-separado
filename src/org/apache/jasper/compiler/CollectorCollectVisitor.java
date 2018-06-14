package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/**
 * A visitor for collecting information on the page and the body of
 * the custom tags.
 */
public class CollectorCollectVisitor extends NodeVisitor {

    private boolean scriptingElementSeen = false;
    private boolean usebeanSeen = false;
    private boolean includeActionSeen = false;
    private boolean paramActionSeen = false;
    private boolean setPropertySeen = false;
    private boolean hasScriptingVars = false;

    @Override
    public void visit(NodeParamAction n) throws JasperException {
        if (n.getValue().isExpression()) {
            scriptingElementSeen = true;
        }
        paramActionSeen = true;
    }

    @Override
    public void visit(NodeIncludeAction n) throws JasperException {
        if (n.getPage().isExpression()) {
            scriptingElementSeen = true;
        }
        includeActionSeen = true;
        visitBody(n);
    }

    @Override
    public void visit(NodeForwardAction n) throws JasperException {
        if (n.getPage().isExpression()) {
            scriptingElementSeen = true;
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeSetProperty n) throws JasperException {
        if (n.getValue() != null && n.getValue().isExpression()) {
            scriptingElementSeen = true;
        }
        setPropertySeen = true;
    }

    @Override
    public void visit(NodeUseBean n) throws JasperException {
        if (n.getBeanName() != null && n.getBeanName().isExpression()) {
            scriptingElementSeen = true;
        }
        usebeanSeen = true;
            visitBody(n);
    }

    @Override
    public void visit(NodePlugIn n) throws JasperException {
        if (n.getHeight() != null && n.getHeight().isExpression()) {
            scriptingElementSeen = true;
        }
        if (n.getWidth() != null && n.getWidth().isExpression()) {
            scriptingElementSeen = true;
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        // Check to see what kinds of element we see as child elements
        checkSeen( n.getNodeChildInfo(), n );
    }

    /**
     * Check all child nodes for various elements and update the given
     * ChildInfo object accordingly.  Visits body in the process.
     */
    private void checkSeen( NodeChildInfo ci, Node n )
        throws JasperException
    {
        // save values collected so far
        boolean scriptingElementSeenSave = scriptingElementSeen;
        scriptingElementSeen = false;
        boolean usebeanSeenSave = usebeanSeen;
        usebeanSeen = false;
        boolean includeActionSeenSave = includeActionSeen;
        includeActionSeen = false;
        boolean paramActionSeenSave = paramActionSeen;
        paramActionSeen = false;
        boolean setPropertySeenSave = setPropertySeen;
        setPropertySeen = false;
        boolean hasScriptingVarsSave = hasScriptingVars;
        hasScriptingVars = false;

        // Scan attribute list for expressions
        if( n instanceof NodeCustomTag ) {
            NodeCustomTag ct = (NodeCustomTag)n;
            NodeJspAttribute[] attrs = ct.getJspAttributes();
            for (int i = 0; attrs != null && i < attrs.length; i++) {
                if (attrs[i].isExpression()) {
                    scriptingElementSeen = true;
                    break;
                }
            }
        }

        visitBody(n);

        if( (n instanceof NodeCustomTag) && !hasScriptingVars) {
            NodeCustomTag ct = (NodeCustomTag)n;
            hasScriptingVars = ct.getVariableInfos().length > 0 ||
                ct.getTagVariableInfos().length > 0;
        }

        // Record if the tag element and its body contains any scriptlet.
        ci.setScriptless(! scriptingElementSeen);
        ci.setHasNodeUseBean(usebeanSeen);
        ci.setHasNodeIncludeAction(includeActionSeen);
        ci.setHasNodeParamAction(paramActionSeen);
        ci.setHasNodeSetProperty(setPropertySeen);
        ci.setHasScriptingVars(hasScriptingVars);

        // Propagate value of scriptingElementSeen up.
        scriptingElementSeen = scriptingElementSeen || scriptingElementSeenSave;
        usebeanSeen = usebeanSeen || usebeanSeenSave;
        setPropertySeen = setPropertySeen || setPropertySeenSave;
        includeActionSeen = includeActionSeen || includeActionSeenSave;
        paramActionSeen = paramActionSeen || paramActionSeenSave;
        hasScriptingVars = hasScriptingVars || hasScriptingVarsSave;
    }

    @Override
    public void visit(NodeJspElement n) throws JasperException {
        if (n.getNameAttribute().isExpression())
            scriptingElementSeen = true;

        NodeJspAttribute[] attrs = n.getJspAttributes();
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].isExpression()) {
                scriptingElementSeen = true;
                break;
            }
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeJspBody n) throws JasperException {
        checkSeen( n.getNodeChildInfo(), n );
    }

    @Override
    public void visit(NodeNamedAttribute n) throws JasperException {
        checkSeen( n.getNodeChildInfo(), n );
    }

    @Override
    public void visit(NodeDeclaration n) throws JasperException {
        scriptingElementSeen = true;
    }

    @Override
    public void visit(NodeExpression n) throws JasperException {
        scriptingElementSeen = true;
    }

    @Override
    public void visit(NodeScriptlet n) throws JasperException {
        scriptingElementSeen = true;
    }

    public void updatePageInfo(PageInfo pageInfo) {
        pageInfo.setScriptless(! scriptingElementSeen);
    }
}