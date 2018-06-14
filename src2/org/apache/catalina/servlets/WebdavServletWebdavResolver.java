package org.apache.catalina.servlets;

import java.io.StringReader;

import javax.servlet.ServletContext;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class WebdavServletWebdavResolver implements EntityResolver {
	private ServletContext context;

	public WebdavServletWebdavResolver(ServletContext theContext) {
		context = theContext;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) {
		context.log(WebdavServlet.getSm().getString("webdavservlet.enternalEntityIgnored",
				publicId, systemId));
		return new InputSource(new StringReader("Ignored external entity"));
	}
}