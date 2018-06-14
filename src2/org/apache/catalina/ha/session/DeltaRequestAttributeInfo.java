package org.apache.catalina.ha.session;

import java.io.IOException;

public class DeltaRequestAttributeInfo implements java.io.Externalizable {
	private String name = null;
	private Object value = null;
	private int action;
	private int type;

	public DeltaRequestAttributeInfo() {
		this(-1, -1, null, null);
	}

	public DeltaRequestAttributeInfo(int type, int action, String name,
			Object value) {
		super();
		init(type, action, name, value);
	}

	public void init(int type, int action, String name, Object value) {
		this.name = name;
		this.value = value;
		this.action = action;
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public int getAction() {
		return action;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public String getName() {
		return name;
	}

	public void recycle() {
		name = null;
		value = null;
		type = -1;
		action = -1;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DeltaRequestAttributeInfo))
			return false;
		DeltaRequestAttributeInfo other = (DeltaRequestAttributeInfo) o;
		return other.getName().equals(this.getName());
	}

	@Override
	public void readExternal(java.io.ObjectInput in) throws IOException,
			ClassNotFoundException {
		// type - int
		// action - int
		// name - String
		// hasvalue - boolean
		// value - object
		type = in.readInt();
		action = in.readInt();
		name = in.readUTF();
		boolean hasValue = in.readBoolean();
		if (hasValue)
			value = in.readObject();
	}

	@Override
	public void writeExternal(java.io.ObjectOutput out) throws IOException {
		// type - int
		// action - int
		// name - String
		// hasvalue - boolean
		// value - object
		out.writeInt(getType());
		out.writeInt(getAction());
		out.writeUTF(getName());
		out.writeBoolean(getValue() != null);
		if (getValue() != null)
			out.writeObject(getValue());
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("AttributeInfo[type=");
		buf.append(getType()).append(", action=").append(getAction());
		buf.append(", name=").append(getName()).append(", value=")
				.append(getValue());
		buf.append(", addr=").append(super.toString()).append("]");
		return buf.toString();
	}

}