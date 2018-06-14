package org.apache.jasper.compiler;

import javax.servlet.jsp.tagext.FunctionInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;

import org.apache.jasper.JasperException;

public class FVVisitor extends ELNodeVisitor {

    private Node n;
    private ValidatorValidateVisitor validatorValidateVisitor;

    public FVVisitor(Node n, ValidatorValidateVisitor validatorValidateVisitor) {
    	this.validatorValidateVisitor = validatorValidateVisitor;
        this.n = n;
    }

    @Override
    public void visit(ELNodeFunction func) throws JasperException {
        String prefix = func.getPrefix();
        String function = func.getName();
        String uri = null;

        if (n.getRoot().isXmlSyntax()) {
            uri = validatorValidateVisitor.findUri(prefix, n);
        } else if (prefix != null) {
            uri = validatorValidateVisitor.getPageInfo().getURI(prefix);
        }

        if (uri == null) {
            if (prefix == null) {
            	validatorValidateVisitor.getErr().jspError(n, "jsp.error.noFunctionPrefix",
                        function);
            } else {
            	validatorValidateVisitor.getErr().jspError(n, "jsp.error.attribute.invalidPrefix",
                        prefix);
            }
        }
        TagLibraryInfo taglib = validatorValidateVisitor.getPageInfo().getTaglib(uri);
        FunctionInfo funcInfo = null;
        if (taglib != null) {
            funcInfo = taglib.getFunction(function);
        }
        if (funcInfo == null) {
        	validatorValidateVisitor.getErr().jspError(n, "jsp.error.noFunction", function);
        }
        // Skip TLD function uniqueness check. Done by Schema ?
        func.setUri(uri);
        func.setFunctionInfo(funcInfo);
        validatorValidateVisitor.processSignature(func);
    }
}