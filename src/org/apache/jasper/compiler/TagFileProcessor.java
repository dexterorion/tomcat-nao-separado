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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.runtime.JspSourceDependent;
import org.apache.jasper.servlet.JspServletWrapper;

/**
 * 1. Processes and extracts the directive info in a tag file. 2. Compiles and
 * loads tag files used in a JSP file.
 * 
 * @author Kin-man Chung
 */

public class TagFileProcessor {

    private Vector<Compiler2> tempVector;

    public static TagInfo parseTagFileDirectives(ParserController pc,
            String name, String path, JarResource jarResource, TagLibraryInfo tagLibInfo)
            throws JasperException {


        ErrorDispatcher err = pc.getCompiler().getErrorDispatcher();

        NodeNodes page = null;
        try {
            page = pc.parseTagFileDirectives(path, jarResource);
        } catch (FileNotFoundException e) {
            err.jspError("jsp.error.file.not.found", path);
        } catch (IOException e) {
            err.jspError("jsp.error.file.not.found", path);
        }

        TagFileProcessorTagFileDirectiveVisitor tagFileVisitor = new TagFileProcessorTagFileDirectiveVisitor(pc
                .getCompiler(), tagLibInfo, name, path);
        page.visit(tagFileVisitor);
        tagFileVisitor.postCheck();

        return tagFileVisitor.getTagInfo();
    }

    /**
     * Compiles and loads a tagfile.
     */
    public Class<?> loadTagFile(Compiler2 compiler, String tagFilePath,
            TagInfo tagInfo, PageInfo parentPageInfo) throws JasperException {

        JarResource tagJarResouce = null;
        if (tagFilePath.startsWith("/META-INF/")) {
            tagJarResouce = 
                compiler.getCompilationContext().getTldLocation(
                        tagInfo.getTagLibrary().getURI()).getJarResource();
        }
        String wrapperUri;
        if (tagJarResouce == null) {
            wrapperUri = tagFilePath;
        } else {
            wrapperUri = tagJarResouce.getEntry(tagFilePath).toString();
        }

        JspCompilationContext ctxt = compiler.getCompilationContext();
        JspRuntimeContext rctxt = ctxt.getRuntimeContext();

        synchronized (rctxt) {
            JspServletWrapper wrapper = rctxt.getWrapper(wrapperUri);
            if (wrapper == null) {
                wrapper = new JspServletWrapper(ctxt.getServletContext(), ctxt
                        .getOptions(), tagFilePath, tagInfo, ctxt
                        .getRuntimeContext(), tagJarResouce);
                rctxt.addWrapper(wrapperUri, wrapper);

                // Use same classloader and classpath for compiling tag files
                wrapper.getJspEngineContext().setClassLoader(
                        ctxt.getClassLoader());
                wrapper.getJspEngineContext().setClassPath(ctxt.getClassPath());
            } else {
                // Make sure that JspCompilationContext gets the latest TagInfo
                // for the tag file. TagInfo instance was created the last
                // time the tag file was scanned for directives, and the tag
                // file may have been modified since then.
                wrapper.getJspEngineContext().setTagInfo(tagInfo);
            }

            Class<?> tagClazz;
            int tripCount = wrapper.incTripCount();
            try {
                if (tripCount > 0) {
                    // When tripCount is greater than zero, a circular
                    // dependency exists. The circularly dependent tag
                    // file is compiled in prototype mode, to avoid infinite
                    // recursion.

                    JspServletWrapper tempWrapper = new JspServletWrapper(ctxt
                            .getServletContext(), ctxt.getOptions(),
                            tagFilePath, tagInfo, ctxt.getRuntimeContext(),
                            ctxt.getTagFileJarResource(tagFilePath));
                    // Use same classloader and classpath for compiling tag files
                    tempWrapper.getJspEngineContext().setClassLoader(
                            ctxt.getClassLoader());
                    tempWrapper.getJspEngineContext().setClassPath(ctxt.getClassPath());
                    tagClazz = tempWrapper.loadTagFilePrototype();
                    tempVector.add(tempWrapper.getJspEngineContext()
                            .getCompiler());
                } else {
                    tagClazz = wrapper.loadTagFile();
                }
            } finally {
                wrapper.decTripCount();
            }

            // Add the dependents for this tag file to its parent's
            // Dependent list. The only reliable dependency information
            // can only be obtained from the tag instance.
            try {
                Object tagIns = tagClazz.newInstance();
                if (tagIns instanceof JspSourceDependent) {
                    Iterator<Entry<String,Long>> iter = ((JspSourceDependent)
                            tagIns).getDependants().entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry<String,Long> entry = iter.next();
                        parentPageInfo.addDependant(entry.getKey(),
                                entry.getValue());
                    }
                }
            } catch (Exception e) {
                // ignore errors
            }

            return tagClazz;
        }
    }

    /**
     * Implements a phase of the translation that compiles (if necessary) the
     * tag files used in a JSP files. The directives in the tag files are
     * assumed to have been processed and encapsulated as TagFileInfo in the
     * CustomTag nodes.
     */
    public void loadTagFiles(Compiler2 compiler, NodeNodes page)
            throws JasperException {

        tempVector = new Vector<Compiler2>();
        page.visit(new TagFileProcessorTagFileLoaderVisitor(this, compiler));
    }

    /**
     * Removed the java and class files for the tag prototype generated from the
     * current compilation.
     * 
     * @param classFileName
     *            If non-null, remove only the class file with with this name.
     */
    public void removeProtoTypeFiles(String classFileName) {
        Iterator<Compiler2> iter = tempVector.iterator();
        while (iter.hasNext()) {
            Compiler2 c = iter.next();
            if (classFileName == null) {
                c.removeGeneratedClassFiles();
            } else if (classFileName.equals(c.getCompilationContext()
                    .getClassFileName())) {
                c.removeGeneratedClassFiles();
                tempVector.remove(c);
                return;
            }
        }
    }
}
