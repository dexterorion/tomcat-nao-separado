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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;

/**
 * Contains static utilities for generating SMAP data based on the
 * current version of Jasper.
 * 
 * @author Jayson Falkner
 * @author Shawn Bayern
 * @author Robert Field (inner SDEInstaller class)
 * @author Mark Roth
 * @author Kin-man Chung
 */
public class SmapUtil {

    //*********************************************************************
    // Constants

    private static final String SMAP_ENCODING = "UTF-8";

    //*********************************************************************
    // Public entry points

    /**
     * Generates an appropriate SMAP representing the current compilation
     * context.  (JSR-045.)
     *
     * @param ctxt Current compilation context
     * @param pageNodes The current JSP page
     * @return a SMAP for the page
     */
    public static String[] generateSmap(
        JspCompilationContext ctxt,
        NodeNodes pageNodes)
        throws IOException {

        // Scan the nodes for presence of Jasper generated inner classes
    	SmapUtilPreScanVisitor psVisitor = new SmapUtilPreScanVisitor();
        try {
            pageNodes.visit(psVisitor);
        } catch (JasperException ex) {
        }
        HashMap<String, SmapStratum> map = psVisitor.getMap();

        // set up our SMAP generator
        SmapGenerator g = new SmapGenerator();
        
        /** Disable reading of input SMAP because:
            1. There is a bug here: getRealPath() is null if .jsp is in a jar
               Bugzilla 14660.
            2. Mappings from other sources into .jsp files are not supported.
            TODO: fix 1. if 2. is not true.
        // determine if we have an input SMAP
        String smapPath = inputSmapPath(ctxt.getRealPath(ctxt.getJspFile()));
            File inputSmap = new File(smapPath);
            if (inputSmap.exists()) {
                byte[] embeddedSmap = null;
            byte[] subSmap = SDEInstaller.readWhole(inputSmap);
            String subSmapString = new String(subSmap, SMAP_ENCODING);
            g.addSmap(subSmapString, "JSP");
        }
        **/

        // now, assemble info about our own stratum (JSP) using JspLineMap
        SmapStratum s = new SmapStratum("JSP");

        g.setOutputFileName(unqualify(ctxt.getServletJavaFileName()));

        // Map out NodeNodes
        evaluateNodes(pageNodes, s, map, ctxt.getOptions().getMappedFile());
        s.optimizeLineSection();
        g.addStratum(s, true);

        if (ctxt.getOptions().isSmapDumped()) {
            File outSmap = new File(ctxt.getClassFileName() + ".smap");
            PrintWriter so =
                new PrintWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(outSmap),
                        SMAP_ENCODING));
            so.print(g.getString());
            so.close();
        }

        String classFileName = ctxt.getClassFileName();
        int innerClassCount = map.size();
        String [] smapInfo = new String[2 + innerClassCount*2];
        smapInfo[0] = classFileName;
        smapInfo[1] = g.getString();

        int count = 2;
        Iterator<Map.Entry<String,SmapStratum>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,SmapStratum> entry = iter.next();
            String innerClass = entry.getKey();
            s = entry.getValue();
            s.optimizeLineSection();
            g = new SmapGenerator();
            g.setOutputFileName(unqualify(ctxt.getServletJavaFileName()));
            g.addStratum(s, true);

            String innerClassFileName =
                classFileName.substring(0, classFileName.indexOf(".class")) +
                '$' + innerClass + ".class";
            if (ctxt.getOptions().isSmapDumped()) {
                File outSmap = new File(innerClassFileName + ".smap");
                PrintWriter so =
                    new PrintWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(outSmap),
                            SMAP_ENCODING));
                so.print(g.getString());
                so.close();
            }
            smapInfo[count] = innerClassFileName;
            smapInfo[count+1] = g.getString();
            count += 2;
        }

        return smapInfo;
    }

    public static void installSmap(String[] smap)
        throws IOException {
        if (smap == null) {
            return;
        }

        for (int i = 0; i < smap.length; i += 2) {
            File outServlet = new File(smap[i]);
            SmapUtilSDEInstaller.install(outServlet,
                    smap[i+1].getBytes(Charset.defaultCharset()));
        }
    }

    //*********************************************************************
    // Private utilities

    /**
     * Returns an unqualified version of the given file path.
     */
    public static String unqualify(String path) {
        path = path.replace('\\', '/');
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static void evaluateNodes(
        NodeNodes nodes,
        SmapStratum s,
        HashMap<String, SmapStratum> innerClassMap,
        boolean breakAtLF) {
        try {
            nodes.visit(new SmapUtilSmapGenVisitor(s, breakAtLF, innerClassMap));
        } catch (JasperException ex) {
        }
    }
}
