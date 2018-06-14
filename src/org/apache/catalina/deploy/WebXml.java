/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.deploy;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationJspPropertyGroupDescriptor;
import org.apache.catalina.core.ApplicationTaglibDescriptor;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.descriptor.XmlIdentifiers;
import org.apache.tomcat.util.res.StringManager3;

/**
 * Representation of common elements of web.xml and web-fragment.xml. Provides
 * a repository for parsed data before the elements are merged.
 * Validation is spread between multiple classes:
 * The digester checks for structural correctness (eg single login-config)
 * This class checks for invalid duplicates (eg filter/servlet names)
 * StandardContext will check validity of values (eg URL formats etc)
 */
public class WebXml {

	private static final String ORDER_OTHERS =
        "org.apache.catalina.order.others";

	private static final StringManager3 sm =
        StringManager3.getManager(Constants4.getPackage());

    private static final Log log = LogFactory.getLog(WebXml.class);

    // Global defaults are overridable but Servlets and Servlet mappings need to
    // be unique. Duplicates normally trigger an error. This flag indicates if
    // newly added Servlet elements are marked as overridable.
    private boolean overridable = false;
    public boolean isOverridable() {
        return isOverridableData();
    }
    public void setOverridable(boolean overridable) {
        this.setOverridableData(overridable);
    }

    // web.xml only elements
    // Absolute Ordering
    private Set<String> absoluteOrdering = null;
    public void createAbsoluteOrdering() {
        if (getAbsoluteOrderingData() == null) {
            setAbsoluteOrderingData(new LinkedHashSet<String>());
        }
    }
    public void addAbsoluteOrdering(String fragmentName) {
        createAbsoluteOrdering();
        getAbsoluteOrderingData().add(fragmentName);
    }
    public void addAbsoluteOrderingOthers() {
        createAbsoluteOrdering();
        getAbsoluteOrderingData().add(ORDER_OTHERS);
    }
    public Set<String> getAbsoluteOrdering() {
        return getAbsoluteOrderingData();
    }

    // web-fragment.xml only elements
    // Relative ordering
    private Set<String> after = new LinkedHashSet<String>();
    public void addAfterOrdering(String fragmentName) {
        getAfterData().add(fragmentName);
    }
    public void addAfterOrderingOthers() {
        if (getBeforeData().contains(ORDER_OTHERS)) {
            throw new IllegalArgumentException(sm.getString(
                    "webXml.multipleOther"));
        }
        getAfterData().add(ORDER_OTHERS);
    }
    public Set<String> getAfterOrdering() { return getAfterData(); }

    private Set<String> before = new LinkedHashSet<String>();
    public void addBeforeOrdering(String fragmentName) {
        getBeforeData().add(fragmentName);
    }
    public void addBeforeOrderingOthers() {
        if (getAfterData().contains(ORDER_OTHERS)) {
            throw new IllegalArgumentException(sm.getString(
                    "webXml.multipleOther"));
        }
        getBeforeData().add(ORDER_OTHERS);
    }
    public Set<String> getBeforeOrdering() { return getBeforeData(); }

    // Common elements and attributes

    // Required attribute of web-app element
    public String getVersion() {
        StringBuilder sb = new StringBuilder(3);
        sb.append(getMajorVersionData());
        sb.append('.');
        sb.append(getMinorVersionData());
        return sb.toString();
    }
    /**
     * Set the version for this web.xml file
     * @param version   Values of <code>null</code> will be ignored
     */
    public void setVersion(String version) {
        if (version == null) {
            return;
        }
        if ("2.4".equals(version)) {
            setMajorVersionData(2);
            setMinorVersionData(4);
        } else if ("2.5".equals(version)) {
            setMajorVersionData(2);
            setMinorVersionData(5);
        } else if ("3.0".equals(version)) {
            setMajorVersionData(3);
            setMinorVersionData(0);
        } else {
            log.warn(sm.getString("webXml.version.unknown", version));
        }
    }


    // Optional publicId attribute
    private String publicId = null;
    public String getPublicId() { return getPublicIdData(); }
    public void setPublicId(String publicId) {
        // Update major and minor version
        if (publicId == null) {
            return;
        }
        if (XmlIdentifiers.getWeb22Public().equals(publicId)) {
            setMajorVersionData(2);
            setMinorVersionData(2);
            this.setPublicIdData(publicId);
        } else if (XmlIdentifiers.getWeb23Public().equals(publicId)) {
            setMajorVersionData(2);
            setMinorVersionData(3);
            this.setPublicIdData(publicId);
        } else {
            log.warn(sm.getString("webXml.unrecognisedPublicId", publicId));
        }
    }

    // Optional metadata-complete attribute
    private boolean metadataComplete = false;
    public boolean isMetadataComplete() { return isMetadataCompleteData(); }
    public void setMetadataComplete(boolean metadataComplete) {
        this.setMetadataCompleteData(metadataComplete); }

    // Optional name element
    private String name = null;
    public String getName() { return getNameData(); }
    public void setName(String name) {
        if (ORDER_OTHERS.equalsIgnoreCase(name)) {
            // This is unusual. This name will be ignored. Log the fact.
            log.warn(sm.getString("webXml.reservedName", name));
        } else {
            this.setNameData(name);
        }
    }

    // Derived major and minor version attributes
    // Default to 3.0 until we know otherwise
    private int majorVersion = 3;
    private int minorVersion = 0;
    public int getMajorVersion() { return getMajorVersionData(); }
    public int getMinorVersion() { return getMinorVersionData(); }

    // web-app elements
    // TODO: Ignored elements:
    // - description
    // - icon

    // display-name - TODO should support multiple with language
    private String displayName = null;
    public String getDisplayName() { return getDisplayNameData(); }
    public void setDisplayName(String displayName) {
        this.setDisplayNameData(displayName);
    }

    // distributable
    private boolean distributable = false;
    public boolean isDistributable() { return isDistributableData(); }
    public void setDistributable(boolean distributable) {
        this.setDistributableData(distributable);
    }

    // context-param
    // TODO: description (multiple with language) is ignored
    private Map<String,String> contextParams = new HashMap<String,String>();
    public void addContextParam(String param, String value) {
        getContextParamsData().put(param, value);
    }
    public Map<String,String> getContextParams() { return getContextParamsData(); }

    // filter
    // TODO: Should support multiple description elements with language
    // TODO: Should support multiple display-name elements with language
    // TODO: Should support multiple icon elements
    // TODO: Description for init-param is ignored
    private Map<String,FilterDef> filters =
        new LinkedHashMap<String,FilterDef>();
    public void addFilter(FilterDef filter) {
        if (getFiltersData().containsKey(filter.getFilterName())) {
            // Filter names must be unique within a web(-fragment).xml
            throw new IllegalArgumentException(
                    sm.getString("webXml.duplicateFilter",
                            filter.getFilterName()));
        }
        getFiltersData().put(filter.getFilterName(), filter);
    }
    public Map<String,FilterDef> getFilters() { return getFiltersData(); }

    // filter-mapping
    private Set<FilterMap> filterMaps = new LinkedHashSet<FilterMap>();
    private Set<String> filterMappingNames = new HashSet<String>();
    public void addFilterMapping(FilterMap filterMap) {
        getFilterMapsData().add(filterMap);
        getFilterMappingNamesData().add(filterMap.getFilterName());
    }
    public Set<FilterMap> getFilterMappings() { return getFilterMapsData(); }

    // listener
    // TODO: description (multiple with language) is ignored
    // TODO: display-name (multiple with language) is ignored
    // TODO: icon (multiple) is ignored
    private Set<String> listeners = new LinkedHashSet<String>();
    public void addListener(String className) {
        getListenersData().add(className);
    }
    public Set<String> getListeners() { return getListenersData(); }

    // servlet
    // TODO: description (multiple with language) is ignored
    // TODO: display-name (multiple with language) is ignored
    // TODO: icon (multiple) is ignored
    // TODO: init-param/description (multiple with language) is ignored
    // TODO: security-role-ref/description (multiple with language) is ignored
    private Map<String,ServletDef> servlets = new HashMap<String,ServletDef>();
    public void addServlet(ServletDef servletDef) {
        getServletsData().put(servletDef.getServletName(), servletDef);
        if (isOverridableData()) {
            servletDef.setOverridable(isOverridableData());
        }
    }
    public Map<String,ServletDef> getServlets() { return getServletsData(); }

    // servlet-mapping
    private Map<String,String> servletMappings = new HashMap<String,String>();
    private Set<String> servletMappingNames = new HashSet<String>();
    public void addServletMapping(String urlPattern, String servletName) {
        String oldServletName = getServletMappingsData().put(urlPattern, servletName);
        if (oldServletName != null) {
            // Duplicate mapping. As per clarification from the Servlet EG,
            // deployment should fail.
            throw new IllegalArgumentException(sm.getString(
                    "webXml.duplicateServletMapping", oldServletName,
                    servletName, urlPattern));
        }
        getServletMappingNamesData().add(servletName);
    }
    public Map<String,String> getServletMappings() { return getServletMappingsData(); }

    // session-config
    // Digester will check there is only one of these
    private SessionConfig sessionConfig = new SessionConfig();
    public void setSessionConfig(SessionConfig sessionConfig) {
        this.setSessionConfigData(sessionConfig);
    }
    public SessionConfig getSessionConfig() { return getSessionConfigData(); }

    // mime-mapping
    private Map<String,String> mimeMappings = new HashMap<String,String>();
    public void addMimeMapping(String extension, String mimeType) {
        getMimeMappingsData().put(extension, mimeType);
    }
    public Map<String,String> getMimeMappings() { return getMimeMappingsData(); }

    // welcome-file-list merge control
    private boolean replaceWelcomeFiles = false;
    private boolean alwaysAddWelcomeFiles = true;
    /**
     * When merging/parsing web.xml files into this web.xml should the current
     * set be completely replaced?
     */
    public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {
        this.setReplaceWelcomeFilesData(replaceWelcomeFiles);
    }
    /**
     * When merging from this web.xml, should the welcome files be added to the
     * target web.xml even if it already contains welcome file definitions.
     */
    public void setAlwaysAddWelcomeFiles(boolean alwaysAddWelcomeFiles) {
        this.setAlwaysAddWelcomeFilesData(alwaysAddWelcomeFiles);
    }

    // welcome-file-list
    private Set<String> welcomeFiles = new LinkedHashSet<String>();
    public void addWelcomeFile(String welcomeFile) {
        if (isReplaceWelcomeFilesData()) {
            getWelcomeFilesData().clear();
            setReplaceWelcomeFilesData(false);
        }
        getWelcomeFilesData().add(welcomeFile);
    }
    public Set<String> getWelcomeFiles() { return getWelcomeFilesData(); }

    // error-page
    private Map<String,ErrorPage> errorPages = new HashMap<String,ErrorPage>();
    public void addErrorPage(ErrorPage errorPage) {
        getErrorPagesData().put(errorPage.getName(), errorPage);
    }
    public Map<String,ErrorPage> getErrorPages() { return getErrorPagesData(); }

    // Digester will check there is only one jsp-config
    // jsp-config/taglib or taglib (2.3 and earlier)
    private Map<String,String> taglibs = new HashMap<String,String>();
    public void addTaglib(String uri, String location) {
        if (getTaglibsData().containsKey(uri)) {
            // Taglib URIs must be unique within a web(-fragment).xml
            throw new IllegalArgumentException(
                    sm.getString("webXml.duplicateTaglibUri", uri));
        }
        getTaglibsData().put(uri, location);
    }
    public Map<String,String> getTaglibs() { return getTaglibsData(); }

    // jsp-config/jsp-property-group
    private Set<JspPropertyGroup> jspPropertyGroups =
        new LinkedHashSet<JspPropertyGroup>();
    public void addJspPropertyGroup(JspPropertyGroup propertyGroup) {
        getJspPropertyGroupsData().add(propertyGroup);
    }
    public Set<JspPropertyGroup> getJspPropertyGroups() {
        return getJspPropertyGroupsData();
    }

    // security-constraint
    // TODO: Should support multiple display-name elements with language
    // TODO: Should support multiple description elements with language
    private Set<SecurityConstraint> securityConstraints =
        new HashSet<SecurityConstraint>();
    public void addSecurityConstraint(SecurityConstraint securityConstraint) {
        getSecurityConstraintsData().add(securityConstraint);
    }
    public Set<SecurityConstraint> getSecurityConstraints() {
        return getSecurityConstraintsData();
    }

    // login-config
    // Digester will check there is only one of these
    private LoginConfig loginConfig = null;
    public void setLoginConfig(LoginConfig loginConfig) {
        this.setLoginConfigDAta(loginConfig);
    }
    public LoginConfig getLoginConfig() { return getLoginConfigData(); }

    // security-role
    // TODO: description (multiple with language) is ignored
    private Set<String> securityRoles = new HashSet<String>();
    public void addSecurityRole(String securityRole) {
        getSecurityRolesData().add(securityRole);
    }
    public Set<String> getSecurityRoles() { return getSecurityRolesData(); }

    // env-entry
    // TODO: Should support multiple description elements with language
    private Map<String,ContextEnvironment> envEntries =
        new HashMap<String,ContextEnvironment>();
    public void addEnvEntry(ContextEnvironment envEntry) {
        if (getEnvEntriesData().containsKey(envEntry.getName())) {
            // env-entry names must be unique within a web(-fragment).xml
            throw new IllegalArgumentException(
                    sm.getString("webXml.duplicateEnvEntry",
                            envEntry.getName()));
        }
        getEnvEntriesData().put(envEntry.getName(),envEntry);
    }
    public Map<String,ContextEnvironment> getEnvEntries() { return getEnvEntriesData(); }

    // ejb-ref
    // TODO: Should support multiple description elements with language
    private Map<String,ContextEjb> ejbRefs = new HashMap<String,ContextEjb>();
    public void addEjbRef(ContextEjb ejbRef) {
        getEjbRefsData().put(ejbRef.getName(),ejbRef);
    }
    public Map<String,ContextEjb> getEjbRefs() { return getEjbRefsData(); }

    // ejb-local-ref
    // TODO: Should support multiple description elements with language
    private Map<String,ContextLocalEjb> ejbLocalRefs =
        new HashMap<String,ContextLocalEjb>();
    public void addEjbLocalRef(ContextLocalEjb ejbLocalRef) {
        getEjbLocalRefsData().put(ejbLocalRef.getName(),ejbLocalRef);
    }
    public Map<String,ContextLocalEjb> getEjbLocalRefs() {
        return getEjbLocalRefsData();
    }

    // service-ref
    // TODO: Should support multiple description elements with language
    // TODO: Should support multiple display-names elements with language
    // TODO: Should support multiple icon elements ???
    private Map<String,ContextService> serviceRefs =
        new HashMap<String,ContextService>();
    public void addServiceRef(ContextService serviceRef) {
        getServiceRefsData().put(serviceRef.getName(), serviceRef);
    }
    public Map<String,ContextService> getServiceRefs() { return getServiceRefsData(); }

    // resource-ref
    // TODO: Should support multiple description elements with language
    private Map<String,ContextResource> resourceRefs =
        new HashMap<String,ContextResource>();
    public void addResourceRef(ContextResource resourceRef) {
        if (getResourceRefsData().containsKey(resourceRef.getName())) {
            // resource-ref names must be unique within a web(-fragment).xml
            throw new IllegalArgumentException(
                    sm.getString("webXml.duplicateResourceRef",
                            resourceRef.getName()));
        }
        getResourceRefsData().put(resourceRef.getName(), resourceRef);
    }
    public Map<String,ContextResource> getResourceRefs() {
        return getResourceRefsData();
    }

    // resource-env-ref
    // TODO: Should support multiple description elements with language
    private Map<String,ContextResourceEnvRef> resourceEnvRefs =
        new HashMap<String,ContextResourceEnvRef>();
    public void addResourceEnvRef(ContextResourceEnvRef resourceEnvRef) {
        if (getResourceEnvRefsData().containsKey(resourceEnvRef.getName())) {
            // resource-env-ref names must be unique within a web(-fragment).xml
            throw new IllegalArgumentException(
                    sm.getString("webXml.duplicateResourceEnvRef",
                            resourceEnvRef.getName()));
        }
        getResourceEnvRefsData().put(resourceEnvRef.getName(), resourceEnvRef);
    }
    public Map<String,ContextResourceEnvRef> getResourceEnvRefs() {
        return getResourceEnvRefsData();
    }

    // message-destination-ref
    // TODO: Should support multiple description elements with language
    private Map<String,MessageDestinationRef> messageDestinationRefs =
        new HashMap<String,MessageDestinationRef>();
    public void addMessageDestinationRef(
            MessageDestinationRef messageDestinationRef) {
        if (getMessageDestinationRefsData().containsKey(
                messageDestinationRef.getName())) {
            // message-destination-ref names must be unique within a
            // web(-fragment).xml
            throw new IllegalArgumentException(sm.getString(
                    "webXml.duplicateMessageDestinationRef",
                    messageDestinationRef.getName()));
        }
        getMessageDestinationRefsData().put(messageDestinationRef.getName(),
                messageDestinationRef);
    }
    public Map<String,MessageDestinationRef> getMessageDestinationRefs() {
        return getMessageDestinationRefsData();
    }

    // message-destination
    // TODO: Should support multiple description elements with language
    // TODO: Should support multiple display-names elements with language
    // TODO: Should support multiple icon elements ???
    private Map<String,MessageDestination> messageDestinations =
        new HashMap<String,MessageDestination>();
    public void addMessageDestination(
            MessageDestination messageDestination) {
        if (getMessageDestinationsData().containsKey(
                messageDestination.getName())) {
            // message-destination names must be unique within a
            // web(-fragment).xml
            throw new IllegalArgumentException(
                    sm.getString("webXml.duplicateMessageDestination",
                            messageDestination.getName()));
        }
        getMessageDestinationsData().put(messageDestination.getName(),
                messageDestination);
    }
    public Map<String,MessageDestination> getMessageDestinations() {
        return getMessageDestinationsData();
    }

    // locale-encoding-mapping-list
    private Map<String,String> localeEncodingMappings =
        new HashMap<String,String>();
    public void addLocaleEncodingMapping(String locale, String encoding) {
        getLocaleEncodingMappingsData().put(locale, encoding);
    }
    public Map<String,String> getLocalEncodingMappings() {
        return getLocaleEncodingMappingsData();
    }

    // post-construct elements
    private Map<String, String> postConstructMethods =
            new HashMap<String, String>();
    public void addPostConstructMethods(String clazz, String method) {
        if (!getPostConstructMethodsData().containsKey(clazz)) {
            getPostConstructMethodsData().put(clazz, method);
        }
    }
    public Map<String, String> getPostConstructMethods() {
        return getPostConstructMethodsData();
    }

    // pre-destroy elements
    private Map<String, String> preDestroyMethods =
            new HashMap<String, String>();
    public void addPreDestroyMethods(String clazz, String method) {
        if (!getPreDestroyMethodsData().containsKey(clazz)) {
            getPreDestroyMethodsData().put(clazz, method);
        }
    }
    public Map<String, String> getPreDestroyMethods() {
        return getPreDestroyMethodsData();
    }

    // Attributes not defined in web.xml or web-fragment.xml

    // URL of JAR / exploded JAR for this web-fragment
    private URL uRL = null;
    public void setURL(URL url) { this.setuRLData(url); }
    public URL getURL() { return getuRLData(); }

    // Name of jar file
    private String jarName = null;
    public void setJarName(String jarName) { this.setJarNameData(jarName); }
    public String getJarName() { return getJarNameData(); }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(32);
        buf.append("Name: ");
        buf.append(getName());
        buf.append(", URL: ");
        buf.append(getURL());
        return buf.toString();
    }

    private static final String INDENT2 = "  ";
    private static final String INDENT4 = "    ";
    private static final String INDENT6 = "      ";

    /**
     * Generate a web.xml in String form that matches the representation stored
     * in this object.
     *
     * @return The complete contents of web.xml as a String
     */
    public String toXml() {
        StringBuilder sb = new StringBuilder(2048);

        // TODO - Various, icon, description etc elements are skipped - mainly
        //        because they are ignored when web.xml is parsed - see above

        // NOTE - Elements need to be written in the order defined in the 2.3
        //        DTD else validation of the merged web.xml will fail

        // NOTE - Some elements need to be skipped based on the version of the
        //        specification being used. Version is validated and starts at
        //        2.2. The version tests used in this method take advantage of
        //        this.

        // Declaration
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        // Root element
        if (getPublicIdData() != null) {
            sb.append("<!DOCTYPE web-app PUBLIC\n");
            sb.append("  \"");
            sb.append(getPublicIdData());
            sb.append("\"\n");
            sb.append("  \"");
            if (XmlIdentifiers.getWeb22Public().equals(getPublicIdData())) {
                sb.append(XmlIdentifiers.getWeb22System());
            } else {
                sb.append(XmlIdentifiers.getWeb23System());
            }
            sb.append("\">\n");
            sb.append("<web-app>");
        } else {
            String javaeeNamespace = null;
            String webXmlSchemaLocation = null;
            String version = getVersion();
            if ("2.4".equals(version)) {
                javaeeNamespace = XmlIdentifiers.getJavaee14Ns();
                webXmlSchemaLocation = XmlIdentifiers.getWeb24Xsd();
            } else if ("2.5".equals(version)) {
                javaeeNamespace = XmlIdentifiers.getJavaee5Ns();
                webXmlSchemaLocation = XmlIdentifiers.getWeb25Xsd();
            } else if ("3.0".equals(version)) {
                javaeeNamespace = XmlIdentifiers.getJavaee6Ns();
                webXmlSchemaLocation = XmlIdentifiers.getWeb30Xsd();
            }
            sb.append("<web-app xmlns=\"");
            sb.append(javaeeNamespace);
            sb.append("\"\n");
            sb.append("         xmlns:xsi=");
            sb.append("\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            sb.append("         xsi:schemaLocation=\"");
            sb.append(javaeeNamespace);
            sb.append(" ");
            sb.append(webXmlSchemaLocation);
            sb.append("\"\n");
            sb.append("         version=\"");
            sb.append(getVersion());
            sb.append("\"");
            if ("2.4".equals(version)) {
                sb.append(">\n\n");
            } else {
                sb.append("\n         metadata-complete=\"true\">\n\n");
            }
        }
        appendElement(sb, INDENT2, "display-name", getDisplayNameData());

        if (isDistributable()) {
            sb.append("  <distributable/>\n\n");
        }

        for (Map.Entry<String, String> entry : getContextParamsData().entrySet()) {
            sb.append("  <context-param>\n");
            appendElement(sb, INDENT4, "param-name", entry.getKey());
            appendElement(sb, INDENT4, "param-value", entry.getValue());
            sb.append("  </context-param>\n");
        }
        sb.append('\n');

        // Filters were introduced in Servlet 2.3
        if (getMajorVersion() > 2 || getMinorVersion() > 2) {
            for (Map.Entry<String, FilterDef> entry : getFiltersData().entrySet()) {
                FilterDef filterDef = entry.getValue();
                sb.append("  <filter>\n");
                appendElement(sb, INDENT4, "description",
                        filterDef.getDescription());
                appendElement(sb, INDENT4, "display-name",
                        filterDef.getDisplayName());
                appendElement(sb, INDENT4, "filter-name",
                        filterDef.getFilterName());
                appendElement(sb, INDENT4, "filter-class",
                        filterDef.getFilterClass());
                // Async support was introduced for Servlet 3.0 onwards
                if (getMajorVersion() != 2) {
                    appendElement(sb, INDENT4, "async-supported",
                            filterDef.getAsyncSupported());
                }
                for (Map.Entry<String, String> param :
                        filterDef.getParameterMap().entrySet()) {
                    sb.append("    <init-param>\n");
                    appendElement(sb, INDENT6, "param-name", param.getKey());
                    appendElement(sb, INDENT6, "param-value", param.getValue());
                    sb.append("    </init-param>\n");
                }
                sb.append("  </filter>\n");
            }
            sb.append('\n');

            for (FilterMap filterMap : getFilterMapsData()) {
                sb.append("  <filter-mapping>\n");
                appendElement(sb, INDENT4, "filter-name",
                        filterMap.getFilterName());
                if (filterMap.getMatchAllServletNames()) {
                    sb.append("    <servlet-name>*</servlet-name>\n");
                } else {
                    for (String servletName : filterMap.getServletNames()) {
                        appendElement(sb, INDENT4, "servlet-name", servletName);
                    }
                }
                if (filterMap.getMatchAllUrlPatterns()) {
                    sb.append("    <url-pattern>*</url-pattern>\n");
                } else {
                    for (String urlPattern : filterMap.getURLPatterns()) {
                        appendElement(sb, INDENT4, "url-pattern", urlPattern);
                    }
                }
                // dispatcher was added in Servlet 2.4
                if (getMajorVersion() > 2 || getMinorVersion() > 3) {
                    for (String dispatcher : filterMap.getDispatcherNames()) {
                        if (getMajorVersion() == 2 &&
                                DispatcherType.ASYNC.name().equals(dispatcher)) {
                            continue;
                        }
                        appendElement(sb, INDENT4, "dispatcher", dispatcher);
                    }
                }
                sb.append("  </filter-mapping>\n");
            }
            sb.append('\n');
        }

        // Listeners were introduced in Servlet 2.3
        if (getMajorVersion() > 2 || getMinorVersion() > 2) {
            for (String listener : getListenersData()) {
                sb.append("  <listener>\n");
                appendElement(sb, INDENT4, "listener-class", listener);
                sb.append("  </listener>\n");
            }
            sb.append('\n');
        }

        for (Map.Entry<String, ServletDef> entry : getServletsData().entrySet()) {
            ServletDef servletDef = entry.getValue();
            sb.append("  <servlet>\n");
            appendElement(sb, INDENT4, "description",
                    servletDef.getDescription());
            appendElement(sb, INDENT4, "display-name",
                    servletDef.getDisplayName());
            appendElement(sb, INDENT4, "servlet-name", entry.getKey());
            appendElement(sb, INDENT4, "servlet-class",
                    servletDef.getServletClass());
            appendElement(sb, INDENT4, "jsp-file", servletDef.getJspFile());
            for (Map.Entry<String, String> param :
                    servletDef.getParameterMap().entrySet()) {
                sb.append("    <init-param>\n");
                appendElement(sb, INDENT6, "param-name", param.getKey());
                appendElement(sb, INDENT6, "param-value", param.getValue());
                sb.append("    </init-param>\n");
            }
            appendElement(sb, INDENT4, "load-on-startup",
                    servletDef.getLoadOnStartup());
            appendElement(sb, INDENT4, "enabled", servletDef.getEnabled());
            // Async support was introduced for Servlet 3.0 onwards
            if (getMajorVersion() != 2) {
                appendElement(sb, INDENT4, "async-supported",
                        servletDef.getAsyncSupported());
            }
            // servlet/run-as was introduced in Servlet 2.3
            if (getMajorVersion() > 2 || getMinorVersion() > 2) {
                if (servletDef.getRunAs() != null) {
                    sb.append("    <run-as>\n");
                    appendElement(sb, INDENT6, "role-name", servletDef.getRunAs());
                    sb.append("    </run-as>\n");
                }
            }
            for (SecurityRoleRef roleRef : servletDef.getSecurityRoleRefs()) {
                sb.append("    <security-role-ref>\n");
                appendElement(sb, INDENT6, "role-name", roleRef.getName());
                appendElement(sb, INDENT6, "role-link", roleRef.getLink());
                sb.append("    </security-role-ref>\n");
            }
            // multipart-config was added in Servlet 3.0
            if (getMajorVersion() != 2) {
                MultipartDef multipartDef = servletDef.getMultipartDef();
                if (multipartDef != null) {
                    sb.append("    <multipart-config>\n");
                    appendElement(sb, INDENT6, "location",
                            multipartDef.getLocation());
                    appendElement(sb, INDENT6, "max-file-size",
                            multipartDef.getMaxFileSize());
                    appendElement(sb, INDENT6, "max-request-size",
                            multipartDef.getMaxRequestSize());
                    appendElement(sb, INDENT6, "file-size-threshold",
                            multipartDef.getFileSizeThreshold());
                    sb.append("    </multipart-config>\n");
                }
            }
            sb.append("  </servlet>\n");
        }
        sb.append('\n');

        for (Map.Entry<String, String> entry : getServletMappingsData().entrySet()) {
            sb.append("  <servlet-mapping>\n");
            appendElement(sb, INDENT4, "servlet-name", entry.getValue());
            appendElement(sb, INDENT4, "url-pattern", entry.getKey());
            sb.append("  </servlet-mapping>\n");
        }
        sb.append('\n');

        if (getSessionConfigData() != null) {
            sb.append("  <session-config>\n");
            appendElement(sb, INDENT4, "session-timeout",
                    getSessionConfigData().getSessionTimeout());
            if (getMajorVersionData() >= 3) {
                sb.append("    <cookie-config>\n");
                appendElement(sb, INDENT6, "name", getSessionConfigData().getCookieName());
                appendElement(sb, INDENT6, "domain",
                        getSessionConfigData().getCookieDomain());
                appendElement(sb, INDENT6, "path", getSessionConfigData().getCookiePath());
                appendElement(sb, INDENT6, "comment",
                        getSessionConfigData().getCookieComment());
                appendElement(sb, INDENT6, "http-only",
                        getSessionConfigData().getCookieHttpOnly());
                appendElement(sb, INDENT6, "secure",
                        getSessionConfigData().getCookieSecure());
                appendElement(sb, INDENT6, "max-age",
                        getSessionConfigData().getCookieMaxAge());
                sb.append("    </cookie-config>\n");
                for (SessionTrackingMode stm :
                        getSessionConfigData().getSessionTrackingModes()) {
                    appendElement(sb, INDENT4, "tracking-mode", stm.name());
                }
            }
            sb.append("  </session-config>\n\n");
        }

        for (Map.Entry<String, String> entry : getMimeMappingsData().entrySet()) {
            sb.append("  <mime-mapping>\n");
            appendElement(sb, INDENT4, "extension", entry.getKey());
            appendElement(sb, INDENT4, "mime-type", entry.getValue());
            sb.append("  </mime-mapping>\n");
        }
        sb.append('\n');

        if (getWelcomeFilesData().size() > 0) {
            sb.append("  <welcome-file-list>\n");
            for (String welcomeFile : getWelcomeFilesData()) {
                appendElement(sb, INDENT4, "welcome-file", welcomeFile);
            }
            sb.append("  </welcome-file-list>\n\n");
        }

        for (ErrorPage errorPage : getErrorPagesData().values()) {
            String exeptionType = errorPage.getExceptionType();
            int errorCode = errorPage.getErrorCode();

            if (exeptionType == null && errorCode == 0 && getMajorVersion() == 2) {
                // Default error pages are only supported from 3.0 onwards
                continue;
            }
            sb.append("  <error-page>\n");
            if (errorPage.getExceptionType() != null) {
                appendElement(sb, INDENT4, "exception-type", exeptionType);
            } else if (errorPage.getErrorCode() > 0) {
                appendElement(sb, INDENT4, "error-code",
                        Integer.toString(errorCode));
            }
            appendElement(sb, INDENT4, "location", errorPage.getLocation());
            sb.append("  </error-page>\n");
        }
        sb.append('\n');

        // jsp-config was added in Servlet 2.4. Prior to that, tag-libs was used
        // directly and jsp-property-group did not exist
        if (getTaglibsData().size() > 0 || getJspPropertyGroupsData().size() > 0) {
            if (getMajorVersion() > 2 || getMinorVersion() > 3) {
                sb.append("  <jsp-config>\n");
            }
            for (Map.Entry<String, String> entry : getTaglibsData().entrySet()) {
                sb.append("    <taglib>\n");
                appendElement(sb, INDENT6, "taglib-uri", entry.getKey());
                appendElement(sb, INDENT6, "taglib-location", entry.getValue());
                sb.append("    </taglib>\n");
            }
            if (getMajorVersion() > 2 || getMinorVersion() > 3) {
                for (JspPropertyGroup jpg : getJspPropertyGroupsData()) {
                    sb.append("    <jsp-property-group>\n");
                    for (String urlPattern : jpg.getUrlPatterns()) {
                        appendElement(sb, INDENT6, "url-pattern", urlPattern);
                    }
                    appendElement(sb, INDENT6, "el-ignored", jpg.getElIgnored());
                    appendElement(sb, INDENT6, "page-encoding",
                            jpg.getPageEncoding());
                    appendElement(sb, INDENT6, "scripting-invalid",
                            jpg.getScriptingInvalid());
                    appendElement(sb, INDENT6, "is-xml", jpg.getIsXml());
                    for (String prelude : jpg.getIncludePreludes()) {
                        appendElement(sb, INDENT6, "include-prelude", prelude);
                    }
                    for (String coda : jpg.getIncludeCodas()) {
                        appendElement(sb, INDENT6, "include-coda", coda);
                    }
                    appendElement(sb, INDENT6, "deferred-syntax-allowed-as-literal",
                            jpg.getDeferredSyntax());
                    appendElement(sb, INDENT6, "trim-directive-whitespaces",
                            jpg.getTrimWhitespace());
                    appendElement(sb, INDENT6, "default-content-type",
                            jpg.getDefaultContentType());
                    appendElement(sb, INDENT6, "buffer", jpg.getBuffer());
                    appendElement(sb, INDENT6, "error-on-undeclared-namespace",
                            jpg.getErrorOnUndeclaredNamespace());
                    sb.append("    </jsp-property-group>\n");
                }
                sb.append("  </jsp-config>\n\n");
            }
        }

        // resource-env-ref was introduced in Servlet 2.3
        if (getMajorVersion() > 2 || getMinorVersion() > 2) {
            for (ContextResourceEnvRef resourceEnvRef : getResourceEnvRefsData().values()) {
                sb.append("  <resource-env-ref>\n");
                appendElement(sb, INDENT4, "description",
                        resourceEnvRef.getDescription());
                appendElement(sb, INDENT4, "resource-env-ref-name",
                        resourceEnvRef.getName());
                appendElement(sb, INDENT4, "resource-env-ref-type",
                        resourceEnvRef.getType());
                // TODO mapped-name
                for (InjectionTarget target :
                        resourceEnvRef.getInjectionTargets()) {
                    sb.append("    <injection-target>\n");
                    appendElement(sb, INDENT6, "injection-target-class",
                            target.getTargetClass());
                    appendElement(sb, INDENT6, "injection-target-name",
                            target.getTargetName());
                    sb.append("    </injection-target>\n");
                }
                // TODO lookup-name
                sb.append("  </resource-env-ref>\n");
            }
            sb.append('\n');
        }

        for (ContextResource resourceRef : getResourceRefsData().values()) {
            sb.append("  <resource-ref>\n");
            appendElement(sb, INDENT4, "description",
                    resourceRef.getDescription());
            appendElement(sb, INDENT4, "res-ref-name", resourceRef.getName());
            appendElement(sb, INDENT4, "res-type", resourceRef.getType());
            appendElement(sb, INDENT4, "res-auth", resourceRef.getAuth());
            // resource-ref/res-sharing-scope was introduced in Servlet 2.3
            if (getMajorVersion() > 2 || getMinorVersion() > 2) {
                appendElement(sb, INDENT4, "res-sharing-scope",
                        resourceRef.getScope());
            }
            // TODO mapped-name
            for (InjectionTarget target : resourceRef.getInjectionTargets()) {
                sb.append("    <injection-target>\n");
                appendElement(sb, INDENT6, "injection-target-class",
                        target.getTargetClass());
                appendElement(sb, INDENT6, "injection-target-name",
                        target.getTargetName());
                sb.append("    </injection-target>\n");
            }
            // TODO lookup-name
            sb.append("  </resource-ref>\n");
        }
        sb.append('\n');

        for (SecurityConstraint constraint : getSecurityConstraintsData()) {
            sb.append("  <security-constraint>\n");
            // security-constraint/display-name was introduced in Servlet 2.3
            if (getMajorVersion() > 2 || getMinorVersion() > 2) {
                appendElement(sb, INDENT4, "display-name",
                        constraint.getDisplayName());
            }
            for (SecurityCollection collection : constraint.findCollections()) {
                sb.append("    <web-resource-collection>\n");
                appendElement(sb, INDENT6, "web-resource-name",
                        collection.getName());
                appendElement(sb, INDENT6, "description",
                        collection.getDescription());
                for (String urlPattern : collection.findPatterns()) {
                    appendElement(sb, INDENT6, "url-pattern", urlPattern);
                }
                for (String method : collection.findMethods()) {
                    appendElement(sb, INDENT6, "http-method", method);
                }
                for (String method : collection.findOmittedMethods()) {
                    appendElement(sb, INDENT6, "http-method-omission", method);
                }
                sb.append("    </web-resource-collection>\n");
            }
            if (constraint.findAuthRoles().length > 0) {
                sb.append("    <auth-constraint>\n");
                for (String role : constraint.findAuthRoles()) {
                    appendElement(sb, INDENT6, "role-name", role);
                }
                sb.append("    </auth-constraint>\n");
            }
            if (constraint.getUserConstraint() != null) {
                sb.append("    <user-data-constraint>\n");
                appendElement(sb, INDENT6, "transport-guarantee",
                        constraint.getUserConstraint());
                sb.append("    </user-data-constraint>\n");
            }
            sb.append("  </security-constraint>\n");
        }
        sb.append('\n');

        if (getLoginConfigData() != null) {
            sb.append("  <login-config>\n");
            appendElement(sb, INDENT4, "auth-method",
                    getLoginConfigData().getAuthMethod());
            appendElement(sb,INDENT4, "realm-name",
                    getLoginConfigData().getRealmName());
            if (getLoginConfigData().getErrorPage() != null ||
                        getLoginConfigData().getLoginPage() != null) {
                sb.append("    <form-login-config>\n");
                appendElement(sb, INDENT6, "form-login-page",
                        getLoginConfigData().getLoginPage());
                appendElement(sb, INDENT6, "form-error-page",
                        getLoginConfigData().getErrorPage());
                sb.append("    </form-login-config>\n");
            }
            sb.append("  </login-config>\n\n");
        }

        for (String roleName : getSecurityRolesData()) {
            sb.append("  <security-role>\n");
            appendElement(sb, INDENT4, "role-name", roleName);
            sb.append("  </security-role>\n");
        }

        for (ContextEnvironment envEntry : getEnvEntriesData().values()) {
            sb.append("  <env-entry>\n");
            appendElement(sb, INDENT4, "description",
                    envEntry.getDescription());
            appendElement(sb, INDENT4, "env-entry-name", envEntry.getName());
            appendElement(sb, INDENT4, "env-entry-type", envEntry.getType());
            appendElement(sb, INDENT4, "env-entry-value", envEntry.getValue());
            // TODO mapped-name
            for (InjectionTarget target : envEntry.getInjectionTargets()) {
                sb.append("    <injection-target>\n");
                appendElement(sb, INDENT6, "injection-target-class",
                        target.getTargetClass());
                appendElement(sb, INDENT6, "injection-target-name",
                        target.getTargetName());
                sb.append("    </injection-target>\n");
            }
            // TODO lookup-name
            sb.append("  </env-entry>\n");
        }
        sb.append('\n');

        for (ContextEjb ejbRef : getEjbRefsData().values()) {
            sb.append("  <ejb-ref>\n");
            appendElement(sb, INDENT4, "description", ejbRef.getDescription());
            appendElement(sb, INDENT4, "ejb-ref-name", ejbRef.getName());
            appendElement(sb, INDENT4, "ejb-ref-type", ejbRef.getType());
            appendElement(sb, INDENT4, "home", ejbRef.getHome());
            appendElement(sb, INDENT4, "remote", ejbRef.getRemote());
            appendElement(sb, INDENT4, "ejb-link", ejbRef.getLink());
            // TODO mapped-name
            for (InjectionTarget target : ejbRef.getInjectionTargets()) {
                sb.append("    <injection-target>\n");
                appendElement(sb, INDENT6, "injection-target-class",
                        target.getTargetClass());
                appendElement(sb, INDENT6, "injection-target-name",
                        target.getTargetName());
                sb.append("    </injection-target>\n");
            }
            // TODO lookup-name
            sb.append("  </ejb-ref>\n");
        }
        sb.append('\n');

        // ejb-local-ref was introduced in Servlet 2.3
        if (getMajorVersion() > 2 || getMinorVersion() > 2) {
            for (ContextLocalEjb ejbLocalRef : getEjbLocalRefsData().values()) {
                sb.append("  <ejb-local-ref>\n");
                appendElement(sb, INDENT4, "description",
                        ejbLocalRef.getDescription());
                appendElement(sb, INDENT4, "ejb-ref-name", ejbLocalRef.getName());
                appendElement(sb, INDENT4, "ejb-ref-type", ejbLocalRef.getType());
                appendElement(sb, INDENT4, "local-home", ejbLocalRef.getHome());
                appendElement(sb, INDENT4, "local", ejbLocalRef.getLocal());
                appendElement(sb, INDENT4, "ejb-link", ejbLocalRef.getLink());
                // TODO mapped-name
                for (InjectionTarget target : ejbLocalRef.getInjectionTargets()) {
                    sb.append("    <injection-target>\n");
                    appendElement(sb, INDENT6, "injection-target-class",
                            target.getTargetClass());
                    appendElement(sb, INDENT6, "injection-target-name",
                            target.getTargetName());
                    sb.append("    </injection-target>\n");
                }
                // TODO lookup-name
                sb.append("  </ejb-local-ref>\n");
            }
            sb.append('\n');
        }

        // service-ref was introduced in Servlet 2.4
        if (getMajorVersion() > 2 || getMinorVersion() > 3) {
            for (ContextService serviceRef : getServiceRefsData().values()) {
                sb.append("  <service-ref>\n");
                appendElement(sb, INDENT4, "description",
                        serviceRef.getDescription());
                appendElement(sb, INDENT4, "display-name",
                        serviceRef.getDisplayname());
                appendElement(sb, INDENT4, "service-ref-name",
                        serviceRef.getName());
                appendElement(sb, INDENT4, "service-interface",
                        serviceRef.getInterface());
                appendElement(sb, INDENT4, "service-ref-type",
                        serviceRef.getType());
                appendElement(sb, INDENT4, "wsdl-file", serviceRef.getWsdlfile());
                appendElement(sb, INDENT4, "jaxrpc-mapping-file",
                        serviceRef.getJaxrpcmappingfile());
                String qname = serviceRef.getServiceqnameNamespaceURI();
                if (qname != null) {
                    qname = qname + ":";
                }
                qname = qname + serviceRef.getServiceqnameLocalpart();
                appendElement(sb, INDENT4, "service-qname", qname);
                Iterator<String> endpointIter = serviceRef.getServiceendpoints();
                while (endpointIter.hasNext()) {
                    String endpoint = endpointIter.next();
                    sb.append("    <port-component-ref>\n");
                    appendElement(sb, INDENT6, "service-endpoint-interface",
                            endpoint);
                    appendElement(sb, INDENT6, "port-component-link",
                            serviceRef.getProperty(endpoint));
                    sb.append("    </port-component-ref>\n");
                }
                Iterator<String> handlerIter = serviceRef.getHandlers();
                while (handlerIter.hasNext()) {
                    String handler = handlerIter.next();
                    sb.append("    <handler>\n");
                    ContextHandler ch = serviceRef.getHandler(handler);
                    appendElement(sb, INDENT6, "handler-name", ch.getName());
                    appendElement(sb, INDENT6, "handler-class",
                            ch.getHandlerclass());
                    sb.append("    </handler>\n");
                }
                // TODO handler-chains
                // TODO mapped-name
                for (InjectionTarget target : serviceRef.getInjectionTargets()) {
                    sb.append("    <injection-target>\n");
                    appendElement(sb, INDENT6, "injection-target-class",
                            target.getTargetClass());
                    appendElement(sb, INDENT6, "injection-target-name",
                            target.getTargetName());
                    sb.append("    </injection-target>\n");
                }
                // TODO lookup-name
                sb.append("  </service-ref>\n");
            }
            sb.append('\n');
        }

        if (!getPostConstructMethodsData().isEmpty()) {
            for (Entry<String, String> entry : getPostConstructMethodsData()
                    .entrySet()) {
                sb.append("  <post-construct>\n");
                appendElement(sb, INDENT4, "lifecycle-callback-class",
                        entry.getKey());
                appendElement(sb, INDENT4, "lifecycle-callback-method",
                        entry.getValue());
                sb.append("  </post-construct>\n");
            }
            sb.append('\n');
        }

        if (!getPreDestroyMethodsData().isEmpty()) {
            for (Entry<String, String> entry : getPreDestroyMethodsData()
                    .entrySet()) {
                sb.append("  <pre-destroy>\n");
                appendElement(sb, INDENT4, "lifecycle-callback-class",
                        entry.getKey());
                appendElement(sb, INDENT4, "lifecycle-callback-method",
                        entry.getValue());
                sb.append("  </pre-destroy>\n");
            }
            sb.append('\n');
        }

        // message-destination-ref, message-destination were introduced in
        // Servlet 2.4
        if (getMajorVersion() > 2 || getMinorVersion() > 3) {
            for (MessageDestinationRef mdr : getMessageDestinationRefsData().values()) {
                sb.append("  <message-destination-ref>\n");
                appendElement(sb, INDENT4, "description", mdr.getDescription());
                appendElement(sb, INDENT4, "message-destination-ref-name",
                        mdr.getName());
                appendElement(sb, INDENT4, "message-destination-type",
                        mdr.getType());
                appendElement(sb, INDENT4, "message-destination-usage",
                        mdr.getUsage());
                appendElement(sb, INDENT4, "message-destination-link",
                        mdr.getLink());
                // TODO mapped-name
                for (InjectionTarget target : mdr.getInjectionTargets()) {
                    sb.append("    <injection-target>\n");
                    appendElement(sb, INDENT6, "injection-target-class",
                            target.getTargetClass());
                    appendElement(sb, INDENT6, "injection-target-name",
                            target.getTargetName());
                    sb.append("    </injection-target>\n");
                }
                // TODO lookup-name
                sb.append("  </message-destination-ref>\n");
            }
            sb.append('\n');

            for (MessageDestination md : getMessageDestinationsData().values()) {
                sb.append("  <message-destination>\n");
                appendElement(sb, INDENT4, "description", md.getDescription());
                appendElement(sb, INDENT4, "display-name", md.getDisplayName());
                appendElement(sb, INDENT4, "message-destination-name",
                        md.getName());
                // TODO mapped-name
                sb.append("  </message-destination>\n");
            }
            sb.append('\n');
        }

        // locale-encoding-mapping-list was introduced in Servlet 2.4
        if (getMajorVersion() > 2 || getMinorVersion() > 3) {
            if (getLocaleEncodingMappingsData().size() > 0) {
                sb.append("  <locale-encoding-mapping-list>\n");
                for (Map.Entry<String, String> entry :
                        getLocaleEncodingMappingsData().entrySet()) {
                    sb.append("    <locale-encoding-mapping>\n");
                    appendElement(sb, INDENT6, "locale", entry.getKey());
                    appendElement(sb, INDENT6, "encoding", entry.getValue());
                    sb.append("    </locale-encoding-mapping>\n");
                }
                sb.append("  </locale-encoding-mapping-list>\n");
            }
        }

        sb.append("</web-app>");
        return sb.toString();
    }

    private static void appendElement(StringBuilder sb, String indent,
            String elementName, String value) {
        if (value == null) {
            return;
        }
        if (value.length() == 0) {
            sb.append(indent);
            sb.append('<');
            sb.append(elementName);
            sb.append("/>\n");
        } else {
            sb.append(indent);
            sb.append('<');
            sb.append(elementName);
            sb.append('>');
            sb.append(escapeXml(value));
            sb.append("</");
            sb.append(elementName);
            sb.append(">\n");
        }
    }

    private static void appendElement(StringBuilder sb, String indent,
            String elementName, Object value) {
        if (value == null) return;
        appendElement(sb, indent, elementName, value.toString());
    }


    /**
     * Escape the 5 entities defined by XML.
     */
    private static String escapeXml(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }


    /**
     * Configure a {@link Context} using the stored web.xml representation.
     *
     * @param context   The context to be configured
     */
    public void configureContext(Context context) {
        // As far as possible, process in alphabetical order so it is easy to
        // check everything is present
        // Some validation depends on correct public ID
        context.setPublicId(getPublicIdData());

        // Everything else in order
        context.setEffectiveMajorVersion(getMajorVersion());
        context.setEffectiveMinorVersion(getMinorVersion());

        for (Entry<String, String> entry : getContextParamsData().entrySet()) {
            context.addParameter(entry.getKey(), entry.getValue());
        }
        context.setDisplayName(getDisplayNameData());
        context.setDistributable(isDistributableData());
        for (ContextLocalEjb ejbLocalRef : getEjbLocalRefsData().values()) {
            context.getNamingResources().addLocalEjb(ejbLocalRef);
        }
        for (ContextEjb ejbRef : getEjbRefsData().values()) {
            context.getNamingResources().addEjb(ejbRef);
        }
        for (ContextEnvironment environment : getEnvEntriesData().values()) {
            context.getNamingResources().addEnvironment(environment);
        }
        for (ErrorPage errorPage : getErrorPagesData().values()) {
            context.addErrorPage(errorPage);
        }
        for (FilterDef filter : getFiltersData().values()) {
            if (filter.getAsyncSupported() == null) {
                filter.setAsyncSupported("false");
            }
            context.addFilterDef(filter);
        }
        for (FilterMap filterMap : getFilterMapsData()) {
            context.addFilterMap(filterMap);
        }
        for (JspPropertyGroup jspPropertyGroup : getJspPropertyGroupsData()) {
            JspPropertyGroupDescriptor descriptor =
                new ApplicationJspPropertyGroupDescriptor(jspPropertyGroup);
            context.getJspConfigDescriptor().getJspPropertyGroups().add(
                    descriptor);
        }
        for (String listener : getListenersData()) {
            context.addApplicationListener(listener);
        }
        for (Entry<String, String> entry : getLocaleEncodingMappingsData().entrySet()) {
            context.addLocaleEncodingMappingParameter(entry.getKey(),
                    entry.getValue());
        }
        // Prevents IAE
        if (getLoginConfigData() != null) {
            context.setLoginConfig(getLoginConfigData());
        }
        for (MessageDestinationRef mdr : getMessageDestinationRefsData().values()) {
            context.getNamingResources().addMessageDestinationRef(mdr);
        }

        // messageDestinations were ignored in Tomcat 6, so ignore here

        context.setIgnoreAnnotations(isMetadataCompleteData());
        for (Entry<String, String> entry : getMimeMappingsData().entrySet()) {
            context.addMimeMapping(entry.getKey(), entry.getValue());
        }
        // Name is just used for ordering
        for (ContextResourceEnvRef resource : getResourceEnvRefsData().values()) {
            context.getNamingResources().addResourceEnvRef(resource);
        }
        for (ContextResource resource : getResourceRefsData().values()) {
            context.getNamingResources().addResource(resource);
        }
        for (SecurityConstraint constraint : getSecurityConstraintsData()) {
            context.addConstraint(constraint);
        }
        for (String role : getSecurityRolesData()) {
            context.addSecurityRole(role);
        }
        for (ContextService service : getServiceRefsData().values()) {
            context.getNamingResources().addService(service);
        }
        for (ServletDef servlet : getServletsData().values()) {
            Wrapper wrapper = context.createWrapper();
            // Description is ignored
            // Display name is ignored
            // Icons are ignored

            // jsp-file gets passed to the JSP Servlet as an init-param

            if (servlet.getLoadOnStartup() != null) {
                wrapper.setLoadOnStartup(servlet.getLoadOnStartup().intValue());
            }
            if (servlet.getEnabled() != null) {
                wrapper.setEnabled(servlet.getEnabled().booleanValue());
            }
            wrapper.setName(servlet.getServletName());
            Map<String,String> params = servlet.getParameterMap();
            for (Entry<String, String> entry : params.entrySet()) {
                wrapper.addInitParameter(entry.getKey(), entry.getValue());
            }
            wrapper.setRunAs(servlet.getRunAs());
            Set<SecurityRoleRef> roleRefs = servlet.getSecurityRoleRefs();
            for (SecurityRoleRef roleRef : roleRefs) {
                wrapper.addSecurityReference(
                        roleRef.getName(), roleRef.getLink());
            }
            wrapper.setServletClass(servlet.getServletClass());
            MultipartDef multipartdef = servlet.getMultipartDef();
            if (multipartdef != null) {
                if (multipartdef.getMaxFileSize() != null &&
                        multipartdef.getMaxRequestSize()!= null &&
                        multipartdef.getFileSizeThreshold() != null) {
                    wrapper.setMultipartConfigElement(new MultipartConfigElement(
                            multipartdef.getLocation(),
                            Long.parseLong(multipartdef.getMaxFileSize()),
                            Long.parseLong(multipartdef.getMaxRequestSize()),
                            Integer.parseInt(
                                    multipartdef.getFileSizeThreshold())));
                } else {
                    wrapper.setMultipartConfigElement(new MultipartConfigElement(
                            multipartdef.getLocation()));
                }
            }
            if (servlet.getAsyncSupported() != null) {
                wrapper.setAsyncSupported(
                        servlet.getAsyncSupported().booleanValue());
            }
            wrapper.setOverridable(servlet.isOverridable());
            context.addChild(wrapper);
        }
        for (Entry<String, String> entry : getServletMappingsData().entrySet()) {
            context.addServletMapping(entry.getKey(), entry.getValue());
        }
        if (getSessionConfigData() != null) {
            if (getSessionConfigData().getSessionTimeout() != null) {
                context.setSessionTimeout(
                        getSessionConfigData().getSessionTimeout().intValue());
            }
            SessionCookieConfig scc =
                context.getServletContext().getSessionCookieConfig();
            scc.setName(getSessionConfigData().getCookieName());
            scc.setDomain(getSessionConfigData().getCookieDomain());
            scc.setPath(getSessionConfigData().getCookiePath());
            scc.setComment(getSessionConfigData().getCookieComment());
            if (getSessionConfigData().getCookieHttpOnly() != null) {
                scc.setHttpOnly(getSessionConfigData().getCookieHttpOnly().booleanValue());
            }
            if (getSessionConfigData().getCookieSecure() != null) {
                scc.setSecure(getSessionConfigData().getCookieSecure().booleanValue());
            }
            if (getSessionConfigData().getCookieMaxAge() != null) {
                scc.setMaxAge(getSessionConfigData().getCookieMaxAge().intValue());
            }
            if (getSessionConfigData().getSessionTrackingModes().size() > 0) {
                context.getServletContext().setSessionTrackingModes(
                        getSessionConfigData().getSessionTrackingModes());
            }
        }
        for (Entry<String, String> entry : getTaglibsData().entrySet()) {
            TaglibDescriptor descriptor = new ApplicationTaglibDescriptor(
                    entry.getValue(), entry.getKey());
            context.getJspConfigDescriptor().getTaglibs().add(descriptor);
        }

        // Context doesn't use version directly

        for (String welcomeFile : getWelcomeFilesData()) {
            /*
             * The following will result in a welcome file of "" so don't add
             * that to the context
             * <welcome-file-list>
             *   <welcome-file/>
             * </welcome-file-list>
             */
            if (welcomeFile != null && welcomeFile.length() > 0) {
                context.addWelcomeFile(welcomeFile);
            }
        }

        // Do this last as it depends on servlets
        for (JspPropertyGroup jspPropertyGroup : getJspPropertyGroupsData()) {
            String jspServletName = context.findServletMapping("*.jsp");
            if (jspServletName == null) {
                jspServletName = "jsp";
            }
            if (context.findChild(jspServletName) != null) {
                for (String urlPattern : jspPropertyGroup.getUrlPatterns()) {
                    context.addServletMapping(urlPattern, jspServletName, true);
                }
            } else {
                if(log.isDebugEnabled()) {
                    for (String urlPattern : jspPropertyGroup.getUrlPatterns()) {
                        log.debug("Skiping " + urlPattern + " , no servlet " +
                                jspServletName);
                    }
                }
            }
        }

        for (Entry<String, String> entry : getPostConstructMethodsData().entrySet()) {
            context.addPostConstructMethod(entry.getKey(), entry.getValue());
        }

        for (Entry<String, String> entry : getPreDestroyMethodsData().entrySet()) {
            context.addPreDestroyMethod(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Merge the supplied web fragments into this main web.xml.
     *
     * @param fragments     The fragments to merge in
     * @return <code>true</code> if merge is successful, else
     *         <code>false</code>
     */
    public boolean merge(Set<WebXml> fragments) {
        // As far as possible, process in alphabetical order so it is easy to
        // check everything is present

        // Merge rules vary from element to element. See SRV.8.2.3

        WebXml temp = new WebXml();

        for (WebXml fragment : fragments) {
            if (!mergeMap(fragment.getContextParams(), getContextParamsData(),
                    temp.getContextParams(), fragment, "Context Parameter")) {
                return false;
            }
        }
        getContextParamsData().putAll(temp.getContextParams());

        if (getDisplayNameData() == null) {
            for (WebXml fragment : fragments) {
                String value = fragment.getDisplayName();
                if (value != null) {
                    if (temp.getDisplayName() == null) {
                        temp.setDisplayName(value);
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictDisplayName",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            setDisplayNameData(temp.getDisplayName());
        }

        if (isDistributableData()) {
            for (WebXml fragment : fragments) {
                if (!fragment.isDistributable()) {
                    setDistributableData(false);
                    break;
                }
            }
        }

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getEjbLocalRefs(), getEjbLocalRefsData(),
                    temp.getEjbLocalRefs(), fragment)) {
                return false;
            }
        }
        getEjbLocalRefsData().putAll(temp.getEjbLocalRefs());

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getEjbRefs(), getEjbRefsData(),
                    temp.getEjbRefs(), fragment)) {
                return false;
            }
        }
        getEjbRefsData().putAll(temp.getEjbRefs());

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getEnvEntries(), getEnvEntriesData(),
                    temp.getEnvEntries(), fragment)) {
                return false;
            }
        }
        getEnvEntriesData().putAll(temp.getEnvEntries());

        for (WebXml fragment : fragments) {
            if (!mergeMap(fragment.getErrorPages(), getErrorPagesData(),
                    temp.getErrorPages(), fragment, "Error Page")) {
                return false;
            }
        }
        getErrorPagesData().putAll(temp.getErrorPages());

        // As per 'clarification' from the Servlet EG, filter definitions in the
        // main web.xml override those in fragments and those in fragments
        // override those in annotations
        List<FilterMap> filterMapsToAdd = new ArrayList<FilterMap>();
        for (WebXml fragment : fragments) {
            for (FilterMap filterMap : fragment.getFilterMappings()) {
                if (!getFilterMappingNamesData().contains(filterMap.getFilterName())) {
                    filterMapsToAdd.add(filterMap);
                }
            }
        }
        for (FilterMap filterMap : filterMapsToAdd) {
            // Additive
            addFilterMapping(filterMap);
        }

        for (WebXml fragment : fragments) {
            for (Map.Entry<String,FilterDef> entry :
                    fragment.getFilters().entrySet()) {
                if (getFiltersData().containsKey(entry.getKey())) {
                    mergeFilter(entry.getValue(),
                            getFiltersData().get(entry.getKey()), false);
                } else {
                    if (temp.getFilters().containsKey(entry.getKey())) {
                        if (!(mergeFilter(entry.getValue(),
                                temp.getFilters().get(entry.getKey()), true))) {
                            log.error(sm.getString(
                                    "webXml.mergeConflictFilter",
                                    entry.getKey(),
                                    fragment.getName(),
                                    fragment.getURL()));

                            return false;
                        }
                    } else {
                        temp.getFilters().put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        getFiltersData().putAll(temp.getFilters());

        for (WebXml fragment : fragments) {
            for (JspPropertyGroup jspPropertyGroup :
                    fragment.getJspPropertyGroups()) {
                // Always additive
                addJspPropertyGroup(jspPropertyGroup);
            }
        }

        for (WebXml fragment : fragments) {
            for (String listener : fragment.getListeners()) {
                // Always additive
                addListener(listener);
            }
        }

        for (WebXml fragment : fragments) {
            if (!mergeMap(fragment.getLocalEncodingMappings(),
                    getLocaleEncodingMappingsData(), temp.getLocalEncodingMappings(),
                    fragment, "Locale Encoding Mapping")) {
                return false;
            }
        }
        getLocaleEncodingMappingsData().putAll(temp.getLocalEncodingMappings());

        if (getLoginConfig() == null) {
            LoginConfig tempLoginConfig = null;
            for (WebXml fragment : fragments) {
                LoginConfig fragmentLoginConfig = fragment.getLoginConfigData();
                if (fragmentLoginConfig != null) {
                    if (tempLoginConfig == null ||
                            fragmentLoginConfig.equals(tempLoginConfig)) {
                        tempLoginConfig = fragmentLoginConfig;
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictLoginConfig",
                                fragment.getName(),
                                fragment.getURL()));
                    }
                }
            }
            setLoginConfigDAta(tempLoginConfig);
        }

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getMessageDestinationRefs(), getMessageDestinationRefsData(),
                    temp.getMessageDestinationRefs(), fragment)) {
                return false;
            }
        }
        getMessageDestinationRefsData().putAll(temp.getMessageDestinationRefs());

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getMessageDestinations(), getMessageDestinationsData(),
                    temp.getMessageDestinations(), fragment)) {
                return false;
            }
        }
        getMessageDestinationsData().putAll(temp.getMessageDestinations());

        for (WebXml fragment : fragments) {
            if (!mergeMap(fragment.getMimeMappings(), getMimeMappingsData(),
                    temp.getMimeMappings(), fragment, "Mime Mapping")) {
                return false;
            }
        }
        getMimeMappingsData().putAll(temp.getMimeMappings());

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getResourceEnvRefs(), getResourceEnvRefsData(),
                    temp.getResourceEnvRefs(), fragment)) {
                return false;
            }
        }
        getResourceEnvRefsData().putAll(temp.getResourceEnvRefs());

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getResourceRefs(), getResourceRefsData(),
                    temp.getResourceRefs(), fragment)) {
                return false;
            }
        }
        getResourceRefsData().putAll(temp.getResourceRefs());

        for (WebXml fragment : fragments) {
            for (SecurityConstraint constraint : fragment.getSecurityConstraints()) {
                // Always additive
                addSecurityConstraint(constraint);
            }
        }

        for (WebXml fragment : fragments) {
            for (String role : fragment.getSecurityRoles()) {
                // Always additive
                addSecurityRole(role);
            }
        }

        for (WebXml fragment : fragments) {
            if (!mergeResourceMap(fragment.getServiceRefs(), getServiceRefsData(),
                    temp.getServiceRefs(), fragment)) {
                return false;
            }
        }
        getServiceRefsData().putAll(temp.getServiceRefs());

        // As per 'clarification' from the Servlet EG, servlet definitions and
        // mappings in the main web.xml override those in fragments and those in
        // fragments override those in annotations
        // Skip servlet definitions and mappings from fragments that are
        // defined in web.xml
        List<Map.Entry<String,String>> servletMappingsToAdd =
            new ArrayList<Map.Entry<String,String>>();
        for (WebXml fragment : fragments) {
            for (Map.Entry<String,String> servletMap :
                    fragment.getServletMappings().entrySet()) {
                if (!getServletMappingNamesData().contains(servletMap.getValue()) &&
                        !getServletMappingsData().containsKey(servletMap.getKey())) {
                    servletMappingsToAdd.add(servletMap);
                }
            }
        }

        // Add fragment mappings
        for (Map.Entry<String,String> mapping : servletMappingsToAdd) {
            addServletMapping(mapping.getKey(), mapping.getValue());
        }

        for (WebXml fragment : fragments) {
            for (Map.Entry<String,ServletDef> entry :
                    fragment.getServlets().entrySet()) {
                if (getServletsData().containsKey(entry.getKey())) {
                    mergeServlet(entry.getValue(),
                            getServletsData().get(entry.getKey()), false);
                } else {
                    if (temp.getServlets().containsKey(entry.getKey())) {
                        if (!(mergeServlet(entry.getValue(),
                                temp.getServlets().get(entry.getKey()), true))) {
                            log.error(sm.getString(
                                    "webXml.mergeConflictServlet",
                                    entry.getKey(),
                                    fragment.getName(),
                                    fragment.getURL()));

                            return false;
                        }
                    } else {
                        temp.getServlets().put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        getServletsData().putAll(temp.getServlets());

        if (getSessionConfigData().getSessionTimeout() == null) {
            for (WebXml fragment : fragments) {
                Integer value = fragment.getSessionConfig().getSessionTimeout();
                if (value != null) {
                    if (temp.getSessionConfig().getSessionTimeout() == null) {
                        temp.getSessionConfig().setSessionTimeout(value.toString());
                    } else if (value.equals(
                            temp.getSessionConfig().getSessionTimeout())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionTimeout",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            if (temp.getSessionConfig().getSessionTimeout() != null) {
                getSessionConfigData().setSessionTimeout(
                        temp.getSessionConfig().getSessionTimeout().toString());
            }
        }

        if (getSessionConfigData().getCookieName() == null) {
            for (WebXml fragment : fragments) {
                String value = fragment.getSessionConfig().getCookieName();
                if (value != null) {
                    if (temp.getSessionConfig().getCookieName() == null) {
                        temp.getSessionConfig().setCookieName(value);
                    } else if (value.equals(
                            temp.getSessionConfig().getCookieName())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookieName",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            getSessionConfigData().setCookieName(
                    temp.getSessionConfig().getCookieName());
        }
        if (getSessionConfigData().getCookieDomain() == null) {
            for (WebXml fragment : fragments) {
                String value = fragment.getSessionConfig().getCookieDomain();
                if (value != null) {
                    if (temp.getSessionConfig().getCookieDomain() == null) {
                        temp.getSessionConfig().setCookieDomain(value);
                    } else if (value.equals(
                            temp.getSessionConfig().getCookieDomain())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookieDomain",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            getSessionConfigData().setCookieDomain(
                    temp.getSessionConfig().getCookieDomain());
        }
        if (getSessionConfigData().getCookiePath() == null) {
            for (WebXml fragment : fragments) {
                String value = fragment.getSessionConfig().getCookiePath();
                if (value != null) {
                    if (temp.getSessionConfig().getCookiePath() == null) {
                        temp.getSessionConfig().setCookiePath(value);
                    } else if (value.equals(
                            temp.getSessionConfig().getCookiePath())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookiePath",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            getSessionConfigData().setCookiePath(
                    temp.getSessionConfig().getCookiePath());
        }
        if (getSessionConfigData().getCookieComment() == null) {
            for (WebXml fragment : fragments) {
                String value = fragment.getSessionConfig().getCookieComment();
                if (value != null) {
                    if (temp.getSessionConfig().getCookieComment() == null) {
                        temp.getSessionConfig().setCookieComment(value);
                    } else if (value.equals(
                            temp.getSessionConfig().getCookieComment())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookieComment",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            getSessionConfigData().setCookieComment(
                    temp.getSessionConfig().getCookieComment());
        }
        if (getSessionConfigData().getCookieHttpOnly() == null) {
            for (WebXml fragment : fragments) {
                Boolean value = fragment.getSessionConfig().getCookieHttpOnly();
                if (value != null) {
                    if (temp.getSessionConfig().getCookieHttpOnly() == null) {
                        temp.getSessionConfig().setCookieHttpOnly(value.toString());
                    } else if (value.equals(
                            temp.getSessionConfig().getCookieHttpOnly())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookieHttpOnly",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            if (temp.getSessionConfig().getCookieHttpOnly() != null) {
                getSessionConfigData().setCookieHttpOnly(
                        temp.getSessionConfig().getCookieHttpOnly().toString());
            }
        }
        if (getSessionConfigData().getCookieSecure() == null) {
            for (WebXml fragment : fragments) {
                Boolean value = fragment.getSessionConfig().getCookieSecure();
                if (value != null) {
                    if (temp.getSessionConfig().getCookieSecure() == null) {
                        temp.getSessionConfig().setCookieSecure(value.toString());
                    } else if (value.equals(
                            temp.getSessionConfig().getCookieSecure())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookieSecure",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            if (temp.getSessionConfig().getCookieSecure() != null) {
                getSessionConfigData().setCookieSecure(
                        temp.getSessionConfig().getCookieSecure().toString());
            }
        }
        if (getSessionConfigData().getCookieMaxAge() == null) {
            for (WebXml fragment : fragments) {
                Integer value = fragment.getSessionConfig().getCookieMaxAge();
                if (value != null) {
                    if (temp.getSessionConfig().getCookieMaxAge() == null) {
                        temp.getSessionConfig().setCookieMaxAge(value.toString());
                    } else if (value.equals(
                            temp.getSessionConfig().getCookieMaxAge())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionCookieMaxAge",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            if (temp.getSessionConfig().getCookieMaxAge() != null) {
                getSessionConfigData().setCookieMaxAge(
                        temp.getSessionConfig().getCookieMaxAge().toString());
            }
        }

        if (getSessionConfigData().getSessionTrackingModes().size() == 0) {
            for (WebXml fragment : fragments) {
                EnumSet<SessionTrackingMode> value =
                    fragment.getSessionConfig().getSessionTrackingModes();
                if (value.size() > 0) {
                    if (temp.getSessionConfig().getSessionTrackingModes().size() == 0) {
                        temp.getSessionConfig().getSessionTrackingModes().addAll(value);
                    } else if (value.equals(
                            temp.getSessionConfig().getSessionTrackingModes())) {
                        // Fragments use same value - no conflict
                    } else {
                        log.error(sm.getString(
                                "webXml.mergeConflictSessionTrackingMode",
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                }
            }
            getSessionConfigData().getSessionTrackingModes().addAll(
                    temp.getSessionConfig().getSessionTrackingModes());
        }

        for (WebXml fragment : fragments) {
            if (!mergeMap(fragment.getTaglibs(), getTaglibsData(),
                    temp.getTaglibs(), fragment, "Taglibs")) {
                return false;
            }
        }
        getTaglibsData().putAll(temp.getTaglibs());

        for (WebXml fragment : fragments) {
            if (fragment.isAlwaysAddWelcomeFilesData() || getWelcomeFilesData().size() == 0) {
                for (String welcomeFile : fragment.getWelcomeFiles()) {
                    addWelcomeFile(welcomeFile);
                }
            }
        }

        if (getPostConstructMethodsData().isEmpty()) {
            for (WebXml fragment : fragments) {
                if (!mergeLifecycleCallback(fragment.getPostConstructMethods(),
                        temp.getPostConstructMethods(), fragment,
                        "Post Construct Methods")) {
                    return false;
                }
            }
            getPostConstructMethodsData().putAll(temp.getPostConstructMethods());
        }

        if (getPreDestroyMethodsData().isEmpty()) {
            for (WebXml fragment : fragments) {
                if (!mergeLifecycleCallback(fragment.getPreDestroyMethods(),
                        temp.getPreDestroyMethods(), fragment,
                        "Pre Destroy Methods")) {
                    return false;
                }
            }
            getPreDestroyMethodsData().putAll(temp.getPreDestroyMethods());
        }

        return true;
    }

    private static <T extends ResourceBase> boolean mergeResourceMap(
            Map<String, T> fragmentResources, Map<String, T> mainResources,
            Map<String, T> tempResources, WebXml fragment) {
        for (T resource : fragmentResources.values()) {
            String resourceName = resource.getName();
            if (mainResources.containsKey(resourceName)) {
                mainResources.get(resourceName).getInjectionTargets().addAll(
                        resource.getInjectionTargets());
            } else {
                // Not defined in main web.xml
                T existingResource = tempResources.get(resourceName);
                if (existingResource != null) {
                    if (!existingResource.equals(resource)) {
                        log.error(sm.getString(
                                "webXml.mergeConflictResource",
                                resourceName,
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                } else {
                    tempResources.put(resourceName, resource);
                }
            }
        }
        return true;
    }

    private static <T> boolean mergeMap(Map<String,T> fragmentMap,
            Map<String,T> mainMap, Map<String,T> tempMap, WebXml fragment,
            String mapName) {
        for (Entry<String, T> entry : fragmentMap.entrySet()) {
            final String key = entry.getKey();
            if (!mainMap.containsKey(key)) {
                // Not defined in main web.xml
                T value = entry.getValue();
                if (tempMap.containsKey(key)) {
                    if (value != null && !value.equals(
                            tempMap.get(key))) {
                        log.error(sm.getString(
                                "webXml.mergeConflictString",
                                mapName,
                                key,
                                fragment.getName(),
                                fragment.getURL()));
                        return false;
                    }
                } else {
                    tempMap.put(key, value);
                }
            }
        }
        return true;
    }

    private static boolean mergeFilter(FilterDef src, FilterDef dest,
            boolean failOnConflict) {
        if (dest.getAsyncSupported() == null) {
            dest.setAsyncSupported(src.getAsyncSupported());
        } else if (src.getAsyncSupported() != null) {
            if (failOnConflict &&
                    !src.getAsyncSupported().equals(dest.getAsyncSupported())) {
                return false;
            }
        }

        if (dest.getFilterClass()  == null) {
            dest.setFilterClass(src.getFilterClass());
        } else if (src.getFilterClass() != null) {
            if (failOnConflict &&
                    !src.getFilterClass().equals(dest.getFilterClass())) {
                return false;
            }
        }

        for (Map.Entry<String,String> srcEntry :
                src.getParameterMap().entrySet()) {
            if (dest.getParameterMap().containsKey(srcEntry.getKey())) {
                if (failOnConflict && !dest.getParameterMap().get(
                        srcEntry.getKey()).equals(srcEntry.getValue())) {
                    return false;
                }
            } else {
                dest.addInitParameter(srcEntry.getKey(), srcEntry.getValue());
            }
        }
        return true;
    }

    private static boolean mergeServlet(ServletDef src, ServletDef dest,
            boolean failOnConflict) {
        // These tests should be unnecessary...
        if (dest.getServletClass() != null && dest.getJspFile() != null) {
            return false;
        }
        if (src.getServletClass() != null && src.getJspFile() != null) {
            return false;
        }


        if (dest.getServletClass() == null && dest.getJspFile() == null) {
            dest.setServletClass(src.getServletClass());
            dest.setJspFile(src.getJspFile());
        } else if (failOnConflict) {
            if (src.getServletClass() != null &&
                    (dest.getJspFile() != null ||
                            !src.getServletClass().equals(dest.getServletClass()))) {
                return false;
            }
            if (src.getJspFile() != null &&
                    (dest.getServletClass() != null ||
                            !src.getJspFile().equals(dest.getJspFile()))) {
                return false;
            }
        }

        // Additive
        for (SecurityRoleRef securityRoleRef : src.getSecurityRoleRefs()) {
            dest.addSecurityRoleRef(securityRoleRef);
        }

        if (dest.getLoadOnStartup() == null) {
            if (src.getLoadOnStartup() != null) {
                dest.setLoadOnStartup(src.getLoadOnStartup().toString());
            }
        } else if (src.getLoadOnStartup() != null) {
            if (failOnConflict &&
                    !src.getLoadOnStartup().equals(dest.getLoadOnStartup())) {
                return false;
            }
        }

        if (dest.getEnabled() == null) {
            if (src.getEnabled() != null) {
                dest.setEnabled(src.getEnabled().toString());
            }
        } else if (src.getEnabled() != null) {
            if (failOnConflict &&
                    !src.getEnabled().equals(dest.getEnabled())) {
                return false;
            }
        }

        for (Map.Entry<String,String> srcEntry :
                src.getParameterMap().entrySet()) {
            if (dest.getParameterMap().containsKey(srcEntry.getKey())) {
                if (failOnConflict && !dest.getParameterMap().get(
                        srcEntry.getKey()).equals(srcEntry.getValue())) {
                    return false;
                }
            } else {
                dest.addInitParameter(srcEntry.getKey(), srcEntry.getValue());
            }
        }

        if (dest.getMultipartDef() == null) {
            dest.setMultipartDef(src.getMultipartDef());
        } else if (src.getMultipartDef() != null) {
            return mergeMultipartDef(src.getMultipartDef(),
                    dest.getMultipartDef(), failOnConflict);
        }

        if (dest.getAsyncSupported() == null) {
            if (src.getAsyncSupported() != null) {
                dest.setAsyncSupported(src.getAsyncSupported().toString());
            }
        } else if (src.getAsyncSupported() != null) {
            if (failOnConflict &&
                    !src.getAsyncSupported().equals(dest.getAsyncSupported())) {
                return false;
            }
        }

        return true;
    }

    private static boolean mergeMultipartDef(MultipartDef src, MultipartDef dest,
            boolean failOnConflict) {

        if (dest.getLocation() == null) {
            dest.setLocation(src.getLocation());
        } else if (src.getLocation() != null) {
            if (failOnConflict &&
                    !src.getLocation().equals(dest.getLocation())) {
                return false;
            }
        }

        if (dest.getFileSizeThreshold() == null) {
            dest.setFileSizeThreshold(src.getFileSizeThreshold());
        } else if (src.getFileSizeThreshold() != null) {
            if (failOnConflict &&
                    !src.getFileSizeThreshold().equals(
                            dest.getFileSizeThreshold())) {
                return false;
            }
        }

        if (dest.getMaxFileSize() == null) {
            dest.setMaxFileSize(src.getMaxFileSize());
        } else if (src.getMaxFileSize() != null) {
            if (failOnConflict &&
                    !src.getMaxFileSize().equals(dest.getMaxFileSize())) {
                return false;
            }
        }

        if (dest.getMaxRequestSize() == null) {
            dest.setMaxRequestSize(src.getMaxRequestSize());
        } else if (src.getMaxRequestSize() != null) {
            if (failOnConflict &&
                    !src.getMaxRequestSize().equals(
                            dest.getMaxRequestSize())) {
                return false;
            }
        }

        return true;
    }


    private static <T> boolean mergeLifecycleCallback(
            Map<String, String> fragmentMap, Map<String, String> tempMap,
            WebXml fragment, String mapName) {
        for (Entry<String, String> entry : fragmentMap.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (tempMap.containsKey(key)) {
                if (value != null && !value.equals(tempMap.get(key))) {
                    log.error(sm.getString("webXml.mergeConflictString",
                            mapName, key, fragment.getName(), fragment.getURL()));
                    return false;
                }
            } else {
                tempMap.put(key, value);
            }
        }
        return true;
    }


    /**
     * Generates the sub-set of the web-fragment.xml files to be processed in
     * the order that the fragments must be processed as per the rules in the
     * Servlet spec.
     *
     * @param application    The application web.xml file
     * @param fragments      The map of fragment names to web fragments
     * @param servletContext The servlet context the fragments are associated
     *                       with
     * @return Ordered list of web-fragment.xml files to process
     */
    public static Set<WebXml> orderWebFragments(WebXml application,
            Map<String,WebXml> fragments, ServletContext servletContext) {

        Set<WebXml> orderedFragments = new LinkedHashSet<WebXml>();

        boolean absoluteOrdering =
            (application.getAbsoluteOrdering() != null);
        boolean orderingPresent = false;

        if (absoluteOrdering) {
            orderingPresent = true;
            // Only those fragments listed should be processed
            Set<String> requestedOrder = application.getAbsoluteOrdering();

            for (String requestedName : requestedOrder) {
                if (WebXml.ORDER_OTHERS.equals(requestedName)) {
                    // Add all fragments not named explicitly at this point
                    for (Entry<String, WebXml> entry : fragments.entrySet()) {
                        if (!requestedOrder.contains(entry.getKey())) {
                            WebXml fragment = entry.getValue();
                            if (fragment != null) {
                                orderedFragments.add(fragment);
                            }
                        }
                    }
                } else {
                    WebXml fragment = fragments.get(requestedName);
                    if (fragment != null) {
                        orderedFragments.add(fragment);
                    } else {
                        log.warn(sm.getString("webXml.wrongFragmentName",requestedName));
                    }
                }
            }
        } else {
            // Stage 1. Make all dependencies bi-directional - this makes the
            //          next stage simpler.
            for (WebXml fragment : fragments.values()) {
                Iterator<String> before =
                        fragment.getBeforeOrdering().iterator();
                while (before.hasNext()) {
                    orderingPresent = true;
                    String beforeEntry = before.next();
                    if (!beforeEntry.equals(ORDER_OTHERS)) {
                        WebXml beforeFragment = fragments.get(beforeEntry);
                        if (beforeFragment == null) {
                            before.remove();
                        } else {
                            beforeFragment.addAfterOrdering(fragment.getName());
                        }
                    }
                }
                Iterator<String> after = fragment.getAfterOrdering().iterator();
                while (after.hasNext()) {
                    orderingPresent = true;
                    String afterEntry = after.next();
                    if (!afterEntry.equals(ORDER_OTHERS)) {
                        WebXml afterFragment = fragments.get(afterEntry);
                        if (afterFragment == null) {
                            after.remove();
                        } else {
                            afterFragment.addBeforeOrdering(fragment.getName());
                        }
                    }
                }
            }

            // Stage 2. Make all fragments that are implicitly before/after
            //          others explicitly so. This is iterative so the next
            //          stage doesn't have to be.
            for (WebXml fragment : fragments.values()) {
                if (fragment.getBeforeOrdering().contains(ORDER_OTHERS)) {
                    makeBeforeOthersExplicit(fragment.getAfterOrdering(), fragments);
                }
                if (fragment.getAfterOrdering().contains(ORDER_OTHERS)) {
                    makeAfterOthersExplicit(fragment.getBeforeOrdering(), fragments);
                }
            }

            // Stage 3. Separate into three groups
            Set<WebXml> beforeSet = new HashSet<WebXml>();
            Set<WebXml> othersSet = new HashSet<WebXml>();
            Set<WebXml> afterSet = new HashSet<WebXml>();

            for (WebXml fragment : fragments.values()) {
                if (fragment.getBeforeOrdering().contains(ORDER_OTHERS)) {
                    beforeSet.add(fragment);
                    fragment.getBeforeOrdering().remove(ORDER_OTHERS);
                } else if (fragment.getAfterOrdering().contains(ORDER_OTHERS)) {
                    afterSet.add(fragment);
                    fragment.getAfterOrdering().remove(ORDER_OTHERS);
                } else {
                    othersSet.add(fragment);
                }
            }

            // Stage 4. Decouple the groups so the ordering requirements for
            //          each fragment in the group only refer to other fragments
            //          in the group. Ordering requirements outside the group
            //          will be handled by processing the groups in order.
            //          Note: Only after ordering requirements are considered.
            //                This is OK because of the processing in stage 1.
            decoupleOtherGroups(beforeSet);
            decoupleOtherGroups(othersSet);
            decoupleOtherGroups(afterSet);

            // Stage 5. Order each group
            //          Note: Only after ordering requirements are considered.
            //                This is OK because of the processing in stage 1.
            orderFragments(orderedFragments, beforeSet);
            orderFragments(orderedFragments, othersSet);
            orderFragments(orderedFragments, afterSet);
        }

        // Avoid NPE when unit testing
        if (servletContext != null) {
            // Publish the ordered fragments
            List<String> orderedJarFileNames = null;
            if (orderingPresent) {
                orderedJarFileNames = new ArrayList<String>();
                for (WebXml fragment: orderedFragments) {
                    orderedJarFileNames.add(fragment.getJarName());
                }
            }
            servletContext.setAttribute(ServletContext.ORDERED_LIBS,
                    orderedJarFileNames);
        }

        return orderedFragments;
    }

    private static void decoupleOtherGroups(Set<WebXml> group) {
        Set<String> names = new HashSet<String>();
        for (WebXml fragment : group) {
            names.add(fragment.getName());
        }
        for (WebXml fragment : group) {
            Iterator<String> after = fragment.getAfterOrdering().iterator();
            while (after.hasNext()) {
                String entry = after.next();
                if (!names.contains(entry)) {
                    after.remove();
                }
            }
        }
    }
    private static void orderFragments(Set<WebXml> orderedFragments,
            Set<WebXml> unordered) {
        Set<WebXml> addedThisRound = new HashSet<WebXml>();
        Set<WebXml> addedLastRound = new HashSet<WebXml>();
        while (unordered.size() > 0) {
            Iterator<WebXml> source = unordered.iterator();
            while (source.hasNext()) {
                WebXml fragment = source.next();
                for (WebXml toRemove : addedLastRound) {
                    fragment.getAfterOrdering().remove(toRemove.getName());
                }
                if (fragment.getAfterOrdering().isEmpty()) {
                    addedThisRound.add(fragment);
                    orderedFragments.add(fragment);
                    source.remove();
                }
            }
            if (addedThisRound.size() == 0) {
                // Circular
                throw new IllegalArgumentException(
                        sm.getString("webXml.mergeConflictOrder"));
            }
            addedLastRound.clear();
            addedLastRound.addAll(addedThisRound);
            addedThisRound.clear();
        }
    }

    private static void makeBeforeOthersExplicit(Set<String> beforeOrdering,
            Map<String, WebXml> fragments) {
        for (String before : beforeOrdering) {
            if (!before.equals(ORDER_OTHERS)) {
                WebXml webXml = fragments.get(before);
                if (!webXml.getBeforeOrdering().contains(ORDER_OTHERS)) {
                    webXml.addBeforeOrderingOthers();
                    makeBeforeOthersExplicit(webXml.getAfterOrdering(), fragments);
                }
            }
        }
    }

    private static void makeAfterOthersExplicit(Set<String> afterOrdering,
            Map<String, WebXml> fragments) {
        for (String after : afterOrdering) {
            if (!after.equals(ORDER_OTHERS)) {
                WebXml webXml = fragments.get(after);
                if (!webXml.getAfterOrdering().contains(ORDER_OTHERS)) {
                    webXml.addAfterOrderingOthers();
                    makeAfterOthersExplicit(webXml.getBeforeOrdering(), fragments);
                }
            }
        }
    }
	public boolean isOverridableData() {
		return overridable;
	}
	public void setOverridableData(boolean overridable) {
		this.overridable = overridable;
	}
	public Set<String> getAbsoluteOrderingData() {
		return absoluteOrdering;
	}
	public void setAbsoluteOrderingData(Set<String> absoluteOrdering) {
		this.absoluteOrdering = absoluteOrdering;
	}
	public Set<String> getAfterData() {
		return after;
	}
	public void setAfterData(Set<String> after) {
		this.after = after;
	}
	public Set<String> getBeforeData() {
		return before;
	}
	public void setBeforeData(Set<String> before) {
		this.before = before;
	}
	public String getPublicIdData() {
		return publicId;
	}
	public void setPublicIdData(String publicId) {
		this.publicId = publicId;
	}
	public boolean isMetadataCompleteData() {
		return metadataComplete;
	}
	public void setMetadataCompleteData(boolean metadataComplete) {
		this.metadataComplete = metadataComplete;
	}
	public String getNameData() {
		return name;
	}
	public void setNameData(String name) {
		this.name = name;
	}
	public int getMinorVersionData() {
		return minorVersion;
	}
	public void setMinorVersionData(int minorVersion) {
		this.minorVersion = minorVersion;
	}
	public int getMajorVersionData() {
		return majorVersion;
	}
	public void setMajorVersionData(int majorVersion) {
		this.majorVersion = majorVersion;
	}
	public String getDisplayNameData() {
		return displayName;
	}
	public void setDisplayNameData(String displayName) {
		this.displayName = displayName;
	}
	public boolean isDistributableData() {
		return distributable;
	}
	public void setDistributableData(boolean distributable) {
		this.distributable = distributable;
	}
	private Map<String,String> getContextParamsData() {
		return contextParams;
	}
	private void setContextParamsData(Map<String,String> contextParams) {
		this.contextParams = contextParams;
	}
	public Map<String,FilterDef> getFiltersData() {
		return filters;
	}
	public void setFiltersData(Map<String,FilterDef> filters) {
		this.filters = filters;
	}
	public Set<FilterMap> getFilterMapsData() {
		return filterMaps;
	}
	public void setFilterMapsData(Set<FilterMap> filterMaps) {
		this.filterMaps = filterMaps;
	}
	public Set<String> getFilterMappingNamesData() {
		return filterMappingNames;
	}
	public void setFilterMappingNamesData(Set<String> filterMappingNames) {
		this.filterMappingNames = filterMappingNames;
	}
	public Set<String> getListenersData() {
		return listeners;
	}
	public void setListenersData(Set<String> listeners) {
		this.listeners = listeners;
	}
	private Map<String,ServletDef> getServletsData() {
		return servlets;
	}
	private void setServletsData(Map<String,ServletDef> servlets) {
		this.servlets = servlets;
	}
	public Map<String,String> getServletMappingsData() {
		return servletMappings;
	}
	public void setServletMappingsData(Map<String,String> servletMappings) {
		this.servletMappings = servletMappings;
	}
	public Set<String> getServletMappingNamesData() {
		return servletMappingNames;
	}
	public void setServletMappingNamesData(Set<String> servletMappingNames) {
		this.servletMappingNames = servletMappingNames;
	}
	public SessionConfig getSessionConfigData() {
		return sessionConfig;
	}
	public void setSessionConfigData(SessionConfig sessionConfig) {
		this.sessionConfig = sessionConfig;
	}
	public Map<String,String> getMimeMappingsData() {
		return mimeMappings;
	}
	public void setMimeMappingsData(Map<String,String> mimeMappings) {
		this.mimeMappings = mimeMappings;
	}
	public boolean isReplaceWelcomeFilesData() {
		return replaceWelcomeFiles;
	}
	public void setReplaceWelcomeFilesData(boolean replaceWelcomeFiles) {
		this.replaceWelcomeFiles = replaceWelcomeFiles;
	}
	public boolean isAlwaysAddWelcomeFilesData() {
		return alwaysAddWelcomeFiles;
	}
	public void setAlwaysAddWelcomeFilesData(boolean alwaysAddWelcomeFiles) {
		this.alwaysAddWelcomeFiles = alwaysAddWelcomeFiles;
	}
	public Set<String> getWelcomeFilesData() {
		return welcomeFiles;
	}
	public void setWelcomeFilesData(Set<String> welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
	}
	public Map<String,ErrorPage> getErrorPagesData() {
		return errorPages;
	}
	public void setErrorPagesData(Map<String,ErrorPage> errorPages) {
		this.errorPages = errorPages;
	}
	public Map<String,String> getTaglibsData() {
		return taglibs;
	}
	public void setTaglibsData(Map<String,String> taglibs) {
		this.taglibs = taglibs;
	}
	public Set<JspPropertyGroup> getJspPropertyGroupsData() {
		return jspPropertyGroups;
	}
	public void setJspPropertyGroupsData(Set<JspPropertyGroup> jspPropertyGroups) {
		this.jspPropertyGroups = jspPropertyGroups;
	}
	public Set<SecurityConstraint> getSecurityConstraintsData() {
		return securityConstraints;
	}
	public void setSecurityConstraintsData(Set<SecurityConstraint> securityConstraints) {
		this.securityConstraints = securityConstraints;
	}
	public LoginConfig getLoginConfigData() {
		return loginConfig;
	}
	public void setLoginConfigDAta(LoginConfig loginConfig) {
		this.loginConfig = loginConfig;
	}
	public Set<String> getSecurityRolesData() {
		return securityRoles;
	}
	public void setSecurityRolesData(Set<String> securityRoles) {
		this.securityRoles = securityRoles;
	}
	public Map<String,ContextEnvironment> getEnvEntriesData() {
		return envEntries;
	}
	public void setEnvEntriesData(Map<String,ContextEnvironment> envEntries) {
		this.envEntries = envEntries;
	}
	public Map<String,ContextEjb> getEjbRefsData() {
		return ejbRefs;
	}
	public void setEjbRefsData(Map<String,ContextEjb> ejbRefs) {
		this.ejbRefs = ejbRefs;
	}
	public Map<String,ContextLocalEjb> getEjbLocalRefsData() {
		return ejbLocalRefs;
	}
	public void setEjbLocalRefsData(Map<String,ContextLocalEjb> ejbLocalRefs) {
		this.ejbLocalRefs = ejbLocalRefs;
	}
	public Map<String,ContextService> getServiceRefsData() {
		return serviceRefs;
	}
	public void setServiceRefsData(Map<String,ContextService> serviceRefs) {
		this.serviceRefs = serviceRefs;
	}
	public Map<String,ContextResource> getResourceRefsData() {
		return resourceRefs;
	}
	public void setResourceRefsData(Map<String,ContextResource> resourceRefs) {
		this.resourceRefs = resourceRefs;
	}
	public Map<String,ContextResourceEnvRef> getResourceEnvRefsData() {
		return resourceEnvRefs;
	}
	public void setResourceEnvRefsData(Map<String,ContextResourceEnvRef> resourceEnvRefs) {
		this.resourceEnvRefs = resourceEnvRefs;
	}
	public Map<String,MessageDestinationRef> getMessageDestinationRefsData() {
		return messageDestinationRefs;
	}
	public void setMessageDestinationRefsData(Map<String,MessageDestinationRef> messageDestinationRefs) {
		this.messageDestinationRefs = messageDestinationRefs;
	}
	public Map<String,MessageDestination> getMessageDestinationsData() {
		return messageDestinations;
	}
	public void setMessageDestinationsData(Map<String,MessageDestination> messageDestinations) {
		this.messageDestinations = messageDestinations;
	}
	public Map<String,String> getLocaleEncodingMappingsData() {
		return localeEncodingMappings;
	}
	public void setLocaleEncodingMappingsData(Map<String,String> localeEncodingMappings) {
		this.localeEncodingMappings = localeEncodingMappings;
	}
	public Map<String, String> getPostConstructMethodsData() {
		return postConstructMethods;
	}
	public void setPostConstructMethodsData(Map<String, String> postConstructMethods) {
		this.postConstructMethods = postConstructMethods;
	}
	public Map<String, String> getPreDestroyMethodsData() {
		return preDestroyMethods;
	}
	public void setPreDestroyMethodsData(Map<String, String> preDestroyMethods) {
		this.preDestroyMethods = preDestroyMethods;
	}
	public URL getuRLData() {
		return uRL;
	}
	public void setuRLData(URL uRL) {
		this.uRL = uRL;
	}
	public String getJarNameData() {
		return jarName;
	}
	public void setJarNameData(String jarName) {
		this.jarName = jarName;
	}
}
