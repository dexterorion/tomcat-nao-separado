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

import java.net.URL;

import org.apache.catalina.util.SchemaResolver;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;

/**
 * Wrapper class around the Digester that hide Digester's initialization details
 *
 * @author Jean-Francois Arcand
 * 
 * @deprecated Use {@link org.apache.tomcat.util.descriptor.DigesterFactory2}
 */
@Deprecated
public class DigesterFactory {
    /**
     * The log.
     */
    private static final Log log = LogFactory.getLog(DigesterFactory.class);

    /**
     * Create a <code>Digester</code> parser with no <code>Rule</code>
     * associated and XML validation turned off.
     *
     * @deprecated  Unused - will be removed in 8.0.x
     */
    @Deprecated
    public static Digester newDigester(){
        return newDigester(false, false, null);
    }

    
    /**
     * Create a <code>Digester</code> parser with XML validation turned off.
     * @param rule an instance of <code>RuleSet</code> used for parsing the xml.
     *
     * @deprecated  Unused - will be removed in 8.0.x
     */
    @Deprecated
    public static Digester newDigester(RuleSet rule){
        return newDigester(false,false,rule);
    }

    
    /**
     * Create a <code>Digester</code> parser.
     * @param xmlValidation turn on/off xml validation
     * @param xmlNamespaceAware turn on/off namespace validation
     * @param rule an instance of <code>RuleSet</code> used for parsing the xml.
     */
    public static Digester newDigester(boolean xmlValidation,
                                       boolean xmlNamespaceAware,
                                       RuleSet rule) {
        Digester digester = new Digester();
        digester.setNamespaceAware(xmlNamespaceAware);
        digester.setValidating(xmlValidation);
        digester.setUseContextClassLoader(true);

        SchemaResolver schemaResolver = new SchemaResolver(digester);
        registerLocalSchema(schemaResolver);
        
        digester.setEntityResolver(schemaResolver);
        if ( rule != null ) {
            digester.addRuleSet(rule);
        }

        return (digester);
    }


    /**
     * Utilities used to force the parser to use local schema, when available,
     * instead of the <code>schemaLocation</code> XML element.
     */
    protected static void registerLocalSchema(SchemaResolver schemaResolver){
        // J2EE
        register(Constants17.getJ2eeschemaresourcepath14(),
                 Constants17.getJ2eeschemapublicid14(),
                 schemaResolver);

        register(Constants17.getJavaeeschemaresourcepath5(),
                Constants17.getJavaeeschemapublicid5(),
                schemaResolver);

        register(Constants17.getJavaeeschemaresourcepath6(),
                Constants17.getJavaeeschemapublicid6(),
                schemaResolver);

        // W3C
        register(Constants17.getW3cschemaresourcepath10(),
                 Constants17.getW3cschemapublicid10(),
                 schemaResolver);

        register(Constants17.getW3cschemadtdresourcepath10(),
                Constants17.getW3cschemadtdpublicid10(),
                schemaResolver);

        register(Constants17.getW3cdatatypesdtdresourcepath10(),
                Constants17.getW3cdatatypesdtdpublicid10(),
                schemaResolver);

        // JSP
        register(Constants17.getJspschemaresourcepath20(),
                 Constants17.getJspschemapublicid20(),
                 schemaResolver);

        register(Constants17.getJspschemaresourcepath21(),
                Constants17.getJspschemapublicid21(),
                schemaResolver);

        register(Constants17.getJspschemaresourcepath22(),
                Constants17.getJspschemapublicid22(),
                schemaResolver);

        // TLD
        register(Constants17.getTlddtdresourcepath11(),  
                 Constants17.getTlddtdpublicid11(),
                 schemaResolver);
        
        register(Constants17.getTlddtdresourcepath12(),
                 Constants17.getTlddtdpublicid12(),
                 schemaResolver);

        register(Constants17.getTldschemaresourcepath20(),
                 Constants17.getTldschemapublicid20(),
                 schemaResolver);

        register(Constants17.getTldschemaresourcepath21(),
                Constants17.getTldschemapublicid21(),
                schemaResolver);

        // web.xml    
        register(Constants17.getWebdtdresourcepath22(),
                 Constants17.getWebdtdpublicid22(),
                 schemaResolver);

        register(Constants17.getWebdtdresourcepath23(),
                 Constants17.getWebdtdpublicid23(),
                 schemaResolver);

        register(Constants17.getWebschemaresourcepath24(),
                 Constants17.getWebschemapublicid24(),
                 schemaResolver);

        register(Constants17.getWebschemaresourcepath25(),
                Constants17.getWebschemapublicid25(),
                schemaResolver);

        register(Constants17.getWebschemaresourcepath30(),
                Constants17.getWebschemapublicid30(),
                schemaResolver);

        register(Constants17.getWebcommonschemaresourcepath30(),
                Constants17.getWebcommonschemapublicid30(),
                schemaResolver);
        
        register(Constants17.getWebfragmentschemaresourcepath30(),
                Constants17.getWebfragmentschemapublicid30(),
                schemaResolver);

        // Web Service
        register(Constants17.getJ2eewebserviceschemaresourcepath11(),
                 Constants17.getJ2eewebserviceschemapublicid11(),
                 schemaResolver);

        register(Constants17.getJ2eewebserviceclientschemaresourcepath11(),
                 Constants17.getJ2eewebserviceclientschemapublicid11(),
                 schemaResolver);

        register(Constants17.getJavaeewebserviceschemaresourcepath12(),
                Constants17.getJavaeewebserviceschemapublicid12(),
                schemaResolver);

        register(Constants17.getJavaeewebserviceclientschemaresourcepath12(),
                Constants17.getJavaeewebserviceclientschemapublicid12(),
                schemaResolver);

        register(Constants17.getJavaeewebserviceschemaresourcepath13(),
                Constants17.getJavaeewebserviceschemapublicid13(),
                schemaResolver);

        register(Constants17.getJavaeewebserviceclientschemaresourcepath13(),
                Constants17.getJavaeewebserviceclientschemapublicid13(),
                schemaResolver);
    }


    /**
     * Load the resource and add it to the resolver.
     */
    protected static void register(String resourceURL, String resourcePublicId,
            SchemaResolver schemaResolver){
        URL url = DigesterFactory.class.getResource(resourceURL);
   
        if(url == null) {
            log.warn("Could not get url for " + resourceURL);
        } else {
            schemaResolver.register(resourcePublicId , url.toString() );
        }
    }

}
