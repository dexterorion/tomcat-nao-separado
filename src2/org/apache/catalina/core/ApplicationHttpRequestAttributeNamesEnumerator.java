package org.apache.catalina.core;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Utility class used to expose the special attributes as being available
 * as request attributes.
 */
public class ApplicationHttpRequestAttributeNamesEnumerator implements Enumeration<String> {

    /**
	 * 
	 */
	private final ApplicationHttpRequest applicationHttpRequest;
	private int pos = -1;
	private int last = -1;
	private Enumeration<String> parentEnumeration = null;
	private String next = null;

    public ApplicationHttpRequestAttributeNamesEnumerator(ApplicationHttpRequest applicationHttpRequest) {
        this.applicationHttpRequest = applicationHttpRequest;
		parentEnumeration = this.applicationHttpRequest.getRequest().getAttributeNames();
        for (int i = this.applicationHttpRequest.getSpecialAttributes().length - 1; i >= 0; i--) {
            if (this.applicationHttpRequest.getAttribute(ApplicationHttpRequest.getSpecials()[i]) != null) {
                last = i;
                break;
            }
        }
    }

    @Override
    public boolean hasMoreElements() {
        return ((pos != last) || (next != null) 
                || ((next = findNext()) != null));
    }

    @Override
    public String nextElement() {
        if (pos != last) {
            for (int i = pos + 1; i <= last; i++) {
                if (this.applicationHttpRequest.getAttribute(ApplicationHttpRequest.getSpecials()[i]) != null) {
                    pos = i;
                    return (ApplicationHttpRequest.getSpecials()[i]);
                }
            }
        }
        String result = next;
        if (next != null) {
            next = findNext();
        } else {
            throw new NoSuchElementException();
        }
        return result;
    }

    protected String findNext() {
        String result = null;
        while ((result == null) && (parentEnumeration.hasMoreElements())) {
            String current = parentEnumeration.nextElement();
            if (!this.applicationHttpRequest.isSpecial(current)) {
                result = current;
            }
        }
        return result;
    }

}