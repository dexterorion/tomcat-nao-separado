package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class DumperDumpVisitor extends NodeVisitor {
	private int indent = 0;

	private String getAttributes(Attributes attrs) {
		if (attrs == null)
			return "";

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < attrs.getLength(); i++) {
			buf.append(" " + attrs.getQName(i) + "=\"" + attrs.getValue(i)
					+ "\"");
		}
		return buf.toString();
	}

	private void printString(String str) {
		printIndent();
		System.out.print(str);
	}

	private void printString(String prefix, String str, String suffix) {
		printIndent();
		if (str != null) {
			System.out.print(prefix + str + suffix);
		} else {
			System.out.print(prefix + suffix);
		}
	}

	private void printAttributes(String prefix, Attributes attrs, String suffix) {
		printString(prefix, getAttributes(attrs), suffix);
	}

	private void dumpBody(Node n) throws JasperException {
		NodeNodes page = n.getBody();
		if (page != null) {
			// indent++;
			page.visit(this);
			// indent--;
		}
	}

	@Override
	public void visit(NodePageDirective n) throws JasperException {
		printAttributes("<%@ page", n.getAttributes(), "%>");
	}

	@Override
	public void visit(NodeTaglibDirective n) throws JasperException {
		printAttributes("<%@ taglib", n.getAttributes(), "%>");
	}

	@Override
	public void visit(NodeIncludeDirective n) throws JasperException {
		printAttributes("<%@ include", n.getAttributes(), "%>");
		dumpBody(n);
	}

	@Override
	public void visit(NodeComment n) throws JasperException {
		printString("<%--", n.getText(), "--%>");
	}

	@Override
	public void visit(NodeDeclaration n) throws JasperException {
		printString("<%!", n.getText(), "%>");
	}

	@Override
	public void visit(NodeExpression n) throws JasperException {
		printString("<%=", n.getText(), "%>");
	}

	@Override
	public void visit(NodeScriptlet n) throws JasperException {
		printString("<%", n.getText(), "%>");
	}

	@Override
	public void visit(NodeIncludeAction n) throws JasperException {
		printAttributes("<jsp:include", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:include>");
	}

	@Override
	public void visit(NodeForwardAction n) throws JasperException {
		printAttributes("<jsp:forward", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:forward>");
	}

	@Override
	public void visit(NodeGetProperty n) throws JasperException {
		printAttributes("<jsp:getProperty", n.getAttributes(), "/>");
	}

	@Override
	public void visit(NodeSetProperty n) throws JasperException {
		printAttributes("<jsp:setProperty", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:setProperty>");
	}

	@Override
	public void visit(NodeUseBean n) throws JasperException {
		printAttributes("<jsp:useBean", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:useBean>");
	}

	@Override
	public void visit(NodePlugIn n) throws JasperException {
		printAttributes("<jsp:plugin", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:plugin>");
	}

	@Override
	public void visit(NodeParamsAction n) throws JasperException {
		printAttributes("<jsp:params", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:params>");
	}

	@Override
	public void visit(NodeParamAction n) throws JasperException {
		printAttributes("<jsp:param", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:param>");
	}

	@Override
	public void visit(NodeNamedAttribute n) throws JasperException {
		printAttributes("<jsp:attribute", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:attribute>");
	}

	@Override
	public void visit(NodeJspBody n) throws JasperException {
		printAttributes("<jsp:body", n.getAttributes(), ">");
		dumpBody(n);
		printString("</jsp:body>");
	}

	@Override
	public void visit(NodeELExpression n) throws JasperException {
		printString("${" + n.getText() + "}");
	}

	@Override
	public void visit(NodeCustomTag n) throws JasperException {
		printAttributes("<" + n.getQName(), n.getAttributes(), ">");
		dumpBody(n);
		printString("</" + n.getQName() + ">");
	}

	@Override
	public void visit(NodeUninterpretedTag n) throws JasperException {
		String tag = n.getQName();
		printAttributes("<" + tag, n.getAttributes(), ">");
		dumpBody(n);
		printString("</" + tag + ">");
	}

	@Override
	public void visit(NodeTemplateText n) throws JasperException {
		printString(n.getText());
	}

	private void printIndent() {
		for (int i = 0; i < indent; i++) {
			System.out.print("  ");
		}
	}
}