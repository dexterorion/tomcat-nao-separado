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


/**
 * String constants for the startup package.
 *
 * @author Craig R. McClanahan
 * @author Jean-Francois Arcand
 */
public final class Constants17 {

    private static final String Package = "org.apache.catalina.startup";

    private static final String ApplicationContextXml = "META-INF/context.xml";
    private static final String ApplicationWebXml = "/WEB-INF/web.xml";
    private static final String DefaultContextXml = "conf/context.xml";
    private static final String DefaultWebXml = "conf/web.xml";
    private static final String HostContextXml = "context.xml.default";
    private static final String HostWebXml = "web.xml.default";

    private static final String DEFAULT_JARS_TO_SKIP =
            "tomcat.util.scan.DefaultJarScanner.jarsToSkip";
    private static final String PLUGGABILITY_JARS_TO_SKIP =
            "org.apache.catalina.startup.ContextConfig.jarsToSkip";
    private static final String TLD_JARS_TO_SKIP =
            "org.apache.catalina.startup.TldConfig.jarsToSkip";

    /**
     * A dummy value used to suppress loading the default web.xml file.
     *
     * <p>
     * It is useful when embedding Tomcat, when the default configuration is
     * done programmatically, e.g. by calling
     * <code>Tomcat.initWebappDefaults(context)</code>.
     *
     * @see Tomcat
     */
    private static final String NoDefaultWebXml = "org/apache/catalina/startup/NO_DEFAULT_XML";

    // J2EE
    private static final String J2eeSchemaPublicId_14 =
        "j2ee_1_4.xsd";
    private static final String J2eeSchemaResourcePath_14 =
        "/javax/servlet/resources/j2ee_1_4.xsd";

    private static final String JavaeeSchemaPublicId_5 =
        "javaee_5.xsd";
    private static final String JavaeeSchemaResourcePath_5 =
        "/javax/servlet/resources/javaee_5.xsd";

    private static final String JavaeeSchemaPublicId_6 =
        "javaee_6.xsd";
    private static final String JavaeeSchemaResourcePath_6 =
        "/javax/servlet/resources/javaee_6.xsd";

    
    // W3C
    private static final String W3cSchemaPublicId_10 =
        "xml.xsd";
    private static final String W3cSchemaResourcePath_10 =
        "/javax/servlet/resources/xml.xsd";

    private static final String W3cSchemaDTDPublicId_10 =
        "XMLSchema.dtd";
    private static final String W3cSchemaDTDResourcePath_10 =
        "/javax/servlet/resources/XMLSchema.dtd";

    private static final String W3cDatatypesDTDPublicId_10 =
        "datatypes.dtd";
    private static final String W3cDatatypesDTDResourcePath_10 =
        "/javax/servlet/resources/datatypes.dtd";

    
    // JSP
    private static final String JspSchemaPublicId_20 =
        "jsp_2_0.xsd";
    private static final String JspSchemaResourcePath_20 =
        "/javax/servlet/jsp/resources/jsp_2_0.xsd";
    
    private static final String JspSchemaPublicId_21 =
        "jsp_2_1.xsd";
    private static final String JspSchemaResourcePath_21 =
        "/javax/servlet/jsp/resources/jsp_2_1.xsd";

    private static final String JspSchemaPublicId_22 =
        "jsp_2_2.xsd";
    private static final String JspSchemaResourcePath_22 =
        "/javax/servlet/jsp/resources/jsp_2_2.xsd";


    // TLD
    private static final String TldDtdPublicId_11 =
        "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN";
    private static final String TldDtdResourcePath_11 =
        "/javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd";

    private static final String TldDtdPublicId_12 =
        "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN";
    private static final String TldDtdResourcePath_12 =
        "/javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd";

    private static final String TldSchemaPublicId_20 =
        "web-jsptaglibrary_2_0.xsd";
    private static final String TldSchemaResourcePath_20 =
        "/javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd";

    private static final String TldSchemaPublicId_21 =
        "web-jsptaglibrary_2_1.xsd";
    private static final String TldSchemaResourcePath_21 =
        "/javax/servlet/jsp/resources/web-jsptaglibrary_2_1.xsd";

    
    // web.xml
    private static final String WebDtdPublicId_22 =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    private static final String WebDtdResourcePath_22 =
        "/javax/servlet/resources/web-app_2_2.dtd";

    private static final String WebDtdPublicId_23 =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    private static final String WebDtdResourcePath_23 =
        "/javax/servlet/resources/web-app_2_3.dtd";

    private static final String WebSchemaPublicId_24 =
        "web-app_2_4.xsd";
    private static final String WebSchemaResourcePath_24 =
        "/javax/servlet/resources/web-app_2_4.xsd";

    private static final String WebSchemaPublicId_25 =
        "web-app_2_5.xsd";
    private static final String WebSchemaResourcePath_25 =
        "/javax/servlet/resources/web-app_2_5.xsd";

    private static final String WebSchemaPublicId_30 =
        "web-app_3_0.xsd";
    private static final String WebSchemaResourcePath_30 =
        "/javax/servlet/resources/web-app_3_0.xsd";

    private static final String WebCommonSchemaPublicId_30 =
        "web-common_3_0.xsd";
    private static final String WebCommonSchemaResourcePath_30 =
        "/javax/servlet/resources/web-common_3_0.xsd";

    private static final String WebFragmentSchemaPublicId_30 =
        "web-fragment_3_0.xsd";
    private static final String WebFragmentSchemaResourcePath_30 =
        "/javax/servlet/resources/web-fragment_3_0.xsd";
    
    // Web service
    private static final String J2eeWebServiceSchemaPublicId_11 =
            "j2ee_web_services_1_1.xsd";
    private static final String J2eeWebServiceSchemaResourcePath_11 =
            "/javax/servlet/resources/j2ee_web_services_1_1.xsd";
    
    private static final String J2eeWebServiceClientSchemaPublicId_11 =
            "j2ee_web_services_client_1_1.xsd";
    private static final String J2eeWebServiceClientSchemaResourcePath_11 =
            "/javax/servlet/resources/j2ee_web_services_client_1_1.xsd";

    private static final String JavaeeWebServiceSchemaPublicId_12 =
        "javaee_web_services_1_2.xsd";
    private static final String JavaeeWebServiceSchemaResourcePath_12 =
        "/javax/servlet/resources/javaee_web_services_1_2.xsd";

    private static final String JavaeeWebServiceClientSchemaPublicId_12 =
        "javaee_web_services_client_1_2.xsd";
    private static final String JavaeeWebServiceClientSchemaResourcePath_12 =
        "/javax/servlet/resources/javaee_web_services_client_1_2.xsd";

    private static final String JavaeeWebServiceSchemaPublicId_13 =
        "javaee_web_services_1_3.xsd";
    private static final String JavaeeWebServiceSchemaResourcePath_13 =
        "/javax/servlet/resources/javaee_web_services_1_3.xsd";

    private static final String JavaeeWebServiceClientSchemaPublicId_13 =
        "javaee_web_services_client_1_3.xsd";
    private static final String JavaeeWebServiceClientSchemaResourcePath_13 =
        "/javax/servlet/resources/javaee_web_services_client_1_3.xsd";
	public static String getPackage() {
		return Package;
	}
	public static String getApplicationcontextxml() {
		return ApplicationContextXml;
	}
	public static String getApplicationwebxml() {
		return ApplicationWebXml;
	}
	public static String getDefaultcontextxml() {
		return DefaultContextXml;
	}
	public static String getDefaultwebxml() {
		return DefaultWebXml;
	}
	public static String getHostcontextxml() {
		return HostContextXml;
	}
	public static String getHostwebxml() {
		return HostWebXml;
	}
	public static String getDefaultJarsToSkip() {
		return DEFAULT_JARS_TO_SKIP;
	}
	public static String getPluggabilityJarsToSkip() {
		return PLUGGABILITY_JARS_TO_SKIP;
	}
	public static String getTldJarsToSkip() {
		return TLD_JARS_TO_SKIP;
	}
	public static String getNodefaultwebxml() {
		return NoDefaultWebXml;
	}
	public static String getJ2eeschemapublicid14() {
		return J2eeSchemaPublicId_14;
	}
	public static String getJ2eeschemaresourcepath14() {
		return J2eeSchemaResourcePath_14;
	}
	public static String getJavaeeschemapublicid5() {
		return JavaeeSchemaPublicId_5;
	}
	public static String getJavaeeschemaresourcepath5() {
		return JavaeeSchemaResourcePath_5;
	}
	public static String getJavaeeschemapublicid6() {
		return JavaeeSchemaPublicId_6;
	}
	public static String getJavaeeschemaresourcepath6() {
		return JavaeeSchemaResourcePath_6;
	}
	public static String getW3cschemapublicid10() {
		return W3cSchemaPublicId_10;
	}
	public static String getW3cschemaresourcepath10() {
		return W3cSchemaResourcePath_10;
	}
	public static String getW3cschemadtdpublicid10() {
		return W3cSchemaDTDPublicId_10;
	}
	public static String getW3cschemadtdresourcepath10() {
		return W3cSchemaDTDResourcePath_10;
	}
	public static String getW3cdatatypesdtdpublicid10() {
		return W3cDatatypesDTDPublicId_10;
	}
	public static String getW3cdatatypesdtdresourcepath10() {
		return W3cDatatypesDTDResourcePath_10;
	}
	public static String getJspschemapublicid20() {
		return JspSchemaPublicId_20;
	}
	public static String getJspschemaresourcepath20() {
		return JspSchemaResourcePath_20;
	}
	public static String getJspschemapublicid21() {
		return JspSchemaPublicId_21;
	}
	public static String getJspschemaresourcepath21() {
		return JspSchemaResourcePath_21;
	}
	public static String getJspschemapublicid22() {
		return JspSchemaPublicId_22;
	}
	public static String getJspschemaresourcepath22() {
		return JspSchemaResourcePath_22;
	}
	public static String getTlddtdpublicid11() {
		return TldDtdPublicId_11;
	}
	public static String getTlddtdresourcepath11() {
		return TldDtdResourcePath_11;
	}
	public static String getTlddtdpublicid12() {
		return TldDtdPublicId_12;
	}
	public static String getTlddtdresourcepath12() {
		return TldDtdResourcePath_12;
	}
	public static String getTldschemapublicid20() {
		return TldSchemaPublicId_20;
	}
	public static String getTldschemaresourcepath20() {
		return TldSchemaResourcePath_20;
	}
	public static String getTldschemapublicid21() {
		return TldSchemaPublicId_21;
	}
	public static String getTldschemaresourcepath21() {
		return TldSchemaResourcePath_21;
	}
	public static String getWebdtdpublicid22() {
		return WebDtdPublicId_22;
	}
	public static String getWebdtdresourcepath22() {
		return WebDtdResourcePath_22;
	}
	public static String getWebdtdpublicid23() {
		return WebDtdPublicId_23;
	}
	public static String getWebdtdresourcepath23() {
		return WebDtdResourcePath_23;
	}
	public static String getWebschemapublicid24() {
		return WebSchemaPublicId_24;
	}
	public static String getWebschemaresourcepath24() {
		return WebSchemaResourcePath_24;
	}
	public static String getWebschemapublicid25() {
		return WebSchemaPublicId_25;
	}
	public static String getWebschemaresourcepath25() {
		return WebSchemaResourcePath_25;
	}
	public static String getWebschemapublicid30() {
		return WebSchemaPublicId_30;
	}
	public static String getWebschemaresourcepath30() {
		return WebSchemaResourcePath_30;
	}
	public static String getWebcommonschemapublicid30() {
		return WebCommonSchemaPublicId_30;
	}
	public static String getWebcommonschemaresourcepath30() {
		return WebCommonSchemaResourcePath_30;
	}
	public static String getWebfragmentschemapublicid30() {
		return WebFragmentSchemaPublicId_30;
	}
	public static String getWebfragmentschemaresourcepath30() {
		return WebFragmentSchemaResourcePath_30;
	}
	public static String getJ2eewebserviceschemapublicid11() {
		return J2eeWebServiceSchemaPublicId_11;
	}
	public static String getJ2eewebserviceschemaresourcepath11() {
		return J2eeWebServiceSchemaResourcePath_11;
	}
	public static String getJ2eewebserviceclientschemapublicid11() {
		return J2eeWebServiceClientSchemaPublicId_11;
	}
	public static String getJ2eewebserviceclientschemaresourcepath11() {
		return J2eeWebServiceClientSchemaResourcePath_11;
	}
	public static String getJavaeewebserviceschemapublicid12() {
		return JavaeeWebServiceSchemaPublicId_12;
	}
	public static String getJavaeewebserviceschemaresourcepath12() {
		return JavaeeWebServiceSchemaResourcePath_12;
	}
	public static String getJavaeewebserviceclientschemapublicid12() {
		return JavaeeWebServiceClientSchemaPublicId_12;
	}
	public static String getJavaeewebserviceclientschemaresourcepath12() {
		return JavaeeWebServiceClientSchemaResourcePath_12;
	}
	public static String getJavaeewebserviceschemapublicid13() {
		return JavaeeWebServiceSchemaPublicId_13;
	}
	public static String getJavaeewebserviceschemaresourcepath13() {
		return JavaeeWebServiceSchemaResourcePath_13;
	}
	public static String getJavaeewebserviceclientschemapublicid13() {
		return JavaeeWebServiceClientSchemaPublicId_13;
	}
	public static String getJavaeewebserviceclientschemaresourcepath13() {
		return JavaeeWebServiceClientSchemaResourcePath_13;
	}

}
