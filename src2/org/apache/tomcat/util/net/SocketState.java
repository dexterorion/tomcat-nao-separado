package org.apache.tomcat.util.net;

/**
 * Different types of socket states to react upon.
 */
public enum SocketState {
    // TODO Add a new state to the AsyncStateMachine and remove
    //      ASYNC_END (if possible)
    OPEN, CLOSED, LONG, ASYNC_END, SENDFILE, UPGRADING_TOMCAT,
    UPGRADING, UPGRADED
}