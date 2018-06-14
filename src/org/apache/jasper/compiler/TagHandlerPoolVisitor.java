package org.apache.jasper.compiler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

public class TagHandlerPoolVisitor extends NodeVisitor {

	private final Vector<String> names;

	/*
	 * Constructor
	 * 
	 * @param v Vector of tag handler pool names to populate
	 */
	public TagHandlerPoolVisitor(Vector<String> v) {
		names = v;
	}

	/*
	 * Gets the name of the tag handler pool for the given custom tag
	 * and adds it to the list of tag handler pool names unless it is
	 * already contained in it.
	 */
	@Override
	public void visit(NodeCustomTag n) throws JasperException {

		if (!n.implementsSimpleTag()) {
			String name = createTagHandlerPoolName(n.getPrefix(),
					n.getLocalName(), n.getAttributes(),
					n.getNodeNamedAttributeNodeNodes(),
					n.hasEmptyBody());
			n.setTagHandlerPoolName(name);
			if (!names.contains(name)) {
				names.add(name);
			}
		}
		visitBody(n);
	}

	/*
	 * Creates the name of the tag handler pool whose tag handlers may
	 * be (re)used to service this action.
	 * 
	 * @return The name of the tag handler pool
	 */
	private String createTagHandlerPoolName(String prefix,
			String shortName, Attributes attrs, NodeNodes namedAttrs,
			boolean hasEmptyBody) {
		StringBuilder poolName = new StringBuilder(64);
		poolName.append("_jspx_tagPool_").append(prefix).append('_')
				.append(shortName);

		if (attrs != null) {
			String[] attrNames = new String[attrs.getLength()
					+ namedAttrs.size()];
			for (int i = 0; i < attrNames.length; i++) {
				attrNames[i] = attrs.getQName(i);
			}
			for (int i = 0; i < namedAttrs.size(); i++) {
				attrNames[attrs.getLength() + i] = ((NodeNamedAttribute) namedAttrs
						.getNode(i)).getQName();
			}
			Arrays.sort(attrNames, Collections.reverseOrder());
			if (attrNames.length > 0) {
				poolName.append('&');
			}
			for (int i = 0; i < attrNames.length; i++) {
				poolName.append('_');
				poolName.append(attrNames[i]);
			}
		}
		if (hasEmptyBody) {
			poolName.append("_nobody");
		}
		return JspUtil.makeJavaIdentifier(poolName.toString());
	}
}