package org.apache.catalina.filters;

import java.util.Calendar;

public enum ExpiresFilterDurationUnit {
    DAY(Calendar.DAY_OF_YEAR), HOUR(Calendar.HOUR), MINUTE(Calendar.MINUTE), MONTH(
            Calendar.MONTH), SECOND(Calendar.SECOND), WEEK(
            Calendar.WEEK_OF_YEAR), YEAR(Calendar.YEAR);
    private final int calendardField;

    private ExpiresFilterDurationUnit(int calendardField) {
        this.calendardField = calendardField;
    }

    public int getCalendardField() {
        return calendardField;
    }

}