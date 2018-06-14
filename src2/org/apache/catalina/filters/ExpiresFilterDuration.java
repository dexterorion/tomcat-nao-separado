package org.apache.catalina.filters;


/**
 * Duration composed of an {@link #amount} and a {@link #unit}
 */
public class ExpiresFilterDuration {

    private final int amount;

    private final ExpiresFilterDurationUnit unit;

    public ExpiresFilterDuration(int amount, ExpiresFilterDurationUnit unit) {
        super();
        this.amount = amount;
        this.unit = unit;
    }

    public int getAmount() {
        return amount;
    }

    public ExpiresFilterDurationUnit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return amount + " " + unit;
    }
}