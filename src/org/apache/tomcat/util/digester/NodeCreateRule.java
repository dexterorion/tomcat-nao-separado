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


package org.apache.tomcat.util.digester;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;


/**
 * A rule implementation that creates a DOM
 * {@link org.w3c.dom.Node Node} containing the XML at the element that matched
 * the rule. Two concrete types of nodes can be created by this rule:
 * <ul>
 *   <li>the default is to create an {@link org.w3c.dom.Element Element} node.
 *   The created element will correspond to the element that matched the rule,
 *   containing all XML content underneath that element.</li>
 *   <li>alternatively, this rule can create nodes of type
 *   {@link org.w3c.dom.DocumentFragment DocumentFragment}, which will contain
 *   only the XML content under the element the rule was triggered on.</li>
 * </ul>
 * The created node will be normalized, meaning it will not contain text nodes 
 * that only contain white space characters.
 * 

 * 
 * <p>The created <code>Node</code> will be pushed on Digester's object stack
 * when done. To use it in the context of another DOM
 * {@link org.w3c.dom.Document Document}, it must be imported first, using the
 * Document method
 * {@link org.w3c.dom.Document#importNode(org.w3c.dom.Node, boolean) importNode()}.
 * </p>
 *
 * <p><strong>Important Note:</strong> This is implemented by replacing the SAX
 * {@link org.xml.sax.ContentHandler ContentHandler} in the parser used by 
 * Digester, and resetting it when the matched element is closed. As a side 
 * effect, rules that would match XML nodes under the element that matches 
 * a <code>NodeCreateRule</code> will never be triggered by Digester, which 
 * usually is the behavior one would expect.</p>
 * 
 * <p><strong>Note</strong> that the current implementation does not set the namespace prefixes
 * in the exported nodes. The (usually more important) namespace URIs are set,
 * of course.</p>
 *
 * @since Digester 1.4
 */

public class NodeCreateRule extends Rule {


    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor. Creates an instance of this rule that will create a
     * DOM {@link org.w3c.dom.Element Element}.
     */
    public NodeCreateRule() throws ParserConfigurationException {

        this(Node.ELEMENT_NODE);

    }


    /**
     * Constructor. Creates an instance of this rule that will create a DOM
     * {@link org.w3c.dom.Element Element}, but lets you specify the JAXP 
     * <code>DocumentBuilder</code> that should be used when constructing the
     * node tree.
     * 
     * @param documentBuilder the JAXP <code>DocumentBuilder</code> to use
     */
    public NodeCreateRule(DocumentBuilder documentBuilder) {

        this(Node.ELEMENT_NODE, documentBuilder);

    }


    /**
     * Constructor. Creates an instance of this rule that will create either a 
     * DOM {@link org.w3c.dom.Element Element} or a DOM 
     * {@link org.w3c.dom.DocumentFragment DocumentFragment}, depending on the
     * value of the <code>nodeType</code> parameter.
     * 
     * @param nodeType the type of node to create, which can be either
     *   {@link org.w3c.dom.Node#ELEMENT_NODE Node.ELEMENT_NODE} or 
     *   {@link org.w3c.dom.Node#DOCUMENT_FRAGMENT_NODE Node.DOCUMENT_FRAGMENT_NODE}
     * @throws IllegalArgumentException if the node type is not supported
     */
    public NodeCreateRule(int nodeType) throws ParserConfigurationException {

        this(nodeType,
             DocumentBuilderFactory.newInstance().newDocumentBuilder());

    }


    /**
     * Constructor. Creates an instance of this rule that will create either a 
     * DOM {@link org.w3c.dom.Element Element} or a DOM 
     * {@link org.w3c.dom.DocumentFragment DocumentFragment}, depending on the
     * value of the <code>nodeType</code> parameter. This constructor lets you
     * specify the JAXP <code>DocumentBuilder</code> that should be used when
     * constructing the node tree.
     * 
     * @param nodeType the type of node to create, which can be either
     *   {@link org.w3c.dom.Node#ELEMENT_NODE Node.ELEMENT_NODE} or 
     *   {@link org.w3c.dom.Node#DOCUMENT_FRAGMENT_NODE Node.DOCUMENT_FRAGMENT_NODE}
     * @param documentBuilder the JAXP <code>DocumentBuilder</code> to use
     * @throws IllegalArgumentException if the node type is not supported
     */
    public NodeCreateRule(int nodeType, DocumentBuilder documentBuilder) {

        if (!((nodeType == Node.DOCUMENT_FRAGMENT_NODE) ||
              (nodeType == Node.ELEMENT_NODE))) {
            throw new IllegalArgumentException(
                "Can only create nodes of type DocumentFragment and Element");
        }
        this.nodeType = nodeType;
        this.documentBuilder = documentBuilder;

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The JAXP <code>DocumentBuilder</code> to use.
     */
    private DocumentBuilder documentBuilder = null;


    /**
     * The type of the node that should be created. Must be one of the
     * constants defined in {@link org.w3c.dom.Node Node}, but currently only
     * {@link org.w3c.dom.Node#ELEMENT_NODE Node.ELEMENT_NODE} and 
     * {@link org.w3c.dom.Node#DOCUMENT_FRAGMENT_NODE Node.DOCUMENT_FRAGMENT_NODE}
     * are allowed values.
     */
    private int nodeType = Node.ELEMENT_NODE;


    // ----------------------------------------------------------- Rule Methods


    /**
     * Implemented to replace the content handler currently in use by a 
     * NodeBuilder.
     * 
     * @param namespaceURI the namespace URI of the matching element, or an 
     *   empty string if the parser is not namespace aware or the element has
     *   no namespace
     * @param name the local name if the parser is namespace aware, or just 
     *   the element name otherwise
     * @param attributes The attribute list of this element
     * @throws Exception indicates a JAXP configuration problem
     */
    @Override
    public void begin(String namespaceURI, String name, Attributes attributes)
        throws Exception {

        XMLReader xmlReader = getDigester().getXMLReader();
        Document doc = documentBuilder.newDocument();
        NodeCreateRuleNodeBuilder builder = null;
        if (nodeType == Node.ELEMENT_NODE) {
            Element element = null;
            if (getDigester().getNamespaceAware()) {
                element =
                    doc.createElementNS(namespaceURI, name);
                for (int i = 0; i < attributes.getLength(); i++) {
                    element.setAttributeNS(attributes.getURI(i),
                                           attributes.getLocalName(i),
                                           attributes.getValue(i));
                }
            } else {
                element = doc.createElement(name);
                for (int i = 0; i < attributes.getLength(); i++) {
                    element.setAttribute(attributes.getQName(i),
                                         attributes.getValue(i));
                }
            }
            builder = new NodeCreateRuleNodeBuilder(this, doc, element);
        } else {
            builder = new NodeCreateRuleNodeBuilder(this, doc, doc.createDocumentFragment());
        }
        xmlReader.setContentHandler(builder);

    }


    /**
     * Pop the Node off the top of the stack.
     * 
     * @param namespace the namespace URI of the matching element, or an 
     *   empty string if the parser is not namespace aware or the element has
     *   no namespace
     * @param name the local name if the parser is namespace aware, or just 
     *   the element name otherwise
     */
    @Override
    public void end(String namespace, String name) throws Exception {

        getDigester().pop();

    }


}
