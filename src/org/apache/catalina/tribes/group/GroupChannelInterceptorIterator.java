package org.apache.catalina.tribes.group;

import java.util.Iterator;

import org.apache.catalina.tribes.ChannelInterceptor;

/**
 *
 * <p>Title: Interceptor Iterator</p>
 *
 * <p>Description: An iterator to loop through the interceptors in a channel</p>
 *
 * @version 1.0
 */
public class GroupChannelInterceptorIterator implements Iterator<ChannelInterceptor> {
    private ChannelInterceptor end;
    private ChannelInterceptor start;
    public GroupChannelInterceptorIterator(ChannelInterceptor start, ChannelInterceptor end) {
        this.end = end;
        this.start = start;
    }

    @Override
    public boolean hasNext() {
        return start!=null && start != end;
    }

    @Override
    public ChannelInterceptor next() {
        ChannelInterceptor result = null;
        if ( hasNext() ) {
            result = start;
            start = start.getNext();
        }
        return result;
    }

    @Override
    public void remove() {
        //empty operation
    }
}