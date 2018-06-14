package org.apache.tomcat.util.modeler;

import javax.management.NotificationFilter;
import javax.management.NotificationListener;

/**
 * Utility class representing a particular registered listener entry.
 */

public class BaseNotificationBroadcasterEntry {

    public BaseNotificationBroadcasterEntry(NotificationListener listener,
                                            NotificationFilter filter,
                                            Object handback) {
        this.setListener(listener);
        this.setFilter(filter);
        this.setHandback(handback);
    }

    public NotificationListener getListener() {
		return listener;
	}

	public void setListener(NotificationListener listener) {
		this.listener = listener;
	}

	public NotificationFilter getFilter() {
		return filter;
	}

	public void setFilter(NotificationFilter filter) {
		this.filter = filter;
	}

	public Object getHandback() {
		return handback;
	}

	public void setHandback(Object handback) {
		this.handback = handback;
	}

	private NotificationFilter filter = null;

    private Object handback = null;

    private NotificationListener listener = null;

}