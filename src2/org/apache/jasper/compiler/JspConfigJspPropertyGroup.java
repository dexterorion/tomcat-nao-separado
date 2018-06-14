package org.apache.jasper.compiler;

public class JspConfigJspPropertyGroup {
	private String path;
	private String extension;
	private JspConfigJspProperty jspProperty;

	public JspConfigJspPropertyGroup(String path, String extension,
			JspConfigJspProperty jspProperty) {
		this.path = path;
		this.extension = extension;
		this.jspProperty = jspProperty;
	}

	public String getPath() {
		return path;
	}

	public String getExtension() {
		return extension;
	}

	public JspConfigJspProperty getJspProperty() {
		return jspProperty;
	}
}