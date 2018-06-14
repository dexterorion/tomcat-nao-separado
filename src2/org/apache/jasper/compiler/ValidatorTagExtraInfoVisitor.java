package org.apache.jasper.compiler;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.ValidationMessage;

import org.apache.jasper.JasperException;

/**
 * A visitor for validating TagExtraInfo classes of all tags
 */
public class ValidatorTagExtraInfoVisitor extends NodeVisitor {

    private ErrorDispatcher err;

    /*
     * Constructor
     */
    public ValidatorTagExtraInfoVisitor(Compiler2 compiler) {
        this.err = compiler.getErrorDispatcher();
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        TagInfo tagInfo = n.getTagInfo();
        if (tagInfo == null) {
            err.jspError(n, "jsp.error.missing.tagInfo", n.getQName());
        }

        ValidationMessage[] errors = tagInfo.validate(n.getTagData());
        if (errors != null && errors.length != 0) {
            StringBuilder errMsg = new StringBuilder();
            errMsg.append("<h3>");
            errMsg.append(Localizer.getMessage(
                    "jsp.error.tei.invalid.attributes", n.getQName()));
            errMsg.append("</h3>");
            for (int i = 0; i < errors.length; i++) {
                errMsg.append("<p>");
                if (errors[i].getId() != null) {
                    errMsg.append(errors[i].getId());
                    errMsg.append(": ");
                }
                errMsg.append(errors[i].getMessage());
                errMsg.append("</p>");
            }

            err.jspError(n, errMsg.toString());
        }

        visitBody(n);
    }
}