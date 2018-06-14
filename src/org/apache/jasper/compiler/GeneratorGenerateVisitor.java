package org.apache.jasper.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.Constants28;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.NodeNamedAttribute;
import org.apache.jasper.runtime.JspRuntimeLibrary;
import org.xml.sax.Attributes;

/**
 * A visitor that generates codes for the elements in the page.
 */
public class GeneratorGenerateVisitor extends NodeVisitor {

	/**
	 * 
	 */
	private final Generator generator;

	/*
	 * Hashtable containing introspection information on tag handlers: <key>:
	 * tag prefix <value>: hashtable containing introspection on tag handlers:
	 * <key>: tag short name <value>: introspection info of tag handler for
	 * <prefix:shortName> tag
	 */
	private final Hashtable<String, Hashtable<String, GeneratorTagHandlerInfo>> handlerInfos;

	private final Hashtable<String, Integer> tagVarNumbers;

	private String parent;

	private boolean isSimpleTagParent; // Is parent a SimpleTag?

	private String pushBodyCountVar;

	private String simpleTagHandlerVar;

	private boolean isSimpleTagHandler;

	private boolean isFragment;

	private final boolean isTagFile;

	private ServletWriter out;

	private final ArrayList<GeneratorGenBuffer> methodsBuffered;

	private final GeneratorFragmentHelperClass fragmentHelperClass;

	private int methodNesting;

	private int charArrayCount;

	private HashMap<String, String> textMap;

	/**
	 * Constructor.
	 */
	public GeneratorGenerateVisitor(Generator generator, boolean isTagFile,
			ServletWriter out, ArrayList<GeneratorGenBuffer> methodsBuffered,
			GeneratorFragmentHelperClass fragmentHelperClass) {

		this.generator = generator;
		this.isTagFile = isTagFile;
		this.setOut(out);
		this.methodsBuffered = methodsBuffered;
		this.fragmentHelperClass = fragmentHelperClass;
		methodNesting = 0;
		handlerInfos = new Hashtable<String, Hashtable<String, GeneratorTagHandlerInfo>>();
		tagVarNumbers = new Hashtable<String, Integer>();
		textMap = new HashMap<String, String>();
	}

	/**
	 * Returns an attribute value, optionally URL encoded. If the value is a
	 * runtime expression, the result is the expression itself, as a string. If
	 * the result is an EL expression, we insert a call to the interpreter. If
	 * the result is a Named Attribute we insert the generated variable name.
	 * Otherwise the result is a string literal, quoted and escaped.
	 *
	 * @param attr
	 *            An JspAttribute object
	 * @param encode
	 *            true if to be URL encoded
	 * @param expectedType
	 *            the expected type for an EL evaluation (ignored for attributes
	 *            that aren't EL expressions)
	 */
	public String attributeValue(NodeJspAttribute attr, boolean encode,
			Class<?> expectedType, boolean isXml) {
		String v = attr.getValue();
		if (!attr.isNodeNamedAttribute() && (v == null))
			return "";

		if (attr.isExpression()) {
			if (encode) {
				return "org.apache.jasper.runtime.JspRuntimeLibrary.URLEncode(String.valueOf("
						+ v + "), request.getCharacterEncoding())";
			}
			return v;
		} else if (attr.isELInterpreterInput()) {
			v = this.generator.getElInterpreter().interpreterCall(
					this.generator.getCtxt(), this.isTagFile, v, expectedType, attr
							.getEL().getMapName(), isXml);
			if (encode) {
				return "org.apache.jasper.runtime.JspRuntimeLibrary.URLEncode("
						+ v + ", request.getCharacterEncoding())";
			}
			return v;
		} else if (attr.isNodeNamedAttribute()) {
			return attr.getNodeNamedAttributeNode().getTemporaryVariableName();
		} else {
			if (encode) {
				return "org.apache.jasper.runtime.JspRuntimeLibrary.URLEncode("
						+ Generator.quote(v)
						+ ", request.getCharacterEncoding())";
			}
			return Generator.quote(v);
		}
	}

	/**
	 * Prints the attribute value specified in the param action, in the form of
	 * name=value string.
	 *
	 * @param n
	 *            the parent node for the param action nodes.
	 */
	private void printParams(Node n, String pageParam, boolean literal)
			throws JasperException {

		String sep;
		if (literal) {
			sep = pageParam.indexOf('?') > 0 ? "\"&\"" : "\"?\"";
		} else {
			sep = "((" + pageParam + ").indexOf('?')>0? '&': '?')";
		}
		if (n.getBody() != null) {
			n.getBody().visit(new ParamVisitor(sep, this));
		}
	}

	@Override
	public void visit(NodeExpression n) throws JasperException {
		n.setBeginJavaLine(getOut().getJavaLine());
		getOut().printin("out.print(");
		getOut().printMultiLn(n.getText());
		getOut().println(");");
		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeScriptlet n) throws JasperException {
		n.setBeginJavaLine(getOut().getJavaLine());
		getOut().printMultiLn(n.getText());
		getOut().println();
		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeELExpression n) throws JasperException {
		n.setBeginJavaLine(getOut().getJavaLine());
		if (!this.generator.getPageInfo().isELIgnored() && (n.getEL() != null)) {
			getOut().printil("out.write("
					+ this.generator.getElInterpreter().interpreterCall(
							this.generator.getCtxt(), this.isTagFile, n.getType()
									+ "{" + n.getText() + "}", String.class, n
									.getEL().getMapName(), false) + ");");
		} else {
			getOut().printil("out.write("
					+ Generator.quote(n.getType() + "{" + n.getText() + "}")
					+ ");");
		}
		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeIncludeAction n) throws JasperException {

		String flush = n.getTextAttribute("flush");
		NodeJspAttribute page = n.getPage();

		boolean isFlush = false; // default to false;
		if ("true".equals(flush))
			isFlush = true;

		n.setBeginJavaLine(getOut().getJavaLine());

		String pageParam;
		if (page.isNodeNamedAttribute()) {
			// If the page for jsp:include was specified via
			// jsp:attribute, first generate code to evaluate
			// that body.
			pageParam = generateNamedAttributeValue(page
					.getNodeNamedAttributeNode());
		} else {
			pageParam = attributeValue(page, false, String.class, n.getRoot()
					.isXmlSyntax());
		}

		// If any of the params have their values specified by
		// jsp:attribute, prepare those values first.
		Node jspBody = findJspBody(n);
		if (jspBody != null) {
			prepareParams(jspBody);
		} else {
			prepareParams(n);
		}

		getOut().printin("org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "
				+ pageParam);
		printParams(n, pageParam, page.isLiteral());
		getOut().println(", out, " + isFlush + ");");

		n.setEndJavaLine(getOut().getJavaLine());
	}

	/**
	 * Scans through all child nodes of the given parent for &lt;param&gt;
	 * subelements. For each &lt;param&gt; element, if its value is specified
	 * via a Named Attribute (&lt;jsp:attribute&gt;), generate the code to
	 * evaluate those bodies first.
	 * <p>
	 * If parent is null, simply returns.
	 */
	private void prepareParams(Node parent) throws JasperException {
		if (parent == null)
			return;

		NodeNodes subelements = parent.getBody();
		if (subelements != null) {
			for (int i = 0; i < subelements.size(); i++) {
				Node n = subelements.getNode(i);
				if (n instanceof NodeParamAction) {
					NodeNodes paramSubElements = n.getBody();
					for (int j = 0; (paramSubElements != null)
							&& (j < paramSubElements.size()); j++) {
						Node m = paramSubElements.getNode(j);
						if (m instanceof NodeNamedAttribute) {
							generateNamedAttributeValue((NodeNamedAttribute) m);
						}
					}
				}
			}
		}
	}

	/**
	 * Finds the &lt;jsp:body&gt; subelement of the given parent node. If not
	 * found, null is returned.
	 */
	private NodeJspBody findJspBody(Node parent) {
		NodeJspBody result = null;

		NodeNodes subelements = parent.getBody();
		for (int i = 0; (subelements != null) && (i < subelements.size()); i++) {
			Node n = subelements.getNode(i);
			if (n instanceof NodeJspBody) {
				result = (NodeJspBody) n;
				break;
			}
		}

		return result;
	}

	@Override
	public void visit(NodeForwardAction n) throws JasperException {
		NodeJspAttribute page = n.getPage();

		n.setBeginJavaLine(getOut().getJavaLine());

		getOut().printil("if (true) {"); // So that javac won't complain about
		getOut().pushIndent(); // codes after "return"

		String pageParam;
		if (page.isNodeNamedAttribute()) {
			// If the page for jsp:forward was specified via
			// jsp:attribute, first generate code to evaluate
			// that body.
			pageParam = generateNamedAttributeValue(page
					.getNodeNamedAttributeNode());
		} else {
			pageParam = attributeValue(page, false, String.class, n.getRoot()
					.isXmlSyntax());
		}

		// If any of the params have their values specified by
		// jsp:attribute, prepare those values first.
		Node jspBody = findJspBody(n);
		if (jspBody != null) {
			prepareParams(jspBody);
		} else {
			prepareParams(n);
		}

		getOut().printin("_jspx_page_context.forward(");
		getOut().print(pageParam);
		printParams(n, pageParam, page.isLiteral());
		getOut().println(");");
		if (isTagFile || isFragment) {
			getOut().printil("throw new javax.servlet.jsp.SkipPageException();");
		} else {
			getOut().printil((methodNesting > 0) ? "return true;" : "return;");
		}
		getOut().popIndent();
		getOut().printil("}");

		n.setEndJavaLine(getOut().getJavaLine());
		// XXX Not sure if we can eliminate dead codes after this.
	}

	@Override
	public void visit(NodeGetProperty n) throws JasperException {
		String name = n.getTextAttribute("name");
		String property = n.getTextAttribute("property");

		n.setBeginJavaLine(getOut().getJavaLine());

		if (this.generator.getBeanInfo().checkVariable(name)) {
			// Bean is defined using useBean, introspect at compile time
			Class<?> bean = this.generator.getBeanInfo().getBeanType(name);
			String beanName = bean.getCanonicalName();
			java.lang.reflect.Method meth = JspRuntimeLibrary.getReadMethod(
					bean, property);
			String methodName = meth.getName();
			getOut().printil("out.write(org.apache.jasper.runtime.JspRuntimeLibrary.toString("
					+ "((("
					+ beanName
					+ ")_jspx_page_context.findAttribute("
					+ "\"" + name + "\"))." + methodName + "())));");
		} else if (!Generator.isStrictGetProperty()
				|| this.generator.getVarInfoNames().contains(name)) {
			// The object is a custom action with an associated
			// VariableInfo entry for this name.
			// Get the class name and then introspect at runtime.
			getOut().printil("out.write(org.apache.jasper.runtime.JspRuntimeLibrary.toString"
					+ "(org.apache.jasper.runtime.JspRuntimeLibrary.handleGetProperty"
					+ "(_jspx_page_context.findAttribute(\""
					+ name
					+ "\"), \""
					+ property + "\")));");
		} else {
			StringBuilder msg = new StringBuilder();
			msg.append("file:");
			msg.append(n.getStart());
			msg.append(" jsp:getProperty for bean with name '");
			msg.append(name);
			msg.append("'. Name was not previously introduced as per JSP.5.3");

			throw new JasperException(msg.toString());
		}

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeSetProperty n) throws JasperException {
		String name = n.getTextAttribute("name");
		String property = n.getTextAttribute("property");
		String param = n.getTextAttribute("param");
		NodeJspAttribute value = n.getValue();

		n.setBeginJavaLine(getOut().getJavaLine());

		if ("*".equals(property)) {
			getOut().printil("org.apache.jasper.runtime.JspRuntimeLibrary.introspect("
					+ "_jspx_page_context.findAttribute("
					+ "\""
					+ name
					+ "\"), request);");
		} else if (value == null) {
			if (param == null)
				param = property; // default to same as property
			getOut().printil("org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper("
					+ "_jspx_page_context.findAttribute(\""
					+ name
					+ "\"), \""
					+ property
					+ "\", request.getParameter(\""
					+ param
					+ "\"), " + "request, \"" + param + "\", false);");
		} else if (value.isExpression()) {
			getOut().printil("org.apache.jasper.runtime.JspRuntimeLibrary.handleSetProperty("
					+ "_jspx_page_context.findAttribute(\""
					+ name
					+ "\"), \""
					+ property + "\",");
			getOut().print(attributeValue(value, false, null, n.getRoot()
					.isXmlSyntax()));
			getOut().println(");");
		} else if (value.isELInterpreterInput()) {
			// We've got to resolve the very call to the interpreter
			// at runtime since we don't know what type to expect
			// in the general case; we thus can't hard-wire the call
			// into the generated code. (XXX We could, however,
			// optimize the case where the bean is exposed with
			// <jsp:useBean>, much as the code here does for
			// getProperty.)

			// The following holds true for the arguments passed to
			// JspRuntimeLibrary.handleSetPropertyExpression():
			// - 'pageContext' is a VariableResolver.
			// - 'this' (either the generated Servlet or the generated tag
			// handler for Tag files) is a FunctionMapper.
			getOut().printil("org.apache.jasper.runtime.JspRuntimeLibrary.handleSetPropertyExpression("
					+ "_jspx_page_context.findAttribute(\""
					+ name
					+ "\"), \""
					+ property
					+ "\", "
					+ Generator.quote(value.getValue())
					+ ", "
					+ "_jspx_page_context, "
					+ value.getEL().getMapName() + ");");
		} else if (value.isNodeNamedAttribute()) {
			// If the value for setProperty was specified via
			// jsp:attribute, first generate code to evaluate
			// that body.
			String valueVarName = generateNamedAttributeValue(value
					.getNodeNamedAttributeNode());
			getOut().printil("org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper("
					+ "_jspx_page_context.findAttribute(\""
					+ name
					+ "\"), \""
					+ property
					+ "\", "
					+ valueVarName
					+ ", null, null, false);");
		} else {
			getOut().printin("org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper("
					+ "_jspx_page_context.findAttribute(\""
					+ name
					+ "\"), \""
					+ property + "\", ");
			getOut().print(attributeValue(value, false, null, n.getRoot()
					.isXmlSyntax()));
			getOut().println(", null, null, false);");
		}

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeUseBean n) throws JasperException {

		String name = n.getTextAttribute("id");
		String scope = n.getTextAttribute("scope");
		String klass = n.getTextAttribute("class");
		String type = n.getTextAttribute("type");
		NodeJspAttribute beanName = n.getBeanName();

		// If "class" is specified, try an instantiation at compile time
		boolean generateNew = false;
		String canonicalName = null; // Canonical name for klass
		if (klass != null) {
			try {
				Class<?> bean = this.generator.getCtxt().getClassLoader().loadClass(
						klass);
				if (klass.indexOf('$') >= 0) {
					// Obtain the canonical type name
					canonicalName = bean.getCanonicalName();
				} else {
					canonicalName = klass;
				}
				int modifiers = bean.getModifiers();
				if (!Modifier.isPublic(modifiers)
						|| Modifier.isInterface(modifiers)
						|| Modifier.isAbstract(modifiers)) {
					throw new Exception("Invalid bean class modifier");
				}
				// Check that there is a 0 arg constructor
				bean.getConstructor(new Class[] {});
				// At compile time, we have determined that the bean class
				// exists, with a public zero constructor, new() can be
				// used for bean instantiation.
				generateNew = true;
			} catch (Exception e) {
				// Cannot instantiate the specified class, either a
				// compilation error or a runtime error will be raised,
				// depending on a compiler flag.
				if (this.generator.getCtxt().getOptions()
						.getErrorOnUseBeanInvalidClassAttribute()) {
					this.generator.getErr().jspError(n, "jsp.error.invalid.bean",
							klass);
				}
				if (canonicalName == null) {
					// Doing our best here to get a canonical name
					// from the binary name, should work 99.99% of time.
					canonicalName = klass.replace('$', '.');
				}
			}
			if (type == null) {
				// if type is unspecified, use "class" as type of bean
				type = canonicalName;
			}
		}

		// JSP.5.1, Sematics, para 1 - lock not required for request or
		// page scope
		String scopename = "javax.servlet.jsp.PageContext.PAGE_SCOPE"; // Default
																		// to
																		// page
		String lock = null;

		if ("request".equals(scope)) {
			scopename = "javax.servlet.jsp.PageContext.REQUEST_SCOPE";
		} else if ("session".equals(scope)) {
			scopename = "javax.servlet.jsp.PageContext.SESSION_SCOPE";
			lock = "session";
		} else if ("application".equals(scope)) {
			scopename = "javax.servlet.jsp.PageContext.APPLICATION_SCOPE";
			lock = "application";
		}

		n.setBeginJavaLine(getOut().getJavaLine());

		// Declare bean
		getOut().printin(type);
		getOut().print(' ');
		getOut().print(name);
		getOut().println(" = null;");

		// Lock (if required) while getting or creating bean
		if (lock != null) {
			getOut().printin("synchronized (");
			getOut().print(lock);
			getOut().println(") {");
			getOut().pushIndent();
		}

		// Locate bean from context
		getOut().printin(name);
		getOut().print(" = (");
		getOut().print(type);
		getOut().print(") _jspx_page_context.getAttribute(");
		getOut().print(Generator.quote(name));
		getOut().print(", ");
		getOut().print(scopename);
		getOut().println(");");

		// Create bean
		/*
		 * Check if bean is already there
		 */
		getOut().printin("if (");
		getOut().print(name);
		getOut().println(" == null){");
		getOut().pushIndent();
		if (klass == null && beanName == null) {
			/*
			 * If both class name and beanName is not specified, the bean must
			 * be found locally, otherwise it's an error
			 */
			getOut().printin("throw new java.lang.InstantiationException(\"bean ");
			getOut().print(name);
			getOut().println(" not found within scope\");");
		} else {
			/*
			 * Instantiate the bean if it is not in the specified scope.
			 */
			if (!generateNew) {
				String binaryName;
				if (beanName != null) {
					if (beanName.isNodeNamedAttribute()) {
						// If the value for beanName was specified via
						// jsp:attribute, first generate code to evaluate
						// that body.
						binaryName = generateNamedAttributeValue(beanName
								.getNodeNamedAttributeNode());
					} else {
						binaryName = attributeValue(beanName, false,
								String.class, n.getRoot().isXmlSyntax());
					}
				} else {
					// Implies klass is not null
					binaryName = Generator.quote(klass);
				}
				getOut().printil("try {");
				getOut().pushIndent();
				getOut().printin(name);
				getOut().print(" = (");
				getOut().print(type);
				getOut().print(") java.beans.Beans.instantiate(");
				getOut().print("this.getClass().getClassLoader(), ");
				getOut().print(binaryName);
				getOut().println(");");
				getOut().popIndent();
				/*
				 * Note: Beans.instantiate throws ClassNotFoundException if the
				 * bean class is abstract.
				 */
				getOut().printil("} catch (java.lang.ClassNotFoundException exc) {");
				getOut().pushIndent();
				getOut().printil("throw new InstantiationException(exc.getMessage());");
				getOut().popIndent();
				getOut().printil("} catch (java.lang.Exception exc) {");
				getOut().pushIndent();
				getOut().printin("throw new javax.servlet.ServletException(");
				getOut().print("\"Cannot create bean of class \" + ");
				getOut().print(binaryName);
				getOut().println(", exc);");
				getOut().popIndent();
				getOut().printil("}"); // close of try
			} else {
				// Implies klass is not null
				// Generate codes to instantiate the bean class
				getOut().printin(name);
				getOut().print(" = new ");
				getOut().print(canonicalName);
				getOut().println("();");
			}
			/*
			 * Set attribute for bean in the specified scope
			 */
			getOut().printin("_jspx_page_context.setAttribute(");
			getOut().print(Generator.quote(name));
			getOut().print(", ");
			getOut().print(name);
			getOut().print(", ");
			getOut().print(scopename);
			getOut().println(");");

			// Only visit the body when bean is instantiated
			visitBody(n);
		}
		getOut().popIndent();
		getOut().printil("}");

		// End of lock block
		if (lock != null) {
			getOut().popIndent();
			getOut().printil("}");
		}

		n.setEndJavaLine(getOut().getJavaLine());
	}

	/**
	 * @return a string for the form 'attr = "value"'
	 */
	private String makeAttr(String attr, String value) {
		if (value == null)
			return "";

		return " " + attr + "=\"" + value + '\"';
	}

	@Override
	public void visit(NodePlugIn n) throws JasperException {
		String type = n.getTextAttribute("type");
		String code = n.getTextAttribute("code");
		String name = n.getTextAttribute("name");
		NodeJspAttribute height = n.getHeight();
		NodeJspAttribute width = n.getWidth();
		String hspace = n.getTextAttribute("hspace");
		String vspace = n.getTextAttribute("vspace");
		String align = n.getTextAttribute("align");
		String iepluginurl = n.getTextAttribute("iepluginurl");
		String nspluginurl = n.getTextAttribute("nspluginurl");
		String codebase = n.getTextAttribute("codebase");
		String archive = n.getTextAttribute("archive");
		String jreversion = n.getTextAttribute("jreversion");

		String widthStr = null;
		if (width != null) {
			if (width.isNodeNamedAttribute()) {
				widthStr = generateNamedAttributeValue(width
						.getNodeNamedAttributeNode());
			} else {
				widthStr = attributeValue(width, false, String.class, n
						.getRoot().isXmlSyntax());
			}
		}

		String heightStr = null;
		if (height != null) {
			if (height.isNodeNamedAttribute()) {
				heightStr = generateNamedAttributeValue(height
						.getNodeNamedAttributeNode());
			} else {
				heightStr = attributeValue(height, false, String.class, n
						.getRoot().isXmlSyntax());
			}
		}

		if (iepluginurl == null)
			iepluginurl = Constants28.getIePluginUrl();
		if (nspluginurl == null)
			nspluginurl = Constants28.getNsPluginUrl();

		n.setBeginJavaLine(getOut().getJavaLine());

		// If any of the params have their values specified by
		// jsp:attribute, prepare those values first.
		// Look for a params node and prepare its param subelements:
		NodeJspBody jspBody = findJspBody(n);
		if (jspBody != null) {
			NodeNodes subelements = jspBody.getBody();
			if (subelements != null) {
				for (int i = 0; i < subelements.size(); i++) {
					Node m = subelements.getNode(i);
					if (m instanceof NodeParamsAction) {
						prepareParams(m);
						break;
					}
				}
			}
		}

		// XXX - Fixed a bug here - width and height can be set
		// dynamically. Double-check if this generation is correct.

		// IE style plugin
		// <object ...>
		// First compose the runtime output string
		String s0 = "<object"
				+ makeAttr("classid", this.generator.getCtxt().getOptions()
						.getIeClassId()) + makeAttr("name", name);

		String s1 = "";
		if (width != null) {
			s1 = " + \" width=\\\"\" + " + widthStr + " + \"\\\"\"";
		}

		String s2 = "";
		if (height != null) {
			s2 = " + \" height=\\\"\" + " + heightStr + " + \"\\\"\"";
		}

		String s3 = makeAttr("hspace", hspace) + makeAttr("vspace", vspace)
				+ makeAttr("align", align) + makeAttr("codebase", iepluginurl)
				+ '>';

		// Then print the output string to the java file
		getOut().printil("out.write(" + Generator.quote(s0) + s1 + s2 + " + "
				+ Generator.quote(s3) + ");");
		getOut().printil("out.write(\"\\n\");");

		// <param > for java_code
		s0 = "<param name=\"java_code\"" + makeAttr("value", code) + '>';
		getOut().printil("out.write(" + Generator.quote(s0) + ");");
		getOut().printil("out.write(\"\\n\");");

		// <param > for java_codebase
		if (codebase != null) {
			s0 = "<param name=\"java_codebase\"" + makeAttr("value", codebase)
					+ '>';
			getOut().printil("out.write(" + Generator.quote(s0) + ");");
			getOut().printil("out.write(\"\\n\");");
		}

		// <param > for java_archive
		if (archive != null) {
			s0 = "<param name=\"java_archive\"" + makeAttr("value", archive)
					+ '>';
			getOut().printil("out.write(" + Generator.quote(s0) + ");");
			getOut().printil("out.write(\"\\n\");");
		}

		// <param > for type
		s0 = "<param name=\"type\""
				+ makeAttr("value",
						"application/x-java-"
								+ type
								+ ((jreversion == null) ? "" : ";version="
										+ jreversion)) + '>';
		getOut().printil("out.write(" + Generator.quote(s0) + ");");
		getOut().printil("out.write(\"\\n\");");

		/*
		 * generate a <param> for each <jsp:param> in the plugin body
		 */
		if (n.getBody() != null)
			n.getBody().visit(new ParamVisitor1(true, this));

		/*
		 * Netscape style plugin part
		 */
		getOut().printil("out.write(" + Generator.quote("<comment>") + ");");
		getOut().printil("out.write(\"\\n\");");
		s0 = "<EMBED"
				+ makeAttr("type",
						"application/x-java-"
								+ type
								+ ((jreversion == null) ? "" : ";version="
										+ jreversion)) + makeAttr("name", name);

		// s1 and s2 are the same as before.

		s3 = makeAttr("hspace", hspace) + makeAttr("vspace", vspace)
				+ makeAttr("align", align)
				+ makeAttr("pluginspage", nspluginurl)
				+ makeAttr("java_code", code)
				+ makeAttr("java_codebase", codebase)
				+ makeAttr("java_archive", archive);
		getOut().printil("out.write(" + Generator.quote(s0) + s1 + s2 + " + "
				+ Generator.quote(s3) + ");");

		/*
		 * Generate a 'attr = "value"' for each <jsp:param> in plugin body
		 */
		if (n.getBody() != null)
			n.getBody().visit(new ParamVisitor1(false, this));

		getOut().printil("out.write(" + Generator.quote("/>") + ");");
		getOut().printil("out.write(\"\\n\");");

		getOut().printil("out.write(" + Generator.quote("<noembed>") + ");");
		getOut().printil("out.write(\"\\n\");");

		/*
		 * Fallback
		 */
		if (n.getBody() != null) {
			visitBody(n);
			getOut().printil("out.write(\"\\n\");");
		}

		getOut().printil("out.write(" + Generator.quote("</noembed>") + ");");
		getOut().printil("out.write(\"\\n\");");

		getOut().printil("out.write(" + Generator.quote("</comment>") + ");");
		getOut().printil("out.write(\"\\n\");");

		getOut().printil("out.write(" + Generator.quote("</object>") + ");");
		getOut().printil("out.write(\"\\n\");");

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeNamedAttribute n) throws JasperException {
		// Don't visit body of this tag - we already did earlier.
	}

	@Override
	public void visit(NodeCustomTag n) throws JasperException {

		// Use plugin to generate more efficient code if there is one.
		if (n.useTagPlugin()) {
			generateTagPlugin(n);
			return;
		}

		GeneratorTagHandlerInfo handlerInfo = getTagHandlerInfo(n);

		// Create variable names
		String baseVar = createTagVarName(n.getQName(), n.getPrefix(),
				n.getLocalName());
		String tagEvalVar = "_jspx_eval_" + baseVar;
		String tagHandlerVar = "_jspx_th_" + baseVar;
		String tagPushBodyCountVar = "_jspx_push_body_count_" + baseVar;

		// If the tag contains no scripting element, generate its codes
		// to a method.
		ServletWriter outSave = null;
		NodeChildInfo ci = n.getNodeChildInfo();
		if (ci.isScriptless() && !ci.hasScriptingVars()) {
			// The tag handler and its body code can reside in a separate
			// method if it is scriptless and does not have any scripting
			// variable defined.

			String tagMethod = "_jspx_meth_" + baseVar;

			// Generate a call to this method
			getOut().printin("if (");
			getOut().print(tagMethod);
			getOut().print("(");
			if (parent != null) {
				getOut().print(parent);
				getOut().print(", ");
			}
			getOut().print("_jspx_page_context");
			if (pushBodyCountVar != null) {
				getOut().print(", ");
				getOut().print(pushBodyCountVar);
			}
			getOut().println("))");
			getOut().pushIndent();
			getOut().printil((methodNesting > 0) ? "return true;" : "return;");
			getOut().popIndent();

			// Set up new buffer for the method
			outSave = getOut();
			/*
			 * For fragments, their bodies will be generated in fragment helper
			 * classes, and the Java line adjustments will be done there, hence
			 * they are set to null here to avoid double adjustments.
			 */
			GeneratorGenBuffer genBuffer = new GeneratorGenBuffer(n,
					n.implementsSimpleTag() ? null : n.getBody());
			methodsBuffered.add(genBuffer);
			setOut(genBuffer.getOut());

			methodNesting++;
			// Generate code for method declaration
			getOut().println();
			getOut().pushIndent();
			getOut().printin("private boolean ");
			getOut().print(tagMethod);
			getOut().print("(");
			if (parent != null) {
				getOut().print("javax.servlet.jsp.tagext.JspTag ");
				getOut().print(parent);
				getOut().print(", ");
			}
			getOut().print("javax.servlet.jsp.PageContext _jspx_page_context");
			if (pushBodyCountVar != null) {
				getOut().print(", int[] ");
				getOut().print(pushBodyCountVar);
			}
			getOut().println(")");
			getOut().printil("        throws java.lang.Throwable {");
			getOut().pushIndent();

			// Initialize local variables used in this method.
			if (!isTagFile) {
				getOut().printil("javax.servlet.jsp.PageContext pageContext = _jspx_page_context;");
			}
			getOut().printil("javax.servlet.jsp.JspWriter out = _jspx_page_context.getOut();");
			Generator.generateLocalVariables(getOut(), n);
		}

		// Add the named objects to the list of 'introduced' names to enable
		// a later test as per JSP.5.3
		VariableInfo[] infos = n.getVariableInfos();
		if (infos != null && infos.length > 0) {
			for (int i = 0; i < infos.length; i++) {
				VariableInfo info = infos[i];
				if (info != null && info.getVarName() != null)
					this.generator.getPageInfo().getVarInfoNames().add(
							info.getVarName());
			}
		}
		TagVariableInfo[] tagInfos = n.getTagVariableInfos();
		if (tagInfos != null && tagInfos.length > 0) {
			for (int i = 0; i < tagInfos.length; i++) {
				TagVariableInfo tagInfo = tagInfos[i];
				if (tagInfo != null) {
					String name = tagInfo.getNameGiven();
					if (name == null) {
						String nameFromAttribute = tagInfo
								.getNameFromAttribute();
						name = n.getAttributeValue(nameFromAttribute);
					}
					this.generator.getPageInfo().getVarInfoNames().add(name);
				}
			}
		}

		if (n.implementsSimpleTag()) {
			generateCustomDoTag(n, handlerInfo, tagHandlerVar);
		} else {
			/*
			 * Classic tag handler: Generate code for start element, body, and
			 * end element
			 */
			generateCustomStart(n, handlerInfo, tagHandlerVar, tagEvalVar,
					tagPushBodyCountVar);

			// visit body
			String tmpParent = parent;
			parent = tagHandlerVar;
			boolean isSimpleTagParentSave = isSimpleTagParent;
			isSimpleTagParent = false;
			String tmpPushBodyCountVar = null;
			if (n.implementsTryCatchFinally()) {
				tmpPushBodyCountVar = pushBodyCountVar;
				pushBodyCountVar = tagPushBodyCountVar;
			}
			boolean tmpIsSimpleTagHandler = isSimpleTagHandler;
			isSimpleTagHandler = false;

			visitBody(n);

			parent = tmpParent;
			isSimpleTagParent = isSimpleTagParentSave;
			if (n.implementsTryCatchFinally()) {
				pushBodyCountVar = tmpPushBodyCountVar;
			}
			isSimpleTagHandler = tmpIsSimpleTagHandler;

			generateCustomEnd(n, tagHandlerVar, tagEvalVar, tagPushBodyCountVar);
		}

		if (ci.isScriptless() && !ci.hasScriptingVars()) {
			// Generate end of method
			if (methodNesting > 0) {
				getOut().printil("return false;");
			}
			getOut().popIndent();
			getOut().printil("}");
			getOut().popIndent();

			methodNesting--;

			// restore previous writer
			setOut(outSave);
		}

	}

	private static final String DOUBLE_QUOTE = "\\\"";

	@Override
	public void visit(NodeUninterpretedTag n) throws JasperException {

		n.setBeginJavaLine(getOut().getJavaLine());

		/*
		 * Write begin tag
		 */
		getOut().printin("out.write(\"<");
		getOut().print(n.getQName());

		Attributes attrs = n.getNonTaglibXmlnsAttributes();
		if (attrs != null) {
			for (int i = 0; i < attrs.getLength(); i++) {
				getOut().print(" ");
				getOut().print(attrs.getQName(i));
				getOut().print("=");
				getOut().print(DOUBLE_QUOTE);
				getOut().print(Generator.escape(attrs.getValue(i).replace("\"",
						"&quot;")));
				getOut().print(DOUBLE_QUOTE);
			}
		}

		attrs = n.getAttributes();
		if (attrs != null) {
			NodeJspAttribute[] jspAttrs = n.getJspAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				getOut().print(" ");
				getOut().print(attrs.getQName(i));
				getOut().print("=");
				if (jspAttrs[i].isELInterpreterInput()) {
					getOut().print("\\\"\" + ");
					String debug = attributeValue(jspAttrs[i], false,
							String.class, n.getRoot().isXmlSyntax());
					getOut().print(debug);
					getOut().print(" + \"\\\"");
				} else {
					getOut().print(DOUBLE_QUOTE);
					getOut().print(Generator.escape(jspAttrs[i].getValue().replace(
							"\"", "&quot;")));
					getOut().print(DOUBLE_QUOTE);
				}
			}
		}

		if (n.getBody() != null) {
			getOut().println(">\");");

			// Visit tag body
			visitBody(n);

			/*
			 * Write end tag
			 */
			getOut().printin("out.write(\"</");
			getOut().print(n.getQName());
			getOut().println(">\");");
		} else {
			getOut().println("/>\");");
		}

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeJspElement n) throws JasperException {

		n.setBeginJavaLine(getOut().getJavaLine());

		// Compute attribute value string for XML-style and named
		// attributes
		Hashtable<String, String> map = new Hashtable<String, String>();
		NodeJspAttribute[] attrs = n.getJspAttributes();
		for (int i = 0; attrs != null && i < attrs.length; i++) {
			String value = null;
			String nvp = null;
			if (attrs[i].isNodeNamedAttribute()) {
				NodeNamedAttribute attr = attrs[i].getNodeNamedAttributeNode();
				NodeJspAttribute omitAttr = attr.getOmit();
				String omit;
				if (omitAttr == null) {
					omit = "false";
				} else {
					omit = attributeValue(omitAttr, false, boolean.class, n
							.getRoot().isXmlSyntax());
					if ("true".equals(omit)) {
						continue;
					}
				}
				value = generateNamedAttributeValue(attrs[i]
						.getNodeNamedAttributeNode());
				if ("false".equals(omit)) {
					nvp = " + \" " + attrs[i].getName() + "=\\\"\" + " + value
							+ " + \"\\\"\"";
				} else {
					nvp = " + (java.lang.Boolean.valueOf(" + omit
							+ ")?\"\":\" " + attrs[i].getName() + "=\\\"\" + "
							+ value + " + \"\\\"\")";
				}
			} else {
				value = attributeValue(attrs[i], false, Object.class, n
						.getRoot().isXmlSyntax());
				nvp = " + \" " + attrs[i].getName() + "=\\\"\" + " + value
						+ " + \"\\\"\"";
			}
			map.put(attrs[i].getName(), nvp);
		}

		// Write begin tag, using XML-style 'name' attribute as the
		// element name
		String elemName = attributeValue(n.getNameAttribute(), false,
				String.class, n.getRoot().isXmlSyntax());
		getOut().printin("out.write(\"<\"");
		getOut().print(" + " + elemName);

		// Write remaining attributes
		Enumeration<String> enumeration = map.keys();
		while (enumeration.hasMoreElements()) {
			String attrName = enumeration.nextElement();
			getOut().print(map.get(attrName));
		}

		// Does the <jsp:element> have nested tags other than
		// <jsp:attribute>
		boolean hasBody = false;
		NodeNodes subelements = n.getBody();
		if (subelements != null) {
			for (int i = 0; i < subelements.size(); i++) {
				Node subelem = subelements.getNode(i);
				if (!(subelem instanceof NodeNamedAttribute)) {
					hasBody = true;
					break;
				}
			}
		}
		if (hasBody) {
			getOut().println(" + \">\");");

			// Smap should not include the body
			n.setEndJavaLine(getOut().getJavaLine());

			// Visit tag body
			visitBody(n);

			// Write end tag
			getOut().printin("out.write(\"</\"");
			getOut().print(" + " + elemName);
			getOut().println(" + \">\");");
		} else {
			getOut().println(" + \"/>\");");
			n.setEndJavaLine(getOut().getJavaLine());
		}
	}

	@Override
	public void visit(NodeTemplateText n) throws JasperException {

		String text = n.getText();

		int textSize = text.length();
		if (textSize == 0) {
			return;
		}

		if (textSize <= 3) {
			// Special case small text strings
			n.setBeginJavaLine(getOut().getJavaLine());
			int lineInc = 0;
			for (int i = 0; i < textSize; i++) {
				char ch = text.charAt(i);
				getOut().printil("out.write(" + Generator.quote(ch) + ");");
				if (i > 0) {
					n.addSmap(lineInc);
				}
				if (ch == '\n') {
					lineInc++;
				}
			}
			n.setEndJavaLine(getOut().getJavaLine());
			return;
		}

		if (this.generator.getCtxt().getOptions().genStringAsCharArray()) {
			// Generate Strings as char arrays, for performance
			ServletWriter caOut;
			if (this.generator.getCharArrayBuffer() == null) {
				this.generator.setCharArrayBuffer(new GeneratorGenBuffer());
				caOut = this.generator.getCharArrayBuffer().getOut();
				caOut.pushIndent();
				textMap = new HashMap<String, String>();
			} else {
				caOut = this.generator.getCharArrayBuffer().getOut();
			}
			// UTF-8 is up to 4 bytes per character
			// String constants are limited to 64k bytes
			// Limit string constants here to 16k characters
			int textIndex = 0;
			int textLength = text.length();
			while (textIndex < textLength) {
				int len = 0;
				if (textLength - textIndex > 16384) {
					len = 16384;
				} else {
					len = textLength - textIndex;
				}
				String output = text.substring(textIndex, textIndex + len);
				String charArrayName = textMap.get(output);
				if (charArrayName == null) {
					charArrayName = "_jspx_char_array_" + charArrayCount++;
					textMap.put(output, charArrayName);
					caOut.printin("static char[] ");
					caOut.print(charArrayName);
					caOut.print(" = ");
					caOut.print(Generator.quote(output));
					caOut.println(".toCharArray();");
				}

				n.setBeginJavaLine(getOut().getJavaLine());
				getOut().printil("out.write(" + charArrayName + ");");
				n.setEndJavaLine(getOut().getJavaLine());

				textIndex = textIndex + len;
			}
			return;
		}

		n.setBeginJavaLine(getOut().getJavaLine());

		getOut().printin();
		StringBuilder sb = new StringBuilder("out.write(\"");
		int initLength = sb.length();
		int count = JspUtil.getChunksize();
		int srcLine = 0; // relative to starting source line
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			--count;
			switch (ch) {
			case '"':
				sb.append('\\').append('\"');
				break;
			case '\\':
				sb.append('\\').append('\\');
				break;
			case '\r':
				sb.append('\\').append('r');
				break;
			case '\n':
				sb.append('\\').append('n');
				srcLine++;

				if (this.generator.isBreakAtLF() || count < 0) {
					// Generate an out.write() when see a '\n' in template
					sb.append("\");");
					getOut().println(sb.toString());
					if (i < text.length() - 1) {
						getOut().printin();
					}
					sb.setLength(initLength);
					count = JspUtil.getChunksize();
				}
				// add a Smap for this line
				n.addSmap(srcLine);
				break;
			case '\t': // Not sure we need this
				sb.append('\\').append('t');
				break;
			default:
				sb.append(ch);
			}
		}

		if (sb.length() > initLength) {
			sb.append("\");");
			getOut().println(sb.toString());
		}

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeJspBody n) throws JasperException {
		if (n.getBody() != null) {
			if (isSimpleTagHandler) {
				getOut().printin(simpleTagHandlerVar);
				getOut().print(".setJspBody(");
				generateJspFragment(n, simpleTagHandlerVar);
				getOut().println(");");
			} else {
				visitBody(n);
			}
		}
	}

	@Override
	public void visit(NodeInvokeAction n) throws JasperException {

		n.setBeginJavaLine(getOut().getJavaLine());

		// Copy virtual page scope of tag file to page scope of invoking
		// page
		getOut().printil("((org.apache.jasper.runtime.JspContextWrapper) this.jspContext).syncBeforeInvoke();");
		String varReaderAttr = n.getTextAttribute("varReader");
		String varAttr = n.getTextAttribute("var");
		if (varReaderAttr != null || varAttr != null) {
			getOut().printil("_jspx_sout = new java.io.StringWriter();");
		} else {
			getOut().printil("_jspx_sout = null;");
		}

		// Invoke fragment, unless fragment is null
		getOut().printin("if (");
		getOut().print(this.generator.toGetterMethod(n.getTextAttribute("fragment")));
		getOut().println(" != null) {");
		getOut().pushIndent();
		getOut().printin(this.generator.toGetterMethod(n
				.getTextAttribute("fragment")));
		getOut().println(".invoke(_jspx_sout);");
		getOut().popIndent();
		getOut().printil("}");

		// Store varReader in appropriate scope
		if (varReaderAttr != null || varAttr != null) {
			String scopeName = n.getTextAttribute("scope");
			getOut().printin("_jspx_page_context.setAttribute(");
			if (varReaderAttr != null) {
				getOut().print(Generator.quote(varReaderAttr));
				getOut().print(", new java.io.StringReader(_jspx_sout.toString())");
			} else {
				getOut().print(Generator.quote(varAttr));
				getOut().print(", _jspx_sout.toString()");
			}
			if (scopeName != null) {
				getOut().print(", ");
				getOut().print(getScopeConstant(scopeName));
			}
			getOut().println(");");
		}

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeDoBodyAction n) throws JasperException {

		n.setBeginJavaLine(getOut().getJavaLine());

		// Copy virtual page scope of tag file to page scope of invoking
		// page
		getOut().printil("((org.apache.jasper.runtime.JspContextWrapper) this.jspContext).syncBeforeInvoke();");

		// Invoke body
		String varReaderAttr = n.getTextAttribute("varReader");
		String varAttr = n.getTextAttribute("var");
		if (varReaderAttr != null || varAttr != null) {
			getOut().printil("_jspx_sout = new java.io.StringWriter();");
		} else {
			getOut().printil("_jspx_sout = null;");
		}
		getOut().printil("if (getJspBody() != null)");
		getOut().pushIndent();
		getOut().printil("getJspBody().invoke(_jspx_sout);");
		getOut().popIndent();

		// Store varReader in appropriate scope
		if (varReaderAttr != null || varAttr != null) {
			String scopeName = n.getTextAttribute("scope");
			getOut().printin("_jspx_page_context.setAttribute(");
			if (varReaderAttr != null) {
				getOut().print(Generator.quote(varReaderAttr));
				getOut().print(", new java.io.StringReader(_jspx_sout.toString())");
			} else {
				getOut().print(Generator.quote(varAttr));
				getOut().print(", _jspx_sout.toString()");
			}
			if (scopeName != null) {
				getOut().print(", ");
				getOut().print(getScopeConstant(scopeName));
			}
			getOut().println(");");
		}

		// Restore EL context
		getOut().printil("jspContext.getELContext().putContext(javax.servlet.jsp.JspContext.class,getJspContext());");

		n.setEndJavaLine(getOut().getJavaLine());
	}

	@Override
	public void visit(NodeAttributeGenerator n) throws JasperException {
		NodeCustomTag tag = n.getTag();
		NodeJspAttribute[] attrs = tag.getJspAttributes();
		for (int i = 0; attrs != null && i < attrs.length; i++) {
			if (attrs[i].getName().equals(n.getName())) {
				getOut().print(evaluateAttribute(getTagHandlerInfo(tag), attrs[i],
						tag, null));
				break;
			}
		}
	}

	private GeneratorTagHandlerInfo getTagHandlerInfo(NodeCustomTag n)
			throws JasperException {
		Hashtable<String, GeneratorTagHandlerInfo> handlerInfosByShortName = handlerInfos
				.get(n.getPrefix());
		if (handlerInfosByShortName == null) {
			handlerInfosByShortName = new Hashtable<String, GeneratorTagHandlerInfo>();
			handlerInfos.put(n.getPrefix(), handlerInfosByShortName);
		}
		GeneratorTagHandlerInfo handlerInfo = handlerInfosByShortName.get(n
				.getLocalName());
		if (handlerInfo == null) {
			handlerInfo = new GeneratorTagHandlerInfo(n,
					n.getTagHandlerClass(), this.generator.getErr());
			handlerInfosByShortName.put(n.getLocalName(), handlerInfo);
		}
		return handlerInfo;
	}

	private void generateTagPlugin(NodeCustomTag n) throws JasperException {
		if (n.getAtSTag() != null) {
			n.getAtSTag().visit(this);
		}
		visitBody(n);
		if (n.getAtETag() != null) {
			n.getAtETag().visit(this);
		}
	}

	private void generateCustomStart(NodeCustomTag n,
			GeneratorTagHandlerInfo handlerInfo, String tagHandlerVar,
			String tagEvalVar, String tagPushBodyCountVar)
			throws JasperException {

		Class<?> tagHandlerClass = handlerInfo.getTagHandlerClass();

		getOut().printin("//  ");
		getOut().println(n.getQName());
		n.setBeginJavaLine(getOut().getJavaLine());

		// Declare AT_BEGIN scripting variables
		declareScriptingVars(n, VariableInfo.getAtBegin());
		saveScriptingVars(n, VariableInfo.getAtBegin());

		String tagHandlerClassName = tagHandlerClass.getCanonicalName();
		if (this.generator.isPoolingEnabled() && !(n.implementsJspIdConsumer())) {
			getOut().printin(tagHandlerClassName);
			getOut().print(" ");
			getOut().print(tagHandlerVar);
			getOut().print(" = ");
			getOut().print("(");
			getOut().print(tagHandlerClassName);
			getOut().print(") ");
			getOut().print(n.getTagHandlerPoolName());
			getOut().print(".get(");
			getOut().print(tagHandlerClassName);
			getOut().println(".class);");
		} else {
			writeNewInstance(tagHandlerVar, tagHandlerClassName);
		}

		// includes setting the context
		generateSetters(n, tagHandlerVar, handlerInfo, false);

		// JspIdConsumer (after context has been set)
		if (n.implementsJspIdConsumer()) {
			getOut().printin(tagHandlerVar);
			getOut().print(".setJspId(\"");
			getOut().print(this.generator.createJspId());
			getOut().println("\");");
		}

		if (n.implementsTryCatchFinally()) {
			getOut().printin("int[] ");
			getOut().print(tagPushBodyCountVar);
			getOut().println(" = new int[] { 0 };");
			getOut().printil("try {");
			getOut().pushIndent();
		}
		getOut().printin("int ");
		getOut().print(tagEvalVar);
		getOut().print(" = ");
		getOut().print(tagHandlerVar);
		getOut().println(".doStartTag();");

		if (!n.implementsBodyTag()) {
			// Synchronize AT_BEGIN scripting variables
			syncScriptingVars(n, VariableInfo.getAtBegin());
		}

		if (!n.hasEmptyBody()) {
			getOut().printin("if (");
			getOut().print(tagEvalVar);
			getOut().println(" != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {");
			getOut().pushIndent();

			// Declare NESTED scripting variables
			declareScriptingVars(n, VariableInfo.getNested());
			saveScriptingVars(n, VariableInfo.getNested());

			if (n.implementsBodyTag()) {
				getOut().printin("if (");
				getOut().print(tagEvalVar);
				getOut().println(" != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {");
				// Assume EVAL_BODY_BUFFERED
				getOut().pushIndent();
				getOut().printil("out = _jspx_page_context.pushBody();");
				if (n.implementsTryCatchFinally()) {
					getOut().printin(tagPushBodyCountVar);
					getOut().println("[0]++;");
				} else if (pushBodyCountVar != null) {
					getOut().printin(pushBodyCountVar);
					getOut().println("[0]++;");
				}
				getOut().printin(tagHandlerVar);
				getOut().println(".setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);");
				getOut().printin(tagHandlerVar);
				getOut().println(".doInitBody();");

				getOut().popIndent();
				getOut().printil("}");

				// Synchronize AT_BEGIN and NESTED scripting variables
				syncScriptingVars(n, VariableInfo.getAtBegin());
				syncScriptingVars(n, VariableInfo.getNested());

			} else {
				// Synchronize NESTED scripting variables
				syncScriptingVars(n, VariableInfo.getNested());
			}

			if (n.implementsIterationTag()) {
				getOut().printil("do {");
				getOut().pushIndent();
			}
		}
		// Map the Java lines that handles start of custom tags to the
		// JSP line for this tag
		n.setEndJavaLine(getOut().getJavaLine());
	}

	private void writeNewInstance(String tagHandlerVar,
			String tagHandlerClassName) {
		if (Constants28.isUseInstanceManagerForTags()) {
			getOut().printin(tagHandlerClassName);
			getOut().print(" ");
			getOut().print(tagHandlerVar);
			getOut().print(" = (");
			getOut().print(tagHandlerClassName);
			getOut().print(")");
			getOut().print(Generator.getVarInstancemanager());
			getOut().print(".newInstance(\"");
			getOut().print(tagHandlerClassName);
			getOut().println("\", this.getClass().getClassLoader());");
		} else {
			getOut().printin(tagHandlerClassName);
			getOut().print(" ");
			getOut().print(tagHandlerVar);
			getOut().print(" = (");
			getOut().print("new ");
			getOut().print(tagHandlerClassName);
			getOut().println("());");
			getOut().printin(Generator.getVarInstancemanager());
			getOut().print(".newInstance(");
			getOut().print(tagHandlerVar);
			getOut().println(");");
		}
	}

	private void writeDestroyInstance(String tagHandlerVar) {
		getOut().printin(Generator.getVarInstancemanager());
		getOut().print(".destroyInstance(");
		getOut().print(tagHandlerVar);
		getOut().println(");");
	}

	private void generateCustomEnd(NodeCustomTag n, String tagHandlerVar,
			String tagEvalVar, String tagPushBodyCountVar) {

		if (!n.hasEmptyBody()) {
			if (n.implementsIterationTag()) {
				getOut().printin("int evalDoAfterBody = ");
				getOut().print(tagHandlerVar);
				getOut().println(".doAfterBody();");

				// Synchronize AT_BEGIN and NESTED scripting variables
				syncScriptingVars(n, VariableInfo.getAtBegin());
				syncScriptingVars(n, VariableInfo.getNested());

				getOut().printil("if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)");
				getOut().pushIndent();
				getOut().printil("break;");
				getOut().popIndent();

				getOut().popIndent();
				getOut().printil("} while (true);");
			}

			restoreScriptingVars(n, VariableInfo.getNested());

			if (n.implementsBodyTag()) {
				getOut().printin("if (");
				getOut().print(tagEvalVar);
				getOut().println(" != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {");
				getOut().pushIndent();
				getOut().printil("out = _jspx_page_context.popBody();");
				if (n.implementsTryCatchFinally()) {
					getOut().printin(tagPushBodyCountVar);
					getOut().println("[0]--;");
				} else if (pushBodyCountVar != null) {
					getOut().printin(pushBodyCountVar);
					getOut().println("[0]--;");
				}
				getOut().popIndent();
				getOut().printil("}");
			}

			getOut().popIndent(); // EVAL_BODY
			getOut().printil("}");
		}

		getOut().printin("if (");
		getOut().print(tagHandlerVar);
		getOut().println(".doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {");
		getOut().pushIndent();
		if (!n.implementsTryCatchFinally()) {
			if (this.generator.isPoolingEnabled()
					&& !(n.implementsJspIdConsumer())) {
				getOut().printin(n.getTagHandlerPoolName());
				getOut().print(".reuse(");
				getOut().print(tagHandlerVar);
				getOut().println(");");
			} else {
				getOut().printin(tagHandlerVar);
				getOut().println(".release();");
				writeDestroyInstance(tagHandlerVar);
			}
		}
		if (isTagFile || isFragment) {
			getOut().printil("throw new javax.servlet.jsp.SkipPageException();");
		} else {
			getOut().printil((methodNesting > 0) ? "return true;" : "return;");
		}
		getOut().popIndent();
		getOut().printil("}");
		// Synchronize AT_BEGIN scripting variables
		syncScriptingVars(n, VariableInfo.getAtBegin());

		// TryCatchFinally
		if (n.implementsTryCatchFinally()) {
			getOut().popIndent(); // try
			getOut().printil("} catch (java.lang.Throwable _jspx_exception) {");
			getOut().pushIndent();

			getOut().printin("while (");
			getOut().print(tagPushBodyCountVar);
			getOut().println("[0]-- > 0)");
			getOut().pushIndent();
			getOut().printil("out = _jspx_page_context.popBody();");
			getOut().popIndent();

			getOut().printin(tagHandlerVar);
			getOut().println(".doCatch(_jspx_exception);");
			getOut().popIndent();
			getOut().printil("} finally {");
			getOut().pushIndent();
			getOut().printin(tagHandlerVar);
			getOut().println(".doFinally();");
		}

		if (this.generator.isPoolingEnabled() && !(n.implementsJspIdConsumer())) {
			getOut().printin(n.getTagHandlerPoolName());
			getOut().print(".reuse(");
			getOut().print(tagHandlerVar);
			getOut().println(");");
		} else {
			getOut().printin(tagHandlerVar);
			getOut().println(".release();");
			writeDestroyInstance(tagHandlerVar);
		}

		if (n.implementsTryCatchFinally()) {
			getOut().popIndent();
			getOut().printil("}");
		}

		// Declare and synchronize AT_END scripting variables (must do this
		// outside the try/catch/finally block)
		declareScriptingVars(n, VariableInfo.getAtEnd());
		syncScriptingVars(n, VariableInfo.getAtEnd());

		restoreScriptingVars(n, VariableInfo.getAtBegin());
	}

	private void generateCustomDoTag(NodeCustomTag n,
			GeneratorTagHandlerInfo handlerInfo, String tagHandlerVar)
			throws JasperException {

		Class<?> tagHandlerClass = handlerInfo.getTagHandlerClass();

		n.setBeginJavaLine(getOut().getJavaLine());
		getOut().printin("//  ");
		getOut().println(n.getQName());

		// Declare AT_BEGIN scripting variables
		declareScriptingVars(n, VariableInfo.getAtBegin());
		saveScriptingVars(n, VariableInfo.getAtBegin());

		String tagHandlerClassName = tagHandlerClass.getCanonicalName();
		writeNewInstance(tagHandlerVar, tagHandlerClassName);

		generateSetters(n, tagHandlerVar, handlerInfo, true);

		// JspIdConsumer (after context has been set)
		if (n.implementsJspIdConsumer()) {
			getOut().printin(tagHandlerVar);
			getOut().print(".setJspId(\"");
			getOut().print(this.generator.createJspId());
			getOut().println("\");");
		}

		// Set the body
		if (findJspBody(n) == null) {
			/*
			 * Encapsulate body of custom tag invocation in JspFragment and pass
			 * it to tag handler's setJspBody(), unless tag body is empty
			 */
			if (!n.hasEmptyBody()) {
				getOut().printin(tagHandlerVar);
				getOut().print(".setJspBody(");
				generateJspFragment(n, tagHandlerVar);
				getOut().println(");");
			}
		} else {
			/*
			 * Body of tag is the body of the <jsp:body> element. The visit
			 * method for that element is going to encapsulate that element's
			 * body in a JspFragment and pass it to the tag handler's
			 * setJspBody()
			 */
			String tmpTagHandlerVar = simpleTagHandlerVar;
			simpleTagHandlerVar = tagHandlerVar;
			boolean tmpIsSimpleTagHandler = isSimpleTagHandler;
			isSimpleTagHandler = true;
			visitBody(n);
			simpleTagHandlerVar = tmpTagHandlerVar;
			isSimpleTagHandler = tmpIsSimpleTagHandler;
		}

		getOut().printin(tagHandlerVar);
		getOut().println(".doTag();");

		restoreScriptingVars(n, VariableInfo.getAtBegin());

		// Synchronize AT_BEGIN scripting variables
		syncScriptingVars(n, VariableInfo.getAtBegin());

		// Declare and synchronize AT_END scripting variables
		declareScriptingVars(n, VariableInfo.getAtEnd());
		syncScriptingVars(n, VariableInfo.getAtEnd());

		// Resource injection
		writeDestroyInstance(tagHandlerVar);

		n.setEndJavaLine(getOut().getJavaLine());
	}

	private void declareScriptingVars(NodeCustomTag n, int scope) {
		if (isFragment) {
			// No need to declare Java variables, if we inside a
			// JspFragment, because a fragment is always scriptless.
			return;
		}

		List<Object> vec = n.getScriptingVars(scope);
		if (vec != null) {
			for (int i = 0; i < vec.size(); i++) {
				Object elem = vec.get(i);
				if (elem instanceof VariableInfo) {
					VariableInfo varInfo = (VariableInfo) elem;
					if (varInfo.getDeclare()) {
						getOut().printin(varInfo.getClassName());
						getOut().print(" ");
						getOut().print(varInfo.getVarName());
						getOut().println(" = null;");
					}
				} else {
					TagVariableInfo tagVarInfo = (TagVariableInfo) elem;
					if (tagVarInfo.getDeclare()) {
						String varName = tagVarInfo.getNameGiven();
						if (varName == null) {
							varName = n.getTagData().getAttributeString(
									tagVarInfo.getNameFromAttribute());
						} else if (tagVarInfo.getNameFromAttribute() != null) {
							// alias
							continue;
						}
						getOut().printin(tagVarInfo.getClassName());
						getOut().print(" ");
						getOut().print(varName);
						getOut().println(" = null;");
					}
				}
			}
		}
	}

	/*
	 * This method is called as part of the custom tag's start element.
	 * 
	 * If the given custom tag has a custom nesting level greater than 0, save
	 * the current values of its scripting variables to temporary variables, so
	 * those values may be restored in the tag's end element. This way, the
	 * scripting variables may be synchronized by the given tag without
	 * affecting their original values.
	 */
	private void saveScriptingVars(NodeCustomTag n, int scope) {
		if (n.getCustomNestingLevel() == 0) {
			return;
		}
		if (isFragment) {
			// No need to declare Java variables, if we inside a
			// JspFragment, because a fragment is always scriptless.
			// Thus, there is no need to save/ restore/ sync them.
			// Note, that JspContextWrapper.syncFoo() methods will take
			// care of saving/ restoring/ sync'ing of JspContext attributes.
			return;
		}

		TagVariableInfo[] tagVarInfos = n.getTagVariableInfos();
		VariableInfo[] varInfos = n.getVariableInfos();
		if ((varInfos.length == 0) && (tagVarInfos.length == 0)) {
			return;
		}

		List<Object> declaredVariables = n.getScriptingVars(scope);

		if (varInfos.length > 0) {
			for (int i = 0; i < varInfos.length; i++) {
				if (varInfos[i].getScope() != scope)
					continue;
				// If the scripting variable has been declared, skip codes
				// for saving and restoring it.
				if (declaredVariables.contains(varInfos[i]))
					continue;
				String varName = varInfos[i].getVarName();
				String tmpVarName = "_jspx_" + varName + "_"
						+ n.getCustomNestingLevel();
				getOut().printin(tmpVarName);
				getOut().print(" = ");
				getOut().print(varName);
				getOut().println(";");
			}
		} else {
			for (int i = 0; i < tagVarInfos.length; i++) {
				if (tagVarInfos[i].getScope() != scope)
					continue;
				// If the scripting variable has been declared, skip codes
				// for saving and restoring it.
				if (declaredVariables.contains(tagVarInfos[i]))
					continue;
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
				getOut().printin(tmpVarName);
				getOut().print(" = ");
				getOut().print(varName);
				getOut().println(";");
			}
		}
	}

	/*
	 * This method is called as part of the custom tag's end element.
	 * 
	 * If the given custom tag has a custom nesting level greater than 0,
	 * restore its scripting variables to their original values that were saved
	 * in the tag's start element.
	 */
	private void restoreScriptingVars(NodeCustomTag n, int scope) {
		if (n.getCustomNestingLevel() == 0) {
			return;
		}
		if (isFragment) {
			// No need to declare Java variables, if we inside a
			// JspFragment, because a fragment is always scriptless.
			// Thus, there is no need to save/ restore/ sync them.
			// Note, that JspContextWrapper.syncFoo() methods will take
			// care of saving/ restoring/ sync'ing of JspContext attributes.
			return;
		}

		TagVariableInfo[] tagVarInfos = n.getTagVariableInfos();
		VariableInfo[] varInfos = n.getVariableInfos();
		if ((varInfos.length == 0) && (tagVarInfos.length == 0)) {
			return;
		}

		List<Object> declaredVariables = n.getScriptingVars(scope);

		if (varInfos.length > 0) {
			for (int i = 0; i < varInfos.length; i++) {
				if (varInfos[i].getScope() != scope)
					continue;
				// If the scripting variable has been declared, skip codes
				// for saving and restoring it.
				if (declaredVariables.contains(varInfos[i]))
					continue;
				String varName = varInfos[i].getVarName();
				String tmpVarName = "_jspx_" + varName + "_"
						+ n.getCustomNestingLevel();
				getOut().printin(varName);
				getOut().print(" = ");
				getOut().print(tmpVarName);
				getOut().println(";");
			}
		} else {
			for (int i = 0; i < tagVarInfos.length; i++) {
				if (tagVarInfos[i].getScope() != scope)
					continue;
				// If the scripting variable has been declared, skip codes
				// for saving and restoring it.
				if (declaredVariables.contains(tagVarInfos[i]))
					continue;
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
				getOut().printin(varName);
				getOut().print(" = ");
				getOut().print(tmpVarName);
				getOut().println(";");
			}
		}
	}

	/*
	 * Synchronizes the scripting variables of the given custom tag for the
	 * given scope.
	 */
	private void syncScriptingVars(NodeCustomTag n, int scope) {
		if (isFragment) {
			// No need to declare Java variables, if we inside a
			// JspFragment, because a fragment is always scriptless.
			// Thus, there is no need to save/ restore/ sync them.
			// Note, that JspContextWrapper.syncFoo() methods will take
			// care of saving/ restoring/ sync'ing of JspContext attributes.
			return;
		}

		TagVariableInfo[] tagVarInfos = n.getTagVariableInfos();
		VariableInfo[] varInfos = n.getVariableInfos();

		if ((varInfos.length == 0) && (tagVarInfos.length == 0)) {
			return;
		}

		if (varInfos.length > 0) {
			for (int i = 0; i < varInfos.length; i++) {
				if (varInfos[i].getScope() == scope) {
					getOut().printin(varInfos[i].getVarName());
					getOut().print(" = (");
					getOut().print(varInfos[i].getClassName());
					getOut().print(") _jspx_page_context.findAttribute(");
					getOut().print(Generator.quote(varInfos[i].getVarName()));
					getOut().println(");");
				}
			}
		} else {
			for (int i = 0; i < tagVarInfos.length; i++) {
				if (tagVarInfos[i].getScope() == scope) {
					String name = tagVarInfos[i].getNameGiven();
					if (name == null) {
						name = n.getTagData().getAttributeString(
								tagVarInfos[i].getNameFromAttribute());
					} else if (tagVarInfos[i].getNameFromAttribute() != null) {
						// alias
						continue;
					}
					getOut().printin(name);
					getOut().print(" = (");
					getOut().print(tagVarInfos[i].getClassName());
					getOut().print(") _jspx_page_context.findAttribute(");
					getOut().print(Generator.quote(name));
					getOut().println(");");
				}
			}
		}
	}

	private String getJspContextVar() {
		if (this.isTagFile) {
			return "this.getJspContext()";
		}
		return "_jspx_page_context";
	}

	private String getExpressionFactoryVar() {
		return Generator.getVarExpressionfactory();
	}

	/*
	 * Creates a tag variable name by concatenating the given prefix and
	 * shortName and encoded to make the resultant string a valid Java
	 * Identifier.
	 */
	private String createTagVarName(String fullName, String prefix,
			String shortName) {

		String varName;
		synchronized (tagVarNumbers) {
			varName = prefix + "_" + shortName + "_";
			if (tagVarNumbers.get(fullName) != null) {
				Integer i = tagVarNumbers.get(fullName);
				varName = varName + i.intValue();
				tagVarNumbers.put(fullName, Integer.valueOf(i.intValue() + 1));
			} else {
				tagVarNumbers.put(fullName, Integer.valueOf(1));
				varName = varName + "0";
			}
		}
		return JspUtil.makeJavaIdentifier(varName);
	}

	@SuppressWarnings("null")
	private String evaluateAttribute(GeneratorTagHandlerInfo handlerInfo,
			NodeJspAttribute attr, NodeCustomTag n, String tagHandlerVar)
			throws JasperException {

		String attrValue = attr.getValue();
		if (attrValue == null) {
			if (attr.isNodeNamedAttribute()) {
				if (n.checkIfAttributeIsJspFragment(attr.getName())) {
					// XXX - no need to generate temporary variable here
					attrValue = generateNamedAttributeJspFragment(
							attr.getNodeNamedAttributeNode(), tagHandlerVar);
				} else {
					attrValue = generateNamedAttributeValue(attr
							.getNodeNamedAttributeNode());
				}
			} else {
				return null;
			}
		}

		String localName = attr.getLocalName();

		Method m = null;
		Class<?>[] c = null;
		if (attr.isDynamic()) {
			c = Generator.getObjectClass();
		} else {
			m = handlerInfo.getSetterMethod(localName);
			if (m == null) {
				this.generator.getErr().jspError(n,
						"jsp.error.unable.to_find_method", attr.getName());
			}
			c = m.getParameterTypes();
			// XXX assert(c.length > 0)
		}

		if (attr.isExpression()) {
			// Do nothing
		} else if (attr.isNodeNamedAttribute()) {
			if (!n.checkIfAttributeIsJspFragment(attr.getName())
					&& !attr.isDynamic()) {
				attrValue = convertString(c[0], attrValue, localName,
						handlerInfo.getPropertyEditorClass(localName), true);
			}
		} else if (attr.isELInterpreterInput()) {

			// results buffer
			StringBuilder sb = new StringBuilder(64);

			TagAttributeInfo tai = attr.getTagAttributeInfo();

			// generate elContext reference
			sb.append(getJspContextVar());
			sb.append(".getELContext()");
			String elContext = sb.toString();
			if (attr.getEL() != null && attr.getEL().getMapName() != null) {
				sb.setLength(0);
				sb.append("new org.apache.jasper.el.ELContextWrapper(");
				sb.append(elContext);
				sb.append(',');
				sb.append(attr.getEL().getMapName());
				sb.append(')');
				elContext = sb.toString();
			}

			// reset buffer
			sb.setLength(0);

			// create our mark
			sb.append(n.getStart().toString());
			sb.append(" '");
			sb.append(attrValue);
			sb.append('\'');
			String mark = sb.toString();

			// reset buffer
			sb.setLength(0);

			// depending on type
			if (attr.isDeferredInput()
					|| ((tai != null) && ValueExpression.class.getName()
							.equals(tai.getTypeName()))) {
				sb.append("new org.apache.jasper.el.JspValueExpression(");
				sb.append(Generator.quote(mark));
				sb.append(',');
				sb.append(getExpressionFactoryVar());
				sb.append(".createValueExpression(");
				if (attr.getEL() != null) { // optimize
					sb.append(elContext);
					sb.append(',');
				}
				sb.append(Generator.quote(attrValue));
				sb.append(',');
				sb.append(JspUtil.toJavaSourceTypeFromTld(attr
						.getExpectedTypeName()));
				sb.append("))");
				// should the expression be evaluated before passing to
				// the setter?
				boolean evaluate = false;
				if (tai != null && tai.canBeRequestTime()) {
					evaluate = true; // JSP.2.3.2
				}
				if (attr.isDeferredInput()) {
					evaluate = false; // JSP.2.3.3
				}
				if (attr.isDeferredInput() && tai != null
						&& tai.canBeRequestTime()) {
					evaluate = !attrValue.contains("#{"); // JSP.2.3.5
				}
				if (evaluate) {
					sb.append(".getValue(");
					sb.append(getJspContextVar());
					sb.append(".getELContext()");
					sb.append(")");
				}
				attrValue = sb.toString();
			} else if (attr.isDeferredMethodInput()
					|| ((tai != null) && MethodExpression.class.getName()
							.equals(tai.getTypeName()))) {
				sb.append("new org.apache.jasper.el.JspMethodExpression(");
				sb.append(Generator.quote(mark));
				sb.append(',');
				sb.append(getExpressionFactoryVar());
				sb.append(".createMethodExpression(");
				sb.append(elContext);
				sb.append(',');
				sb.append(Generator.quote(attrValue));
				sb.append(',');
				sb.append(JspUtil.toJavaSourceTypeFromTld(attr
						.getExpectedTypeName()));
				sb.append(',');
				sb.append("new java.lang.Class[] {");

				String[] p = attr.getParameterTypeNames();
				for (int i = 0; i < p.length; i++) {
					sb.append(JspUtil.toJavaSourceTypeFromTld(p[i]));
					sb.append(',');
				}
				if (p.length > 0) {
					sb.setLength(sb.length() - 1);
				}

				sb.append("}))");
				attrValue = sb.toString();
			} else {
				// run attrValue through the expression interpreter
				String mapName = (attr.getEL() != null) ? attr.getEL()
						.getMapName() : null;
				attrValue = this.generator.getElInterpreter().interpreterCall(
						this.generator.getCtxt(), this.isTagFile, attrValue, c[0],
						mapName, false);
			}
		} else {
			attrValue = convertString(c[0], attrValue, localName,
					handlerInfo.getPropertyEditorClass(localName), false);
		}
		return attrValue;
	}

	/**
	 * Generate code to create a map for the alias variables
	 *
	 * @return the name of the map
	 */
	private String generateAliasMap(NodeCustomTag n, String tagHandlerVar) {

		TagVariableInfo[] tagVars = n.getTagVariableInfos();
		String aliasMapVar = null;

		boolean aliasSeen = false;
		for (int i = 0; i < tagVars.length; i++) {

			String nameFrom = tagVars[i].getNameFromAttribute();
			if (nameFrom != null) {
				String aliasedName = n.getAttributeValue(nameFrom);
				if (aliasedName == null)
					continue;

				if (!aliasSeen) {
					getOut().printin("java.util.HashMap ");
					aliasMapVar = tagHandlerVar + "_aliasMap";
					getOut().print(aliasMapVar);
					getOut().println(" = new java.util.HashMap();");
					aliasSeen = true;
				}
				getOut().printin(aliasMapVar);
				getOut().print(".put(");
				getOut().print(Generator.quote(tagVars[i].getNameGiven()));
				getOut().print(", ");
				getOut().print(Generator.quote(aliasedName));
				getOut().println(");");
			}
		}
		return aliasMapVar;
	}

	private void generateSetters(NodeCustomTag n, String tagHandlerVar,
			GeneratorTagHandlerInfo handlerInfo, boolean simpleTag)
			throws JasperException {

		// Set context
		if (simpleTag) {
			// Generate alias map
			String aliasMapVar = null;
			if (n.isTagFile()) {
				aliasMapVar = generateAliasMap(n, tagHandlerVar);
			}
			getOut().printin(tagHandlerVar);
			if (aliasMapVar == null) {
				getOut().println(".setJspContext(_jspx_page_context);");
			} else {
				getOut().print(".setJspContext(_jspx_page_context, ");
				getOut().print(aliasMapVar);
				getOut().println(");");
			}
		} else {
			getOut().printin(tagHandlerVar);
			getOut().println(".setPageContext(_jspx_page_context);");
		}

		// Set parent
		if (isTagFile && parent == null) {
			getOut().printin(tagHandlerVar);
			getOut().print(".setParent(");
			getOut().print("new javax.servlet.jsp.tagext.TagAdapter(");
			getOut().print("(javax.servlet.jsp.tagext.SimpleTag) this ));");
		} else if (!simpleTag) {
			getOut().printin(tagHandlerVar);
			getOut().print(".setParent(");
			if (parent != null) {
				if (isSimpleTagParent) {
					getOut().print("new javax.servlet.jsp.tagext.TagAdapter(");
					getOut().print("(javax.servlet.jsp.tagext.SimpleTag) ");
					getOut().print(parent);
					getOut().println("));");
				} else {
					getOut().print("(javax.servlet.jsp.tagext.Tag) ");
					getOut().print(parent);
					getOut().println(");");
				}
			} else {
				getOut().println("null);");
			}
		} else {
			// The setParent() method need not be called if the value being
			// passed is null, since SimpleTag instances are not reused
			if (parent != null) {
				getOut().printin(tagHandlerVar);
				getOut().print(".setParent(");
				getOut().print(parent);
				getOut().println(");");
			}
		}

		// need to handle deferred values and methods
		NodeJspAttribute[] attrs = n.getJspAttributes();
		for (int i = 0; attrs != null && i < attrs.length; i++) {
			String attrValue = evaluateAttribute(handlerInfo, attrs[i], n,
					tagHandlerVar);

			Mark m = n.getStart();
			getOut().printil("// " + m.getFile() + "(" + m.getLineNumber() + ","
					+ m.getColumnNumber() + ") "
					+ attrs[i].getTagAttributeInfo());
			if (attrs[i].isDynamic()) {
				getOut().printin(tagHandlerVar);
				getOut().print(".");
				getOut().print("setDynamicAttribute(");
				String uri = attrs[i].getURI();
				if ("".equals(uri) || (uri == null)) {
					getOut().print("null");
				} else {
					getOut().print("\"" + attrs[i].getURI() + "\"");
				}
				getOut().print(", \"");
				getOut().print(attrs[i].getLocalName());
				getOut().print("\", ");
				getOut().print(attrValue);
				getOut().println(");");
			} else {
				getOut().printin(tagHandlerVar);
				getOut().print(".");
				getOut().print(handlerInfo.getSetterMethod(attrs[i].getLocalName())
						.getName());
				getOut().print("(");
				getOut().print(attrValue);
				getOut().println(");");
			}
		}
	}

	/*
	 * @param c The target class to which to coerce the given string @param s
	 * The string value @param attrName The name of the attribute whose value is
	 * being supplied @param propEditorClass The property editor for the given
	 * attribute @param isNamedAttribute true if the given attribute is a named
	 * attribute (that is, specified using the jsp:attribute standard action),
	 * and false otherwise
	 */
	private String convertString(Class<?> c, String s, String attrName,
			Class<?> propEditorClass, boolean isNamedAttribute) {

		String quoted = s;
		if (!isNamedAttribute) {
			quoted = Generator.quote(s);
		}

		if (propEditorClass != null) {
			String className = c.getCanonicalName();
			return "("
					+ className
					+ ")org.apache.jasper.runtime.JspRuntimeLibrary.getValueFromBeanInfoPropertyEditor("
					+ className + ".class, \"" + attrName + "\", " + quoted
					+ ", " + propEditorClass.getCanonicalName() + ".class)";
		} else if (c == String.class) {
			return quoted;
		} else if (c == boolean.class) {
			return JspUtil.coerceToPrimitiveBoolean(s, isNamedAttribute);
		} else if (c == Boolean.class) {
			return JspUtil.coerceToBoolean(s, isNamedAttribute);
		} else if (c == byte.class) {
			return JspUtil.coerceToPrimitiveByte(s, isNamedAttribute);
		} else if (c == Byte.class) {
			return JspUtil.coerceToByte(s, isNamedAttribute);
		} else if (c == char.class) {
			return JspUtil.coerceToChar(s, isNamedAttribute);
		} else if (c == Character.class) {
			return JspUtil.coerceToCharacter(s, isNamedAttribute);
		} else if (c == double.class) {
			return JspUtil.coerceToPrimitiveDouble(s, isNamedAttribute);
		} else if (c == Double.class) {
			return JspUtil.coerceToDouble(s, isNamedAttribute);
		} else if (c == float.class) {
			return JspUtil.coerceToPrimitiveFloat(s, isNamedAttribute);
		} else if (c == Float.class) {
			return JspUtil.coerceToFloat(s, isNamedAttribute);
		} else if (c == int.class) {
			return JspUtil.coerceToInt(s, isNamedAttribute);
		} else if (c == Integer.class) {
			return JspUtil.coerceToInteger(s, isNamedAttribute);
		} else if (c == short.class) {
			return JspUtil.coerceToPrimitiveShort(s, isNamedAttribute);
		} else if (c == Short.class) {
			return JspUtil.coerceToShort(s, isNamedAttribute);
		} else if (c == long.class) {
			return JspUtil.coerceToPrimitiveLong(s, isNamedAttribute);
		} else if (c == Long.class) {
			return JspUtil.coerceToLong(s, isNamedAttribute);
		} else if (c == Object.class) {
			return quoted;
		} else {
			String className = c.getCanonicalName();
			return "("
					+ className
					+ ")org.apache.jasper.runtime.JspRuntimeLibrary.getValueFromPropertyEditorManager("
					+ className + ".class, \"" + attrName + "\", " + quoted
					+ ")";
		}
	}

	/*
	 * Converts the scope string representation, whose possible values are
	 * "page", "request", "session", and "application", to the corresponding
	 * scope constant.
	 */
	private String getScopeConstant(String scope) {
		String scopeName = "javax.servlet.jsp.PageContext.PAGE_SCOPE"; // Default
																		// to
																		// page

		if ("request".equals(scope)) {
			scopeName = "javax.servlet.jsp.PageContext.REQUEST_SCOPE";
		} else if ("session".equals(scope)) {
			scopeName = "javax.servlet.jsp.PageContext.SESSION_SCOPE";
		} else if ("application".equals(scope)) {
			scopeName = "javax.servlet.jsp.PageContext.APPLICATION_SCOPE";
		}

		return scopeName;
	}

	/**
	 * Generates anonymous JspFragment inner class which is passed as an
	 * argument to SimpleTag.setJspBody().
	 */
	private void generateJspFragment(Node n, String tagHandlerVar)
			throws JasperException {
		// XXX - A possible optimization here would be to check to see
		// if the only child of the parent node is TemplateText. If so,
		// we know there won't be any parameters, etc, so we can
		// generate a low-overhead JspFragment that just echoes its
		// body. The implementation of this fragment can come from
		// the org.apache.jasper.runtime package as a support class.
		GeneratorFragmentHelperClassFragment fragment = fragmentHelperClass
				.openFragment(n, methodNesting);
		ServletWriter outSave = getOut();
		setOut(fragment.getGeneratorGenBuffer().getOut());
		String tmpParent = parent;
		parent = "_jspx_parent";
		boolean isSimpleTagParentSave = isSimpleTagParent;
		isSimpleTagParent = true;
		boolean tmpIsFragment = isFragment;
		isFragment = true;
		String pushBodyCountVarSave = pushBodyCountVar;
		if (pushBodyCountVar != null) {
			// Use a fixed name for push body count, to simplify code gen
			pushBodyCountVar = "_jspx_push_body_count";
		}
		visitBody(n);
		setOut(outSave);
		parent = tmpParent;
		isSimpleTagParent = isSimpleTagParentSave;
		isFragment = tmpIsFragment;
		pushBodyCountVar = pushBodyCountVarSave;
		fragmentHelperClass.closeFragment(fragment, methodNesting);
		// XXX - Need to change pageContext to jspContext if
		// we're not in a place where pageContext is defined (e.g.
		// in a fragment or in a tag file.
		getOut().print("new " + fragmentHelperClass.getClassName() + "( "
				+ fragment.getId() + ", _jspx_page_context, " + tagHandlerVar
				+ ", " + pushBodyCountVar + ")");
	}

	/**
	 * Generate the code required to obtain the runtime value of the given named
	 * attribute.
	 *
	 * @return The name of the temporary variable the result is stored in.
	 */
	public String generateNamedAttributeValue(NodeNamedAttribute n)
			throws JasperException {

		String varName = n.getTemporaryVariableName();

		// If the only body element for this named attribute node is
		// template text, we need not generate an extra call to
		// pushBody and popBody. Maybe we can further optimize
		// here by getting rid of the temporary variable, but in
		// reality it looks like javac does this for us.
		NodeNodes body = n.getBody();
		if (body != null) {
			boolean templateTextOptimization = false;
			if (body.size() == 1) {
				Node bodyElement = body.getNode(0);
				if (bodyElement instanceof NodeTemplateText) {
					templateTextOptimization = true;
					getOut().printil("java.lang.String "
							+ varName
							+ " = "
							+ Generator.quote(((NodeTemplateText) bodyElement)
									.getText()) + ";");
				}
			}

			// XXX - Another possible optimization would be for
			// lone EL expressions (no need to pushBody here either).

			if (!templateTextOptimization) {
				getOut().printil("out = _jspx_page_context.pushBody();");
				visitBody(n);
				getOut().printil("java.lang.String " + varName + " = "
						+ "((javax.servlet.jsp.tagext.BodyContent)"
						+ "out).getString();");
				getOut().printil("out = _jspx_page_context.popBody();");
			}
		} else {
			// Empty body must be treated as ""
			getOut().printil("java.lang.String " + varName + " = \"\";");
		}

		return varName;
	}

	/**
	 * Similar to generateNamedAttributeValue, but create a JspFragment instead.
	 *
	 * @param n
	 *            The parent node of the named attribute
	 * @param tagHandlerVar
	 *            The variable the tag handler is stored in, so the fragment
	 *            knows its parent tag.
	 * @return The name of the temporary variable the fragment is stored in.
	 */
	public String generateNamedAttributeJspFragment(NodeNamedAttribute n,
			String tagHandlerVar) throws JasperException {
		String varName = n.getTemporaryVariableName();

		getOut().printin("javax.servlet.jsp.tagext.JspFragment " + varName + " = ");
		generateJspFragment(n, tagHandlerVar);
		getOut().println(";");

		return varName;
	}

	public ServletWriter getOut() {
		return out;
	}

	public void setOut(ServletWriter out) {
		this.out = out;
	}
}