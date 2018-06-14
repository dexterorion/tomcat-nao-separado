package org.apache.jasper.compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.JasperException;

/**
 * A visitor the tag file
 */
public class TagFileProcessorTagFileDirectiveVisitor extends NodeVisitor {

    private static final JspUtilValidAttribute[] tagDirectiveAttrs = {
            new JspUtilValidAttribute("display-name"),
            new JspUtilValidAttribute("body-content"),
            new JspUtilValidAttribute("dynamic-attributes"),
            new JspUtilValidAttribute("small-icon"),
            new JspUtilValidAttribute("large-icon"),
            new JspUtilValidAttribute("description"),
            new JspUtilValidAttribute("example"),
            new JspUtilValidAttribute("pageEncoding"),
            new JspUtilValidAttribute("language"),
            new JspUtilValidAttribute("import"),
            new JspUtilValidAttribute("deferredSyntaxAllowedAsLiteral"), // JSP 2.1
            new JspUtilValidAttribute("trimDirectiveWhitespaces"), // JSP 2.1
            new JspUtilValidAttribute("isELIgnored") };

    private static final JspUtilValidAttribute[] attributeDirectiveAttrs = {
            new JspUtilValidAttribute("name", true),
            new JspUtilValidAttribute("required"),
            new JspUtilValidAttribute("fragment"),
            new JspUtilValidAttribute("rtexprvalue"),
            new JspUtilValidAttribute("type"),
            new JspUtilValidAttribute("deferredValue"),            // JSP 2.1
            new JspUtilValidAttribute("deferredValueType"),        // JSP 2.1
            new JspUtilValidAttribute("deferredMethod"),           // JSP 2
            new JspUtilValidAttribute("deferredMethodSignature"),  // JSP 21
            new JspUtilValidAttribute("description") };

    private static final JspUtilValidAttribute[] variableDirectiveAttrs = {
            new JspUtilValidAttribute("name-given"),
            new JspUtilValidAttribute("name-from-attribute"),
            new JspUtilValidAttribute("alias"),
            new JspUtilValidAttribute("variable-class"),
            new JspUtilValidAttribute("scope"),
            new JspUtilValidAttribute("declare"),
            new JspUtilValidAttribute("description") };

    private ErrorDispatcher err;

    private TagLibraryInfo tagLibInfo;

    private String name = null;

    private String path = null;

    private TagExtraInfo tei = null;

    private String bodycontent = null;

    private String description = null;

    private String displayName = null;

    private String smallIcon = null;

    private String largeIcon = null;

    private String dynamicAttrsMapName;

    private String example = null;

    private Vector<TagAttributeInfo> attributeVector;

    private Vector<TagVariableInfo> variableVector;

    private static final String ATTR_NAME = "the name attribute of the attribute directive";

    private static final String VAR_NAME_GIVEN = "the name-given attribute of the variable directive";

    private static final String VAR_NAME_FROM = "the name-from-attribute attribute of the variable directive";

    private static final String VAR_ALIAS = "the alias attribute of the variable directive";

    private static final String TAG_DYNAMIC = "the dynamic-attributes attribute of the tag directive";

    private HashMap<String,TagFileProcessorTagFileDirectiveVisitorNameEntry> nameTable =
        new HashMap<String,TagFileProcessorTagFileDirectiveVisitorNameEntry>();

    private HashMap<String,TagFileProcessorTagFileDirectiveVisitorNameEntry> nameFromTable =
        new HashMap<String,TagFileProcessorTagFileDirectiveVisitorNameEntry>();

    public TagFileProcessorTagFileDirectiveVisitor(Compiler2 compiler,
            TagLibraryInfo tagLibInfo, String name, String path) {
        err = compiler.getErrorDispatcher();
        this.tagLibInfo = tagLibInfo;
        this.name = name;
        this.path = path;
        attributeVector = new Vector<TagAttributeInfo>();
        variableVector = new Vector<TagVariableInfo>();
    }

    @Override
    public void visit(NodeTagDirective n) throws JasperException {

        JspUtil.checkAttributes("Tag directive", n, tagDirectiveAttrs, err);

        bodycontent = checkConflict(n, bodycontent, "body-content");
        if (bodycontent != null
                && !bodycontent
                        .equalsIgnoreCase(TagInfo.getBodyContentEmpty())
                && !bodycontent
                        .equalsIgnoreCase(TagInfo.getBodyContentTagDependent())
                && !bodycontent
                        .equalsIgnoreCase(TagInfo.getBodyContentScriptless())) {
            err.jspError(n, "jsp.error.tagdirective.badbodycontent",
                    bodycontent);
        }
        dynamicAttrsMapName = checkConflict(n, dynamicAttrsMapName,
                "dynamic-attributes");
        if (dynamicAttrsMapName != null) {
            checkUniqueName(dynamicAttrsMapName, TAG_DYNAMIC, n);
        }
        smallIcon = checkConflict(n, smallIcon, "small-icon");
        largeIcon = checkConflict(n, largeIcon, "large-icon");
        description = checkConflict(n, description, "description");
        displayName = checkConflict(n, displayName, "display-name");
        example = checkConflict(n, example, "example");
    }

    private String checkConflict(Node n, String oldAttrValue, String attr)
            throws JasperException {

        String result = oldAttrValue;
        String attrValue = n.getAttributeValue(attr);
        if (attrValue != null) {
            if (oldAttrValue != null && !oldAttrValue.equals(attrValue)) {
                err.jspError(n, "jsp.error.tag.conflict.attr", attr,
                        oldAttrValue, attrValue);
            }
            result = attrValue;
        }
        return result;
    }

    @Override
    public void visit(NodeAttributeDirective n) throws JasperException {

        JspUtil.checkAttributes("Attribute directive", n,
                attributeDirectiveAttrs, err);

        // JSP 2.1 Table JSP.8-3
        // handle deferredValue and deferredValueType
        boolean deferredValue = false;
        boolean deferredValueSpecified = false;
        String deferredValueString = n.getAttributeValue("deferredValue");
        if (deferredValueString != null) {
            deferredValueSpecified = true;
            deferredValue = JspUtil.booleanValue(deferredValueString);
        }
        String deferredValueType = n.getAttributeValue("deferredValueType");
        if (deferredValueType != null) {
            if (deferredValueSpecified && !deferredValue) {
                err.jspError(n, "jsp.error.deferredvaluetypewithoutdeferredvalue");
            } else {
                deferredValue = true;
            }
        } else if (deferredValue) {
            deferredValueType = "java.lang.Object";
        } else {
            deferredValueType = "java.lang.String";
        }

        // JSP 2.1 Table JSP.8-3
        // handle deferredMethod and deferredMethodSignature
        boolean deferredMethod = false;
        boolean deferredMethodSpecified = false;
        String deferredMethodString = n.getAttributeValue("deferredMethod");
        if (deferredMethodString != null) {
            deferredMethodSpecified = true;
            deferredMethod = JspUtil.booleanValue(deferredMethodString);
        }
        String deferredMethodSignature = n
                .getAttributeValue("deferredMethodSignature");
        if (deferredMethodSignature != null) {
            if (deferredMethodSpecified && !deferredMethod) {
                err.jspError(n, "jsp.error.deferredmethodsignaturewithoutdeferredmethod");
            } else {
                deferredMethod = true;
            }
        } else if (deferredMethod) {
            deferredMethodSignature = "void methodname()";
        }

        if (deferredMethod && deferredValue) {
            err.jspError(n, "jsp.error.deferredmethodandvalue");
        }
        
        String attrName = n.getAttributeValue("name");
        boolean required = JspUtil.booleanValue(n
                .getAttributeValue("required"));
        boolean rtexprvalue = true;
        String rtexprvalueString = n.getAttributeValue("rtexprvalue");
        if (rtexprvalueString != null) {
            rtexprvalue = JspUtil.booleanValue(rtexprvalueString);
        }
        boolean fragment = JspUtil.booleanValue(n
                .getAttributeValue("fragment"));
        String type = n.getAttributeValue("type");
        if (fragment) {
            // type is fixed to "JspFragment" and a translation error
            // must occur if specified.
            if (type != null) {
                err.jspError(n, "jsp.error.fragmentwithtype");
            }
            // rtexprvalue is fixed to "true" and a translation error
            // must occur if specified.
            rtexprvalue = true;
            if (rtexprvalueString != null) {
                err.jspError(n, "jsp.error.frgmentwithrtexprvalue");
            }
        } else {
            if (type == null)
                type = "java.lang.String";
            
            if (deferredValue) {
                type = ValueExpression.class.getName();
            } else if (deferredMethod) {
                type = MethodExpression.class.getName();
            }
        }

        if (("2.0".equals(tagLibInfo.getRequiredVersion()) || ("1.2".equals(tagLibInfo.getRequiredVersion())))
                && (deferredMethodSpecified || deferredMethod
                        || deferredValueSpecified || deferredValue)) {
            err.jspError("jsp.error.invalid.version", path);
        }
        
        TagAttributeInfo tagAttributeInfo = new TagAttributeInfo(attrName,
                required, type, rtexprvalue, fragment, null, deferredValue,
                deferredMethod, deferredValueType, deferredMethodSignature);
        attributeVector.addElement(tagAttributeInfo);
        checkUniqueName(attrName, ATTR_NAME, n, tagAttributeInfo);
    }

    @Override
    public void visit(NodeVariableDirective n) throws JasperException {

        JspUtil.checkAttributes("Variable directive", n,
                variableDirectiveAttrs, err);

        String nameGiven = n.getAttributeValue("name-given");
        String nameFromAttribute = n
                .getAttributeValue("name-from-attribute");
        if (nameGiven == null && nameFromAttribute == null) {
            err.jspError("jsp.error.variable.either.name");
        }

        if (nameGiven != null && nameFromAttribute != null) {
            err.jspError("jsp.error.variable.both.name");
        }

        String alias = n.getAttributeValue("alias");
        if (nameFromAttribute != null && alias == null
                || nameFromAttribute == null && alias != null) {
            err.jspError("jsp.error.variable.alias");
        }

        String className = n.getAttributeValue("variable-class");
        if (className == null)
            className = "java.lang.String";

        String declareStr = n.getAttributeValue("declare");
        boolean declare = true;
        if (declareStr != null)
            declare = JspUtil.booleanValue(declareStr);

        int scope = VariableInfo.getNested();
        String scopeStr = n.getAttributeValue("scope");
        if (scopeStr != null) {
            if ("NESTED".equals(scopeStr)) {
                // Already the default
            } else if ("AT_BEGIN".equals(scopeStr)) {
                scope = VariableInfo.getAtBegin();
            } else if ("AT_END".equals(scopeStr)) {
                scope = VariableInfo.getAtEnd();
            }
        }

        if (nameFromAttribute != null) {
            /*
             * An alias has been specified. We use 'nameGiven' to hold the
             * value of the alias, and 'nameFromAttribute' to hold the name
             * of the attribute whose value (at invocation-time) denotes the
             * name of the variable that is being aliased
             */
            nameGiven = alias;
            checkUniqueName(nameFromAttribute, VAR_NAME_FROM, n);
            checkUniqueName(alias, VAR_ALIAS, n);
        } else {
            // name-given specified
            checkUniqueName(nameGiven, VAR_NAME_GIVEN, n);
        }

        variableVector.addElement(new TagVariableInfo(nameGiven,
                nameFromAttribute, className, declare, scope));
    }

    public TagInfo getTagInfo() throws JasperException {

        if (name == null) {
            // XXX Get it from tag file name
        }

        if (bodycontent == null) {
            bodycontent = TagInfo.getBodyContentScriptless();
        }

        String tagClassName = JspUtil.getTagHandlerClassName(
                path, tagLibInfo.getReliableURN(), err);

        TagVariableInfo[] tagVariableInfos = new TagVariableInfo[variableVector
                .size()];
        variableVector.copyInto(tagVariableInfos);

        TagAttributeInfo[] tagAttributeInfo = new TagAttributeInfo[attributeVector
                .size()];
        attributeVector.copyInto(tagAttributeInfo);

        return new JasperTagInfo(name, tagClassName, bodycontent,
                description, tagLibInfo, tei, tagAttributeInfo,
                displayName, smallIcon, largeIcon, tagVariableInfos,
                dynamicAttrsMapName);
    }

    /**
     * Reports a translation error if names specified in attributes of
     * directives are not unique in this translation unit.
     * 
     * The value of the following attributes must be unique. 1. 'name'
     * attribute of an attribute directive 2. 'name-given' attribute of a
     * variable directive 3. 'alias' attribute of variable directive 4.
     * 'dynamic-attributes' of a tag directive except that
     * 'dynamic-attributes' can (and must) have the same value when it
     * appears in multiple tag directives.
     * 
     * Also, 'name-from' attribute of a variable directive cannot have the
     * same value as that from another variable directive.
     */
    private void checkUniqueName(String name, String type, Node n)
            throws JasperException {
        checkUniqueName(name, type, n, null);
    }

    private void checkUniqueName(String name, String type, Node n,
            TagAttributeInfo attr) throws JasperException {

        HashMap<String, TagFileProcessorTagFileDirectiveVisitorNameEntry> table = (type == VAR_NAME_FROM) ? nameFromTable : nameTable;
        TagFileProcessorTagFileDirectiveVisitorNameEntry nameEntry = table.get(name);
        if (nameEntry != null) {
            if (!TAG_DYNAMIC.equals(type) ||
                    !TAG_DYNAMIC.equals(nameEntry.getType())) {
                int line = nameEntry.getNode().getStart().getLineNumber();
                err.jspError(n, "jsp.error.tagfile.nameNotUnique", type,
                        nameEntry.getType(), Integer.toString(line));
            }
        } else {
            table.put(name, new TagFileProcessorTagFileDirectiveVisitorNameEntry(type, n, attr));
        }
    }

    /**
     * Perform miscellaneous checks after the nodes are visited.
     */
    public void postCheck() throws JasperException {
        // Check that var.name-from-attributes has valid values.
        Iterator<String> iter = nameFromTable.keySet().iterator();
        while (iter.hasNext()) {
            String nameFrom = iter.next();
            TagFileProcessorTagFileDirectiveVisitorNameEntry nameEntry = nameTable.get(nameFrom);
            TagFileProcessorTagFileDirectiveVisitorNameEntry nameFromEntry = nameFromTable.get(nameFrom);
            Node nameFromNode = nameFromEntry.getNode();
            if (nameEntry == null) {
                err.jspError(nameFromNode,
                        "jsp.error.tagfile.nameFrom.noAttribute", nameFrom);
            } else {
                Node node = nameEntry.getNode();
                TagAttributeInfo tagAttr = nameEntry.getTagAttributeInfo();
                if (!"java.lang.String".equals(tagAttr.getTypeName())
                        || !tagAttr.isRequired()
                        || tagAttr.canBeRequestTime()) {
                    err.jspError(nameFromNode,
                            "jsp.error.tagfile.nameFrom.badAttribute",
                            nameFrom, Integer.toString(node.getStart()
                                    .getLineNumber()));
                }
            }
        }
    }
}