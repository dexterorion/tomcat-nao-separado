package org.apache.tomcat.util.log;

public enum Mode {
    DEBUG(false), INFO_THEN_DEBUG(true), INFO(false);

    private final boolean fallToDebug;

    Mode(boolean fallToDebug) {
        this.fallToDebug = fallToDebug;
    }

    /**
     * @deprecated Unused, removed in Tomcat 8.
     */
    @Deprecated
    public boolean fallToDebug() {
        return fallToDebug;
    }
}