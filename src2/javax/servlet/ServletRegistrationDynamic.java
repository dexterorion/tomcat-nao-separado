package javax.servlet;

import java.util.Set;

public interface ServletRegistrationDynamic extends ServletRegistration, RegistrationDynamic {
    public void setLoadOnStartup(int loadOnStartup);
    public void setMultipartConfig(MultipartConfigElement multipartConfig);
    public void setRunAsRole(String roleName);
    public Set<String> setServletSecurity(ServletSecurityElement constraint);
}