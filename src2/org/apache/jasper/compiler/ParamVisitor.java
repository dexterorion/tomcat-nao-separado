package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ParamVisitor extends NodeVisitor {
	private GeneratorGenerateVisitor generatorGenerateVisitor; 
	private String separator;

	public ParamVisitor(String separator, GeneratorGenerateVisitor generatorGenerateVisitor) {
		this.generatorGenerateVisitor = generatorGenerateVisitor;
		this.separator = separator;
	}

	@Override
	public void visit(NodeParamAction n) throws JasperException {

		generatorGenerateVisitor.getOut().print(" + ");
		generatorGenerateVisitor.getOut().print(separator);
		generatorGenerateVisitor.getOut().print(" + ");
		generatorGenerateVisitor.getOut().print("org.apache.jasper.runtime.JspRuntimeLibrary."
				+ "URLEncode("
				+ Generator.quote(n.getTextAttribute("name"))
				+ ", request.getCharacterEncoding())");
		generatorGenerateVisitor.getOut().print("+ \"=\" + ");
		generatorGenerateVisitor.getOut().print(generatorGenerateVisitor.attributeValue(n.getValue(), true, String.class, n
				.getRoot().isXmlSyntax()));

		// The separator is '&' after the second use
		separator = "\"&\"";
	}
}