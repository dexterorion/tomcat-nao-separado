package org.apache.jasper.util;

/**
 * Implementation of a doubly linked list entry.
 * All implementation details are private.
 * For the consumer of the above collection, this
 * is simply garbage in, garbage out.
 */
public class FastRemovalDequeueEntry<T> {

    /** Is this entry still valid? */
    private boolean valid = true;
    /** The content this entry is valid for. */
    private final T content;
    /** Optional content that was displaced by this entry */
    private T replaced = null;
    /** Pointer to next element in queue. */
    private FastRemovalDequeueEntry<T> next = null;
    /** Pointer to previous element in queue. */
    private FastRemovalDequeueEntry<T> previous = null;

    public FastRemovalDequeueEntry(T object) {
        content = object;
    }

    public final boolean getValid() {
        return valid;
    }

    public final void setValid(final boolean valid) {
        this.valid = valid;
    }

    public final T getContent() {
        return content;
    }

    public final T getReplaced() {
        return replaced;
    }

    public final void setReplaced(final T replaced) {
        this.replaced = replaced;
    }

    public final void clearReplaced() {
        this.replaced = null;
    }

    public final FastRemovalDequeueEntry<T> getNext() {
        return next;
    }

    public final void setNext(final FastRemovalDequeueEntry<T> next) {
        this.next = next;
    }

    public final FastRemovalDequeueEntry<T> getPrevious() {
        return previous;
    }

    public final void setPrevious(final FastRemovalDequeueEntry<T> previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "Entry-" + content.toString();
    }
}