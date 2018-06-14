package org.apache.jasper.runtime;

import javax.servlet.jsp.tagext.Tag;

public class PerThreadTagHandlerPoolPerThreadData {
    private Tag handlers[];
    private int current;
	public Tag[] getHandlers() {
		return handlers;
	}
	public void setHandlers(Tag handlers[]) {
		this.handlers = handlers;
	}
	public int getCurrent() {
		return current;
	}
	public int setCurrent(int current) {
		this.current = current;
		return current;
	}
}