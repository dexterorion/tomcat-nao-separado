package org.apache.tomcat.util.digester;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The SAX content handler that does all the actual work of assembling the 
 * DOM node tree from the SAX events.
 */
public class NodeCreateRuleNodeBuilder extends DefaultHandler {
    // ------------------------------------------------------- Constructors


    /**
	 * 
	 */
	private final NodeCreateRule nodeCreateRule;


	/**
     * Constructor.
     * 
     * <p>Stores the content handler currently used by Digester so it can 
     * be reset when done, and initializes the DOM objects needed to 
     * build the node.</p>
     * 
     * @param doc the document to use to create nodes
     * @param root the root node
     * @param nodeCreateRule TODO
     * @throws ParserConfigurationException if the DocumentBuilderFactory 
     *   could not be instantiated
     * @throws SAXException if the XMLReader could not be instantiated by 
     *   Digester (should not happen)
     */
    public NodeCreateRuleNodeBuilder(NodeCreateRule nodeCreateRule, Document doc, Node root)
        throws ParserConfigurationException, SAXException {

        this.nodeCreateRule = nodeCreateRule;
		this.doc = doc;
        this.root = root;
        this.top = root;
        
        oldContentHandler = this.nodeCreateRule.getDigester().getXMLReader().getContentHandler();

    }


    // ------------------------------------------------- Instance Variables


    /**
     * The content handler used by Digester before it was set to this 
     * content handler.
     */
    private ContentHandler oldContentHandler = null;


    /**
     * Depth of the current node, relative to the element where the content
     * handler was put into action.
     */
    private int depth = 0;


    /**
     * A DOM Document used to create the various Node instances.
     */
    private Document doc = null;


    /**
     * The DOM node that will be pushed on Digester's stack.
     */
    private Node root = null;


    /**
     * The current top DOM mode.
     */
    private Node top = null;


    // --------------------------------------------- ContentHandler Methods


    /**
     * Appends a {@link org.w3c.dom.Text Text} node to the current node.
     * 
     * @param ch the characters from the XML document
     * @param start the start position in the array
     * @param length the number of characters to read from the array
     * @throws SAXException if the DOM implementation throws an exception
     */
    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {

        try {
            String str = new String(ch, start, length);
            if (str.trim().length() > 0) { 
                top.appendChild(doc.createTextNode(str));
            }
        } catch (DOMException e) {
            throw new SAXException(e.getMessage(), e);
        }

    }


    /**
     * Checks whether control needs to be returned to Digester.
     * 
     * @param namespaceURI the namespace URI
     * @param localName the local name
     * @param qName the qualified (prefixed) name
     * @throws SAXException if the DOM implementation throws an exception
     */
    @Override
    public void endElement(String namespaceURI, String localName,
                           String qName)
        throws SAXException {
        
        try {
            if (depth == 0) {
                this.nodeCreateRule.getDigester().getXMLReader().setContentHandler(
                    oldContentHandler);
                this.nodeCreateRule.getDigester().push(root);
                this.nodeCreateRule.getDigester().endElement(namespaceURI, localName, qName);
            }

            top = top.getParentNode();
            depth--;
        } catch (DOMException e) {
            throw new SAXException(e.getMessage(), e);
        }

    }


    /**
     * Adds a new
     * {@link org.w3c.dom.ProcessingInstruction ProcessingInstruction} to 
     * the current node.
     * 
     * @param target the processing instruction target
     * @param data the processing instruction data, or null if none was 
     *   supplied
     * @throws SAXException if the DOM implementation throws an exception
     */
    @Override
    public void processingInstruction(String target, String data)
        throws SAXException {
        
        try {
            top.appendChild(doc.createProcessingInstruction(target, data));
        } catch (DOMException e) {
            throw new SAXException(e.getMessage(), e);
        }

    }


    /**
     * Adds a new child {@link org.w3c.dom.Element Element} to the current
     * node.
     * 
     * @param namespaceURI the namespace URI
     * @param localName the local name
     * @param qName the qualified (prefixed) name
     * @param atts the list of attributes
     * @throws SAXException if the DOM implementation throws an exception
     */
    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
        throws SAXException {

        try {
            Node previousTop = top;
            if ((localName == null) || (localName.length() == 0)) { 
                top = doc.createElement(qName);
            } else {
                top = doc.createElementNS(namespaceURI, localName);
            }
            for (int i = 0; i < atts.getLength(); i++) {
                Attr attr = null;
                if ((atts.getLocalName(i) == null) ||
                    (atts.getLocalName(i).length() == 0)) {
                    attr = doc.createAttribute(atts.getQName(i));
                    attr.setNodeValue(atts.getValue(i));
                    ((Element)top).setAttributeNode(attr);
                } else {
                    attr = doc.createAttributeNS(atts.getURI(i),
                                                 atts.getLocalName(i));
                    attr.setNodeValue(atts.getValue(i));
                    ((Element)top).setAttributeNodeNS(attr);
                }
            }
            previousTop.appendChild(top);
            depth++;
        } catch (DOMException e) {
            throw new SAXException(e.getMessage(), e);
        }

    }

}