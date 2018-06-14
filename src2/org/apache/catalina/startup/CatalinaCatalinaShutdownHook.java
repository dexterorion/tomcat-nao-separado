package org.apache.catalina.startup;

import java.util.logging.LogManager;

import org.apache.juli.ClassLoaderLogManager;
import org.apache.tomcat.util.ExceptionUtils2;

// XXX Should be moved to embedded !
/**
 * Shutdown hook which will perform a clean shutdown of Catalina if needed.
 */
public class CatalinaCatalinaShutdownHook extends Thread {

    /**
	 * 
	 */
	private final Catalina catalina;

	/**
	 * @param catalina
	 */
	public CatalinaCatalinaShutdownHook(Catalina catalina) {
		this.catalina = catalina;
	}

	@Override
    public void run() {
        try {
            if (this.catalina.getServer() != null) {
                this.catalina.stop();
            }
        } catch (Throwable ex) {
            ExceptionUtils2.handleThrowable(ex);
            Catalina.getLog().error(Catalina.getSm().getString("catalina.shutdownHookFail"), ex);
        } finally {
            // If JULI is used, shut JULI down *after* the server shuts down
            // so log messages aren't lost
            LogManager logManager = LogManager.getLogManager();
            if (logManager instanceof ClassLoaderLogManager) {
                ((ClassLoaderLogManager) logManager).shutdown();
            }
        }
    }
}