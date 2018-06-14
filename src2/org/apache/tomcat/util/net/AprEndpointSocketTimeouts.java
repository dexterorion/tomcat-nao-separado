package org.apache.tomcat.util.net;

public class AprEndpointSocketTimeouts {
    private int size;

    private long[] sockets;
    private long[] timeouts;
    private int pos = 0;

    public AprEndpointSocketTimeouts(int size) {
        this.size = 0;
        sockets = new long[size];
        timeouts = new long[size];
    }

    public void add(long socket, long timeout) {
        sockets[size] = socket;
        timeouts[size] = timeout;
        size++;
    }

    /**
     * Removes the specified socket from the poller.
     *
     * @return The configured timeout for the socket or zero if the socket
     *         was not in the list of socket timeouts
     */
    public long remove(long socket) {
        long result = 0;
        for (int i = 0; i < size; i++) {
            if (sockets[i] == socket) {
                result = timeouts[i];
                sockets[i] = sockets[size - 1];
                timeouts[i] = timeouts[size - 1];
                size--;
                break;
            }
        }
        return result;
    }

    public long check(long date) {
        while (pos < size) {
            if (date >= timeouts[pos]) {
                long result = sockets[pos];
                sockets[pos] = sockets[size - 1];
                timeouts[pos] = timeouts[size - 1];
                size--;
                return result;
            }
            pos++;
        }
        pos = 0;
        return 0;
    }

}