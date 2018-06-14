package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class DeclarationVisitor extends NodeVisitor {
	private Generator generator;
	
	private boolean getServletInfoGenerated = false;

	public DeclarationVisitor(Generator generator) {
		this.generator = generator;
	}
	
	/*
	 * Generates getServletInfo() method that returns the value of the
	 * page directive's 'info' attribute, if present.
	 * 
	 * The Validator has already ensured that if the translation unit
	 * contains more than one page directive with an 'info' attribute,
	 * their values match.
	 */
	@Override
	public void visit(NodePageDirective n) throws JasperException {

		if (getServletInfoGenerated) {
			return;
		}

		String info = n.getAttributeValue("info");
		if (info == null)
			return;

		getServletInfoGenerated = true;
		generator.getOutData().printil("public java.lang.String getServletInfo() {");
		generator.getOutData().pushIndent();
		generator.getOutData().printin("return ");
		generator.getOutData().print(Generator.quote(info));
		generator.getOutData().println(";");
		generator.getOutData().popIndent();
		generator.getOutData().printil("}");
		generator.getOutData().println();
	}

	@Override
	public void visit(NodeDeclaration n) throws JasperException {
		n.setBeginJavaLine(generator.getOutData().getJavaLine());
		generator.getOutData().printMultiLn(n.getText());
		generator.getOutData().println();
		n.setEndJavaLine(generator.getOutData().getJavaLine());
	}

	// Custom Tags may contain declarations from tag plugins.
	@Override
	public void visit(NodeCustomTag n) throws JasperException {
		if (n.useTagPlugin()) {
			if (n.getAtSTag() != null) {
				n.getAtSTag().visit(this);
			}
			visitBody(n);
			if (n.getAtETag() != null) {
				n.getAtETag().visit(this);
			}
		} else {
			visitBody(n);
		}
	}
}