package org.apache.jasper.compiler;

import java.util.Vector;

import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.JasperException;

public class ScriptingVarVisitor extends NodeVisitor {
	private Generator generator;
	
	private final Vector<String> vars;

	public ScriptingVarVisitor(Generator generator) {
		this.generator = generator; 
		vars = new Vector<String>();
	}

	@Override
	public void visit(NodeCustomTag n) throws JasperException {
		// XXX - Actually there is no need to declare those
		// "_jspx_" + varName + "_" + nestingLevel variables when we are
		// inside a JspFragment.

		if (n.getCustomNestingLevel() > 0) {
			TagVariableInfo[] tagVarInfos = n.getTagVariableInfos();
			VariableInfo[] varInfos = n.getVariableInfos();

			if (varInfos.length > 0) {
				for (int i = 0; i < varInfos.length; i++) {
					String varName = varInfos[i].getVarName();
					String tmpVarName = "_jspx_" + varName + "_"
							+ n.getCustomNestingLevel();
					if (!vars.contains(tmpVarName)) {
						vars.add(tmpVarName);
						generator.getOutData().printin(varInfos[i].getClassName());
						generator.getOutData().print(" ");
						generator.getOutData().print(tmpVarName);
						generator.getOutData().print(" = ");
						generator.getOutData().print(null);
						generator.getOutData().println(";");
					}
				}
			} else {
				for (int i = 0; i < tagVarInfos.length; i++) {
					String varName = tagVarInfos[i].getNameGiven();
					if (varName == null) {
						varName = n.getTagData().getAttributeString(
								tagVarInfos[i].getNameFromAttribute());
					} else if (tagVarInfos[i].getNameFromAttribute() != null) {
						// alias
						continue;
					}
					String tmpVarName = "_jspx_" + varName + "_"
							+ n.getCustomNestingLevel();
					if (!vars.contains(tmpVarName)) {
						vars.add(tmpVarName);
						generator.getOutData().printin(tagVarInfos[i].getClassName());
						generator.getOutData().print(" ");
						generator.getOutData().print(tmpVarName);
						generator.getOutData().print(" = ");
						generator.getOutData().print(null);
						generator.getOutData().println(";");
					}
				}
			}
		}

		visitBody(n);
	}
}