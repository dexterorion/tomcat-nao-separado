package org.apache.tomcat.util.buf;

import java.nio.charset.Charset;

public class StringCacheByteEntry {

	private byte[] name = null;
	private Charset charset = null;
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
		if (obj instanceof StringCacheByteEntry) {
			return getValueData().equals(((StringCacheByteEntry) obj).getValueData());
		}
		return false;
	}

	public byte[] getName() {
		return getNameData();
	}

	public void setName(byte[] name) {
		this.setNameData(name);
	}

	public Charset getCharset() {
		return getCharsetData();
	}

	public void setCharset(Charset charset) {
		this.setCharsetData(charset);
	}

	public String getValue() {
		return getValueData();
	}

	public void setValue(String value) {
		this.setValueData(value);
	}

	public byte[] getNameData() {
		return name;
	}

	public void setNameData(byte[] name) {
		this.name = name;
	}

	public Charset getCharsetData() {
		return charset;
	}

	public void setCharsetData(Charset charset) {
		this.charset = charset;
	}

	public String getValueData() {
		return value;
	}

	public void setValueData(String value) {
		this.value = value;
	}

}