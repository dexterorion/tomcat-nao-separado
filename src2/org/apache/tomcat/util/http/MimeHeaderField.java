package org.apache.tomcat.util.http;

import org.apache.tomcat.util.buf.MessageBytes;

public class MimeHeaderField {
    // multiple headers with same name - a linked list will
    // speed up name enumerations and search ( both cpu and
    // GC)
    private MimeHeaderField next;
    private MimeHeaderField prev;

    private  final MessageBytes nameB = MessageBytes.newInstance();
    private  final MessageBytes valueB = MessageBytes.newInstance();

    /**
     * Creates a new, uninitialized header field.
     */
    public MimeHeaderField() {
        // NO-OP
    }

    public void recycle() {
        nameB.recycle();
        valueB.recycle();
        setNext(null);
    }

    public MessageBytes getName() {
        return nameB;
    }

    public MessageBytes getValue() {
        return valueB;
    }

	public MimeHeaderField getNext() {
		return next;
	}

	public void setNext(MimeHeaderField next) {
		this.next = next;
	}

	public MimeHeaderField getPrev() {
		return prev;
	}

	public void setPrev(MimeHeaderField prev) {
		this.prev = prev;
	}
}