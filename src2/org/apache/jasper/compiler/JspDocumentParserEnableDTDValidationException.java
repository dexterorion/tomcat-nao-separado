package org.apache.jasper.compiler;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/*
 * Exception indicating that a DOCTYPE declaration is present, but
 * validation is turned off.
 */
public class JspDocumentParserEnableDTDValidationException extends SAXParseException {

	private static final long serialVersionUID = 1L;

	public JspDocumentParserEnableDTDValidationException(String message, Locator loc) {
		super(message, loc);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		// This class does not provide a stack trace
		return this;
	}
}