package org.apache.jasper.compiler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper2;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.tagext.FunctionInfo;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagInfo;

import org.apache.jasper.JasperException;
import org.apache.jasper.el.ELContextImpl;
import org.xml.sax.Attributes;

/**
 * A visitor for validating nodes other than page directives
 */
public class ValidatorValidateVisitor extends NodeVisitor {

    private PageInfo pageInfo;

    private ErrorDispatcher err;

    private ClassLoader loader;

    private final StringBuilder buf = new StringBuilder(32);

    private static final JspUtilValidAttribute[] jspRootAttrs = {
            new JspUtilValidAttribute("xsi:schemaLocation"),
            new JspUtilValidAttribute("version", true) };

    private static final JspUtilValidAttribute[] includeDirectiveAttrs = { new JspUtilValidAttribute(
            "file", true) };

    private static final JspUtilValidAttribute[] taglibDirectiveAttrs = {
            new JspUtilValidAttribute("uri"),
            new JspUtilValidAttribute("tagdir"),
            new JspUtilValidAttribute("prefix", true) };

    private static final JspUtilValidAttribute[] includeActionAttrs = {
            new JspUtilValidAttribute("page", true),
            new JspUtilValidAttribute("flush") };

    private static final JspUtilValidAttribute[] paramActionAttrs = {
            new JspUtilValidAttribute("name", true),
            new JspUtilValidAttribute("value", true) };

    private static final JspUtilValidAttribute[] forwardActionAttrs = {
            new JspUtilValidAttribute("page", true) };

    private static final JspUtilValidAttribute[] getPropertyAttrs = {
            new JspUtilValidAttribute("name", true),
            new JspUtilValidAttribute("property", true) };

    private static final JspUtilValidAttribute[] setPropertyAttrs = {
            new JspUtilValidAttribute("name", true),
            new JspUtilValidAttribute("property", true),
            new JspUtilValidAttribute("value", false),
            new JspUtilValidAttribute("param") };

    private static final JspUtilValidAttribute[] useBeanAttrs = {
            new JspUtilValidAttribute("id", true),
            new JspUtilValidAttribute("scope"),
            new JspUtilValidAttribute("class"),
            new JspUtilValidAttribute("type"),
            new JspUtilValidAttribute("beanName", false) };

    private static final JspUtilValidAttribute[] plugInAttrs = {
            new JspUtilValidAttribute("type", true),
            new JspUtilValidAttribute("code", true),
            new JspUtilValidAttribute("codebase"),
            new JspUtilValidAttribute("align"),
            new JspUtilValidAttribute("archive"),
            new JspUtilValidAttribute("height", false),
            new JspUtilValidAttribute("hspace"),
            new JspUtilValidAttribute("jreversion"),
            new JspUtilValidAttribute("name"),
            new JspUtilValidAttribute("vspace"),
            new JspUtilValidAttribute("width", false),
            new JspUtilValidAttribute("nspluginurl"),
            new JspUtilValidAttribute("iepluginurl") };

    private static final JspUtilValidAttribute[] attributeAttrs = {
            new JspUtilValidAttribute("name", true),
            new JspUtilValidAttribute("trim"),
            new JspUtilValidAttribute("omit")};

    private static final JspUtilValidAttribute[] invokeAttrs = {
            new JspUtilValidAttribute("fragment", true),
            new JspUtilValidAttribute("var"),
            new JspUtilValidAttribute("varReader"),
            new JspUtilValidAttribute("scope") };

    private static final JspUtilValidAttribute[] doBodyAttrs = {
            new JspUtilValidAttribute("var"),
            new JspUtilValidAttribute("varReader"),
            new JspUtilValidAttribute("scope") };

    private static final JspUtilValidAttribute[] jspOutputAttrs = {
            new JspUtilValidAttribute("omit-xml-declaration"),
            new JspUtilValidAttribute("doctype-root-element"),
            new JspUtilValidAttribute("doctype-public"),
            new JspUtilValidAttribute("doctype-system") };

    private final ExpressionFactory expressionFactory;

    /*
     * Constructor
     */
    public ValidatorValidateVisitor(Compiler2 compiler) {
        this.setPageInfo(compiler.getPageInfo());
        this.setErr(compiler.getErrorDispatcher());
        this.setLoader(compiler.getCompilationContext().getClassLoader());
        // Get the cached EL expression factory for this context
        expressionFactory =
                JspFactory.getDefaultFactory().getJspApplicationContext(
                compiler.getCompilationContext().getServletContext()).
                getExpressionFactory();
    }

    @Override
    public void visit(NodeJspRoot n) throws JasperException {
        JspUtil.checkAttributes("Jsp:root", n, jspRootAttrs, getErr());
        String version = n.getTextAttribute("version");
        if (!version.equals("1.2") && !version.equals("2.0") &&
                !version.equals("2.1") && !version.equals("2.2")) {
            getErr().jspError(n, "jsp.error.jsproot.version.invalid", version);
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeIncludeDirective n) throws JasperException {
        JspUtil.checkAttributes("Include directive", n,
                includeDirectiveAttrs, getErr());
        visitBody(n);
    }

    @Override
    public void visit(NodeTaglibDirective n) throws JasperException {
        JspUtil.checkAttributes("Taglib directive", n,
                taglibDirectiveAttrs, getErr());
        // Either 'uri' or 'tagdir' attribute must be specified
        String uri = n.getAttributeValue("uri");
        String tagdir = n.getAttributeValue("tagdir");
        if (uri == null && tagdir == null) {
            getErr().jspError(n, "jsp.error.taglibDirective.missing.location");
        }
        if (uri != null && tagdir != null) {
            getErr()
                    .jspError(n,
                            "jsp.error.taglibDirective.both_uri_and_tagdir");
        }
    }

    @Override
    public void visit(NodeParamAction n) throws JasperException {
        JspUtil.checkAttributes("Param action", n, paramActionAttrs, getErr());
        // make sure the value of the 'name' attribute is not a
        // request-time expression
        throwErrorIfExpression(n, "name", "jsp:param");
        n.setValue(getJspAttribute(null, "value", null, null, n
                .getAttributeValue("value"), n, null, false));
        visitBody(n);
    }

    @Override
    public void visit(NodeParamsAction n) throws JasperException {
        // Make sure we've got at least one nested jsp:param
        NodeNodes subElems = n.getBody();
        if (subElems == null) {
            getErr().jspError(n, "jsp.error.params.emptyBody");
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeIncludeAction n) throws JasperException {
        JspUtil.checkAttributes("Include action", n, includeActionAttrs,
                getErr());
        n.setPage(getJspAttribute(null, "page", null, null, n
                .getAttributeValue("page"), n, null, false));
        visitBody(n);
    }

    @Override
    public void visit(NodeForwardAction n) throws JasperException {
        JspUtil.checkAttributes("Forward", n, forwardActionAttrs, getErr());
        n.setPage(getJspAttribute(null, "page", null, null, n
                .getAttributeValue("page"), n, null, false));
        visitBody(n);
    }

    @Override
    public void visit(NodeGetProperty n) throws JasperException {
        JspUtil.checkAttributes("GetProperty", n, getPropertyAttrs, getErr());
    }

    @Override
    public void visit(NodeSetProperty n) throws JasperException {
        JspUtil.checkAttributes("SetProperty", n, setPropertyAttrs, getErr());
        String property = n.getTextAttribute("property");
        String param = n.getTextAttribute("param");
        String value = n.getAttributeValue("value");

        n.setValue(getJspAttribute(null, "value", null, null, value,
                n, null, false));

        boolean valueSpecified = n.getValue() != null;

        if ("*".equals(property)) {
            if (param != null || valueSpecified)
                getErr().jspError(n, "jsp.error.setProperty.invalid");

        } else if (param != null && valueSpecified) {
            getErr().jspError(n, "jsp.error.setProperty.invalid");
        }

        visitBody(n);
    }

    @Override
    public void visit(NodeUseBean n) throws JasperException {
        JspUtil.checkAttributes("UseBean", n, useBeanAttrs, getErr());

        String name = n.getTextAttribute("id");
        String scope = n.getTextAttribute("scope");
        JspUtil.checkScope(scope, n, getErr());
        String className = n.getTextAttribute("class");
        String type = n.getTextAttribute("type");
        BeanRepository beanInfo = getPageInfo().getBeanRepository();

        if (className == null && type == null)
            getErr().jspError(n, "jsp.error.usebean.missingType");

        if (beanInfo.checkVariable(name))
            getErr().jspError(n, "jsp.error.usebean.duplicate");

        if ("session".equals(scope) && !getPageInfo().isSession())
            getErr().jspError(n, "jsp.error.usebean.noSession");

        NodeJspAttribute jattr = getJspAttribute(null, "beanName", null,
                null, n.getAttributeValue("beanName"), n, null, false);
        n.setBeanName(jattr);
        if (className != null && jattr != null)
            getErr().jspError(n, "jsp.error.usebean.notBoth");

        if (className == null)
            className = type;

        beanInfo.addBean(n, name, className, scope);

        visitBody(n);
    }

    @Override
    public void visit(NodePlugIn n) throws JasperException {
        JspUtil.checkAttributes("Plugin", n, plugInAttrs, getErr());

        throwErrorIfExpression(n, "type", "jsp:plugin");
        throwErrorIfExpression(n, "code", "jsp:plugin");
        throwErrorIfExpression(n, "codebase", "jsp:plugin");
        throwErrorIfExpression(n, "align", "jsp:plugin");
        throwErrorIfExpression(n, "archive", "jsp:plugin");
        throwErrorIfExpression(n, "hspace", "jsp:plugin");
        throwErrorIfExpression(n, "jreversion", "jsp:plugin");
        throwErrorIfExpression(n, "name", "jsp:plugin");
        throwErrorIfExpression(n, "vspace", "jsp:plugin");
        throwErrorIfExpression(n, "nspluginurl", "jsp:plugin");
        throwErrorIfExpression(n, "iepluginurl", "jsp:plugin");

        String type = n.getTextAttribute("type");
        if (type == null)
            getErr().jspError(n, "jsp.error.plugin.notype");
        if (!type.equals("bean") && !type.equals("applet"))
            getErr().jspError(n, "jsp.error.plugin.badtype");
        if (n.getTextAttribute("code") == null)
            getErr().jspError(n, "jsp.error.plugin.nocode");

        NodeJspAttribute width = getJspAttribute(null, "width", null,
                null, n.getAttributeValue("width"), n, null, false);
        n.setWidth(width);

        NodeJspAttribute height = getJspAttribute(null, "height", null,
                null, n.getAttributeValue("height"), n, null, false);
        n.setHeight(height);

        visitBody(n);
    }

    @Override
    public void visit(NodeNamedAttribute n) throws JasperException {
        JspUtil.checkAttributes("Attribute", n, attributeAttrs, getErr());
        n.setOmit(getJspAttribute(null, "omit", null, null, n
                .getAttributeValue("omit"), n, null, false));
        visitBody(n);
    }

    @Override
    public void visit(NodeJspBody n) throws JasperException {
        visitBody(n);
    }

    @Override
    public void visit(NodeDeclaration n) throws JasperException {
        if (getPageInfo().isScriptingInvalid()) {
            getErr().jspError(n.getStart(), "jsp.error.no.scriptlets");
        }
    }

    @Override
    public void visit(NodeExpression n) throws JasperException {
        if (getPageInfo().isScriptingInvalid()) {
            getErr().jspError(n.getStart(), "jsp.error.no.scriptlets");
        }
    }

    @Override
    public void visit(NodeScriptlet n) throws JasperException {
        if (getPageInfo().isScriptingInvalid()) {
            getErr().jspError(n.getStart(), "jsp.error.no.scriptlets");
        }
    }

    @Override
    public void visit(NodeELExpression n) throws JasperException {
        // exit if we are ignoring EL all together
        if (getPageInfo().isELIgnored())
            return;

        // JSP.2.2 - '#{' not allowed in template text
        if (n.getType() == '#') {
            if (!getPageInfo().isDeferredSyntaxAllowedAsLiteral()) {
                getErr().jspError(n, "jsp.error.el.template.deferred");
            } else {
                return;
            }
        }

        // build expression
        StringBuilder expr = this.getBuffer();
        expr.append(n.getType()).append('{').append(n.getText())
                .append('}');
        ELNodeNodes el = ELParser2.parse(expr.toString(), getPageInfo()
                .isDeferredSyntaxAllowedAsLiteral());

        // validate/prepare expression
        prepareExpression(el, n, expr.toString());

        // store it
        n.setEL(el);
    }

    @Override
    public void visit(NodeUninterpretedTag n) throws JasperException {
        if (n.getNodeNamedAttributeNodeNodes().size() != 0) {
            getErr().jspError(n, "jsp.error.namedAttribute.invalidUse");
        }

        Attributes attrs = n.getAttributes();
        if (attrs != null) {
            int attrSize = attrs.getLength();
            NodeJspAttribute[] jspAttrs = new NodeJspAttribute[attrSize];
            for (int i = 0; i < attrSize; i++) {
                // JSP.2.2 - '#{' not allowed in template text
                String value = attrs.getValue(i);
                if (!getPageInfo().isDeferredSyntaxAllowedAsLiteral()) {
                    if (containsDeferredSyntax(value)) {
                        getErr().jspError(n, "jsp.error.el.template.deferred");
                    }
                }
                jspAttrs[i] = getJspAttribute(null, attrs.getQName(i),
                        attrs.getURI(i), attrs.getLocalName(i), value, n,
                        null, false);
            }
            n.setJspAttributes(jspAttrs);
        }

        visitBody(n);
    }

    /*
     * Look for a #{ sequence that isn't preceded by \.
     */
    private boolean containsDeferredSyntax(String value) {
        if (value == null) {
            return false;
        }

        int i = 0;
        int len = value.length();
        boolean prevCharIsEscape = false;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '#' && (i+1) < len && value.charAt(i+1) == '{' && !prevCharIsEscape) {
                return true;
            } else if (c == '\\') {
                prevCharIsEscape = true;
            } else {
                prevCharIsEscape = false;
            }
            i++;
        }
        return false;
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {

        TagInfo tagInfo = n.getTagInfo();
        if (tagInfo == null) {
            getErr().jspError(n, "jsp.error.missing.tagInfo", n.getQName());
        }

        /*
         * The bodycontent of a SimpleTag cannot be JSP.
         */
        if (n.implementsSimpleTag()
                && tagInfo.getBodyContent().equalsIgnoreCase(
                        TagInfo.getBodyContentJsp())) {
            getErr().jspError(n, "jsp.error.simpletag.badbodycontent", tagInfo
                    .getTagClassName());
        }

        /*
         * If the tag handler declares in the TLD that it supports dynamic
         * attributes, it also must implement the DynamicAttributes
         * interface.
         */
        if (tagInfo.hasDynamicAttributes()
                && !n.implementsDynamicAttributes()) {
            getErr().jspError(n, "jsp.error.dynamic.attributes.not.implemented",
                    n.getQName());
        }

        /*
         * Make sure all required attributes are present, either as
         * attributes or named attributes (<jsp:attribute>). Also make sure
         * that the same attribute is not specified in both attributes or
         * named attributes.
         */
        TagAttributeInfo[] tldAttrs = tagInfo.getAttributes();
        String customActionUri = n.getURI();
        Attributes attrs = n.getAttributes();
        int attrsSize = (attrs == null) ? 0 : attrs.getLength();
        for (int i = 0; i < tldAttrs.length; i++) {
            String attr = null;
            if (attrs != null) {
                attr = attrs.getValue(tldAttrs[i].getName());
                if (attr == null) {
                    attr = attrs.getValue(customActionUri, tldAttrs[i]
                            .getName());
                }
            }
            NodeNamedAttribute na = n.getNodeNamedAttributeNode(tldAttrs[i]
                    .getName());

            if (tldAttrs[i].isRequired() && attr == null && na == null) {
                getErr().jspError(n, "jsp.error.missing_attribute", tldAttrs[i]
                        .getName(), n.getLocalName());
            }
            if (attr != null && na != null) {
                getErr().jspError(n, "jsp.error.duplicate.name.jspattribute",
                        tldAttrs[i].getName());
            }
        }

        NodeNodes naNodes = n.getNodeNamedAttributeNodeNodes();
        int jspAttrsSize = naNodes.size() + attrsSize;
        NodeJspAttribute[] jspAttrs = null;
        if (jspAttrsSize > 0) {
            jspAttrs = new NodeJspAttribute[jspAttrsSize];
        }
        Hashtable<String, Object> tagDataAttrs = new Hashtable<String, Object>(attrsSize);

        checkXmlAttributes(n, jspAttrs, tagDataAttrs);
        checkNamedAttributes(n, jspAttrs, attrsSize, tagDataAttrs);

        TagData tagData = new TagData(tagDataAttrs);

        // JSP.C1: It is a (translation time) error for an action that
        // has one or more variable subelements to have a TagExtraInfo
        // class that returns a non-null object.
        TagExtraInfo tei = tagInfo.getTagExtraInfo();
        if (tei != null && tei.getVariableInfo(tagData) != null
                && tei.getVariableInfo(tagData).length > 0
                && tagInfo.getTagVariableInfos().length > 0) {
            getErr().jspError("jsp.error.non_null_tei_and_var_subelems", n
                    .getQName());
        }

        n.setTagData(tagData);
        n.setJspAttributes(jspAttrs);

        visitBody(n);
    }

    @Override
    public void visit(NodeJspElement n) throws JasperException {

        Attributes attrs = n.getAttributes();
        if (attrs == null) {
            getErr().jspError(n, "jsp.error.jspelement.missing.name");
        }
        @SuppressWarnings("null") // Exception will have been thrown above
        int xmlAttrLen = attrs.getLength();

        NodeNodes namedAttrs = n.getNodeNamedAttributeNodeNodes();

        // XML-style 'name' attribute, which is mandatory, must not be
        // included in JspAttribute array
        int jspAttrSize = xmlAttrLen - 1 + namedAttrs.size();

        NodeJspAttribute[] jspAttrs = new NodeJspAttribute[jspAttrSize];
        int jspAttrIndex = 0;

        // Process XML-style attributes
        for (int i = 0; i < xmlAttrLen; i++) {
            if ("name".equals(attrs.getLocalName(i))) {
                n.setNameAttribute(getJspAttribute(null, attrs.getQName(i),
                        attrs.getURI(i), attrs.getLocalName(i), attrs
                                .getValue(i), n, null, false));
            } else {
                if (jspAttrIndex < jspAttrSize) {
                    jspAttrs[jspAttrIndex++] = getJspAttribute(null,
                            attrs.getQName(i), attrs.getURI(i),
                            attrs.getLocalName(i), attrs.getValue(i), n,
                            null, false);
                }
            }
        }
        if (n.getNameAttribute() == null) {
            getErr().jspError(n, "jsp.error.jspelement.missing.name");
        }

        // Process named attributes
        for (int i = 0; i < namedAttrs.size(); i++) {
            NodeNamedAttribute na = (NodeNamedAttribute) namedAttrs
                    .getNode(i);
            jspAttrs[jspAttrIndex++] = new NodeJspAttribute(na, null,
                    false);
        }

        n.setJspAttributes(jspAttrs);

        visitBody(n);
    }

    @Override
    public void visit(NodeJspOutput n) throws JasperException {
        JspUtil.checkAttributes("jsp:output", n, jspOutputAttrs, getErr());

        if (n.getBody() != null) {
            getErr().jspError(n, "jsp.error.jspoutput.nonemptybody");
        }

        String omitXmlDecl = n.getAttributeValue("omit-xml-declaration");
        String doctypeName = n.getAttributeValue("doctype-root-element");
        String doctypePublic = n.getAttributeValue("doctype-public");
        String doctypeSystem = n.getAttributeValue("doctype-system");

        String omitXmlDeclOld = getPageInfo().getOmitXmlDecl();
        String doctypeNameOld = getPageInfo().getDoctypeName();
        String doctypePublicOld = getPageInfo().getDoctypePublic();
        String doctypeSystemOld = getPageInfo().getDoctypeSystem();

        if (omitXmlDecl != null && omitXmlDeclOld != null
                && !omitXmlDecl.equals(omitXmlDeclOld)) {
            getErr().jspError(n, "jsp.error.jspoutput.conflict",
                    "omit-xml-declaration", omitXmlDeclOld, omitXmlDecl);
        }

        if (doctypeName != null && doctypeNameOld != null
                && !doctypeName.equals(doctypeNameOld)) {
            getErr().jspError(n, "jsp.error.jspoutput.conflict",
                    "doctype-root-element", doctypeNameOld, doctypeName);
        }

        if (doctypePublic != null && doctypePublicOld != null
                && !doctypePublic.equals(doctypePublicOld)) {
            getErr().jspError(n, "jsp.error.jspoutput.conflict",
                    "doctype-public", doctypePublicOld, doctypePublic);
        }

        if (doctypeSystem != null && doctypeSystemOld != null
                && !doctypeSystem.equals(doctypeSystemOld)) {
            getErr().jspError(n, "jsp.error.jspoutput.conflict",
                    "doctype-system", doctypeSystemOld, doctypeSystem);
        }

        if (doctypeName == null && doctypeSystem != null
                || doctypeName != null && doctypeSystem == null) {
            getErr().jspError(n, "jsp.error.jspoutput.doctypenamesystem");
        }

        if (doctypePublic != null && doctypeSystem == null) {
            getErr().jspError(n, "jsp.error.jspoutput.doctypepulicsystem");
        }

        if (omitXmlDecl != null) {
            getPageInfo().setOmitXmlDecl(omitXmlDecl);
        }
        if (doctypeName != null) {
            getPageInfo().setDoctypeName(doctypeName);
        }
        if (doctypeSystem != null) {
            getPageInfo().setDoctypeSystem(doctypeSystem);
        }
        if (doctypePublic != null) {
            getPageInfo().setDoctypePublic(doctypePublic);
        }
    }

    @Override
    public void visit(NodeInvokeAction n) throws JasperException {

        JspUtil.checkAttributes("Invoke", n, invokeAttrs, getErr());

        String scope = n.getTextAttribute("scope");
        JspUtil.checkScope(scope, n, getErr());

        String var = n.getTextAttribute("var");
        String varReader = n.getTextAttribute("varReader");
        if (scope != null && var == null && varReader == null) {
            getErr().jspError(n, "jsp.error.missing_var_or_varReader");
        }
        if (var != null && varReader != null) {
            getErr().jspError(n, "jsp.error.var_and_varReader");
        }
    }

    @Override
    public void visit(NodeDoBodyAction n) throws JasperException {

        JspUtil.checkAttributes("DoBody", n, doBodyAttrs, getErr());

        String scope = n.getTextAttribute("scope");
        JspUtil.checkScope(scope, n, getErr());

        String var = n.getTextAttribute("var");
        String varReader = n.getTextAttribute("varReader");
        if (scope != null && var == null && varReader == null) {
            getErr().jspError(n, "jsp.error.missing_var_or_varReader");
        }
        if (var != null && varReader != null) {
            getErr().jspError(n, "jsp.error.var_and_varReader");
        }
    }

    /*
     * Make sure the given custom action does not have any invalid
     * attributes.
     *
     * A custom action and its declared attributes always belong to the same
     * namespace, which is identified by the prefix name of the custom tag
     * invocation. For example, in this invocation:
     *
     * <my:test a="1" b="2" c="3"/>, the action
     *
     * "test" and its attributes "a", "b", and "c" all belong to the
     * namespace identified by the prefix "my". The above invocation would
     * be equivalent to:
     *
     * <my:test my:a="1" my:b="2" my:c="3"/>
     *
     * An action attribute may have a prefix different from that of the
     * action invocation only if the underlying tag handler supports dynamic
     * attributes, in which case the attribute with the different prefix is
     * considered a dynamic attribute.
     */
    private void checkXmlAttributes(NodeCustomTag n,
            NodeJspAttribute[] jspAttrs, Hashtable<String, Object> tagDataAttrs)
            throws JasperException {

        TagInfo tagInfo = n.getTagInfo();
        if (tagInfo == null) {
            getErr().jspError(n, "jsp.error.missing.tagInfo", n.getQName());
        }
        TagAttributeInfo[] tldAttrs = tagInfo.getAttributes();
        Attributes attrs = n.getAttributes();

        for (int i = 0; attrs != null && i < attrs.getLength(); i++) {
            boolean found = false;

            boolean runtimeExpression = ((n.getRoot().isXmlSyntax() && attrs.getValue(i).startsWith("%="))
                    || (!n.getRoot().isXmlSyntax() && attrs.getValue(i).startsWith("<%=")));
            boolean elExpression = false;
            boolean deferred = false;
            double libraryVersion = Double.parseDouble(
                    tagInfo.getTagLibrary().getRequiredVersion());
            boolean deferredSyntaxAllowedAsLiteral =
                getPageInfo().isDeferredSyntaxAllowedAsLiteral() ||
                libraryVersion < 2.1;

            String xmlAttributeValue = attrs.getValue(i);

            ELNodeNodes el = null;
            if (!runtimeExpression && !getPageInfo().isELIgnored()) {
                el = ELParser2.parse(xmlAttributeValue,
                        deferredSyntaxAllowedAsLiteral);
                Iterator<ELNode> nodes = el.iterator();
                while (nodes.hasNext()) {
                    ELNode node = nodes.next();
                    if (node instanceof ELNodeRoot) {
                        if (((ELNodeRoot) node).getType() == '$') {
                            if (elExpression && deferred) {
                                getErr().jspError(n,
                                        "jsp.error.attribute.deferredmix");
                            }
                            elExpression = true;
                        } else if (((ELNodeRoot) node).getType() == '#') {
                            if (elExpression && !deferred) {
                                getErr().jspError(n,
                                        "jsp.error.attribute.deferredmix");
                            }
                            elExpression = true;
                            deferred = true;
                        }
                    }
                }
            }

            boolean expression = runtimeExpression || elExpression;

            // When attribute is not an expression,
            // contains its textual value with \$ and \# escaping removed.
            String textAttributeValue;
            if (!elExpression && el != null) {
                // Should be a single Text node
                Iterator<ELNode> it = el.iterator();
                if (it.hasNext()) {
                    textAttributeValue = ((ELNodeText) it.next())
                            .getText();
                } else {
                    textAttributeValue = "";
                }
            } else {
                textAttributeValue = xmlAttributeValue;
            }
            for (int j = 0; tldAttrs != null && j < tldAttrs.length; j++) {
                if (attrs.getLocalName(i).equals(tldAttrs[j].getName())
                        && (attrs.getURI(i) == null
                                || attrs.getURI(i).length() == 0 || attrs
                                .getURI(i).equals(n.getURI()))) {

                    TagAttributeInfo tldAttr = tldAttrs[j];
                    if (tldAttr.canBeRequestTime()
                            || tldAttr.isDeferredMethod() || tldAttr.isDeferredValue()) { // JSP 2.1

                        if (!expression) {

                            String expectedType = null;
                            if (tldAttr.isDeferredMethod()) {
                                // The String literal must be castable to what is declared as type
                                // for the attribute
                                String m = tldAttr.getMethodSignature();
                                if (m != null) {
                                    m = m.trim();
                                    int rti = m.indexOf(' ');
                                    if (rti > 0) {
                                        expectedType = m.substring(0, rti).trim();
                                    }
                                } else {
                                    expectedType = "java.lang.Object";
                                }
                                if ("void".equals(expectedType)) {
                                    // Can't specify a literal for a
                                    // deferred method with an expected type
                                    // of void - JSP.2.3.4
                                    getErr().jspError(n,
                                            "jsp.error.literal_with_void",
                                            tldAttr.getName());
                                }
                            }
                            if (tldAttr.isDeferredValue()) {
                                // The String literal must be castable to what is declared as type
                                // for the attribute
                                expectedType = tldAttr.getExpectedTypeName();
                            }
                            if (expectedType != null) {
                                Class<?> expectedClass = String.class;
                                try {
                                    expectedClass = JspUtil.toClass(expectedType, getLoader());
                                } catch (ClassNotFoundException e) {
                                    getErr().jspError
                                        (n, "jsp.error.unknown_attribute_type",
                                         tldAttr.getName(), expectedType);
                                }
                                // Check casting - not possible for all types
                                if (String.class.equals(expectedClass) ||
                                        expectedClass == Long.TYPE ||
                                        expectedClass == Double.TYPE ||
                                        expectedClass == Byte.TYPE ||
                                        expectedClass == Short.TYPE ||
                                        expectedClass == Integer.TYPE ||
                                        expectedClass == Float.TYPE ||
                                        Number.class.isAssignableFrom(expectedClass) ||
                                        Character.class.equals(expectedClass) ||
                                        Character.TYPE == expectedClass ||
                                        Boolean.class.equals(expectedClass) ||
                                        Boolean.TYPE == expectedClass ||
                                        expectedClass.isEnum()) {
                                    try {
                                        expressionFactory.coerceToType(textAttributeValue, expectedClass);
                                    } catch (Exception e) {
                                        getErr().jspError
                                            (n, "jsp.error.coerce_to_type",
                                             tldAttr.getName(), expectedType, textAttributeValue);
                                    }
                                }
                            }

                            jspAttrs[i] = new NodeJspAttribute(tldAttr,
                                    attrs.getQName(i), attrs.getURI(i),
                                    attrs.getLocalName(i),
                                    textAttributeValue, false, null, false);
                        } else {

                            if (deferred && !tldAttr.isDeferredMethod() && !tldAttr.isDeferredValue()) {
                                // No deferred expressions allowed for this attribute
                                getErr().jspError(n, "jsp.error.attribute.custom.non_rt_with_expr",
                                        tldAttr.getName());
                            }
                            if (!deferred && !tldAttr.canBeRequestTime()) {
                                // Only deferred expressions are allowed for this attribute
                                getErr().jspError(n, "jsp.error.attribute.custom.non_rt_with_expr",
                                        tldAttr.getName());
                            }

                            // EL or Runtime expression
                            jspAttrs[i] = getJspAttribute(tldAttr,
                                    attrs.getQName(i), attrs.getURI(i),
                                    attrs.getLocalName(i),
                                    xmlAttributeValue, n, el, false);
                        }

                    } else {
                        // Attribute does not accept any expressions.
                        // Make sure its value does not contain any.
                        if (expression) {
                            getErr().jspError(n, "jsp.error.attribute.custom.non_rt_with_expr",
                                            tldAttr.getName());
                        }
                        jspAttrs[i] = new NodeJspAttribute(tldAttr,
                                attrs.getQName(i), attrs.getURI(i),
                                attrs.getLocalName(i),
                                textAttributeValue, false, null, false);
                    }
                    if (expression) {
                        tagDataAttrs.put(attrs.getQName(i),
                                TagData.getRequestTimeValue());
                    } else {
                        tagDataAttrs.put(attrs.getQName(i),
                                textAttributeValue);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (tagInfo.hasDynamicAttributes()) {
                    jspAttrs[i] = getJspAttribute(null, attrs.getQName(i),
                            attrs.getURI(i), attrs.getLocalName(i),
                            xmlAttributeValue, n, el, true);
                } else {
                    getErr().jspError(n, "jsp.error.bad_attribute", attrs
                            .getQName(i), n.getLocalName());
                }
            }
        }
    }

    /*
     * Make sure the given custom action does not have any invalid named
     * attributes
     */
    private void checkNamedAttributes(NodeCustomTag n,
            NodeJspAttribute[] jspAttrs, int start,
            Hashtable<String, Object> tagDataAttrs)
            throws JasperException {

        TagInfo tagInfo = n.getTagInfo();
        if (tagInfo == null) {
            getErr().jspError(n, "jsp.error.missing.tagInfo", n.getQName());
        }
        TagAttributeInfo[] tldAttrs = tagInfo.getAttributes();
        NodeNodes naNodes = n.getNodeNamedAttributeNodeNodes();

        for (int i = 0; i < naNodes.size(); i++) {
            NodeNamedAttribute na = (NodeNamedAttribute) naNodes
                    .getNode(i);
            boolean found = false;
            for (int j = 0; j < tldAttrs.length; j++) {
                /*
                 * See above comment about namespace matches. For named
                 * attributes, we use the prefix instead of URI as the match
                 * criterion, because in the case of a JSP document, we'd
                 * have to keep track of which namespaces are in scope when
                 * parsing a named attribute, in order to determine the URI
                 * that the prefix of the named attribute's name matches to.
                 */
                String attrPrefix = na.getPrefix();
                if (na.getLocalName().equals(tldAttrs[j].getName())
                        && (attrPrefix == null || attrPrefix.length() == 0 || attrPrefix
                                .equals(n.getPrefix()))) {
                    jspAttrs[start + i] = new NodeJspAttribute(na,
                            tldAttrs[j], false);
                    ValidatorValidateVisitorNamedAttributeVisitor nav = null;
                    if (na.getBody() != null) {
                        nav = new ValidatorValidateVisitorNamedAttributeVisitor();
                        na.getBody().visit(nav);
                    }
                    if (nav != null && nav.hasDynamicContent()) {
                        tagDataAttrs.put(na.getName(),
                                TagData.getRequestTimeValue());
                    } else {
                        tagDataAttrs.put(na.getName(), na.getText());
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (tagInfo.hasDynamicAttributes()) {
                    jspAttrs[start + i] = new NodeJspAttribute(na, null,
                            true);
                } else {
                    getErr().jspError(n, "jsp.error.bad_attribute",
                            na.getName(), n.getLocalName());
                }
            }
        }
    }

    /**
     * Preprocess attributes that can be expressions. Expression delimiters
     * are stripped.
     * <p>
     * If value is null, checks if there are any NamedAttribute subelements
     * in the tree node, and if so, constructs a JspAttribute out of a child
     * NamedAttribute node.
     *
     * @param el EL expression, if already parsed by the caller (so that we
     *  can skip re-parsing it)
     */
    private NodeJspAttribute getJspAttribute(TagAttributeInfo tai,
            String qName, String uri, String localName, String value,
            Node n, ELNodeNodes el, boolean dynamic)
            throws JasperException {

        NodeJspAttribute result = null;

        // XXX Is it an error to see "%=foo%" in non-Xml page?
        // (We won't see "<%=foo%> in xml page because '<' is not a
        // valid attribute value in xml).

        if (value != null) {
            if (n.getRoot().isXmlSyntax() && value.startsWith("%=")) {
                result = new NodeJspAttribute(tai, qName, uri, localName,
                        value.substring(2, value.length() - 1), true, null,
                        dynamic);
            } else if (!n.getRoot().isXmlSyntax()
                    && value.startsWith("<%=")) {
                result = new NodeJspAttribute(tai, qName, uri, localName,
                        value.substring(3, value.length() - 2), true, null,
                        dynamic);
            } else {
                if (!getPageInfo().isELIgnored()) {
                    // The attribute can contain expressions but is not a
                    // scriptlet expression; thus, we want to run it through
                    // the expression interpreter

                    // validate expression syntax if string contains
                    // expression(s)
                    if (el == null) {
                        el = ELParser2.parse(value,
                            getPageInfo().isDeferredSyntaxAllowedAsLiteral());
                    }

                    if (el.containsEL()) {
                        validateFunctions(el, n);
                    } else {
                        // Get text with \$ and \# escaping removed.
                        // Should be a single Text node
                        Iterator<ELNode> it = el.iterator();
                        if (it.hasNext()) {
                            value = ((ELNodeText) it.next()).getText();
                        } else {
                            value = "";
                        }
                        el = null;
                    }
                }

                if (n instanceof NodeUninterpretedTag &&
                        n.getRoot().isXmlSyntax()) {
                    // Attribute values of uninterpreted tags will have been
                    // XML un-escaped during parsing. Since these attributes
                    // are part of an uninterpreted tag the value needs to
                    // be re-escaped before being included in the output.
                    // The wrinkle is that the output of any EL must not be
                    // re-escaped as that must be output as is.
                    if (el != null) {
                        ValidatorValidateVisitorXmlEscapeNonELVisitor v = new ValidatorValidateVisitorXmlEscapeNonELVisitor(
                                getPageInfo().isDeferredSyntaxAllowedAsLiteral());
                        el.visit(v);
                        value = v.getText();
                    } else {
                        value = Validator.xmlEscape(value);
                    }
                }

                result = new NodeJspAttribute(tai, qName, uri, localName,
                        value, false, el, dynamic);

                if (el != null) {
                    ELContextImpl ctx = new ELContextImpl();
                    ctx.setFunctionMapper(getFunctionMapper(el));

                    try {
                        result.validateEL(this.getPageInfo()
                                .getExpressionFactory(), ctx);
                    } catch (ELException e) {
                        this.getErr().jspError(n.getStart(),
                                "jsp.error.invalid.expression", value, e
                                        .toString());
                    }
                }
            }
        } else {
            // Value is null. Check for any NamedAttribute subnodes
            // that might contain the value for this attribute.
            // Otherwise, the attribute wasn't found so we return null.

            NodeNamedAttribute namedAttributeNode = n
                    .getNodeNamedAttributeNode(qName);
            if (namedAttributeNode != null) {
                result = new NodeJspAttribute(namedAttributeNode, tai,
                        dynamic);
            }
        }

        return result;
    }

    /*
     * Return an empty StringBuilder [not thread-safe]
     */
    private StringBuilder getBuffer() {
        this.buf.setLength(0);
        return this.buf;
    }

    /*
     * Checks to see if the given attribute value represents a runtime or EL
     * expression.
     */
    private boolean isExpression(Node n, String value, boolean checkDeferred) {

        boolean runtimeExpression = ((n.getRoot().isXmlSyntax() && value.startsWith("%="))
                || (!n.getRoot().isXmlSyntax() && value.startsWith("<%=")));
        boolean elExpression = false;

        if (!runtimeExpression && !getPageInfo().isELIgnored()) {
            Iterator<ELNode> nodes = ELParser2.parse(value,
                    getPageInfo().isDeferredSyntaxAllowedAsLiteral()).iterator();
            while (nodes.hasNext()) {
                ELNode node = nodes.next();
                if (node instanceof ELNodeRoot) {
                    if (((ELNodeRoot) node).getType() == '$') {
                        elExpression = true;
                        break;
                    } else if (checkDeferred &&
                            !getPageInfo().isDeferredSyntaxAllowedAsLiteral() &&
                            ((ELNodeRoot) node).getType() == '#') {
                        elExpression = true;
                        break;
                    }
                }
            }
        }

        return runtimeExpression || elExpression;

    }

    /*
     * Throws exception if the value of the attribute with the given name in
     * the given node is given as an RT or EL expression, but the spec
     * requires a static value.
     */
    private void throwErrorIfExpression(Node n, String attrName,
            String actionName) throws JasperException {
        if (n.getAttributes() != null
                && n.getAttributes().getValue(attrName) != null
                && isExpression(n, n.getAttributes().getValue(attrName), true)) {
            getErr().jspError(n,
                    "jsp.error.attribute.standard.non_rt_with_expr",
                    attrName, actionName);
        }
    }

    public String findUri(String prefix, Node n) {

        for (Node p = n; p != null; p = p.getParent()) {
            Attributes attrs = p.getTaglibAttributes();
            if (attrs == null) {
                continue;
            }
            for (int i = 0; i < attrs.getLength(); i++) {
                String name = attrs.getQName(i);
                int k = name.indexOf(':');
                if (prefix == null && k < 0) {
                    // prefix not specified and a default ns found
                    return attrs.getValue(i);
                }
                if (prefix != null && k >= 0
                        && prefix.equals(name.substring(k + 1))) {
                    return attrs.getValue(i);
                }
            }
        }
        return null;
    }

    /**
     * Validate functions in EL expressions
     */
    private void validateFunctions(ELNodeNodes el, Node n)
            throws JasperException {

        el.visit(new FVVisitor(n, this));
    }

    private void prepareExpression(ELNodeNodes el, Node n, String expr)
            throws JasperException {
        validateFunctions(el, n);

        // test it out
        ELContextImpl ctx = new ELContextImpl();
        ctx.setFunctionMapper(this.getFunctionMapper(el));
        ExpressionFactory ef = this.getPageInfo().getExpressionFactory();
        try {
            ef.createValueExpression(ctx, expr, Object.class);
        } catch (ELException e) {

        }
    }

    public void processSignature(ELNodeFunction func)
            throws JasperException {
        func.setMethodName(getMethod(func));
        func.setParameters(getParameters(func));
    }

    /**
     * Get the method name from the signature.
     */
    private String getMethod(ELNodeFunction func) throws JasperException {
        FunctionInfo funcInfo = func.getFunctionInfo();
        String signature = funcInfo.getFunctionSignature();

        int start = signature.indexOf(' ');
        if (start < 0) {
            getErr().jspError("jsp.error.tld.fn.invalid.signature", func
                    .getPrefix(), func.getName());
        }
        int end = signature.indexOf('(');
        if (end < 0) {
            getErr().jspError(
                    "jsp.error.tld.fn.invalid.signature.parenexpected",
                    func.getPrefix(), func.getName());
        }
        return signature.substring(start + 1, end).trim();
    }

    /**
     * Get the parameters types from the function signature.
     *
     * @return An array of parameter class names
     */
    private String[] getParameters(ELNodeFunction func)
            throws JasperException {
        FunctionInfo funcInfo = func.getFunctionInfo();
        String signature = funcInfo.getFunctionSignature();
        ArrayList<String> params = new ArrayList<String>();
        // Signature is of the form
        // <return-type> S <method-name S? '('
        // < <arg-type> ( ',' <arg-type> )* )? ')'
        int start = signature.indexOf('(') + 1;
        boolean lastArg = false;
        while (true) {
            int p = signature.indexOf(',', start);
            if (p < 0) {
                p = signature.indexOf(')', start);
                if (p < 0) {
                    getErr().jspError("jsp.error.tld.fn.invalid.signature", func
                            .getPrefix(), func.getName());
                }
                lastArg = true;
            }
            String arg = signature.substring(start, p).trim();
            if (!"".equals(arg)) {
                params.add(arg);
            }
            if (lastArg) {
                break;
            }
            start = p + 1;
        }
        return params.toArray(new String[params.size()]);
    }

    private FunctionMapper2 getFunctionMapper(ELNodeNodes el)
            throws JasperException {

        ValidateFunctionMapper fmapper = new ValidateFunctionMapper();
        el.visit(new MapperELVisitor(fmapper, this));
        return fmapper;
    }

	public PageInfo getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(PageInfo pageInfo) {
		this.pageInfo = pageInfo;
	}

	public ErrorDispatcher getErr() {
		return err;
	}

	public void setErr(ErrorDispatcher err) {
		this.err = err;
	}

	public ClassLoader getLoader() {
		return loader;
	}

	public void setLoader(ClassLoader loader) {
		this.loader = loader;
	}
}