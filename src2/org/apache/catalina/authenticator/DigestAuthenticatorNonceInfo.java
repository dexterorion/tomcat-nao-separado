package org.apache.catalina.authenticator;

public class DigestAuthenticatorNonceInfo {
    private volatile long timestamp;
    private volatile boolean seen[];
    private volatile int offset;
    private volatile int count = 0;

    public DigestAuthenticatorNonceInfo(long currentTime, int seenWindowSize) {
        this.timestamp = currentTime;
        seen = new boolean[seenWindowSize];
        offset = seenWindowSize / 2;
    }

    public synchronized boolean nonceCountValid(long nonceCount) {
        if ((count - offset) >= nonceCount ||
                (nonceCount > count - offset + seen.length)) {
            return false;
        }
        int checkIndex = (int) ((nonceCount + offset) % seen.length);
        if (seen[checkIndex]) {
            return false;
        } else {
            seen[checkIndex] = true;
            seen[count % seen.length] = false;
            count++;
            return true;
        }
    }

    public long getTimestamp() {
        return timestamp;
    }
}