/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.tomcat.util.http.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.catalina.Host;
import org.apache.tomcat.util.buf.Ascii;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.res.StringManager3;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.naming.Context;

/**
 * Mapper, which implements the servlet API mapping rules (which are derived
 * from the HTTP rules).
 *
 * @author Remy Maucherat
 */
public final class Mapper {


    private static final Log log = LogFactory.getLog(Mapper.class);

    private static final StringManager3 sm =
        StringManager3.getManager(Mapper.class.getPackage().getName());

    // ----------------------------------------------------- Instance Variables


    /**
     * Array containing the virtual hosts definitions.
     */
    private MapperHost[] hosts = new MapperHost[0];


    /**
     * Default host name.
     */
    private String defaultHostName = null;

    /**
     * ContextVersion associated with this Mapper, used for wrapper mapping.
     *
     * <p>
     * It is used only by Mapper in a Context. Is not used by Mapper in a
     * Connector.
     *
     * @see #setContext(String, String[], javax.naming.Context)
     */
    private MapperContextVersion context = new MapperContextVersion();


    // --------------------------------------------------------- Public Methods


    /**
     * Set default host.
     *
     * @param defaultHostName Default host name
     */
    public void setDefaultHostName(String defaultHostName) {
        this.defaultHostName = defaultHostName;
    }

    /**
     * Add a new host to the mapper.
     *
     * @param name Virtual host name
     * @param aliases Alias names for the virtual host
     * @param host Host object
     */
    public synchronized void addHost(String name, String[] aliases,
                                     Object host) {
    	MapperHost[] newHosts = new MapperHost[hosts.length + 1];
    	MapperHost newHost = new MapperHost(name, host);
        if (insertMap(hosts, newHosts, newHost)) {
            hosts = newHosts;
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("mapper.addHost.success", name));
            }
        } else {
        	MapperHost duplicate = hosts[find(hosts, name)];
            if (duplicate.getObject() == host) {
                // The host is already registered in the mapper.
                // E.g. it might have been added by addContextVersion()
                if (log.isDebugEnabled()) {
                    log.debug(sm.getString("mapper.addHost.sameHost", name));
                }
                newHost = duplicate;
            } else {
                log.error(sm.getString("mapper.duplicateHost", name,
                        duplicate.getRealHostName()));
                // Do not add aliases, as removeHost(hostName) won't be able to
                // remove them
                return;
            }
        }
        List<MapperHost> newAliases = new ArrayList<MapperHost>(aliases.length);
        for (String alias : aliases) {
        	MapperHost newAlias = new MapperHost(alias, newHost);
            if (addHostAliasImpl(newAlias)) {
                newAliases.add(newAlias);
            }
        }
        newHost.addAliases(newAliases);
    }


    /**
     * Remove a host from the mapper.
     *
     * @param name Virtual host name
     */
    public synchronized void removeHost(String name) {
        // Find and remove the old host
    	MapperHost host = exactFind(hosts, name);
        if (host == null || host.isAlias()) {
            return;
        }
        MapperHost[] newHosts = hosts.clone();
        // Remove real host and all its aliases
        int j = 0;
        for (int i = 0; i < newHosts.length; i++) {
            if (newHosts[i].getRealHost() != host) {
                newHosts[j++] = newHosts[i];
            }
        }
        hosts = Arrays.copyOf(newHosts, j);
    }

    /**
     * Add an alias to an existing host.
     * @param name  The name of the host
     * @param alias The alias to add
     */
    public synchronized void addHostAlias(String name, String alias) {
    	MapperHost realHost = exactFind(hosts, name);
        if (realHost == null) {
            // Should not be adding an alias for a host that doesn't exist but
            // just in case...
            return;
        }
        MapperHost newAlias = new MapperHost(alias, realHost);
        if (addHostAliasImpl(newAlias)) {
            realHost.addAlias(newAlias);
        }
    }

    private boolean addHostAliasImpl(MapperHost newAlias) {
    	MapperHost[] newHosts = new MapperHost[hosts.length + 1];
        if (insertMap(hosts, newHosts, newAlias)) {
            hosts = newHosts;
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("mapper.addHostAlias.success",
                        newAlias.getName(), newAlias.getRealHostName()));
            }
            return true;
        } else {
        	MapperHost duplicate = hosts[find(hosts, newAlias.getName())];
            if (duplicate.getRealHost() == newAlias.getRealHost()) {
                // A duplicate Alias for the same Host.
                // A harmless redundancy. E.g.
                // <Host name="localhost"><Alias>localhost</Alias></Host>
                if (log.isDebugEnabled()) {
                    log.debug(sm.getString("mapper.addHostAlias.sameHost",
                            newAlias.getName(), newAlias.getRealHostName()));
                }
                return false;
            }
            log.error(sm.getString("mapper.duplicateHostAlias", newAlias.getName(),
                    newAlias.getRealHostName(), duplicate.getRealHostName()));
            return false;
        }
    }

    /**
     * Remove a host alias
     * @param alias The alias to remove
     */
    public synchronized void removeHostAlias(String alias) {
        // Find and remove the alias
    	MapperHost host = exactFind(hosts, alias);
        if (host == null || !host.isAlias()) {
            return;
        }
        MapperHost[] newHosts = new MapperHost[hosts.length - 1];
        if (removeMap(hosts, newHosts, alias)) {
            hosts = newHosts;
            host.getRealHost().removeAlias(host);
        }

    }

    /**
     * Replace {@link Host#contextList} field in <code>realHost</code> and
     * all its aliases with a new value.
     */
    private void updateMapperContextList(MapperHost realHost, MapperContextList newMapperContextList) {
        realHost.setContextList(newMapperContextList);
        for (MapperHost alias : realHost.getAliases()) {
            alias.setContextList(newMapperContextList);
        }
    }

    /**
     * Set context, used for wrapper mapping (request dispatcher).
     *
     * @param welcomeResources Welcome files defined for this context
     * @param resources Static resources of the context
     */
    public void setContext(String path, String[] welcomeResources,
                           Context resources) {
        context.setPath(path);
        context.setWelcomeResources(welcomeResources);
        context.setResources(resources);
    }


    /**
     * Add a new Context to an existing Host.
     *
     * @param hostName Virtual host name this context belongs to
     * @param host Host object
     * @param path Context path
     * @param version Context version
     * @param context Context object
     * @param welcomeResources Welcome files defined for this context
     * @param resources Static resources of the context
     * @deprecated Use {@link #addContextVersion(String, Object, String, String, Object, String[], javax.naming.Context, Collection)}
     */
    @Deprecated
    public void addContextVersion(String hostName, Object host, String path,
            String version, Object context, String[] welcomeResources,
            Context resources) {
        addContextVersion(hostName, host, path, version, context,
                welcomeResources, resources, null);
    }

    /**
     * Add a new Context to an existing Host.
     *
     * @param hostName Virtual host name this context belongs to
     * @param host Host object
     * @param path Context path
     * @param version Context version
     * @param context Context object
     * @param welcomeResources Welcome files defined for this context
     * @param resources Static resources of the context
     * @param wrappers Information on wrapper mappings
     */
    public void addContextVersion(String hostName, Object host, String path,
            String version, Object context, String[] welcomeResources,
            Context resources, Collection<WrapperMappingInfo> wrappers) {

    	MapperHost mappedHost = exactFind(hosts, hostName);
        if (mappedHost == null) {
            addHost(hostName, new String[0], host);
            mappedHost = exactFind(hosts, hostName);
            if (mappedHost == null) {
                log.error("No host found: " + hostName);
                return;
            }
        }
        if (mappedHost.isAlias()) {
            log.error("No host found: " + hostName);
            return;
        }
        int slashCount = slashCount(path);
        synchronized (mappedHost) {
        	MapperContextVersion newContextVersion = new MapperContextVersion(version, context);
            newContextVersion.setPath(path);
            newContextVersion.setSlashCount(slashCount);
            newContextVersion.setWelcomeResources(welcomeResources);
            newContextVersion.setResources(resources);
            if (wrappers != null) {
                addWrappers(newContextVersion, wrappers);
            }

            MapperContextList contextList = mappedHost.getContextList();
            MapperContext mappedContext = exactFind(contextList.getContexts(), path);
            if (mappedContext == null) {
                mappedContext = new MapperContext(path, newContextVersion);
                MapperContextList newMapperContextList = contextList.addContext(
                        mappedContext, slashCount);
                if (newMapperContextList != null) {
                    updateMapperContextList(mappedHost, newMapperContextList);
                }
            } else {
            	MapperContextVersion[] contextVersions = mappedContext.getVersions();
            	MapperContextVersion[] newContextVersions =
                    new MapperContextVersion[contextVersions.length + 1];
                if (insertMap(contextVersions, newContextVersions, newContextVersion)) {
                    mappedContext.setVersions(newContextVersions);
                } else {
                    // Re-registration after Context.reload()
                    // Replace ContextVersion with the new one
                    int pos = find(contextVersions, version);
                    if (pos >= 0 && contextVersions[pos].getName().equals(version)) {
                        contextVersions[pos] = newContextVersion;
                    }
                }
            }
        }

    }


    /**
     * Remove a context from an existing host.
     *
     * @param hostName Virtual host name this context belongs to
     * @param path Context path
     * @param version Context version
     */
    public void removeContextVersion(String hostName, String path,
            String version) {
    	MapperHost host = exactFind(hosts, hostName);
        if (host == null || host.isAlias()) {
            return;
        }
        synchronized (host) {
            MapperContextList contextList = host.getContextList();
            MapperContext context = exactFind(contextList.getContexts(), path);
            if (context == null) {
                return;
            }

            MapperContextVersion[] contextVersions = context.getVersions();
            MapperContextVersion[] newContextVersions =
                new MapperContextVersion[contextVersions.length - 1];
            if (removeMap(contextVersions, newContextVersions, version)) {
                if (newContextVersions.length == 0) {
                    // Remove the context
                    MapperContextList newMapperContextList = contextList.removeContext(path);
                    if (newMapperContextList != null) {
                        updateMapperContextList(host, newMapperContextList);
                    }
                } else {
                    context.setVersions(newContextVersions);
                }
            }
        }
    }


    /**
     * Mark a context as being reloaded. Reversion of this state is performed
     * by calling <code>addContextVersion(...)</code> when context starts up.
     *
     * @param ctxt      The actual context
     * @param hostName  Virtual host name this context belongs to
     * @param contextPath Context path
     * @param version   Context version
     */
    public void pauseContextVersion(Object ctxt, String hostName,
            String contextPath, String version) {

    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, true);
        if (contextVersion == null || !ctxt.equals(contextVersion.getObject())) {
            return;
        }
        contextVersion.markPaused();
    }


    private MapperContextVersion findContextVersion(String hostName,
            String contextPath, String version, boolean silent) {
    	MapperHost host = exactFind(hosts, hostName);
        if (host == null || host.isAlias()) {
            if (!silent) {
                log.error("No host found: " + hostName);
            }
            return null;
        }
        MapperContext context = exactFind(host.getContextList().getContexts(), contextPath);
        if (context == null) {
            if (!silent) {
                log.error("No context found: " + contextPath);
            }
            return null;
        }
        MapperContextVersion contextVersion = exactFind(context.getVersions(), version);
        if (contextVersion == null) {
            if (!silent) {
                log.error("No context version found: " + contextPath + " "
                        + version);
            }
            return null;
        }
        return contextVersion;
    }


    public void addWrapper(String hostName, String contextPath, String version,
                           String path, Object wrapper, boolean jspWildCard,
                           boolean resourceOnly) {
    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, false);
        if (contextVersion == null) {
            return;
        }
        addWrapper(contextVersion, path, wrapper, jspWildCard, resourceOnly);
    }


    public void addWrapper(String path, Object wrapper, boolean jspWildCard,
            boolean resourceOnly) {
        addWrapper(context, path, wrapper, jspWildCard, resourceOnly);
    }

    public void addWrappers(String hostName, String contextPath,
            String version, Collection<WrapperMappingInfo> wrappers) {
    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, false);
        if (contextVersion == null) {
            return;
        }
        addWrappers(contextVersion, wrappers);
    }

    /**
     * Adds wrappers to the given context.
     *
     * @param contextVersion The context to which to add the wrappers
     * @param wrappers Information on wrapper mappings
     */
    private void addWrappers(MapperContextVersion contextVersion,
            Collection<WrapperMappingInfo> wrappers) {
        for (WrapperMappingInfo wrapper : wrappers) {
            addWrapper(contextVersion, wrapper.getMapping(),
                    wrapper.getWrapper(), wrapper.isJspWildCard(),
                    wrapper.isResourceOnly());
        }
    }

    /**
     * Adds a wrapper to the given context.
     *
     * @param context The context to which to add the wrapper
     * @param path Wrapper mapping
     * @param wrapper The Wrapper object
     * @param jspWildCard true if the wrapper corresponds to the JspServlet
     *   and the mapping path contains a wildcard; false otherwise
     * @param resourceOnly true if this wrapper always expects a physical
     *                     resource to be present (such as a JSP)
     */
    protected void addWrapper(MapperContextVersion context, String path,
            Object wrapper, boolean jspWildCard, boolean resourceOnly) {

        synchronized (context) {
            if (path.endsWith("/*")) {
                // Wildcard wrapper
                String name = path.substring(0, path.length() - 2);
                MapperWrapper newWrapper = new MapperWrapper(name, wrapper, jspWildCard,
                        resourceOnly);
                MapperWrapper[] oldWrappers = context.getWildcardWrappers();
                MapperWrapper[] newWrappers =
                    new MapperWrapper[oldWrappers.length + 1];
                if (insertMap(oldWrappers, newWrappers, newWrapper)) {
                    context.setWildcardWrappers(newWrappers);
                    int slashCount = slashCount(newWrapper.getName());
                    if (slashCount > context.getNesting()) {
                        context.setNesting(slashCount);
                    }
                }
            } else if (path.startsWith("*.")) {
                // Extension wrapper
                String name = path.substring(2);
                MapperWrapper newWrapper = new MapperWrapper(name, wrapper, jspWildCard,
                        resourceOnly);
                MapperWrapper[] oldWrappers = context.getExtensionWrappers();
                MapperWrapper[] newWrappers =
                    new MapperWrapper[oldWrappers.length + 1];
                if (insertMap(oldWrappers, newWrappers, newWrapper)) {
                    context.setExtensionWrappers(newWrappers);
                }
            } else if (path.equals("/")) {
                // Default wrapper
            	MapperWrapper newWrapper = new MapperWrapper("", wrapper, jspWildCard,
                        resourceOnly);
                context.setDefaultWrapper(newWrapper);
            } else {
                // Exact wrapper
                final String name;
                if (path.length() == 0) {
                    // Special case for the Context Root mapping which is
                    // treated as an exact match
                    name = "/";
                } else {
                    name = path;
                }
                MapperWrapper newWrapper = new MapperWrapper(name, wrapper, jspWildCard,
                        resourceOnly);
                MapperWrapper[] oldWrappers = context.getExactWrappers();
                MapperWrapper[] newWrappers =
                    new MapperWrapper[oldWrappers.length + 1];
                if (insertMap(oldWrappers, newWrappers, newWrapper)) {
                    context.setExactWrappers(newWrappers);
                }
            }
        }
    }


    /**
     * Remove a wrapper from the context associated with this wrapper.
     *
     * @param path Wrapper mapping
     */
    public void removeWrapper(String path) {
        removeWrapper(context, path);
    }


    /**
     * Remove a wrapper from an existing context.
     *
     * @param hostName Virtual host name this wrapper belongs to
     * @param contextPath Context path this wrapper belongs to
     * @param path Wrapper mapping
     */
    public void removeWrapper(String hostName, String contextPath,
            String version, String path) {
    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, true);
        if (contextVersion == null || contextVersion.isPaused()) {
            return;
        }
        removeWrapper(contextVersion, path);
    }

    protected void removeWrapper(MapperContextVersion context, String path) {

        if (log.isDebugEnabled()) {
            log.debug(sm.getString("mapper.removeWrapper", context.getName(), path));
        }

        synchronized (context) {
            if (path.endsWith("/*")) {
                // Wildcard wrapper
                String name = path.substring(0, path.length() - 2);
                MapperWrapper[] oldWrappers = context.getWildcardWrappers();
                if (oldWrappers.length == 0) {
                    return;
                }
                MapperWrapper[] newWrappers =
                    new MapperWrapper[oldWrappers.length - 1];
                if (removeMap(oldWrappers, newWrappers, name)) {
                    // Recalculate nesting
                    context.setNesting(0);
                    for (int i = 0; i < newWrappers.length; i++) {
                        int slashCount = slashCount(newWrappers[i].getName());
                        if (slashCount > context.getNesting()) {
                            context.setNesting(slashCount);
                        }
                    }
                    context.setWildcardWrappers(newWrappers);
                }
            } else if (path.startsWith("*.")) {
                // Extension wrapper
                String name = path.substring(2);
                MapperWrapper[] oldWrappers = context.getExtensionWrappers();
                if (oldWrappers.length == 0) {
                    return;
                }
                MapperWrapper[] newWrappers =
                    new MapperWrapper[oldWrappers.length - 1];
                if (removeMap(oldWrappers, newWrappers, name)) {
                    context.setExtensionWrappers(newWrappers);
                }
            } else if (path.equals("/")) {
                // Default wrapper
                context.setDefaultWrapper(null);
            } else {
                // Exact wrapper
                String name;
                if (path.length() == 0) {
                    // Special case for the Context Root mapping which is
                    // treated as an exact match
                    name = "/";
                } else {
                    name = path;
                }
                MapperWrapper[] oldWrappers = context.getExactWrappers();
                if (oldWrappers.length == 0) {
                    return;
                }
                MapperWrapper[] newWrappers =
                    new MapperWrapper[oldWrappers.length - 1];
                if (removeMap(oldWrappers, newWrappers, name)) {
                    context.setExactWrappers(newWrappers);
                }
            }
        }
    }


    /**
     * Add a welcome file to the given context.
     *
     * @param hostName
     * @param contextPath
     * @param welcomeFile
     */
    public void addWelcomeFile(String hostName, String contextPath,
            String version, String welcomeFile) {
    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, false);
        if (contextVersion == null) {
            return;
        }
        int len = contextVersion.getWelcomeResources().length + 1;
        String[] newWelcomeResources = new String[len];
        System.arraycopy(contextVersion.getWelcomeResources(), 0,
                newWelcomeResources, 0, len - 1);
        newWelcomeResources[len - 1] = welcomeFile;
        contextVersion.setWelcomeResources(newWelcomeResources);
    }

    /**
     * Remove a welcome file from the given context.
     *
     * @param hostName
     * @param contextPath
     * @param welcomeFile
     */
    public void removeWelcomeFile(String hostName, String contextPath,
            String version, String welcomeFile) {
    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, false);
        if (contextVersion == null || contextVersion.isPaused()) {
            return;
        }
        int match = -1;
        for (int i = 0; i < contextVersion.getWelcomeResources().length; i++) {
            if (welcomeFile.equals(contextVersion.getWelcomeResources()[i])) {
                match = i;
                break;
            }
        }
        if (match > -1) {
            int len = contextVersion.getWelcomeResources().length - 1;
            String[] newWelcomeResources = new String[len];
            System.arraycopy(contextVersion.getWelcomeResources(), 0,
                    newWelcomeResources, 0, match);
            if (match < len) {
                System.arraycopy(contextVersion.getWelcomeResources(), match + 1,
                        newWelcomeResources, match, len - match);
            }
            contextVersion.setWelcomeResources(newWelcomeResources);
        }
    }

    /**
     * Clear the welcome files for the given context.
     *
     * @param hostName
     * @param contextPath
     */
    public void clearWelcomeFiles(String hostName, String contextPath,
            String version) {
    	MapperContextVersion contextVersion = findContextVersion(hostName,
                contextPath, version, false);
        if (contextVersion == null) {
            return;
        }
        contextVersion.setWelcomeResources(new String[0]);
    }


    /**
     * Map the specified host name and URI, mutating the given mapping data.
     *
     * @param host Virtual host name
     * @param uri URI
     * @param mappingData This structure will contain the result of the mapping
     *                    operation
     */
    public void map(MessageBytes host, MessageBytes uri, String version,
                    MappingData mappingData)
        throws Exception {

        if (host.isNull()) {
            host.getCharChunk().append(defaultHostName);
        }
        host.toChars();
        uri.toChars();
        internalMap(host.getCharChunk(), uri.getCharChunk(), version,
                mappingData);

    }


    /**
     * Map the specified URI relative to the context,
     * mutating the given mapping data.
     *
     * @param uri URI
     * @param mappingData This structure will contain the result of the mapping
     *                    operation
     */
    public void map(MessageBytes uri, MappingData mappingData)
        throws Exception {

        uri.toChars();
        CharChunk uricc = uri.getCharChunk();
        uricc.setLimit(-1);
        internalMapWrapper(context, uricc, mappingData);

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Map the specified URI.
     */
    private final void internalMap(CharChunk host, CharChunk uri,
            String version, MappingData mappingData) throws Exception {

        if (mappingData.getHost() != null) {
            // The legacy code (dating down at least to Tomcat 4.1) just
            // skipped all mapping work in this case. That behaviour has a risk
            // of returning an inconsistent result.
            // I do not see a valid use case for it.
            throw new AssertionError();
        }

        uri.setLimit(-1);

        // Virtual host mapping
        MapperHost[] hosts = this.hosts;
        MapperHost mappedHost = exactFindIgnoreCase(hosts, host);
        if (mappedHost == null) {
            if (defaultHostName == null) {
                return;
            }
            mappedHost = exactFind(hosts, defaultHostName);
            if (mappedHost == null) {
                return;
            }
        }
        mappingData.setHost(mappedHost.getObject());

        // Context mapping
        MapperContextList contextList = mappedHost.getContextList();
        MapperContext[] contexts = contextList.getContexts();
        int nesting = contextList.getNesting();

        int pos = find(contexts, uri);
        if (pos == -1) {
            return;
        }

        int lastSlash = -1;
        int uriEnd = uri.getEnd();
        int length = -1;
        boolean found = false;
        MapperContext context = null;
        while (pos >= 0) {
            context = contexts[pos];
            if (uri.startsWith(context.getName())) {
                length = context.getName().length();
                if (uri.getLength() == length) {
                    found = true;
                    break;
                } else if (uri.startsWithIgnoreCase("/", length)) {
                    found = true;
                    break;
                }
            }
            if (lastSlash == -1) {
                lastSlash = nthSlash(uri, nesting + 1);
            } else {
                lastSlash = lastSlash(uri);
            }
            uri.setEnd(lastSlash);
            pos = find(contexts, uri);
        }
        uri.setEnd(uriEnd);

        if (!found) {
            if (contexts[0].getName().equals("")) {
                context = contexts[0];
            } else {
                context = null;
            }
        }
        if (context == null) {
            return;
        }

        mappingData.getContextPath().setString(context.getName());

        MapperContextVersion contextVersion = null;
        MapperContextVersion[] contextVersions = context.getVersions();
        final int versionCount = contextVersions.length;
        if (versionCount > 1) {
            Object[] contextObjects = new Object[contextVersions.length];
            for (int i = 0; i < contextObjects.length; i++) {
                contextObjects[i] = contextVersions[i].getObject();
            }
            mappingData.setContexts(contextObjects);
            if (version != null) {
                contextVersion = exactFind(contextVersions, version);
            }
        }
        if (contextVersion == null) {
            // Return the latest version
            // The versions array is known to contain at least one element
            contextVersion = contextVersions[versionCount - 1];
        }

        mappingData.setContext(contextVersion.getObject());
        mappingData.setContextSlashCount(contextVersion.getSlashCount());

        // Wrapper mapping
        if (!contextVersion.isPaused()) {
            internalMapWrapper(contextVersion, uri, mappingData);
        }

    }


    /**
     * Wrapper mapping.
     */
    private final void internalMapWrapper(MapperContextVersion contextVersion,
                                          CharChunk path,
                                          MappingData mappingData)
        throws Exception {

        int pathOffset = path.getOffset();
        int pathEnd = path.getEnd();
        int servletPath = pathOffset;
        boolean noServletPath = false;

        int length = contextVersion.getPath().length();
        if (length != (pathEnd - pathOffset)) {
            servletPath = pathOffset + length;
        } else {
            noServletPath = true;
            path.append('/');
            pathOffset = path.getOffset();
            pathEnd = path.getEnd();
            servletPath = pathOffset+length;
        }

        path.setOffset(servletPath);

        // Rule 1 -- Exact Match
        MapperWrapper[] exactWrappers = contextVersion.getExactWrappers();
        internalMapExactWrapper(exactWrappers, path, mappingData);

        // Rule 2 -- Prefix Match
        boolean checkJspWelcomeFiles = false;
        MapperWrapper[] wildcardWrappers = contextVersion.getWildcardWrappers();
        if (mappingData.getWrapper() == null) {
            internalMapWildcardWrapper(wildcardWrappers, contextVersion.getNesting(),
                                       path, mappingData);
            if (mappingData.getWrapper() != null && mappingData.isJspWildCard()) {
                char[] buf = path.getBuffer();
                if (buf[pathEnd - 1] == '/') {
                    /*
                     * Path ending in '/' was mapped to JSP servlet based on
                     * wildcard match (e.g., as specified in url-pattern of a
                     * jsp-property-group.
                     * Force the context's welcome files, which are interpreted
                     * as JSP files (since they match the url-pattern), to be
                     * considered. See Bugzilla 27664.
                     */
                    mappingData.setWrapper(null);
                    checkJspWelcomeFiles = true;
                } else {
                    // See Bugzilla 27704
                    mappingData.getWrapperPath().setChars(buf, path.getStart(),
                                                     path.getLength());
                    mappingData.getPathInfo().recycle();
                }
            }
        }

        if(mappingData.getWrapper() == null && noServletPath) {
            // The path is empty, redirect to "/"
            mappingData.getRedirectPath().setChars
                (path.getBuffer(), pathOffset, pathEnd-pathOffset);
            path.setEnd(pathEnd - 1);
            return;
        }

        // Rule 3 -- Extension Match
        MapperWrapper[] extensionWrappers = contextVersion.getExtensionWrappers();
        if (mappingData.getWrapper() == null && !checkJspWelcomeFiles) {
            internalMapExtensionWrapper(extensionWrappers, path, mappingData,
                    true);
        }

        // Rule 4 -- Welcome resources processing for servlets
        if (mappingData.getWrapper() == null) {
            boolean checkWelcomeFiles = checkJspWelcomeFiles;
            if (!checkWelcomeFiles) {
                char[] buf = path.getBuffer();
                checkWelcomeFiles = (buf[pathEnd - 1] == '/');
            }
            if (checkWelcomeFiles) {
                for (int i = 0; (i < contextVersion.getWelcomeResources().length)
                         && (mappingData.getWrapper() == null); i++) {
                    path.setOffset(pathOffset);
                    path.setEnd(pathEnd);
                    path.append(contextVersion.getWelcomeResources()[i], 0,
                            contextVersion.getWelcomeResources()[i].length());
                    path.setOffset(servletPath);

                    // Rule 4a -- Welcome resources processing for exact macth
                    internalMapExactWrapper(exactWrappers, path, mappingData);

                    // Rule 4b -- Welcome resources processing for prefix match
                    if (mappingData.getWrapper() == null) {
                        internalMapWildcardWrapper
                            (wildcardWrappers, contextVersion.getNesting(),
                             path, mappingData);
                    }

                    // Rule 4c -- Welcome resources processing
                    //            for physical folder
                    if (mappingData.getWrapper() == null
                        && contextVersion.getResources() != null) {
                        Object file = null;
                        String pathStr = path.toString();
                        try {
                            file = contextVersion.getResources().lookup(pathStr);
                        } catch(NamingException nex) {
                            // Swallow not found, since this is normal
                        }
                        if (file != null && !(file instanceof DirContext) ) {
                            internalMapExtensionWrapper(extensionWrappers, path,
                                                        mappingData, true);
                            if (mappingData.getWrapper() == null
                                && contextVersion.getDefaultWrapper() != null) {
                                mappingData.setWrapper(contextVersion.getDefaultWrapper().getObject());
                                mappingData.getRequestPath().setChars
                                    (path.getBuffer(), path.getStart(),
                                     path.getLength());
                                mappingData.getWrapperPath().setChars
                                    (path.getBuffer(), path.getStart(),
                                     path.getLength());
                                mappingData.getRequestPath().setString(pathStr);
                                mappingData.getWrapperPath().setString(pathStr);
                            }
                        }
                    }
                }

                path.setOffset(servletPath);
                path.setEnd(pathEnd);
            }

        }

        /* welcome file processing - take 2
         * Now that we have looked for welcome files with a physical
         * backing, now look for an extension mapping listed
         * but may not have a physical backing to it. This is for
         * the case of index.jsf, index.do, etc.
         * A watered down version of rule 4
         */
        if (mappingData.getWrapper() == null) {
            boolean checkWelcomeFiles = checkJspWelcomeFiles;
            if (!checkWelcomeFiles) {
                char[] buf = path.getBuffer();
                checkWelcomeFiles = (buf[pathEnd - 1] == '/');
            }
            if (checkWelcomeFiles) {
                for (int i = 0; (i < contextVersion.getWelcomeResources().length)
                         && (mappingData.getWrapper() == null); i++) {
                    path.setOffset(pathOffset);
                    path.setEnd(pathEnd);
                    path.append(contextVersion.getWelcomeResources()[i], 0,
                                contextVersion.getWelcomeResources()[i].length());
                    path.setOffset(servletPath);
                    internalMapExtensionWrapper(extensionWrappers, path,
                                                mappingData, false);
                }

                path.setOffset(servletPath);
                path.setEnd(pathEnd);
            }
        }


        // Rule 7 -- Default servlet
        if (mappingData.getWrapper() == null && !checkJspWelcomeFiles) {
            if (contextVersion.getDefaultWrapper() != null) {
                mappingData.setWrapper(contextVersion.getDefaultWrapper().getObject());
                mappingData.getRequestPath().setChars
                    (path.getBuffer(), path.getStart(), path.getLength());
                mappingData.getWrapperPath().setChars
                    (path.getBuffer(), path.getStart(), path.getLength());
            }
            // Redirection to a folder
            char[] buf = path.getBuffer();
            if (contextVersion.getResources() != null && buf[pathEnd -1 ] != '/') {
                Object file = null;
                String pathStr = path.toString();
                try {
                    file = contextVersion.getResources().lookup(pathStr);
                } catch(NamingException nex) {
                    // Swallow, since someone else handles the 404
                }
                if (file != null && file instanceof DirContext) {
                    // Note: this mutates the path: do not do any processing
                    // after this (since we set the redirectPath, there
                    // shouldn't be any)
                    path.setOffset(pathOffset);
                    path.append('/');
                    mappingData.getRedirectPath().setChars
                        (path.getBuffer(), path.getStart(), path.getLength());
                } else {
                    mappingData.getRequestPath().setString(pathStr);
                    mappingData.getWrapperPath().setString(pathStr);
                }
            }
        }

        path.setOffset(pathOffset);
        path.setEnd(pathEnd);

    }


    /**
     * Exact mapping.
     */
    private final void internalMapExactWrapper
        (MapperWrapper[] wrappers, CharChunk path, MappingData mappingData) {
    	MapperWrapper wrapper = exactFind(wrappers, path);
        if (wrapper != null) {
            mappingData.getRequestPath().setString(wrapper.getName());
            mappingData.setWrapper(wrapper.getObject());
            if (path.equals("/")) {
                // Special handling for Context Root mapped servlet
                mappingData.getPathInfo().setString("/");
                mappingData.getWrapperPath().setString("");
                // This seems wrong but it is what the spec says...
                mappingData.getContextPath().setString("");
            } else {
                mappingData.getWrapperPath().setString(wrapper.getName());
            }
        }
    }


    /**
     * Wildcard mapping.
     */
    private final void internalMapWildcardWrapper
        (MapperWrapper[] wrappers, int nesting, CharChunk path,
         MappingData mappingData) {

        int pathEnd = path.getEnd();

        int lastSlash = -1;
        int length = -1;
        int pos = find(wrappers, path);
        if (pos != -1) {
            boolean found = false;
            while (pos >= 0) {
                if (path.startsWith(wrappers[pos].getName())) {
                    length = wrappers[pos].getName().length();
                    if (path.getLength() == length) {
                        found = true;
                        break;
                    } else if (path.startsWithIgnoreCase("/", length)) {
                        found = true;
                        break;
                    }
                }
                if (lastSlash == -1) {
                    lastSlash = nthSlash(path, nesting + 1);
                } else {
                    lastSlash = lastSlash(path);
                }
                path.setEnd(lastSlash);
                pos = find(wrappers, path);
            }
            path.setEnd(pathEnd);
            if (found) {
                mappingData.getWrapperPath().setString(wrappers[pos].getName());
                if (path.getLength() > length) {
                    mappingData.getPathInfo().setChars
                        (path.getBuffer(),
                         path.getOffset() + length,
                         path.getLength() - length);
                }
                mappingData.getRequestPath().setChars
                    (path.getBuffer(), path.getOffset(), path.getLength());
                mappingData.setWrapper(wrappers[pos].getObject());
                mappingData.setJspWildCard(wrappers[pos].isJspWildCard());
            }
        }
    }


    /**
     * Extension mappings.
     *
     * @param wrappers          Set of wrappers to check for matches
     * @param path              Path to map
     * @param mappingData       Mapping data for result
     * @param resourceExpected  Is this mapping expecting to find a resource
     */
    private final void internalMapExtensionWrapper(MapperWrapper[] wrappers,
            CharChunk path, MappingData mappingData, boolean resourceExpected) {
        char[] buf = path.getBuffer();
        int pathEnd = path.getEnd();
        int servletPath = path.getOffset();
        int slash = -1;
        for (int i = pathEnd - 1; i >= servletPath; i--) {
            if (buf[i] == '/') {
                slash = i;
                break;
            }
        }
        if (slash >= 0) {
            int period = -1;
            for (int i = pathEnd - 1; i > slash; i--) {
                if (buf[i] == '.') {
                    period = i;
                    break;
                }
            }
            if (period >= 0) {
                path.setOffset(period + 1);
                path.setEnd(pathEnd);
                MapperWrapper wrapper = exactFind(wrappers, path);
                if (wrapper != null
                        && (resourceExpected || !wrapper.isResourceOnly())) {
                    mappingData.getWrapperPath().setChars(buf, servletPath, pathEnd
                            - servletPath);
                    mappingData.getRequestPath().setChars(buf, servletPath, pathEnd
                            - servletPath);
                    mappingData.setWrapper(wrapper.getObject());
                }
                path.setOffset(servletPath);
                path.setEnd(pathEnd);
            }
        }
    }


    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static final int find(MapperMapElement[] map, CharChunk name) {
        return find(map, name, name.getStart(), name.getEnd());
    }


    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static final int find(MapperMapElement[] map, CharChunk name,
                                  int start, int end) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }

        if (compare(name, start, end, map[0].getName()) < 0 ) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) / 2;
            int result = compare(name, start, end, map[i].getName());
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = compare(name, start, end, map[b].getName());
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }

    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static final int findIgnoreCase(MapperMapElement[] map, CharChunk name) {
        return findIgnoreCase(map, name, name.getStart(), name.getEnd());
    }


    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static final int findIgnoreCase(MapperMapElement[] map, CharChunk name,
                                  int start, int end) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }
        if (compareIgnoreCase(name, start, end, map[0].getName()) < 0 ) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) / 2;
            int result = compareIgnoreCase(name, start, end, map[i].getName());
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = compareIgnoreCase(name, start, end, map[b].getName());
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }


    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     * @see #exactFind(MapElement[], String)
     */
    private static final int find(MapperMapElement[] map, String name) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }

        if (name.compareTo(map[0].getName()) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) / 2;
            int result = name.compareTo(map[i].getName());
            if (result > 0) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = name.compareTo(map[b].getName());
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }


    /**
     * Find a map element given its name in a sorted array of map elements. This
     * will return the element that you were searching for. Otherwise it will
     * return <code>null</code>.
     * @see #find(MapElement[], String)
     */
    private static final <E extends MapperMapElement> E exactFind(E[] map,
            String name) {
        int pos = find(map, name);
        if (pos >= 0) {
            E result = map[pos];
            if (name.equals(result.getName())) {
                return result;
            }
        }
        return null;
    }

    /**
     * Find a map element given its name in a sorted array of map elements. This
     * will return the element that you were searching for. Otherwise it will
     * return <code>null</code>.
     */
    private static final <E extends MapperMapElement> E exactFind(E[] map,
            CharChunk name) {
        int pos = find(map, name);
        if (pos >= 0) {
            E result = map[pos];
            if (name.equals(result.getName())) {
                return result;
            }
        }
        return null;
    }

    /**
     * Find a map element given its name in a sorted array of map elements. This
     * will return the element that you were searching for. Otherwise it will
     * return <code>null</code>.
     * @see #findIgnoreCase(MapElement[], CharChunk)
     */
    private static final <E extends MapperMapElement> E exactFindIgnoreCase(E[] map,
            CharChunk name) {
        int pos = findIgnoreCase(map, name);
        if (pos >= 0) {
            E result = map[pos];
            if (name.equalsIgnoreCase(result.getName())) {
                return result;
            }
        }
        return null;
    }


    /**
     * Compare given char chunk with String.
     * Return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    private static final int compare(CharChunk name, int start, int end,
                                     String compareTo) {
        int result = 0;
        char[] c = name.getBuffer();
        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (c[i + start] > compareTo.charAt(i)) {
                result = 1;
            } else if (c[i + start] < compareTo.charAt(i)) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }


    /**
     * Compare given char chunk with String ignoring case.
     * Return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    private static final int compareIgnoreCase(CharChunk name, int start, int end,
                                     String compareTo) {
        int result = 0;
        char[] c = name.getBuffer();
        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (Ascii.toLower(c[i + start]) > Ascii.toLower(compareTo.charAt(i))) {
                result = 1;
            } else if (Ascii.toLower(c[i + start]) < Ascii.toLower(compareTo.charAt(i))) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }


    /**
     * Find the position of the last slash in the given char chunk.
     */
    private static final int lastSlash(CharChunk name) {

        char[] c = name.getBuffer();
        int end = name.getEnd();
        int start = name.getStart();
        int pos = end;

        while (pos > start) {
            if (c[--pos] == '/') {
                break;
            }
        }

        return (pos);

    }


    /**
     * Find the position of the nth slash, in the given char chunk.
     */
    private static final int nthSlash(CharChunk name, int n) {

        char[] c = name.getBuffer();
        int end = name.getEnd();
        int start = name.getStart();
        int pos = start;
        int count = 0;

        while (pos < end) {
            if ((c[pos++] == '/') && ((++count) == n)) {
                pos--;
                break;
            }
        }

        return (pos);

    }


    /**
     * Return the slash count in a given string.
     */
    public static final int slashCount(String name) {
        int pos = -1;
        int count = 0;
        while ((pos = name.indexOf('/', pos + 1)) != -1) {
            count++;
        }
        return count;
    }


    /**
     * Insert into the right place in a sorted MapElement array, and prevent
     * duplicates.
     */
    public static final boolean insertMap
        (MapperMapElement[] oldMap, MapperMapElement[] newMap, MapperMapElement newElement) {
        int pos = find(oldMap, newElement.getName());
        if ((pos != -1) && (newElement.getName().equals(oldMap[pos].getName()))) {
            return false;
        }
        System.arraycopy(oldMap, 0, newMap, 0, pos + 1);
        newMap[pos + 1] = newElement;
        System.arraycopy
            (oldMap, pos + 1, newMap, pos + 2, oldMap.length - pos - 1);
        return true;
    }


    /**
     * Insert into the right place in a sorted MapElement array.
     */
    public static final boolean removeMap
        (MapperMapElement[] oldMap, MapperMapElement[] newMap, String name) {
        int pos = find(oldMap, name);
        if ((pos != -1) && (name.equals(oldMap[pos].getName()))) {
            System.arraycopy(oldMap, 0, newMap, 0, pos);
            System.arraycopy(oldMap, pos + 1, newMap, pos,
                             oldMap.length - pos - 1);
            return true;
        }
        return false;
    }
}
