package javax.servlet;

public interface RegistrationDynamic extends Registration {
    
    /**
     * 
     * @param isAsyncSupported
     * @throws IllegalStateException
     */
    public void setAsyncSupported(boolean isAsyncSupported);
}