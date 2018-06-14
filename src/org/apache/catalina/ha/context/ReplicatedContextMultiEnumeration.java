package org.apache.catalina.ha.context;

import java.util.Enumeration;

public class ReplicatedContextMultiEnumeration<T> implements Enumeration<T> {
	private Enumeration<T>[] e = null;

	public ReplicatedContextMultiEnumeration(Enumeration<T>[] lists) {
		e = lists;
	}

	@Override
	public boolean hasMoreElements() {
		for (int i = 0; i < e.length; i++) {
			if (e[i].hasMoreElements())
				return true;
		}
		return false;
	}

	@Override
	public T nextElement() {
		for (int i = 0; i < e.length; i++) {
			if (e[i].hasMoreElements())
				return e[i].nextElement();
		}
		return null;

	}
}