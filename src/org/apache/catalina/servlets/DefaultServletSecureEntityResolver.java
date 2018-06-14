package org.apache.catalina.servlets;

import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class DefaultServletSecureEntityResolver implements EntityResolver2 {

	@Override
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		throw new SAXException(DefaultServlet.getSm().getString(
				"defaultServlet.blockExternalEntity", publicId, systemId));
	}

	@Override
	public InputSource getExternalSubset(String name, String baseURI)
			throws SAXException, IOException {
		throw new SAXException(DefaultServlet.getSm().getString(
				"defaultServlet.blockExternalSubset", name, baseURI));
	}

	@Override
	public InputSource resolveEntity(String name, String publicId,
			String baseURI, String systemId) throws SAXException,
			IOException {
		throw new SAXException(DefaultServlet.getSm().getString(
				"defaultServlet.blockExternalEntity2", name, publicId,
				baseURI, systemId));
	}
}