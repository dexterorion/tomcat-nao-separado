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
import java.io.FileNotFoundException;
import java.util.StringTokenizer;

import org.apache.jasper.Constants28;
import org.apache.jasper.JasperException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;

/**
 * Main JSP compiler class. This class uses Ant for compiling.
 *
 * @author Anil K. Vijendran
 * @author Mandar Raje
 * @author Pierre Delisle
 * @author Kin-man Chung
 * @author Remy Maucherat
 * @author Mark Roth
 */
public class AntCompiler extends Compiler2 {

    private final Log log = LogFactory.getLog(AntCompiler.class); // must not be static
    
    private static final Object javacLock = new Object();

    static {
        System.setErr(new AntCompilerSystemLogHandler(System.err));
    }

    // ----------------------------------------------------- Instance Variables

    private Project project = null;
    private AntCompilerJasperAntLogger logger;

    // ------------------------------------------------------------ Constructor

    // Lazy eval - if we don't need to compile we probably don't need the project
    protected Project getProject() {
        
        if (project != null)
            return project;
        
        // Initializing project
        project = new Project();
        logger = new AntCompilerJasperAntLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener( logger);
        if (System.getProperty(Constants28.getCatalinaHomeProp()) != null) {
            project.setBasedir(System.getProperty(Constants28.getCatalinaHomeProp()));
        }
        
        if( getOptions().getCompiler() != null ) {
            if( log.isDebugEnabled() )
                log.debug("Compiler " + getOptions().getCompiler() );
            project.setProperty("build.compiler", getOptions().getCompiler() );
        }
        project.init();
        return project;
    }
    
    /** 
     * Compile the servlet from .java file to .class file
     */
    @Override
    protected void generateClass(String[] smap)
        throws FileNotFoundException, JasperException, Exception {
        
        long t1 = 0;
        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }

        String javaEncoding = getCtxt().getOptions().getJavaEncoding();
        String javaFileName = getCtxt().getServletJavaFileName();
        String classpath = getCtxt().getClassPath(); 
        
        String sep = System.getProperty("path.separator");
        
        StringBuilder errorReport = new StringBuilder();
        
        StringBuilder info=new StringBuilder();
        info.append("Compile: javaFileName=" + javaFileName + "\n" );
        info.append("    classpath=" + classpath + "\n" );
        
        // Start capturing the System.err output for this thread
        AntCompilerSystemLogHandler.setThread();
        
        // Initializing javac task
        getProject();
        Javac javac = (Javac) project.createTask("javac");
        
        // Initializing classpath
        Path path = new Path(project);
        path.setPath(System.getProperty("java.class.path"));
        info.append("    cp=" + System.getProperty("java.class.path") + "\n");
        StringTokenizer tokenizer = new StringTokenizer(classpath, sep);
        while (tokenizer.hasMoreElements()) {
            String pathElement = tokenizer.nextToken();
            File repository = new File(pathElement);
            path.setLocation(repository);
            info.append("    cp=" + repository + "\n");
        }
        
        if( log.isDebugEnabled() )
            log.debug( "Using classpath: " + System.getProperty("java.class.path") + sep
                    + classpath);
        
        // Initializing sourcepath
        Path srcPath = new Path(project);
        srcPath.setLocation(getOptions().getScratchDir());
        
        info.append("    work dir=" + getOptions().getScratchDir() + "\n");
        
        // Initialize and set java extensions
        String exts = System.getProperty("java.ext.dirs");
        if (exts != null) {
            Path extdirs = new Path(project);
            extdirs.setPath(exts);
            javac.setExtdirs(extdirs);
            info.append("    extension dir=" + exts + "\n");
        }

        // Add endorsed directories if any are specified and we're forking
        // See Bugzilla 31257
        if(getCtxt().getOptions().getFork()) {
            String endorsed = System.getProperty("java.endorsed.dirs");
            if(endorsed != null) {
                Javac.ImplementationSpecificArgument endorsedArg = 
                    javac.createCompilerArg();
                endorsedArg.setLine("-J-Djava.endorsed.dirs=" +
                        quotePathList(endorsed));
                info.append("    endorsed dir=" + quotePathList(endorsed) +
                        "\n");
            } else {
                info.append("    no endorsed dirs specified\n");
            }
        }
        
        // Configure the compiler object
        javac.setEncoding(javaEncoding);
        javac.setClasspath(path);
        javac.setDebug(getCtxt().getOptions().getClassDebugInfo());
        javac.setSrcdir(srcPath);
        javac.setTempdir(getOptions().getScratchDir());
        javac.setOptimize(! getCtxt().getOptions().getClassDebugInfo() );
        javac.setFork(getCtxt().getOptions().getFork());
        info.append("    srcDir=" + srcPath + "\n" );
        
        // Set the Java compiler to use
        if (getOptions().getCompiler() != null) {
            javac.setCompiler(getOptions().getCompiler());
            info.append("    compiler=" + getOptions().getCompiler() + "\n");
        }

        if (getOptions().getCompilerTargetVM() != null) {
            javac.setTarget(getOptions().getCompilerTargetVM());
            info.append("   compilerTargetVM=" + getOptions().getCompilerTargetVM() + "\n");
        }

        if (getOptions().getCompilerSourceVM() != null) {
            javac.setSource(getOptions().getCompilerSourceVM());
            info.append("   compilerSourceVM=" + getOptions().getCompilerSourceVM() + "\n");
        }
        
        // Build includes path
        PatternSet.NameEntry includes = javac.createInclude();
        
        includes.setName(getCtxt().getJavaPath());
        info.append("    include="+ getCtxt().getJavaPath() + "\n" );
        
        BuildException be = null;
        
        try {
            if (getCtxt().getOptions().getFork()) {
                javac.execute();
            } else {
                synchronized(javacLock) {
                    javac.execute();
                }
            }
        } catch (BuildException e) {
            be = e;
            log.error(Localizer.getMessage("jsp.error.javac"), e);
            log.error(Localizer.getMessage("jsp.error.javac.env") + info.toString());
        }
        
        errorReport.append(logger.getReport());

        // Stop capturing the System.err output for this thread
        String errorCapture = AntCompilerSystemLogHandler.unsetThread();
        if (errorCapture != null) {
            errorReport.append(Constants28.getNewline());
            errorReport.append(errorCapture);
        }

        if (!getCtxt().keepGenerated()) {
            File javaFile = new File(javaFileName);
            javaFile.delete();
        }
        
        if (be != null) {
            String errorReportString = errorReport.toString();
            log.error(Localizer.getMessage("jsp.error.compilation", javaFileName, errorReportString));
            JavacErrorDetail[] javacErrors = ErrorDispatcher.parseJavacErrors(
                    errorReportString, javaFileName, getPageNodes());
            if (javacErrors != null) {
                getErrDispatcher().javacError(javacErrors);
            } else {
                getErrDispatcher().javacError(errorReportString, be);
            }
        }
        
        if( log.isDebugEnabled() ) {
            long t2 = System.currentTimeMillis();
            log.debug("Compiled " + getCtxt().getServletJavaFileName() + " "
                      + (t2-t1) + "ms");
        }
        
        logger = null;
        project = null;
        
        if (getCtxt().isPrototypeMode()) {
            return;
        }
        
        // JSR45 Support
        if (! getOptions().isSmapSuppressed()) {
            SmapUtil.installSmap(smap);
        }
    }

    private String quotePathList(String list) {
        StringBuilder result = new StringBuilder(list.length() + 10);
        StringTokenizer st = new StringTokenizer(list, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.indexOf(' ') == -1) {
                result.append(token);
            } else {
                result.append('\"');
                result.append(token);
                result.append('\"');
            }
            if (st.hasMoreTokens()) {
                result.append(File.pathSeparatorChar);
            }
        }
        return result.toString();
    }
}
