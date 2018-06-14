package org.apache.jasper.compiler;

public class JspUtilValidAttribute {
    private String name;

    private boolean mandatory;

    public JspUtilValidAttribute(String name, boolean mandatory) {
        this.setName(name);
        this.setMandatory(mandatory);
    }

    public JspUtilValidAttribute(String name) {
        this(name, false);
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
}