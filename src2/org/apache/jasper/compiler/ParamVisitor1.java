package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

public class ParamVisitor1 extends NodeVisitor {
	private GeneratorGenerateVisitor generatorGenerateVisitor;
	private final boolean ie;

	public ParamVisitor1(boolean ie, GeneratorGenerateVisitor generatorGenerateVisitor) {
		this.generatorGenerateVisitor = generatorGenerateVisitor;
		this.ie = ie;
	}

	@Override
	public void visit(NodeParamAction n) throws JasperException {

		String name = n.getTextAttribute("name");
		if (name.equalsIgnoreCase("object"))
			name = "java_object";
		else if (name.equalsIgnoreCase("type"))
			name = "java_type";

		n.setBeginJavaLine(generatorGenerateVisitor.getOut().getJavaLine());
		// XXX - Fixed a bug here - value used to be output
		// inline, which is only okay if value is not an EL
		// expression. Also, key/value pairs for the
		// embed tag were not being generated correctly.
		// Double check that this is now the correct behavior.
		if (ie) {
			// We want something of the form
			// out.println( "<param name=\"blah\"
			// value=\"" + ... + "\">" );
			generatorGenerateVisitor.getOut().printil("out.write( \"<param name=\\\""
					+ Generator.escape(name)
					+ "\\\" value=\\\"\" + "
					+ generatorGenerateVisitor.attributeValue(n.getValue(), false, String.class,
							n.getRoot().isXmlSyntax())
					+ " + \"\\\">\" );");
			generatorGenerateVisitor.getOut().printil("out.write(\"\\n\");");
		} else {
			// We want something of the form
			// out.print( " blah=\"" + ... + "\"" );
			generatorGenerateVisitor.getOut().printil("out.write( \" "
					+ Generator.escape(name)
					+ "=\\\"\" + "
					+ generatorGenerateVisitor.attributeValue(n.getValue(), false, String.class,
							n.getRoot().isXmlSyntax())
					+ " + \"\\\"\" );");
		}

		n.setEndJavaLine(generatorGenerateVisitor.getOut().getJavaLine());
	}
}