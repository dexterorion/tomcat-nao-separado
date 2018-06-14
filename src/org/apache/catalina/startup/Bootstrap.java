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


package org.apache.catalina.startup;


import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.catalina.Globals;
import org.apache.catalina.security.SecurityClassLoad;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/**
 * Bootstrap loader for Catalina.  This application constructs a class loader
 * for use in loading the Catalina internal classes (by accumulating all of the
 * JAR files found in the "server" directory under "catalina.home"), and
 * starts the regular execution of the container.  The purpose of this
 * roundabout approach is to keep the Catalina internal classes (and any
 * other classes they depend on, such as an XML parser) out of the system
 * class path and therefore not visible to application level classes.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */

public final class Bootstrap {

    private static final Log log = LogFactory.getLog(Bootstrap.class);


    // ------------------------------------------------------- Static Variables


    /**
     * Daemon object used by main.
     */
    private static Bootstrap daemon = null;


    // -------------------------------------------------------------- Variables


    /**
     * Daemon reference.
     */
    private Object catalinaDaemon = null;


    private ClassLoader commonLoader = null;
    private ClassLoader catalinaLoader = null;
    private ClassLoader sharedLoader = null;


    // -------------------------------------------------------- Private Methods


    private void initClassLoaders() {
        try {
            setCommonLoaderData(createClassLoader("common", null));
            if( getCommonLoaderData() == null ) {
                // no config file, default to this loader - we might be in a 'single' env.
                setCommonLoaderData(this.getClass().getClassLoader());
            }
            setCatalinaLoaderData(createClassLoader("server", getCommonLoaderData()));
            setSharedLoaderData(createClassLoader("shared", getCommonLoaderData()));
        } catch (Throwable t) {
            handleThrowable(t);
            log.error("Class loader creation threw exception", t);
            System.exit(1);
        }
    }


    private ClassLoader createClassLoader(String name, ClassLoader parent)
        throws Exception {

        String value = CatalinaProperties.getProperty(name + ".loader");
        if ((value == null) || (value.equals("")))
            return parent;

        value = replace(value);

        List<ClassLoaderFactoryRepository> repositories = new ArrayList<ClassLoaderFactoryRepository>();

        StringTokenizer tokenizer = new StringTokenizer(value, ",");
        while (tokenizer.hasMoreElements()) {
            String repository = tokenizer.nextToken().trim();
            if (repository.length() == 0) {
                continue;
            }

            // Check for a JAR URL repository
            try {
                @SuppressWarnings("unused")
                URL url = new URL(repository);
                repositories.add(
                        new ClassLoaderFactoryRepository(repository, ClassLoaderFactoryRepositoryType.URL));
                continue;
            } catch (MalformedURLException e) {
                // Ignore
            }

            // Local repository
            if (repository.endsWith("*.jar")) {
                repository = repository.substring
                    (0, repository.length() - "*.jar".length());
                repositories.add(
                        new ClassLoaderFactoryRepository(repository, ClassLoaderFactoryRepositoryType.GLOB));
            } else if (repository.endsWith(".jar")) {
                repositories.add(
                        new ClassLoaderFactoryRepository(repository, ClassLoaderFactoryRepositoryType.JAR));
            } else {
                repositories.add(
                        new ClassLoaderFactoryRepository(repository, ClassLoaderFactoryRepositoryType.DIR));
            }
        }

        ClassLoader classLoader = ClassLoaderFactory.createClassLoader
            (repositories, parent);

        // Retrieving MBean server
        MBeanServer mBeanServer = null;
        if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
        } else {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        // Register the server classloader
        ObjectName objectName =
            new ObjectName("Catalina:type=ServerClassLoader,name=" + name);
        mBeanServer.registerMBean(classLoader, objectName);

        return classLoader;

    }

    /**
     * System property replacement in the given string.
     * 
     * @param str The original string
     * @return the modified string
     */
    protected String replace(String str) {
        // Implementation is copied from ClassLoaderLogManager.replace(),
        // but added special processing for catalina.home and catalina.base.
        String result = str;
        int pos_start = str.indexOf("${");
        if (pos_start >= 0) {
            StringBuilder builder = new StringBuilder();
            int pos_end = -1;
            while (pos_start >= 0) {
                builder.append(str, pos_end + 1, pos_start);
                pos_end = str.indexOf('}', pos_start + 2);
                if (pos_end < 0) {
                    pos_end = pos_start - 1;
                    break;
                }
                String propName = str.substring(pos_start + 2, pos_end);
                String replacement;
                if (propName.length() == 0) {
                    replacement = null;
                } else if (Globals.getCatalinaHomeProp().equals(propName)) {
                    replacement = getCatalinaHome();
                } else if (Globals.getCatalinaBaseProp().equals(propName)) {
                    replacement = getCatalinaBase();
                } else {
                    replacement = System.getProperty(propName);
                }
                if (replacement != null) {
                    builder.append(replacement);
                } else {
                    builder.append(str, pos_start, pos_end + 1);
                }
                pos_start = str.indexOf("${", pos_end + 1);
            }
            builder.append(str, pos_end + 1, str.length());
            result = builder.toString();
        }
        return result;
    }


    /**
     * Initialize daemon.
     */
    public void init()
        throws Exception
    {

        // Set Catalina path
        setCatalinaHome();
        setCatalinaBase();

        initClassLoaders();

        Thread.currentThread().setContextClassLoader(getCatalinaLoaderData());

        SecurityClassLoad.securityClassLoad(getCatalinaLoaderData());

        // Load our startup class and call its process() method
        if (log.isDebugEnabled())
            log.debug("Loading startup class");
        Class<?> startupClass =
            getCatalinaLoaderData().loadClass
            ("org.apache.catalina.startup.Catalina");
        Object startupInstance = startupClass.newInstance();

        // Set the shared extensions class loader
        if (log.isDebugEnabled())
            log.debug("Setting startup class properties");
        String methodName = "setParentClassLoader";
        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Class.forName("java.lang.ClassLoader");
        Object paramValues[] = new Object[1];
        paramValues[0] = getSharedLoaderData();
        Method method =
            startupInstance.getClass().getMethod(methodName, paramTypes);
        method.invoke(startupInstance, paramValues);

        setCatalinaDaemonData(startupInstance);

    }


    /**
     * Load daemon.
     */
    private void load(String[] arguments)
        throws Exception {

        // Call the load() method
        String methodName = "load";
        Object param[];
        Class<?> paramTypes[];
        if (arguments==null || arguments.length==0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method =
            getCatalinaDaemonData().getClass().getMethod(methodName, paramTypes);
        if (log.isDebugEnabled())
            log.debug("Calling startup class " + method);
        method.invoke(getCatalinaDaemonData(), param);

    }


    /**
     * getServer() for configtest
     */
    private Object getServer() throws Exception {

        String methodName = "getServer";
        Method method =
            getCatalinaDaemonData().getClass().getMethod(methodName);
        return method.invoke(getCatalinaDaemonData());

    }


    // ----------------------------------------------------------- Main Program


    /**
     * Load the Catalina daemon.
     */
    public void init(String[] arguments)
        throws Exception {

        init();
        load(arguments);

    }


    /**
     * Start the Catalina daemon.
     */
    public void start()
        throws Exception {
        if( getCatalinaDaemonData()==null ) init();

        Method method = getCatalinaDaemonData().getClass().getMethod("start", (Class [] )null);
        method.invoke(getCatalinaDaemonData(), (Object [])null);

    }


    /**
     * Stop the Catalina Daemon.
     */
    public void stop()
        throws Exception {

        Method method = getCatalinaDaemonData().getClass().getMethod("stop", (Class [] ) null);
        method.invoke(getCatalinaDaemonData(), (Object [] ) null);

    }


    /**
     * Stop the standalone server.
     */
    public void stopServer()
        throws Exception {

        Method method =
            getCatalinaDaemonData().getClass().getMethod("stopServer", (Class []) null);
        method.invoke(getCatalinaDaemonData(), (Object []) null);

    }


   /**
     * Stop the standalone server.
     */
    public void stopServer(String[] arguments)
        throws Exception {

        Object param[];
        Class<?> paramTypes[];
        if (arguments==null || arguments.length==0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method =
            getCatalinaDaemonData().getClass().getMethod("stopServer", paramTypes);
        method.invoke(getCatalinaDaemonData(), param);

    }


    /**
     * Set flag.
     */
    public void setAwait(boolean await)
        throws Exception {

        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Boolean.TYPE;
        Object paramValues[] = new Object[1];
        paramValues[0] = Boolean.valueOf(await);
        Method method =
            getCatalinaDaemonData().getClass().getMethod("setAwait", paramTypes);
        method.invoke(getCatalinaDaemonData(), paramValues);

    }

    public boolean getAwait()
        throws Exception
    {
        Class<?> paramTypes[] = new Class[0];
        Object paramValues[] = new Object[0];
        Method method =
            getCatalinaDaemonData().getClass().getMethod("getAwait", paramTypes);
        Boolean b=(Boolean)method.invoke(getCatalinaDaemonData(), paramValues);
        return b.booleanValue();
    }


    /**
     * Destroy the Catalina Daemon.
     */
    public void destroy() {

        // FIXME

    }


    /**
     * Main method and entry point when starting Tomcat via the provided
     * scripts.
     *
     * @param args Command line arguments to be processed
     */
    public static void main(String args[]) {

        if (getDaemonData() == null) {
            // Don't set daemon until init() has completed
            Bootstrap bootstrap = new Bootstrap();
            try {
                bootstrap.init();
            } catch (Throwable t) {
                handleThrowable(t);
                t.printStackTrace();
                return;
            }
            setDaemonData(bootstrap);
        } else {
            // When running as a service the call to stop will be on a new
            // thread so make sure the correct class loader is used to prevent
            // a range of class not found exceptions.
            Thread.currentThread().setContextClassLoader(getDaemonData().getCatalinaLoaderData());
        }

        try {
            String command = "start";
            if (args.length > 0) {
                command = args[args.length - 1];
            }

            if (command.equals("startd")) {
                args[args.length - 1] = "start";
                getDaemonData().load(args);
                getDaemonData().start();
            } else if (command.equals("stopd")) {
                args[args.length - 1] = "stop";
                getDaemonData().stop();
            } else if (command.equals("start")) {
                getDaemonData().setAwait(true);
                getDaemonData().load(args);
                getDaemonData().start();
            } else if (command.equals("stop")) {
                getDaemonData().stopServer(args);
            } else if (command.equals("configtest")) {
                getDaemonData().load(args);
                if (null==getDaemonData().getServer()) {
                    System.exit(1);
                }
                System.exit(0);
            } else {
                log.warn("Bootstrap: command \"" + command + "\" does not exist.");
            }
        } catch (Throwable t) {
            // Unwrap the Exception for clearer error reporting
            if (t instanceof InvocationTargetException &&
                    t.getCause() != null) {
                t = t.getCause();
            }
            handleThrowable(t);
            t.printStackTrace();
            System.exit(1);
        }

    }

    public void setCatalinaHome(String s) {
        System.setProperty(Globals.getCatalinaHomeProp(), s);
    }

    public void setCatalinaBase(String s) {
        System.setProperty(Globals.getCatalinaBaseProp(), s);
    }


    /**
     * Set the <code>catalina.base</code> System property to the current
     * working directory if it has not been set.
     */
    private void setCatalinaBase() {

        if (System.getProperty(Globals.getCatalinaBaseProp()) != null)
            return;
        if (System.getProperty(Globals.getCatalinaHomeProp()) != null)
            System.setProperty(Globals.getCatalinaBaseProp(),
                               System.getProperty(Globals.getCatalinaHomeProp()));
        else
            System.setProperty(Globals.getCatalinaBaseProp(),
                               System.getProperty("user.dir"));

    }


    /**
     * Set the <code>catalina.home</code> System property to the current
     * working directory if it has not been set.
     */
    private void setCatalinaHome() {

        if (System.getProperty(Globals.getCatalinaHomeProp()) != null)
            return;
        File bootstrapJar =
            new File(System.getProperty("user.dir"), "bootstrap.jar");
        if (bootstrapJar.exists()) {
            try {
                System.setProperty
                    (Globals.getCatalinaHomeProp(),
                     (new File(System.getProperty("user.dir"), ".."))
                     .getCanonicalPath());
            } catch (Exception e) {
                // Ignore
                System.setProperty(Globals.getCatalinaHomeProp(),
                                   System.getProperty("user.dir"));
            }
        } else {
            System.setProperty(Globals.getCatalinaHomeProp(),
                               System.getProperty("user.dir"));
        }

    }


    /**
     * Get the value of the catalina.home environment variable.
     */
    public static String getCatalinaHome() {
        return System.getProperty(Globals.getCatalinaHomeProp(),
                                  System.getProperty("user.dir"));
    }


    /**
     * Get the value of the catalina.base environment variable.
     */
    public static String getCatalinaBase() {
        return System.getProperty(Globals.getCatalinaBaseProp(), getCatalinaHome());
    }


    // Copied from ExceptionUtils since that class is not visible during start
    private static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }


	public static Bootstrap getDaemonData() {
		return daemon;
	}


	public static void setDaemonData(Bootstrap daemon) {
		Bootstrap.daemon = daemon;
	}


	public Object getCatalinaDaemonData() {
		return catalinaDaemon;
	}


	public void setCatalinaDaemonData(Object catalinaDaemon) {
		this.catalinaDaemon = catalinaDaemon;
	}


	public ClassLoader getCommonLoaderData() {
		return commonLoader;
	}


	public void setCommonLoaderData(ClassLoader commonLoader) {
		this.commonLoader = commonLoader;
	}


	private ClassLoader getCatalinaLoaderData() {
		return catalinaLoader;
	}


	private void setCatalinaLoaderData(ClassLoader catalinaLoader) {
		this.catalinaLoader = catalinaLoader;
	}


	public ClassLoader getSharedLoaderData() {
		return sharedLoader;
	}


	public void setSharedLoaderData(ClassLoader sharedLoader) {
		this.sharedLoader = sharedLoader;
	}
}
