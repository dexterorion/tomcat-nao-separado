package org.apache.el.util;


/*
 * This class duplicates code in javax.el.Util. When making changes keep
 * the code in sync.
 */
public class ReflectionUtilMatchResult implements Comparable<ReflectionUtilMatchResult> {

    private final int exact;
    private final int assignable;
    private final int coercible;
    private final boolean bridge;

    public ReflectionUtilMatchResult(int exact, int assignable, int coercible, boolean bridge) {
        this.exact = exact;
        this.assignable = assignable;
        this.coercible = coercible;
        this.bridge = bridge;
    }

    public int getExact() {
        return exact;
    }

    public int getAssignable() {
        return assignable;
    }

    public int getCoercible() {
        return coercible;
    }

    public boolean isBridge() {
        return bridge;
    }

    @Override
    public int compareTo(ReflectionUtilMatchResult o) {
        if (this.getExact() < o.getExact()) {
            return -1;
        } else if (this.getExact() > o.getExact()) {
            return 1;
        } else {
            if (this.getAssignable() < o.getAssignable()) {
                return -1;
            } else if (this.getAssignable() > o.getAssignable()) {
                return 1;
            } else {
                if (this.getCoercible() < o.getCoercible()) {
                    return -1;
                } else if (this.getCoercible() > o.getCoercible()) {
                    return 1;
                } else {
                    // The nature of bridge methods is such that it actually
                    // doesn't matter which one we pick as long as we pick
                    // one. That said, pick the 'right' one (the non-bridge
                    // one) anyway.
                    if (o.isBridge() && !this.isBridge()) {
                        return 1;
                    } else if (!o.isBridge() && this.isBridge()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
        }
    }
}