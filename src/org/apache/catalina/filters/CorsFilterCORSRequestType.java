package org.apache.catalina.filters;

// -------------------------------------------------------------- Constants
/**
 * Enumerates varies types of CORS requests. Also, provides utility methods
 * to determine the request type.
 */
public enum CorsFilterCORSRequestType {
    /**
     * A simple HTTP request, i.e. it shouldn't be pre-flighted.
     */
    SIMPLE,
    /**
     * A HTTP request that needs to be pre-flighted.
     */
    ACTUAL,
    /**
     * A pre-flight CORS request, to get meta information, before a
     * non-simple HTTP request is sent.
     */
    PRE_FLIGHT,
    /**
     * Not a CORS request, but a normal request.
     */
    NOT_CORS,
    /**
     * An invalid CORS request, i.e. it qualifies to be a CORS request, but
     * fails to be a valid one.
     */
    INVALID_CORS
}