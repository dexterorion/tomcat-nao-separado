package org.apache.catalina.filters;

import java.util.List;

public class ExpiresFilterExpiresConfiguration {
    /**
     * List of duration elements.
     */
    private final List<ExpiresFilterDuration> durations;

    /**
     * Starting point of the elaspse to set in the response.
     */
    private final ExpiresFilterStartingPoint startingPoint;

    public ExpiresFilterExpiresConfiguration(ExpiresFilterStartingPoint startingPoint,
            List<ExpiresFilterDuration> durations) {
        super();
        this.startingPoint = startingPoint;
        this.durations = durations;
    }

    public List<ExpiresFilterDuration> getDurations() {
        return durations;
    }

    public ExpiresFilterStartingPoint getStartingPoint() {
        return startingPoint;
    }

    @Override
    public String toString() {
        return "ExpiresConfiguration[startingPoint=" + startingPoint +
                ", duration=" + durations + "]";
    }
}