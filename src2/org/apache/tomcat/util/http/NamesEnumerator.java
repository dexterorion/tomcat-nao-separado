package org.apache.tomcat.util.http;

import java.util.Enumeration;

/** Enumerate the distinct header names.
    Each nextElement() is O(n) ( a comparison is
    done with all previous elements ).

    This is less frequent than add() -
    we want to keep add O(1).
*/
public class NamesEnumerator implements Enumeration<String> {
    private int pos;
    private int size;
    private String next;
    private MimeHeaders headers;

    public NamesEnumerator(MimeHeaders headers) {
        this.headers=headers;
        pos=0;
        size = headers.size();
        findNext();
    }

    private void findNext() {
        next=null;
        for(; pos< size; pos++ ) {
            next=headers.getName( pos ).toString();
            for( int j=0; j<pos ; j++ ) {
                if( headers.getName( j ).equalsIgnoreCase( next )) {
                    // duplicate.
                    next=null;
                    break;
                }
            }
            if( next!=null ) {
                // it's not a duplicate
                break;
            }
        }
        // next time findNext is called it will try the
        // next element
        pos++;
    }

    @Override
    public boolean hasMoreElements() {
        return next!=null;
    }

    @Override
    public String nextElement() {
        String current=next;
        findNext();
        return current;
    }
}