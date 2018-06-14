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
package org.apache.tomcat.util.descriptor;

/**
 * Defines constants for well-known Public and System identifiers documented by
 * the Servlet and JSP specifications.
 */
public final class XmlIdentifiers {

	// from W3C
	private static final String XML_2001_XSD = "http://www.w3.org/2001/xml.xsd";
	private static final String DATATYPES_PUBLIC = "datatypes";
	private static final String XSD_10_PUBLIC = "-//W3C//DTD XMLSCHEMA 200102//EN";

	// from J2EE 1.2
	private static final String WEB_22_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
	private static final String WEB_22_SYSTEM = "http://java.sun.com/dtd/web-app_2_2.dtd";
	private static final String TLD_11_PUBLIC = "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN";
	private static final String TLD_11_SYSTEM = "http://java.sun.com/dtd/web-jsptaglibrary_1_1.dtd";

	// from J2EE 1.3
	private static final String WEB_23_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
	private static final String WEB_23_SYSTEM = "http://java.sun.com/dtd/web-app_2_3.dtd";
	private static final String TLD_12_PUBLIC = "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN";
	private static final String TLD_12_SYSTEM = "http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd";

	// from J2EE 1.4
	private static final String JAVAEE_1_4_NS = "http://java.sun.com/xml/ns/j2ee";
	private static final String WEB_24_XSD = JAVAEE_1_4_NS + "/web-app_2_4.xsd";
	private static final String TLD_20_XSD = JAVAEE_1_4_NS
			+ "/web-jsptaglibrary_2_0.xsd";
	private static final String WEBSERVICES_11_XSD = "http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd";

	// from JavaEE 5
	private static final String JAVAEE_5_NS = "http://java.sun.com/xml/ns/javaee";
	private static final String WEB_25_XSD = JAVAEE_5_NS + "/web-app_2_5.xsd";
	private static final String TLD_21_XSD = JAVAEE_5_NS
			+ "/web-jsptaglibrary_2_1.xsd";
	private static final String WEBSERVICES_12_XSD = JAVAEE_5_NS
			+ "javaee_web_services_1_2.xsd";

	// from JavaEE 6
	private static final String JAVAEE_6_NS = JAVAEE_5_NS;
	private static final String WEB_30_XSD = JAVAEE_6_NS + "/web-app_3_0.xsd";
	private static final String WEB_FRAGMENT_30_XSD = JAVAEE_6_NS
			+ "/web-fragment_3_0.xsd";
	private static final String WEBSERVICES_13_XSD = JAVAEE_6_NS
			+ "/javaee_web_services_1_3.xsd";

	public XmlIdentifiers() {
	}

	public static String getXml2001Xsd() {
		return XML_2001_XSD;
	}

	public static String getDatatypesPublic() {
		return DATATYPES_PUBLIC;
	}

	public static String getXsd10Public() {
		return XSD_10_PUBLIC;
	}

	public static String getWeb22Public() {
		return WEB_22_PUBLIC;
	}

	public static String getWeb22System() {
		return WEB_22_SYSTEM;
	}

	public static String getTld11Public() {
		return TLD_11_PUBLIC;
	}

	public static String getTld11System() {
		return TLD_11_SYSTEM;
	}

	public static String getWeb23Public() {
		return WEB_23_PUBLIC;
	}

	public static String getWeb23System() {
		return WEB_23_SYSTEM;
	}

	public static String getTld12Public() {
		return TLD_12_PUBLIC;
	}

	public static String getTld12System() {
		return TLD_12_SYSTEM;
	}

	public static String getWeb24Xsd() {
		return WEB_24_XSD;
	}

	public static String getTld20Xsd() {
		return TLD_20_XSD;
	}

	public static String getWebservices11Xsd() {
		return WEBSERVICES_11_XSD;
	}

	public static String getWeb25Xsd() {
		return WEB_25_XSD;
	}

	public static String getTld21Xsd() {
		return TLD_21_XSD;
	}

	public static String getWebservices12Xsd() {
		return WEBSERVICES_12_XSD;
	}

	public static String getWeb30Xsd() {
		return WEB_30_XSD;
	}

	public static String getWebFragment30Xsd() {
		return WEB_FRAGMENT_30_XSD;
	}

	public static String getWebservices13Xsd() {
		return WEBSERVICES_13_XSD;
	}

	public static String getJavaee14Ns() {
		return JAVAEE_1_4_NS;
	}

	public static String getJavaee5Ns() {
		return JAVAEE_5_NS;
	}

	public static String getJavaee6Ns() {
		return JAVAEE_6_NS;
	}

}