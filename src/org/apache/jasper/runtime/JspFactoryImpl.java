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
package org.apache.jasper.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import org.apache.jasper.Constants28;
import org.apache.jasper.util.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Implementation of JspFactory.
 *
 * @author Anil K. Vijendran
 */
public class JspFactoryImpl extends JspFactory {

    // Logger
    private final Log log = LogFactory.getLog(JspFactoryImpl.class);

    private static final String SPEC_VERSION = "2.2";
    private static final boolean USE_POOL = 
        Boolean.valueOf(System.getProperty("org.apache.jasper.runtime.JspFactoryImpl.USE_POOL", "true")).booleanValue();
    private static final int POOL_SIZE = 
        Integer.valueOf(System.getProperty("org.apache.jasper.runtime.JspFactoryImpl.POOL_SIZE", "8")).intValue();

    private ThreadLocal<JspFactoryImplPageContextPool> localPool = new ThreadLocal<JspFactoryImplPageContextPool>();

    @Override
    public PageContext getPageContext(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {

        if( Constants28.isSecurityEnabled() ) {
            JspFactoryImplPrivilegedGetPageContext dp = new JspFactoryImplPrivilegedGetPageContext(
                    this, servlet, request, response, errorPageURL,
                    needsSession, bufferSize, autoflush);
            return AccessController.doPrivileged(dp);
        } else {
            return internalGetPageContext(servlet, request, response,
                    errorPageURL, needsSession,
                    bufferSize, autoflush);
        }
    }

    @Override
    public void releasePageContext(PageContext pc) {
        if( pc == null )
            return;
        if( Constants28.isSecurityEnabled() ) {
            JspFactoryImplPrivilegedReleasePageContext dp = new JspFactoryImplPrivilegedReleasePageContext(
                    this,pc);
            AccessController.doPrivileged(dp);
        } else {
            internalReleasePageContext(pc);
        }
    }

    @Override
    public JspEngineInfo getEngineInfo() {
        return new JspEngineInfo() {
            @Override
            public String getSpecificationVersion() {
                return SPEC_VERSION;
            }
        };
    }

    public PageContext internalGetPageContext(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {
        try {
            PageContext pc;
            if (USE_POOL) {
                JspFactoryImplPageContextPool pool = localPool.get();
                if (pool == null) {
                    pool = new JspFactoryImplPageContextPool();
                    localPool.set(pool);
                }
                pc = pool.get();
                if (pc == null) {
                    pc = new PageContextImpl();
                }
            } else {
                pc = new PageContextImpl();
            }
            pc.initialize(servlet, request, response, errorPageURL, 
                    needsSession, bufferSize, autoflush);
            return pc;
        } catch (Throwable ex) {
            ExceptionUtils.handleThrowable(ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            log.fatal("Exception initializing page context", ex);
            return null;
        }
    }

    void internalReleasePageContext(PageContext pc) {
        pc.release();
        if (USE_POOL && (pc instanceof PageContextImpl)) {
            localPool.get().put(pc);
        }
    }

    @Override
    public JspApplicationContext getJspApplicationContext(
            final ServletContext context) {
        if (Constants28.isSecurityEnabled()) {
            return AccessController.doPrivileged(
                    new PrivilegedAction<JspApplicationContext>() {
                @Override
                public JspApplicationContext run() {
                    return JspApplicationContextImpl.getInstance(context);
                }
            });
        } else {
            return JspApplicationContextImpl.getInstance(context);
        }
    }

	public static int getPoolSize() {
		return POOL_SIZE;
	}
}
