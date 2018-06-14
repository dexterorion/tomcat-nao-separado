package org.apache.tomcat.util.net;

import java.nio.channels.SelectionKey;

public class NioBlockingSelectorKeyReference {
	private SelectionKey key = null;

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

	@Override
	public void finalize() throws Throwable {
		if (key != null && key.isValid()) {
			NioBlockingSelector.getLog()
					.warn("Possible key leak, cancelling key in the finalizer.");
			try {
				key.cancel();
			} catch (Exception ignore) {
			}
		}
		key = null;

		super.finalize();
	}
}