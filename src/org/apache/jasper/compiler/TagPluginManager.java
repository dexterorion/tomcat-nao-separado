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
package org.apache.jasper.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.apache.jasper.Constants28;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.tagplugin.TagPlugin;
import org.apache.jasper.compiler.tagplugin.TagPluginContext;
import org.apache.jasper.xmlparser.ParserUtils;
import org.apache.jasper.xmlparser.TreeNode;

/**
 * Manages tag plugin optimizations.
 * @author Kin-man Chung
 */
public class TagPluginManager {

    private static final String META_INF_JASPER_TAG_PLUGINS_XML =
            "META-INF/org.apache.jasper/tagPlugins.xml";
    private static final String TAG_PLUGINS_XML = "/WEB-INF/tagPlugins.xml";
    private static final String TAG_PLUGINS_ROOT_ELEM = "tag-plugins";

    private boolean initialized = false;
    private HashMap<String, TagPlugin> tagPlugins = null;
    private ServletContext ctxt;

    public TagPluginManager(ServletContext ctxt) {
        this.ctxt = ctxt;
    }

    public void apply(NodeNodes page, ErrorDispatcher err, PageInfo pageInfo)
            throws JasperException {

        init(err);
        if (tagPlugins == null || tagPlugins.size() == 0) {
            return;
        }

        page.visit(new NodeVisitor1(this, pageInfo));
    }

    private void init(ErrorDispatcher err) throws JasperException {
        if (initialized)
            return;

        tagPlugins = new HashMap<String, TagPlugin>();

        Enumeration<URL> urls = null;
        try {
            urls = ctxt.getClassLoader().getResources(
                    META_INF_JASPER_TAG_PLUGINS_XML);
        } catch (IOException ioe) {
            throw new JasperException(ioe);
        }

        if (urls != null) {
            while(urls.hasMoreElements()) {
                URL url = urls.nextElement();
                InputStream is = null;
                try {
                    is = url.openStream();
                    loadTagPlugins(err, is);
                } catch(IOException ioe) {
                    throw new JasperException(ioe);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ioe) {
                            throw new JasperException(ioe);
                        }
                    }
                }
            }
        }

        InputStream is = null;
        try {
            is = ctxt.getResourceAsStream(TAG_PLUGINS_XML);
            if (is != null) {
                loadTagPlugins(err, is);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                throw new JasperException(ioe);
            }
        }

        initialized = true;
    }


    private void loadTagPlugins(ErrorDispatcher err, InputStream is)
            throws JasperException {

        String blockExternalString = ctxt.getInitParameter(
                Constants28.getXmlBlockExternalInitParam());
        boolean blockExternal;
        if (blockExternalString == null) {
            blockExternal = true;
        } else {
            blockExternal = Boolean.parseBoolean(blockExternalString);
        }
        
        ParserUtils pu = new ParserUtils(false, blockExternal);
        
        TreeNode root = pu.parseXMLDocument(TAG_PLUGINS_XML, is);
        if (root == null) {
            return;
        }

        if (!TAG_PLUGINS_ROOT_ELEM.equals(root.getName())) {
            err.jspError("jsp.error.plugin.wrongRootElement", TAG_PLUGINS_XML,
                         TAG_PLUGINS_ROOT_ELEM);
        }

        tagPlugins = new HashMap<String, TagPlugin>();
        Iterator<TreeNode> pluginList = root.findChildren("tag-plugin");
        while (pluginList.hasNext()) {
            TreeNode pluginNode = pluginList.next();
            TreeNode tagClassNode = pluginNode.findChild("tag-class");
            if (tagClassNode == null) {
                // Error
                return;
            }
            String tagClass = tagClassNode.getBody().trim();
            TreeNode pluginClassNode = pluginNode.findChild("plugin-class");
            if (pluginClassNode == null) {
                // Error
                return;
            }

            String pluginClassStr = pluginClassNode.getBody();
            TagPlugin tagPlugin = null;
            try {
                Class<?> pluginClass =
                        ctxt.getClassLoader().loadClass(pluginClassStr);
                tagPlugin = (TagPlugin) pluginClass.newInstance();
            } catch (Exception e) {
                throw new JasperException(e);
            }
            if (tagPlugin == null) {
                return;
            }
            tagPlugins.put(tagClass, tagPlugin);
        }
        initialized = true;
    }

    /**
     * Invoke tag plugin for the given custom tag, if a plugin exists for
     * the custom tag's tag handler.
     *
     * The given custom tag node will be manipulated by the plugin.
     */
    public void invokePlugin(NodeCustomTag n, PageInfo pageInfo) {
        TagPlugin tagPlugin = tagPlugins.get(n.getTagHandlerClass().getName());
        if (tagPlugin == null) {
            return;
        }

        TagPluginContext tagPluginContext = new TagPluginContextImpl1(n, pageInfo);
        n.setTagPluginContext(tagPluginContext);
        tagPlugin.doTag(tagPluginContext);
    }
}
