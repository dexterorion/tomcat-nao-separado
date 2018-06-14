package org.apache.tomcat.util.buf;

public class StringCacheCharEntry {

	private char[] name = null;
	private String value = null;

	@Override
	public String toString() {
		return getValueData();
	}

	@Override
	public int hashCode() {
		return getValueData().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringCacheCharEntry) {
			return getValueData().equals(((StringCacheCharEntry) obj).getValueData());
		}
		return false;
	}

	public char[] getName() {
		return getNameData();
	}

	public void setName(char[] name) {
		this.setNameData(name);
	}

	public String getValue() {
		return getValueData();
	}

	public void setValue(String value) {
		this.setValueData(value);
	}

	public String getValueData() {
		return value;
	}

	public void setValueData(String value) {
		this.value = value;
	}

	public char[] getNameData() {
		return name;
	}

	public void setNameData(char[] name) {
		this.name = name;
	}

}