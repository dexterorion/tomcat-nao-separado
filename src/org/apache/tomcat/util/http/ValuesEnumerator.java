package org.apache.tomcat.util.http;

import java.util.Enumeration;

import org.apache.tomcat.util.buf.MessageBytes;

/** Enumerate the values for a (possibly ) multiple
    value element.
*/
public class ValuesEnumerator implements Enumeration<String> {
    private int pos;
    private int size;
    private MessageBytes next;
    private MimeHeaders headers;
    private String name;

    public ValuesEnumerator(MimeHeaders headers, String name) {
        this.name=name;
        this.headers=headers;
        pos=0;
        size = headers.size();
        findNext();
    }

    private void findNext() {
        next=null;
        for(; pos< size; pos++ ) {
            MessageBytes n1=headers.getName( pos );
            if( n1.equalsIgnoreCase( name )) {
                next=headers.getValue( pos );
                break;
            }
        }
        pos++;
    }

    @Override
    public boolean hasMoreElements() {
        return next!=null;
    }

    @Override
    public String nextElement() {
        MessageBytes current=next;
        findNext();
        return current.toString();
    }
}