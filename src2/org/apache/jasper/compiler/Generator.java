/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jasper.compiler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;

import org.apache.jasper.Constants28;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;

/**
 * Generate Java source from Nodes
 *
 * @author Anil K. Vijendran
 * @author Danno Ferrin
 * @author Mandar Raje
 * @author Rajiv Mordani
 * @author Pierre Delisle
 *
 *         Tomcat 4.1.x and Tomcat 5:
 * @author Kin-man Chung
 * @author Jan Luehe
 * @author Shawn Bayern
 * @author Mark Roth
 * @author Denis Benoit
 *
 *         Tomcat 6.x
 * @author Jacob Hookom
 * @author Remy Maucherat
 */

public class Generator {

	private static final Class<?>[] OBJECT_CLASS = { Object.class };

	private static final String VAR_EXPRESSIONFACTORY = System.getProperty(
			"org.apache.jasper.compiler.Generator.VAR_EXPRESSIONFACTORY",
			"_el_expressionfactory");
	private static final String VAR_INSTANCEMANAGER = System.getProperty(
			"org.apache.jasper.compiler.Generator.VAR_INSTANCEMANAGER",
			"_jsp_instancemanager");
	private static final boolean POOL_TAGS_WITH_EXTENDS = Boolean
			.getBoolean("org.apache.jasper.compiler.Generator.POOL_TAGS_WITH_EXTENDS");

	/*
	 * System property that controls if the requirement to have the object used
	 * in jsp:getProperty action to be previously "introduced" to the JSP
	 * processor (see JSP.5.3) is enforced.
	 */
	private static final boolean STRICT_GET_PROPERTY = Boolean.valueOf(
			System.getProperty(
					"org.apache.jasper.compiler.Generator.STRICT_GET_PROPERTY",
					"true")).booleanValue();

	private final ServletWriter out;

	private final ArrayList<GeneratorGenBuffer> methodsBuffered;

	private final GeneratorFragmentHelperClass fragmentHelperClass;

	private final ErrorDispatcher err;

	private final BeanRepository beanInfo;

	private final Set<String> varInfoNames;

	private final JspCompilationContext ctxt;

	private final boolean isPoolingEnabled;

	private final boolean breakAtLF;

	private String jspIdPrefix;

	private int jspId;

	private final PageInfo pageInfo;

	private final Vector<String> tagHandlerPoolNames;

	private GeneratorGenBuffer charArrayBuffer;

	private final DateFormat timestampFormat;

	private final ELInterpreter elInterpreter;

	/**
	 * @param s
	 *            the input string
	 * @return quoted and escaped string, per Java rule
	 */
	public static String quote(String s) {

		if (s == null)
			return "null";

		return '"' + escape(s) + '"';
	}

	/**
	 * @param s
	 *            the input string
	 * @return escaped string, per Java rule
	 */
	public static String escape(String s) {

		if (s == null)
			return "";

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"')
				b.append('\\').append('"');
			else if (c == '\\')
				b.append('\\').append('\\');
			else if (c == '\n')
				b.append('\\').append('n');
			else if (c == '\r')
				b.append('\\').append('r');
			else
				b.append(c);
		}
		return b.toString();
	}

	/**
	 * Single quote and escape a character
	 */
	public static String quote(char c) {

		StringBuilder b = new StringBuilder();
		b.append('\'');
		if (c == '\'')
			b.append('\\').append('\'');
		else if (c == '\\')
			b.append('\\').append('\\');
		else if (c == '\n')
			b.append('\\').append('n');
		else if (c == '\r')
			b.append('\\').append('r');
		else
			b.append(c);
		b.append('\'');
		return b.toString();
	}

	String createJspId() {
		if (this.getJspIdPrefixData() == null) {
			StringBuilder sb = new StringBuilder(32);
			String name = getCtxtData().getServletJavaFileName();
			sb.append("jsp_");
			// Cast to long to avoid issue with Integer.MIN_VALUE
			sb.append(Math.abs((long) name.hashCode()));
			sb.append('_');
			this.setJspIdPrefixData(sb.toString());
		}
		this.setJspIdData(this.getJspIdData() + 1);
		return this.getJspIdPrefixData() + (this.getJspIdData()-1);
	}

	/**
	 * Generates declarations. This includes "info" of the page directive, and
	 * scriptlet declarations.
	 */
	private void generateDeclarations(NodeNodes page) throws JasperException {
		getOutData().println();
		page.visit(new DeclarationVisitor(this));
	}

	/**
	 * Compiles list of tag handler pool names.
	 */
	private void compileTagHandlerPoolList(NodeNodes page)
			throws JasperException {

		page.visit(new TagHandlerPoolVisitor(getTagHandlerPoolNamesData()));
	}

	private void declareTemporaryScriptingVars(NodeNodes page)
			throws JasperException {

		page.visit(new ScriptingVarVisitor(this));
	}

	/**
	 * Generates the _jspInit() method for instantiating the tag handler pools.
	 * For tag file, _jspInit has to be invoked manually, and the ServletConfig
	 * object explicitly passed.
	 *
	 * In JSP 2.1, we also instantiate an ExpressionFactory
	 */
	private void generateInit() {

		if (getCtxtData().isTagFile()) {
			getOutData().printil("private void _jspInit(javax.servlet.ServletConfig config) {");
		} else {
			getOutData().printil("public void _jspInit() {");
		}

		getOutData().pushIndent();
		if (isPoolingEnabledData()) {
			for (int i = 0; i < getTagHandlerPoolNamesData().size(); i++) {
				getOutData().printin(getTagHandlerPoolNamesData().elementAt(i));
				getOutData().print(" = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(");
				if (getCtxtData().isTagFile()) {
					getOutData().print("config");
				} else {
					getOutData().print("getServletConfig()");
				}
				getOutData().println(");");
			}
		}

		getOutData().printin(VAR_EXPRESSIONFACTORY);
		getOutData().print(" = _jspxFactory.getJspApplicationContext(");
		if (getCtxtData().isTagFile()) {
			getOutData().print("config");
		} else {
			getOutData().print("getServletConfig()");
		}
		getOutData().println(".getServletContext()).getExpressionFactory();");

		getOutData().printin(VAR_INSTANCEMANAGER);
		getOutData().print(" = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(");
		if (getCtxtData().isTagFile()) {
			getOutData().print("config");
		} else {
			getOutData().print("getServletConfig()");
		}
		getOutData().println(");");

		getOutData().popIndent();
		getOutData().printil("}");
		getOutData().println();
	}

	/**
	 * Generates the _jspDestroy() method which is responsible for calling the
	 * release() method on every tag handler in any of the tag handler pools.
	 */
	private void generateDestroy() {

		getOutData().printil("public void _jspDestroy() {");
		getOutData().pushIndent();

		if (isPoolingEnabledData()) {
			for (int i = 0; i < getTagHandlerPoolNamesData().size(); i++) {
				getOutData().printin(getTagHandlerPoolNamesData().elementAt(i));
				getOutData().println(".release();");
			}
		}

		getOutData().popIndent();
		getOutData().printil("}");
		getOutData().println();
	}

	/**
	 * Generate preamble package name (shared by servlet and tag handler
	 * preamble generation)
	 */
	private void genPreamblePackage(String packageName) {
		if (!"".equals(packageName) && packageName != null) {
			getOutData().printil("package " + packageName + ";");
			getOutData().println();
		}
	}

	/**
	 * Generate preamble imports (shared by servlet and tag handler preamble
	 * generation)
	 */
	private void genPreambleImports() {
		Iterator<String> iter = getPageInfoData().getImports().iterator();
		while (iter.hasNext()) {
			getOutData().printin("import ");
			getOutData().print(iter.next());
			getOutData().println(";");
		}

		getOutData().println();
	}

	/**
	 * Generation of static initializers in preamble. For example, dependent
	 * list, el function map, prefix map. (shared by servlet and tag handler
	 * preamble generation)
	 */
	private void genPreambleStaticInitializers() {
		getOutData().printil("private static final javax.servlet.jsp.JspFactory _jspxFactory =");
		getOutData().printil("        javax.servlet.jsp.JspFactory.getDefaultFactory();");
		getOutData().println();

		// Static data for getDependants()
		getOutData().printil("private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;");
		getOutData().println();
		Map<String, Long> dependants = getPageInfoData().getDependants();
		Iterator<Entry<String, Long>> iter = dependants.entrySet().iterator();
		if (!dependants.isEmpty()) {
			getOutData().printil("static {");
			getOutData().pushIndent();
			getOutData().printin("_jspx_dependants = new java.util.HashMap<java.lang.String,java.lang.Long>(");
			getOutData().print("" + dependants.size());
			getOutData().println(");");
			while (iter.hasNext()) {
				Entry<String, Long> entry = iter.next();
				getOutData().printin("_jspx_dependants.put(\"");
				getOutData().print(entry.getKey());
				getOutData().print("\", Long.valueOf(");
				getOutData().print(entry.getValue().toString());
				getOutData().println("L));");
			}
			getOutData().popIndent();
			getOutData().printil("}");
			getOutData().println();
		}
	}

	/**
	 * Declare tag handler pools (tags of the same type and with the same
	 * attribute set share the same tag handler pool) (shared by servlet and tag
	 * handler preamble generation)
	 *
	 * In JSP 2.1, we also scope an instance of ExpressionFactory
	 */
	private void genPreambleClassVariableDeclarations() {
		if (isPoolingEnabledData() && !getTagHandlerPoolNamesData().isEmpty()) {
			for (int i = 0; i < getTagHandlerPoolNamesData().size(); i++) {
				getOutData().printil("private org.apache.jasper.runtime.TagHandlerPool "
						+ getTagHandlerPoolNamesData().elementAt(i) + ";");
			}
			getOutData().println();
		}
		getOutData().printin("private javax.el.ExpressionFactory ");
		getOutData().print(VAR_EXPRESSIONFACTORY);
		getOutData().println(";");
		getOutData().printin("private org.apache.tomcat.InstanceManager ");
		getOutData().print(VAR_INSTANCEMANAGER);
		getOutData().println(";");
		getOutData().println();
	}

	/**
	 * Declare general-purpose methods (shared by servlet and tag handler
	 * preamble generation)
	 */
	private void genPreambleMethods() {
		// Method used to get compile time file dependencies
		getOutData().printil("public java.util.Map<java.lang.String,java.lang.Long> getDependants() {");
		getOutData().pushIndent();
		getOutData().printil("return _jspx_dependants;");
		getOutData().popIndent();
		getOutData().printil("}");
		getOutData().println();

		generateInit();
		generateDestroy();
	}

	/**
	 * Generates the beginning of the static portion of the servlet.
	 */
	private void generatePreamble(NodeNodes page) throws JasperException {

		String servletPackageName = getCtxtData().getServletPackageName();
		String servletClassName = getCtxtData().getServletClassName();
		String serviceMethodName = Constants28.getServiceMethodName();

		// First the package name:
		genPreamblePackage(servletPackageName);

		// Generate imports
		genPreambleImports();

		// Generate class declaration
		getOutData().printin("public final class ");
		getOutData().print(servletClassName);
		getOutData().print(" extends ");
		getOutData().println(getPageInfoData().getExtends());
		getOutData().printin("    implements org.apache.jasper.runtime.JspSourceDependent");
		if (!getPageInfoData().isThreadSafe()) {
			getOutData().println(",");
			getOutData().printin("                 javax.servlet.SingleThreadModel");
		}
		getOutData().println(" {");
		getOutData().pushIndent();

		// Class body begins here
		generateDeclarations(page);

		// Static initializations here
		genPreambleStaticInitializers();

		// Class variable declarations
		genPreambleClassVariableDeclarations();

		// Methods here
		genPreambleMethods();

		// Now the service method
		getOutData().printin("public void ");
		getOutData().print(serviceMethodName);
		getOutData().println("(final javax.servlet.http.HttpServletRequest request, final javax.servlet.http.HttpServletResponse response)");
		getOutData().println("        throws java.io.IOException, javax.servlet.ServletException {");

		getOutData().pushIndent();
		getOutData().println();

		// Local variable declarations
		getOutData().printil("final javax.servlet.jsp.PageContext pageContext;");

		if (getPageInfoData().isSession())
			getOutData().printil("javax.servlet.http.HttpSession session = null;");

		if (getPageInfoData().isErrorPage()) {
			getOutData().printil("java.lang.Throwable exception = org.apache.jasper.runtime.JspRuntimeLibrary.getThrowable(request);");
			getOutData().printil("if (exception != null) {");
			getOutData().pushIndent();
			getOutData().printil("response.setStatus(javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);");
			getOutData().popIndent();
			getOutData().printil("}");
		}

		getOutData().printil("final javax.servlet.ServletContext application;");
		getOutData().printil("final javax.servlet.ServletConfig config;");
		getOutData().printil("javax.servlet.jsp.JspWriter out = null;");
		getOutData().printil("final java.lang.Object page = this;");

		getOutData().printil("javax.servlet.jsp.JspWriter _jspx_out = null;");
		getOutData().printil("javax.servlet.jsp.PageContext _jspx_page_context = null;");
		getOutData().println();

		declareTemporaryScriptingVars(page);
		getOutData().println();

		getOutData().printil("try {");
		getOutData().pushIndent();

		getOutData().printin("response.setContentType(");
		getOutData().print(quote(getPageInfoData().getContentType()));
		getOutData().println(");");

		if (getCtxtData().getOptions().isXpoweredBy()) {
			getOutData().printil("response.addHeader(\"X-Powered-By\", \"JSP/2.1\");");
		}

		getOutData().printil("pageContext = _jspxFactory.getPageContext(this, request, response,");
		getOutData().printin("\t\t\t");
		getOutData().print(quote(getPageInfoData().getErrorPage()));
		getOutData().print(", " + getPageInfoData().isSession());
		getOutData().print(", " + getPageInfoData().getBuffer());
		getOutData().print(", " + getPageInfoData().isAutoFlush());
		getOutData().println(");");
		getOutData().printil("_jspx_page_context = pageContext;");

		getOutData().printil("application = pageContext.getServletContext();");
		getOutData().printil("config = pageContext.getServletConfig();");

		if (getPageInfoData().isSession())
			getOutData().printil("session = pageContext.getSession();");
		getOutData().printil("out = pageContext.getOut();");
		getOutData().printil("_jspx_out = out;");
		getOutData().println();
	}

	/**
	 * Generates an XML Prolog, which includes an XML declaration and an XML
	 * doctype declaration.
	 */
	private void generateXmlProlog(NodeNodes page) {

		/*
		 * An XML declaration is generated under the following conditions: -
		 * 'omit-xml-declaration' attribute of <jsp:output> action is set to
		 * "no" or "false" - JSP document without a <jsp:root>
		 */
		String omitXmlDecl = getPageInfoData().getOmitXmlDecl();
		if ((omitXmlDecl != null && !JspUtil.booleanValue(omitXmlDecl))
				|| (omitXmlDecl == null && page.getRoot().isXmlSyntax()
						&& !getPageInfoData().hasJspRoot() && !getCtxtData().isTagFile())) {
			String cType = getPageInfoData().getContentType();
			String charSet = cType.substring(cType.indexOf("charset=") + 8);
			getOutData().printil("out.write(\"<?xml version=\\\"1.0\\\" encoding=\\\""
					+ charSet + "\\\"?>\\n\");");
		}

		/*
		 * Output a DOCTYPE declaration if the doctype-root-element appears. If
		 * doctype-public appears: <!DOCTYPE name PUBLIC "doctypePublic"
		 * "doctypeSystem"> else <!DOCTYPE name SYSTEM "doctypeSystem" >
		 */

		String doctypeName = getPageInfoData().getDoctypeName();
		if (doctypeName != null) {
			String doctypePublic = getPageInfoData().getDoctypePublic();
			String doctypeSystem = getPageInfoData().getDoctypeSystem();
			getOutData().printin("out.write(\"<!DOCTYPE ");
			getOutData().print(doctypeName);
			if (doctypePublic == null) {
				getOutData().print(" SYSTEM \\\"");
			} else {
				getOutData().print(" PUBLIC \\\"");
				getOutData().print(doctypePublic);
				getOutData().print("\\\" \\\"");
			}
			getOutData().print(doctypeSystem);
			getOutData().println("\\\">\\n\");");
		}
	}

	public static void generateLocalVariables(ServletWriter out, Node n)
			throws JasperException {
		NodeChildInfo ci;
		if (n instanceof NodeCustomTag) {
			ci = ((NodeCustomTag) n).getNodeChildInfo();
		} else if (n instanceof NodeJspBody) {
			ci = ((NodeJspBody) n).getNodeChildInfo();
		} else if (n instanceof NodeNamedAttribute) {
			ci = ((NodeNamedAttribute) n).getNodeChildInfo();
		} else {
			// Cannot access err since this method is static, but at
			// least flag an error.
			throw new JasperException("Unexpected Node Type");
			// err.getString(
			// "jsp.error.internal.unexpected_node_type" ) );
		}

		if (ci.hasNodeUseBean()) {
			out.printil("javax.servlet.http.HttpSession session = _jspx_page_context.getSession();");
			out.printil("javax.servlet.ServletContext application = _jspx_page_context.getServletContext();");
		}
		if (ci.hasNodeUseBean() || ci.hasNodeIncludeAction()
				|| ci.hasNodeSetProperty() || ci.hasNodeParamAction()) {
			out.printil("javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest)_jspx_page_context.getRequest();");
		}
		if (ci.hasNodeIncludeAction()) {
			out.printil("javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse)_jspx_page_context.getResponse();");
		}
	}

	/**
	 * Common part of postamble, shared by both servlets and tag files.
	 */
	private void genCommonPostamble() {
		// Append any methods that were generated in the buffer.
		for (int i = 0; i < getMethodsBufferedData().size(); i++) {
			GeneratorGenBuffer methodBuffer = getMethodsBufferedData().get(i);
			methodBuffer.adjustJavaLines(getOutData().getJavaLine() - 1);
			getOutData().printMultiLn(methodBuffer.toString());
		}

		// Append the helper class
		if (getFragmentHelperClassData().isUsed()) {
			getFragmentHelperClassData().generatePostamble();
			getFragmentHelperClassData().adjustJavaLines(getOutData().getJavaLine() - 1);
			getOutData().printMultiLn(getFragmentHelperClassData().toString());
		}

		// Append char array declarations
		if (getCharArrayBufferData() != null) {
			getOutData().printMultiLn(getCharArrayBufferData().toString());
		}

		// Close the class definition
		getOutData().popIndent();
		getOutData().printil("}");
	}

	/**
	 * Generates the ending part of the static portion of the servlet.
	 */
	private void generatePostamble() {
		getOutData().popIndent();
		getOutData().printil("} catch (java.lang.Throwable t) {");
		getOutData().pushIndent();
		getOutData().printil("if (!(t instanceof javax.servlet.jsp.SkipPageException)){");
		getOutData().pushIndent();
		getOutData().printil("out = _jspx_out;");
		getOutData().printil("if (out != null && out.getBufferSize() != 0)");
		getOutData().pushIndent();
		getOutData().printil("try {");
		getOutData().pushIndent();
		getOutData().printil("if (response.isCommitted()) {");
		getOutData().pushIndent();
		getOutData().printil("out.flush();");
		getOutData().popIndent();
		getOutData().printil("} else {");
		getOutData().pushIndent();
		getOutData().printil("out.clearBuffer();");
		getOutData().popIndent();
		getOutData().printil("}");
		getOutData().popIndent();
		getOutData().printil("} catch (java.io.IOException e) {}");
		getOutData().popIndent();
		getOutData().printil("if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);");
		getOutData().printil("else throw new ServletException(t);");
		getOutData().popIndent();
		getOutData().printil("}");
		getOutData().popIndent();
		getOutData().printil("} finally {");
		getOutData().pushIndent();
		getOutData().printil("_jspxFactory.releasePageContext(_jspx_page_context);");
		getOutData().popIndent();
		getOutData().printil("}");

		// Close the service method
		getOutData().popIndent();
		getOutData().printil("}");

		// Generated methods, helper classes, etc.
		genCommonPostamble();
	}

	/**
	 * Constructor.
	 */
	public Generator(ServletWriter out, Compiler2 compiler)
			throws JasperException {
		this.out = out;
		methodsBuffered = new ArrayList<GeneratorGenBuffer>();
		setCharArrayBufferData(null);
		err = compiler.getErrorDispatcher();
		ctxt = compiler.getCompilationContext();
		fragmentHelperClass = new GeneratorFragmentHelperClass("Helper");
		pageInfo = compiler.getPageInfo();

		ELInterpreter elInterpreter = null;
		try {
			elInterpreter = ELInterpreterFactory.getELInterpreter(compiler
					.getCompilationContext().getServletContext());
		} catch (Exception e) {
			getErrData().jspError("jsp.error.el_interpreter_class.instantiation",
					e.getMessage());
		}
		this.elInterpreter = elInterpreter;

		/*
		 * Temporary hack. If a JSP page uses the "extends" attribute of the
		 * page directive, the _jspInit() method of the generated servlet class
		 * will not be called (it is only called for those generated servlets
		 * that extend HttpJspBase, the default), causing the tag handler pools
		 * not to be initialized and resulting in a NPE. The JSP spec needs to
		 * clarify whether containers can override init() and destroy(). For
		 * now, we just disable tag pooling for pages that use "extends".
		 */
		if (getPageInfoData().getExtends(false) == null || POOL_TAGS_WITH_EXTENDS) {
			isPoolingEnabled = getCtxtData().getOptions().isPoolingEnabled();
		} else {
			isPoolingEnabled = false;
		}
		beanInfo = getPageInfoData().getBeanRepository();
		varInfoNames = getPageInfoData().getVarInfoNames();
		breakAtLF = getCtxtData().getOptions().getMappedFile();
		if (isPoolingEnabledData()) {
			tagHandlerPoolNames = new Vector<String>();
		} else {
			tagHandlerPoolNames = null;
		}
		timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		getTimestampFormatData().setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * The main entry for Generator.
	 *
	 * @param out
	 *            The servlet output writer
	 * @param compiler
	 *            The compiler
	 * @param page
	 *            The input page
	 */
	public static void generate(ServletWriter out, Compiler2 compiler,
			NodeNodes page) throws JasperException {

		Generator gen = new Generator(out, compiler);

		if (gen.isPoolingEnabledData()) {
			gen.compileTagHandlerPoolList(page);
		}
		gen.generateCommentHeader();
		if (gen.getCtxtData().isTagFile()) {
			JasperTagInfo tagInfo = (JasperTagInfo) gen.getCtxtData().getTagInfo();
			gen.generateTagHandlerPreamble(tagInfo, page);

			if (gen.getCtxtData().isPrototypeMode()) {
				return;
			}

			gen.generateXmlProlog(page);
			gen.getFragmentHelperClassData().generatePreamble();
			page.visit(new GeneratorGenerateVisitor(gen, gen.getCtxtData().isTagFile(),
					out, gen.getMethodsBufferedData(), gen.getFragmentHelperClassData()));
			gen.generateTagHandlerPostamble(tagInfo);
		} else {
			gen.generatePreamble(page);
			gen.generateXmlProlog(page);
			gen.getFragmentHelperClassData().generatePreamble();
			page.visit(new GeneratorGenerateVisitor(gen, gen.getCtxtData().isTagFile(),
					out, gen.getMethodsBufferedData(), gen.getFragmentHelperClassData()));
			gen.generatePostamble();
		}
	}

	private void generateCommentHeader() {
		getOutData().println("/*");
		getOutData().println(" * Generated by the Jasper component of Apache Tomcat");
		getOutData().println(" * Version: " + getCtxtData().getServletContext().getServerInfo());
		getOutData().println(" * Generated at: " + getTimestampFormatData().format(new Date())
				+ " UTC");
		getOutData().println(" * Note: The last modified time of this file was set to");
		getOutData().println(" *       the last modified time of the source file after");
		getOutData().println(" *       generation to assist with modification tracking.");
		getOutData().println(" */");
	}

	/*
	 * Generates tag handler preamble.
	 */
	private void generateTagHandlerPreamble(JasperTagInfo tagInfo, NodeNodes tag)
			throws JasperException {

		// Generate package declaration
		String className = tagInfo.getTagClassName();
		int lastIndex = className.lastIndexOf('.');
		if (lastIndex != -1) {
			String pkgName = className.substring(0, lastIndex);
			genPreamblePackage(pkgName);
			className = className.substring(lastIndex + 1);
		}

		// Generate imports
		genPreambleImports();

		// Generate class declaration
		getOutData().printin("public final class ");
		getOutData().println(className);
		getOutData().printil("    extends javax.servlet.jsp.tagext.SimpleTagSupport");
		getOutData().printin("    implements org.apache.jasper.runtime.JspSourceDependent");
		if (tagInfo.hasDynamicAttributes()) {
			getOutData().println(",");
			getOutData().printin("               javax.servlet.jsp.tagext.DynamicAttributes");
		}
		getOutData().println(" {");
		getOutData().println();
		getOutData().pushIndent();

		/*
		 * Class body begins here
		 */
		generateDeclarations(tag);

		// Static initializations here
		genPreambleStaticInitializers();

		getOutData().printil("private javax.servlet.jsp.JspContext jspContext;");

		// Declare writer used for storing result of fragment/body invocation
		// if 'varReader' or 'var' attribute is specified
		getOutData().printil("private java.io.Writer _jspx_sout;");

		// Class variable declarations
		genPreambleClassVariableDeclarations();

		generateSetJspContext(tagInfo);

		// Tag-handler specific declarations
		generateTagHandlerAttributes(tagInfo);
		if (tagInfo.hasDynamicAttributes())
			generateSetDynamicAttribute();

		// Methods here
		genPreambleMethods();

		// Now the doTag() method
		getOutData().printil("public void doTag() throws javax.servlet.jsp.JspException, java.io.IOException {");

		if (getCtxtData().isPrototypeMode()) {
			getOutData().printil("}");
			getOutData().popIndent();
			getOutData().printil("}");
			return;
		}

		getOutData().pushIndent();

		/*
		 * According to the spec, 'pageContext' must not be made available as an
		 * implicit object in tag files. Declare _jspx_page_context, so we can
		 * share the code generator with JSPs.
		 */
		getOutData().printil("javax.servlet.jsp.PageContext _jspx_page_context = (javax.servlet.jsp.PageContext)jspContext;");

		// Declare implicit objects.
		getOutData().printil("javax.servlet.http.HttpServletRequest request = "
				+ "(javax.servlet.http.HttpServletRequest) _jspx_page_context.getRequest();");
		getOutData().printil("javax.servlet.http.HttpServletResponse response = "
				+ "(javax.servlet.http.HttpServletResponse) _jspx_page_context.getResponse();");
		getOutData().printil("javax.servlet.http.HttpSession session = _jspx_page_context.getSession();");
		getOutData().printil("javax.servlet.ServletContext application = _jspx_page_context.getServletContext();");
		getOutData().printil("javax.servlet.ServletConfig config = _jspx_page_context.getServletConfig();");
		getOutData().printil("javax.servlet.jsp.JspWriter out = jspContext.getOut();");
		getOutData().printil("_jspInit(config);");

		// set current JspContext on ELContext
		getOutData().printil("jspContext.getELContext().putContext(javax.servlet.jsp.JspContext.class,jspContext);");

		generatePageScopedVariables(tagInfo);

		declareTemporaryScriptingVars(tag);
		getOutData().println();

		getOutData().printil("try {");
		getOutData().pushIndent();
	}

	private void generateTagHandlerPostamble(TagInfo tagInfo) {
		getOutData().popIndent();

		// Have to catch Throwable because a classic tag handler
		// helper method is declared to throw Throwable.
		getOutData().printil("} catch( java.lang.Throwable t ) {");
		getOutData().pushIndent();
		getOutData().printil("if( t instanceof javax.servlet.jsp.SkipPageException )");
		getOutData().printil("    throw (javax.servlet.jsp.SkipPageException) t;");
		getOutData().printil("if( t instanceof java.io.IOException )");
		getOutData().printil("    throw (java.io.IOException) t;");
		getOutData().printil("if( t instanceof java.lang.IllegalStateException )");
		getOutData().printil("    throw (java.lang.IllegalStateException) t;");
		getOutData().printil("if( t instanceof javax.servlet.jsp.JspException )");
		getOutData().printil("    throw (javax.servlet.jsp.JspException) t;");
		getOutData().printil("throw new javax.servlet.jsp.JspException(t);");
		getOutData().popIndent();
		getOutData().printil("} finally {");
		getOutData().pushIndent();

		// handle restoring VariableMapper
		TagAttributeInfo[] attrInfos = tagInfo.getAttributes();
		for (int i = 0; i < attrInfos.length; i++) {
			if (attrInfos[i].isDeferredMethod()
					|| attrInfos[i].isDeferredValue()) {
				getOutData().printin("_el_variablemapper.setVariable(");
				getOutData().print(quote(attrInfos[i].getName()));
				getOutData().print(",_el_ve");
				getOutData().print(i);
				getOutData().println(");");
			}
		}

		// restore nested JspContext on ELContext
		getOutData().printil("jspContext.getELContext().putContext(javax.servlet.jsp.JspContext.class,super.getJspContext());");

		getOutData().printil("((org.apache.jasper.runtime.JspContextWrapper) jspContext).syncEndTagFile();");
		if (isPoolingEnabledData() && !getTagHandlerPoolNamesData().isEmpty()) {
			getOutData().printil("_jspDestroy();");
		}
		getOutData().popIndent();
		getOutData().printil("}");

		// Close the doTag method
		getOutData().popIndent();
		getOutData().printil("}");

		// Generated methods, helper classes, etc.
		genCommonPostamble();
	}

	/**
	 * Generates declarations for tag handler attributes, and defines the getter
	 * and setter methods for each.
	 */
	private void generateTagHandlerAttributes(TagInfo tagInfo) {

		if (tagInfo.hasDynamicAttributes()) {
			getOutData().printil("private java.util.HashMap _jspx_dynamic_attrs = new java.util.HashMap();");
		}

		// Declare attributes
		TagAttributeInfo[] attrInfos = tagInfo.getAttributes();
		for (int i = 0; i < attrInfos.length; i++) {
			getOutData().printin("private ");
			if (attrInfos[i].isFragment()) {
				getOutData().print("javax.servlet.jsp.tagext.JspFragment ");
			} else {
				getOutData().print(JspUtil.toJavaSourceType(attrInfos[i].getTypeName()));
				getOutData().print(" ");
			}
			getOutData().print(JspUtil.makeJavaIdentifierForAttribute(attrInfos[i]
					.getName()));
			getOutData().println(";");
		}
		getOutData().println();

		// Define attribute getter and setter methods
		for (int i = 0; i < attrInfos.length; i++) {
			String javaName = JspUtil
					.makeJavaIdentifierForAttribute(attrInfos[i].getName());

			// getter method
			getOutData().printin("public ");
			if (attrInfos[i].isFragment()) {
				getOutData().print("javax.servlet.jsp.tagext.JspFragment ");
			} else {
				getOutData().print(JspUtil.toJavaSourceType(attrInfos[i].getTypeName()));
				getOutData().print(" ");
			}
			getOutData().print(toGetterMethod(attrInfos[i].getName()));
			getOutData().println(" {");
			getOutData().pushIndent();
			getOutData().printin("return this.");
			getOutData().print(javaName);
			getOutData().println(";");
			getOutData().popIndent();
			getOutData().printil("}");
			getOutData().println();

			// setter method
			getOutData().printin("public void ");
			getOutData().print(toSetterMethodName(attrInfos[i].getName()));
			if (attrInfos[i].isFragment()) {
				getOutData().print("(javax.servlet.jsp.tagext.JspFragment ");
			} else {
				getOutData().print("(");
				getOutData().print(JspUtil.toJavaSourceType(attrInfos[i].getTypeName()));
				getOutData().print(" ");
			}
			getOutData().print(javaName);
			getOutData().println(") {");
			getOutData().pushIndent();
			getOutData().printin("this.");
			getOutData().print(javaName);
			getOutData().print(" = ");
			getOutData().print(javaName);
			getOutData().println(";");
			if (getCtxtData().isTagFile()) {
				// Tag files should also set jspContext attributes
				getOutData().printin("jspContext.setAttribute(\"");
				getOutData().print(attrInfos[i].getName());
				getOutData().print("\", ");
				getOutData().print(javaName);
				getOutData().println(");");
			}
			getOutData().popIndent();
			getOutData().printil("}");
			getOutData().println();
		}
	}

	/*
	 * Generate setter for JspContext so we can create a wrapper and store both
	 * the original and the wrapper. We need the wrapper to mask the page
	 * context from the tag file and simulate a fresh page context. We need the
	 * original to do things like sync AT_BEGIN and AT_END scripting variables.
	 */
	private void generateSetJspContext(TagInfo tagInfo) {

		boolean nestedSeen = false;
		boolean atBeginSeen = false;
		boolean atEndSeen = false;

		// Determine if there are any aliases
		boolean aliasSeen = false;
		TagVariableInfo[] tagVars = tagInfo.getTagVariableInfos();
		for (int i = 0; i < tagVars.length; i++) {
			if (tagVars[i].getNameFromAttribute() != null
					&& tagVars[i].getNameGiven() != null) {
				aliasSeen = true;
				break;
			}
		}

		if (aliasSeen) {
			getOutData().printil("public void setJspContext(javax.servlet.jsp.JspContext ctx, java.util.Map aliasMap) {");
		} else {
			getOutData().printil("public void setJspContext(javax.servlet.jsp.JspContext ctx) {");
		}
		getOutData().pushIndent();
		getOutData().printil("super.setJspContext(ctx);");
		getOutData().printil("java.util.ArrayList _jspx_nested = null;");
		getOutData().printil("java.util.ArrayList _jspx_at_begin = null;");
		getOutData().printil("java.util.ArrayList _jspx_at_end = null;");

		for (int i = 0; i < tagVars.length; i++) {

			switch (tagVars[i].getScope()) {
			case 0:
				if (!nestedSeen) {
					getOutData().printil("_jspx_nested = new java.util.ArrayList();");
					nestedSeen = true;
				}
				getOutData().printin("_jspx_nested.add(");
				break;

			case 1:
				if (!atBeginSeen) {
					getOutData().printil("_jspx_at_begin = new java.util.ArrayList();");
					atBeginSeen = true;
				}
				getOutData().printin("_jspx_at_begin.add(");
				break;

			case 2:
				if (!atEndSeen) {
					getOutData().printil("_jspx_at_end = new java.util.ArrayList();");
					atEndSeen = true;
				}
				getOutData().printin("_jspx_at_end.add(");
				break;
			}

			getOutData().print(quote(tagVars[i].getNameGiven()));
			getOutData().println(");");
		}
		if (aliasSeen) {
			getOutData().printil("this.jspContext = new org.apache.jasper.runtime.JspContextWrapper(ctx, _jspx_nested, _jspx_at_begin, _jspx_at_end, aliasMap);");
		} else {
			getOutData().printil("this.jspContext = new org.apache.jasper.runtime.JspContextWrapper(ctx, _jspx_nested, _jspx_at_begin, _jspx_at_end, null);");
		}
		getOutData().popIndent();
		getOutData().printil("}");
		getOutData().println();
		getOutData().printil("public javax.servlet.jsp.JspContext getJspContext() {");
		getOutData().pushIndent();
		getOutData().printil("return this.jspContext;");
		getOutData().popIndent();
		getOutData().printil("}");
	}

	/*
	 * Generates implementation of
	 * javax.servlet.jsp.tagext.DynamicAttributes.setDynamicAttribute() method,
	 * which saves each dynamic attribute that is passed in so that a scoped
	 * variable can later be created for it.
	 */
	public void generateSetDynamicAttribute() {
		getOutData().printil("public void setDynamicAttribute(java.lang.String uri, java.lang.String localName, java.lang.Object value) throws javax.servlet.jsp.JspException {");
		getOutData().pushIndent();
		/*
		 * According to the spec, only dynamic attributes with no uri are to be
		 * present in the Map; all other dynamic attributes are ignored.
		 */
		getOutData().printil("if (uri == null)");
		getOutData().pushIndent();
		getOutData().printil("_jspx_dynamic_attrs.put(localName, value);");
		getOutData().popIndent();
		getOutData().popIndent();
		getOutData().printil("}");
	}

	/*
	 * Creates a page-scoped variable for each declared tag attribute. Also, if
	 * the tag accepts dynamic attributes, a page-scoped variable is made
	 * available for each dynamic attribute that was passed in.
	 */
	private void generatePageScopedVariables(JasperTagInfo tagInfo) {

		// "normal" attributes
		TagAttributeInfo[] attrInfos = tagInfo.getAttributes();
		boolean variableMapperVar = false;
		for (int i = 0; i < attrInfos.length; i++) {
			String attrName = attrInfos[i].getName();

			// handle assigning deferred vars to VariableMapper, storing
			// previous values under '_el_ve[i]' for later re-assignment
			if (attrInfos[i].isDeferredValue()
					|| attrInfos[i].isDeferredMethod()) {

				// we need to scope the modified VariableMapper for consistency
				// and performance
				if (!variableMapperVar) {
					getOutData().printil("javax.el.VariableMapper _el_variablemapper = jspContext.getELContext().getVariableMapper();");
					variableMapperVar = true;
				}

				getOutData().printin("javax.el.ValueExpression _el_ve");
				getOutData().print(i);
				getOutData().print(" = _el_variablemapper.setVariable(");
				getOutData().print(quote(attrName));
				getOutData().print(',');
				if (attrInfos[i].isDeferredMethod()) {
					getOutData().print(VAR_EXPRESSIONFACTORY);
					getOutData().print(".createValueExpression(");
					getOutData().print(toGetterMethod(attrName));
					getOutData().print(",javax.el.MethodExpression.class)");
				} else {
					getOutData().print(toGetterMethod(attrName));
				}
				getOutData().println(");");
			} else {
				getOutData().printil("if( " + toGetterMethod(attrName) + " != null ) ");
				getOutData().pushIndent();
				getOutData().printin("_jspx_page_context.setAttribute(");
				getOutData().print(quote(attrName));
				getOutData().print(", ");
				getOutData().print(toGetterMethod(attrName));
				getOutData().println(");");
				getOutData().popIndent();
			}
		}

		// Expose the Map containing dynamic attributes as a page-scoped var
		if (tagInfo.hasDynamicAttributes()) {
			getOutData().printin("_jspx_page_context.setAttribute(\"");
			getOutData().print(tagInfo.getDynamicAttributesMapName());
			getOutData().print("\", _jspx_dynamic_attrs);");
		}
	}

	/*
	 * Generates the getter method for the given attribute name.
	 */
	public String toGetterMethod(String attrName) {
		char[] attrChars = attrName.toCharArray();
		attrChars[0] = Character.toUpperCase(attrChars[0]);
		return "get" + new String(attrChars) + "()";
	}

	/*
	 * Generates the setter method name for the given attribute name.
	 */
	private String toSetterMethodName(String attrName) {
		char[] attrChars = attrName.toCharArray();
		attrChars[0] = Character.toUpperCase(attrChars[0]);
		return "set" + new String(attrChars);
	}

	public ELInterpreter getElInterpreter() {
		return getElInterpreterData();
	}

	public boolean isBreakAtLF() {
		return isBreakAtLFData();
	}

	public Set<String> getVarInfoNames() {
		return getVarInfoNamesData();
	}

	public BeanRepository getBeanInfo() {
		return getBeanInfoData();
	}

	public static boolean isStrictGetProperty() {
		return STRICT_GET_PROPERTY;
	}

	public static Class<?>[] getObjectClass() {
		return OBJECT_CLASS;
	}

	public String getJspIdPrefix() {
		return getJspIdPrefixData();
	}

	public void setJspIdPrefix(String jspIdPrefix) {
		this.setJspIdPrefixData(jspIdPrefix);
	}

	public int getJspId() {
		return getJspIdData();
	}

	public void setJspId(int jspId) {
		this.setJspIdData(jspId);
	}

	public GeneratorGenBuffer getCharArrayBuffer() {
		return getCharArrayBufferData();
	}

	public void setCharArrayBuffer(GeneratorGenBuffer charArrayBuffer) {
		this.setCharArrayBufferData(charArrayBuffer);
	}

	public static String getVarExpressionfactory() {
		return VAR_EXPRESSIONFACTORY;
	}

	public static String getVarInstancemanager() {
		return VAR_INSTANCEMANAGER;
	}

	public static boolean isPoolTagsWithExtends() {
		return POOL_TAGS_WITH_EXTENDS;
	}

	public ServletWriter getOut() {
		return getOutData();
	}

	public ArrayList<GeneratorGenBuffer> getMethodsBuffered() {
		return getMethodsBufferedData();
	}

	public GeneratorFragmentHelperClass getFragmentHelperClass() {
		return getFragmentHelperClassData();
	}

	public ErrorDispatcher getErr() {
		return getErrData();
	}

	public JspCompilationContext getCtxt() {
		return getCtxtData();
	}

	public boolean isPoolingEnabled() {
		return isPoolingEnabledData();
	}

	public PageInfo getPageInfo() {
		return getPageInfoData();
	}

	public Vector<String> getTagHandlerPoolNames() {
		return getTagHandlerPoolNamesData();
	}

	public DateFormat getTimestampFormat() {
		return getTimestampFormatData();
	}

	public ServletWriter getOutData() {
		return out;
	}

	public ArrayList<GeneratorGenBuffer> getMethodsBufferedData() {
		return methodsBuffered;
	}

	public GeneratorFragmentHelperClass getFragmentHelperClassData() {
		return fragmentHelperClass;
	}

	public ErrorDispatcher getErrData() {
		return err;
	}

	public BeanRepository getBeanInfoData() {
		return beanInfo;
	}

	public Set<String> getVarInfoNamesData() {
		return varInfoNames;
	}

	public JspCompilationContext getCtxtData() {
		return ctxt;
	}

	public boolean isPoolingEnabledData() {
		return isPoolingEnabled;
	}

	public boolean isBreakAtLFData() {
		return breakAtLF;
	}

	public String getJspIdPrefixData() {
		return jspIdPrefix;
	}

	public void setJspIdPrefixData(String jspIdPrefix) {
		this.jspIdPrefix = jspIdPrefix;
	}

	public int getJspIdData() {
		return jspId;
	}

	public void setJspIdData(int jspId) {
		this.jspId = jspId;
	}

	public PageInfo getPageInfoData() {
		return pageInfo;
	}

	public Vector<String> getTagHandlerPoolNamesData() {
		return tagHandlerPoolNames;
	}

	public GeneratorGenBuffer getCharArrayBufferData() {
		return charArrayBuffer;
	}

	public void setCharArrayBufferData(GeneratorGenBuffer charArrayBuffer) {
		this.charArrayBuffer = charArrayBuffer;
	}

	public DateFormat getTimestampFormatData() {
		return timestampFormat;
	}

	public ELInterpreter getElInterpreterData() {
		return elInterpreter;
	}

}
