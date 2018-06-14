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
package org.apache.catalina.ha.context;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.ha.CatalinaCluster;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.tipis.AbstractReplicatedMapMapOwner;
import org.apache.catalina.tribes.tipis.ReplicatedMap;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * @author Filip Hanik
 * @version 1.0
 */
public class ReplicatedContext extends StandardContext implements AbstractReplicatedMapMapOwner {
    private int mapSendOptions = Channel.SEND_OPTIONS_DEFAULT;
    private static final Log log = LogFactory.getLog( ReplicatedContext.class );
    private static long DEFAULT_REPL_TIMEOUT = 15000;//15 seconds

    /**
     * Start this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void startInternal() throws LifecycleException {

        try {
            CatalinaCluster catclust = (CatalinaCluster)this.getCluster();
            if (this.getContext() == null) this.setContext(new ReplicatedContextReplApplContext(this));
            if ( catclust != null ) {
                ReplicatedMap<String,Object> map =
                        new ReplicatedMap<String,Object>(this,
                                catclust.getChannel(),DEFAULT_REPL_TIMEOUT,
                                getName(),getClassLoaders());
                map.setChannelSendOptions(mapSendOptions);
                ((ReplicatedContextReplApplContext)this.getContext()).setAttributeMap(map);
                if (getAltDDName() != null) getContext().setAttribute(Globals.getAltDdAttr(), getAltDDName());
            }
            super.startInternal();
        }  catch ( Exception x ) {
            log.error("Unable to start ReplicatedContext",x);
            throw new LifecycleException("Failed to start ReplicatedContext",x);
        }
    }

    /**
     * Stop this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void stopInternal() throws LifecycleException {

        super.stopInternal();

        Map<String,Object> map =
                ((ReplicatedContextReplApplContext)this.getContext()).getAttributeMap();
        if ( map!=null && map instanceof ReplicatedMap) {
            ((ReplicatedMap<?,?>)map).breakdown();
        }
    }


    public void setMapSendOptions(int mapSendOptions) {
        this.mapSendOptions = mapSendOptions;
    }

    public int getMapSendOptions() {
        return mapSendOptions;
    }

    public ClassLoader[] getClassLoaders() {
        Loader loader = null;
        ClassLoader classLoader = null;
        loader = this.getLoader();
        if (loader != null) classLoader = loader.getClassLoader();
        if ( classLoader == null ) classLoader = Thread.currentThread().getContextClassLoader();
        if ( classLoader == Thread.currentThread().getContextClassLoader() ) {
            return new ClassLoader[] {classLoader};
        } else {
            return new ClassLoader[] {classLoader,Thread.currentThread().getContextClassLoader()};
        }
    }

    @Override
    public ServletContext getServletContext() {
        if (getContext() == null) {
            setContext(new ReplicatedContextReplApplContext(this));
            if (getAltDDName() != null)
                getContext().setAttribute(Globals.getAltDdAttr(),getAltDDName());
        }

        return ((ReplicatedContextReplApplContext)getContext()).getFacade();

    }

    @Override
    public void objectMadePrimay(Object key, Object value) {
        //noop
    }
}