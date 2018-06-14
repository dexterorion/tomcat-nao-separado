package org.apache.jasper.compiler;

import java.io.CharArrayWriter;
import java.util.ListIterator;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

/*
 * Second-pass Visitor responsible for producing XML view and assigning
 * each element a unique jsp:id attribute.
 */
public class PageDataImplSecondPassVisitor extends NodeVisitor implements
		TagConstants {

	private NodeRoot root;
	private StringBuilder buf;
	private Compiler2 compiler;
	private String jspIdPrefix;
	private boolean resetDefaultNS = false;

	// Current value of jsp:id attribute
	private int jspId;

	/*
	 * Constructor
	 */
	public PageDataImplSecondPassVisitor(NodeRoot root, StringBuilder buf,
			Compiler2 compiler, String jspIdPrefix) {
		this.root = root;
		this.buf = buf;
		this.compiler = compiler;
		this.jspIdPrefix = jspIdPrefix;
	}

	/*
	 * Visits root node.
	 */
	@Override
	public void visit(NodeRoot n) throws JasperException {
		if (n == this.root) {
			// top-level page
			appendXmlProlog();
			appendTag(n);
		} else {
			boolean resetDefaultNSSave = resetDefaultNS;
			if (n.isXmlSyntax()) {
				resetDefaultNS = true;
			}
			visitBody(n);
			resetDefaultNS = resetDefaultNSSave;
		}
	}

	/*
	 * Visits jsp:root element of JSP page in XML syntax.
	 * 
	 * Any nested jsp:root elements (from pages included via an include
	 * directive) are ignored.
	 */
	@Override
	public void visit(NodeJspRoot n) throws JasperException {
		visitBody(n);
	}

	@Override
	public void visit(NodePageDirective n) throws JasperException {
		appendPageDirective(n);
	}

	@Override
	public void visit(NodeIncludeDirective n) throws JasperException {
		// expand in place
		visitBody(n);
	}

	@Override
	public void visit(NodeComment n) throws JasperException {
		// Comments are ignored in XML view
	}

	@Override
	public void visit(NodeDeclaration n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeExpression n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeScriptlet n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeJspElement n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeELExpression n) throws JasperException {
		if (!n.getRoot().isXmlSyntax()) {
			buf.append("<").append(JSP_TEXT_ACTION);
			buf.append(" ");
			buf.append(jspIdPrefix);
			buf.append(":id=\"");
			buf.append(jspId++).append("\">");
		}
		buf.append("${");
		buf.append(JspUtil.escapeXml(n.getText()));
		buf.append("}");
		if (!n.getRoot().isXmlSyntax()) {
			buf.append(JSP_TEXT_ACTION_END);
		}
		buf.append("\n");
	}

	@Override
	public void visit(NodeIncludeAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeForwardAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeGetProperty n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeSetProperty n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeParamAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeParamsAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeFallBackAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeUseBean n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodePlugIn n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeNamedAttribute n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeJspBody n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeCustomTag n) throws JasperException {
		boolean resetDefaultNSSave = resetDefaultNS;
		appendTag(n, resetDefaultNS);
		resetDefaultNS = resetDefaultNSSave;
	}

	@Override
	public void visit(NodeUninterpretedTag n) throws JasperException {
		boolean resetDefaultNSSave = resetDefaultNS;
		appendTag(n, resetDefaultNS);
		resetDefaultNS = resetDefaultNSSave;
	}

	@Override
	public void visit(NodeJspText n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeDoBodyAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeInvokeAction n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeTagDirective n) throws JasperException {
		appendTagDirective(n);
	}

	@Override
	public void visit(NodeAttributeDirective n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeVariableDirective n) throws JasperException {
		appendTag(n);
	}

	@Override
	public void visit(NodeTemplateText n) throws JasperException {
		/*
		 * If the template text came from a JSP page written in JSP syntax,
		 * create a jsp:text element for it (JSP 5.3.2).
		 */
		appendText(n.getText(), !n.getRoot().isXmlSyntax());
	}

	/*
	 * Appends the given tag, including its body, to the XML view.
	 */
	private void appendTag(Node n) throws JasperException {
		appendTag(n, false);
	}

	/*
	 * Appends the given tag, including its body, to the XML view, and
	 * optionally reset default namespace to "", if none specified.
	 */
	private void appendTag(Node n, boolean addDefaultNS) throws JasperException {

		NodeNodes body = n.getBody();
		String text = n.getText();

		buf.append("<").append(n.getQName());
		buf.append("\n");

		printAttributes(n, addDefaultNS);
		buf.append("  ").append(jspIdPrefix).append(":id").append("=\"");
		buf.append(jspId++).append("\"\n");

		if (ROOT_ACTION.equals(n.getLocalName()) || body != null
				|| text != null) {
			buf.append(">\n");
			if (ROOT_ACTION.equals(n.getLocalName())) {
				if (compiler.getCompilationContext().isTagFile()) {
					appendTagDirective();
				} else {
					appendPageDirective();
				}
			}
			if (body != null) {
				body.visit(this);
			} else {
				appendText(text, false);
			}
			buf.append("</" + n.getQName() + ">\n");
		} else {
			buf.append("/>\n");
		}
	}

	/*
	 * Appends the page directive with the given attributes to the XML view.
	 * 
	 * Since the import attribute of the page directive is the only page
	 * attribute that is allowed to appear multiple times within the same
	 * document, and since XML allows only single-value attributes, the values
	 * of multiple import attributes must be combined into one, separated by
	 * comma.
	 * 
	 * If the given page directive contains just 'contentType' and/or
	 * 'pageEncoding' attributes, we ignore it, as we've already appended a page
	 * directive containing just these two attributes.
	 */
	private void appendPageDirective(NodePageDirective n) {
		boolean append = false;
		Attributes attrs = n.getAttributes();
		int len = (attrs == null) ? 0 : attrs.getLength();
		for (int i = 0; i < len; i++) {
			@SuppressWarnings("null")
			// If attrs==null, len == 0
			String attrName = attrs.getQName(i);
			if (!"pageEncoding".equals(attrName)
					&& !"contentType".equals(attrName)) {
				append = true;
				break;
			}
		}
		if (!append) {
			return;
		}

		buf.append("<").append(n.getQName());
		buf.append("\n");

		// append jsp:id
		buf.append("  ").append(jspIdPrefix).append(":id").append("=\"");
		buf.append(jspId++).append("\"\n");

		// append remaining attributes
		for (int i = 0; i < len; i++) {
			@SuppressWarnings("null")
			// If attrs==null, len == 0
			String attrName = attrs.getQName(i);
			if ("import".equals(attrName) || "contentType".equals(attrName)
					|| "pageEncoding".equals(attrName)) {
				/*
				 * Page directive's 'import' attribute is considered further
				 * down, and its 'pageEncoding' and 'contentType' attributes are
				 * ignored, since we've already appended a new page directive
				 * containing just these two attributes
				 */
				continue;
			}
			String value = attrs.getValue(i);
			buf.append("  ").append(attrName).append("=\"");
			buf.append(JspUtil.getExprInXml(value)).append("\"\n");
		}
		if (n.getImports().size() > 0) {
			// Concatenate names of imported classes/packages
			boolean first = true;
			ListIterator<String> iter = n.getImports().listIterator();
			while (iter.hasNext()) {
				if (first) {
					first = false;
					buf.append("  import=\"");
				} else {
					buf.append(",");
				}
				buf.append(JspUtil.getExprInXml(iter.next()));
			}
			buf.append("\"\n");
		}
		buf.append("/>\n");
	}

	/*
	 * Appends a page directive with 'pageEncoding' and 'contentType'
	 * attributes.
	 * 
	 * The value of the 'pageEncoding' attribute is hard-coded to UTF-8, whereas
	 * the value of the 'contentType' attribute, which is identical to what the
	 * container will pass to ServletResponse.setContentType(), is derived from
	 * the pageInfo.
	 */
	private void appendPageDirective() {
		buf.append("<").append(JSP_PAGE_DIRECTIVE_ACTION);
		buf.append("\n");

		// append jsp:id
		buf.append("  ").append(jspIdPrefix).append(":id").append("=\"");
		buf.append(jspId++).append("\"\n");
		buf.append("  ").append("pageEncoding").append("=\"UTF-8\"\n");
		buf.append("  ").append("contentType").append("=\"");
		buf.append(compiler.getPageInfo().getContentType()).append("\"\n");
		buf.append("/>\n");
	}

	/*
	 * Appends the tag directive with the given attributes to the XML view.
	 * 
	 * If the given tag directive contains just a 'pageEncoding' attributes, we
	 * ignore it, as we've already appended a tag directive containing just this
	 * attributes.
	 */
	private void appendTagDirective(NodeTagDirective n) throws JasperException {

		boolean append = false;
		Attributes attrs = n.getAttributes();
		int len = (attrs == null) ? 0 : attrs.getLength();
		for (int i = 0; i < len; i++) {
			@SuppressWarnings("null")
			// If attrs==null, len == 0
			String attrName = attrs.getQName(i);
			if (!"pageEncoding".equals(attrName)) {
				append = true;
				break;
			}
		}
		if (!append) {
			return;
		}

		appendTag(n);
	}

	/*
	 * Appends a tag directive containing a single 'pageEncoding' attribute
	 * whose value is hard-coded to UTF-8.
	 */
	private void appendTagDirective() {
		buf.append("<").append(JSP_TAG_DIRECTIVE_ACTION);
		buf.append("\n");

		// append jsp:id
		buf.append("  ").append(jspIdPrefix).append(":id").append("=\"");
		buf.append(jspId++).append("\"\n");
		buf.append("  ").append("pageEncoding").append("=\"UTF-8\"\n");
		buf.append("/>\n");
	}

	private void appendText(String text, boolean createJspTextElement) {
		if (createJspTextElement) {
			buf.append("<").append(JSP_TEXT_ACTION);
			buf.append("\n");

			// append jsp:id
			buf.append("  ").append(jspIdPrefix).append(":id").append("=\"");
			buf.append(jspId++).append("\"\n");
			buf.append(">\n");

			appendCDATA(text);
			buf.append(JSP_TEXT_ACTION_END);
			buf.append("\n");
		} else {
			appendCDATA(text);
		}
	}

	/*
	 * Appends the given text as a CDATA section to the XML view, unless the
	 * text has already been marked as CDATA.
	 */
	private void appendCDATA(String text) {
		buf.append(PageDataImpl.getCdataStartSection());
		buf.append(escapeCDATA(text));
		buf.append(PageDataImpl.getCdataEndSection());
	}

	/*
	 * Escapes any occurrences of "]]>" (by replacing them with "]]&gt;") within
	 * the given text, so it can be included in a CDATA section.
	 */
	private String escapeCDATA(String text) {
		if (text == null)
			return "";
		int len = text.length();
		CharArrayWriter result = new CharArrayWriter(len);
		for (int i = 0; i < len; i++) {
			if (((i + 2) < len) && (text.charAt(i) == ']')
					&& (text.charAt(i + 1) == ']')
					&& (text.charAt(i + 2) == '>')) {
				// match found
				result.write(']');
				result.write(']');
				result.write('&');
				result.write('g');
				result.write('t');
				result.write(';');
				i += 2;
			} else {
				result.write(text.charAt(i));
			}
		}
		return result.toString();
	}

	/*
	 * Appends the attributes of the given Node to the XML view.
	 */
	private void printAttributes(Node n, boolean addDefaultNS) {

		/*
		 * Append "xmlns" attributes that represent tag libraries
		 */
		Attributes attrs = n.getTaglibAttributes();
		int len = (attrs == null) ? 0 : attrs.getLength();
		for (int i = 0; i < len; i++) {
			@SuppressWarnings("null")
			// If attrs==null, len == 0
			String name = attrs.getQName(i);
			String value = attrs.getValue(i);
			buf.append("  ").append(name).append("=\"").append(value)
					.append("\"\n");
		}

		/*
		 * Append "xmlns" attributes that do not represent tag libraries
		 */
		attrs = n.getNonTaglibXmlnsAttributes();
		len = (attrs == null) ? 0 : attrs.getLength();
		boolean defaultNSSeen = false;
		for (int i = 0; i < len; i++) {
			@SuppressWarnings("null")
			// If attrs==null, len == 0
			String name = attrs.getQName(i);
			String value = attrs.getValue(i);
			buf.append("  ").append(name).append("=\"").append(value)
					.append("\"\n");
			defaultNSSeen |= "xmlns".equals(name);
		}
		if (addDefaultNS && !defaultNSSeen) {
			buf.append("  xmlns=\"\"\n");
		}
		resetDefaultNS = false;

		/*
		 * Append all other attributes
		 */
		attrs = n.getAttributes();
		len = (attrs == null) ? 0 : attrs.getLength();
		for (int i = 0; i < len; i++) {
			@SuppressWarnings("null")
			// If attrs==null, len == 0
			String name = attrs.getQName(i);
			String value = attrs.getValue(i);
			buf.append("  ").append(name).append("=\"");
			buf.append(JspUtil.getExprInXml(value)).append("\"\n");
		}
	}

	/*
	 * Appends XML prolog with encoding declaration.
	 */
	private void appendXmlProlog() {
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
	}
}