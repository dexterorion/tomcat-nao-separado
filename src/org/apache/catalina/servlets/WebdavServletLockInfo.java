package org.apache.catalina.servlets;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.catalina.util.XMLWriter;
import org.apache.tomcat.util.http.FastHttpDateFormat;

/**
 * Holds a lock information.
 */
public class WebdavServletLockInfo {

	// -------------------------------------------------------- Constructor

	/**
	 * 
	 */
	private final WebdavServlet webdavServlet;

	/**
	 * Constructor.
	 * 
	 * @param webdavServlet
	 *            TODO
	 */
	public WebdavServletLockInfo(WebdavServlet webdavServlet) {
		this.webdavServlet = webdavServlet;
		// Ignore
	}

	// ------------------------------------------------- Instance Variables

	private String path = "/";
	private String type = "write";
	private String scope = "exclusive";
	private int depth = 0;
	private String owner = "";
	private Vector<String> tokens = new Vector<String>();
	private long expiresAt = 0;
	private Date creationDate = new Date();

	// ----------------------------------------------------- Public Methods

	/**
	 * Get a String representation of this lock token.
	 */
	@Override
	public String toString() {

		StringBuilder result = new StringBuilder("Type:");
		result.append(type);
		result.append("\nScope:");
		result.append(scope);
		result.append("\nDepth:");
		result.append(depth);
		result.append("\nOwner:");
		result.append(owner);
		result.append("\nExpiration:");
		result.append(FastHttpDateFormat.formatDate(expiresAt, null));
		Enumeration<String> tokensList = tokens.elements();
		while (tokensList.hasMoreElements()) {
			result.append("\nToken:");
			result.append(tokensList.nextElement());
		}
		result.append("\n");
		return result.toString();
	}

	/**
	 * Return true if the lock has expired.
	 */
	public boolean hasExpired() {
		return (System.currentTimeMillis() > expiresAt);
	}

	/**
	 * Return true if the lock is exclusive.
	 */
	public boolean isExclusive() {

		return (scope.equals("exclusive"));

	}

	/**
	 * Get an XML representation of this lock token. This method will append an
	 * XML fragment to the given XML writer.
	 */
	public void toXML(XMLWriter generatedXML) {

		generatedXML.writeElement("D", "activelock", XMLWriter.getOpening());

		generatedXML.writeElement("D", "locktype", XMLWriter.getOpening());
		generatedXML.writeElement("D", type, XMLWriter.getNoContent());
		generatedXML.writeElement("D", "locktype", XMLWriter.getClosing());

		generatedXML.writeElement("D", "lockscope", XMLWriter.getOpening());
		generatedXML.writeElement("D", scope, XMLWriter.getNoContent());
		generatedXML.writeElement("D", "lockscope", XMLWriter.getClosing());

		generatedXML.writeElement("D", "depth", XMLWriter.getOpening());
		if (depth == this.webdavServlet.getMaxDepth()) {
			generatedXML.writeText("Infinity");
		} else {
			generatedXML.writeText("0");
		}
		generatedXML.writeElement("D", "depth", XMLWriter.getClosing());

		generatedXML.writeElement("D", "owner", XMLWriter.getOpening());
		generatedXML.writeText(owner);
		generatedXML.writeElement("D", "owner", XMLWriter.getClosing());

		generatedXML.writeElement("D", "timeout", XMLWriter.getOpening());
		long timeout = (expiresAt - System.currentTimeMillis()) / 1000;
		generatedXML.writeText("Second-" + timeout);
		generatedXML.writeElement("D", "timeout", XMLWriter.getClosing());

		generatedXML.writeElement("D", "locktoken", XMLWriter.getOpening());
		Enumeration<String> tokensList = tokens.elements();
		while (tokensList.hasMoreElements()) {
			generatedXML.writeElement("D", "href", XMLWriter.getOpening());
			generatedXML.writeText("opaquelocktoken:"
					+ tokensList.nextElement());
			generatedXML.writeElement("D", "href", XMLWriter.getClosing());
		}
		generatedXML.writeElement("D", "locktoken", XMLWriter.getClosing());

		generatedXML.writeElement("D", "activelock", XMLWriter.getClosing());

	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Vector<String> getTokens() {
		return tokens;
	}

	public void setTokens(Vector<String> tokens) {
		this.tokens = tokens;
	}

	public long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(long expiresAt) {
		this.expiresAt = expiresAt;
	}

	public WebdavServlet getWebdavServlet() {
		return webdavServlet;
	}

}