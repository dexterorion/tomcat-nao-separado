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
package org.apache.tomcat.util.modeler.modules;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry2;


public class MbeansDescriptorsSerSource extends ModelerSource
{
    private static final Log log = LogFactory.getLog(MbeansDescriptorsSerSource.class);
    private Registry2 registry;
    private String type;
    private List<ObjectName> mbeans=new ArrayList<ObjectName>();

    public void setRegistry(Registry2 reg) {
        this.registry=reg;
    }

    /**
     * @deprecated Unused. Will be removed in Tomcat 8.0.x
     */
    @Deprecated
    public void setLocation( String loc ) {
        super.setLocation(loc);
    }

    /** Used if a single component is loaded
     *
     * @param type
     */
    public void setType( String type ) {
       this.type=type;
    }

    public void setSource( Object source ) {
        super.setSource(source);
    }

    @Override
    public List<ObjectName> loadDescriptors( Registry2 registry, String type,
            Object source) throws Exception {
        setRegistry(registry);
        setType(type);
        setSource(source);
        execute();
        return mbeans;
    }

    public void execute() throws Exception {
        if( registry==null ) registry=Registry2.getRegistry(null, null);
        long t1=System.currentTimeMillis();
        try {
            InputStream stream=null;
            if( getSource() instanceof URL ) {
                stream=((URL)getSource()).openStream();
            }
            if( getSource() instanceof InputStream ) {
                stream=(InputStream)getSource();
            }
            if( stream==null ) {
                throw new Exception( "Can't process "+ getSource());
            }
            ObjectInputStream ois=new ObjectInputStream(stream);
            Thread.currentThread().setContextClassLoader(ManagedBean.class.getClassLoader());
            Object obj=ois.readObject();
            //log.info("Reading " + obj);
            ManagedBean beans[]=(ManagedBean[])obj;
            // after all are read without error
            for( int i=0; i<beans.length; i++ ) {
                registry.addManagedBean(beans[i]);
            }

        } catch( Exception ex ) {
            log.error( "Error reading descriptors " + getSource() + " " +  ex.toString(),
                    ex);
            throw ex;
        }
        long t2=System.currentTimeMillis();
        log.info( "Reading descriptors ( ser ) " + (t2-t1));
    }
}
