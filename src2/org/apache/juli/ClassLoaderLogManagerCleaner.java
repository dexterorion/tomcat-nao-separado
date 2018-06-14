package org.apache.juli;

public final class ClassLoaderLogManagerCleaner extends Thread {
    
    /**
	 * 
	 */
	private final ClassLoaderLogManager classLoaderLogManager;

	/**
	 * @param classLoaderLogManager
	 */
	public ClassLoaderLogManagerCleaner(ClassLoaderLogManager classLoaderLogManager) {
		this.classLoaderLogManager = classLoaderLogManager;
	}

	@Override
    public void run() {
        if (this.classLoaderLogManager.isUseShutdownHook()) {
            this.classLoaderLogManager.shutdown();
        }
    }

}