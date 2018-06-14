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

package org.apache.tomcat.util.scan;

/**
 * String constants for the scan package.
 */
public final class Constants35 {

    private static final String Package = "org.apache.tomcat.util.scan";

    /* System properties */
    private static final String SKIP_JARS_PROPERTY =
        "tomcat.util.scan.DefaultJarScanner.jarsToSkip";

    /* Commons strings */
    private static final String JAR_EXT = ".jar";
    private static final String WEB_INF_LIB = "/WEB-INF/lib/";

    /* Context attributes - used to pass short-cuts to Jasper */
    private static final String MERGED_WEB_XML =
        "org.apache.tomcat.util.scan.MergedWebXml";

	public static String getSkipJarsProperty() {
		return SKIP_JARS_PROPERTY;
	}

	public static String getJarExt() {
		return JAR_EXT;
	}

	public static String getWebInfLib() {
		return WEB_INF_LIB;
	}

	public static String getMergedWebXml() {
		return MERGED_WEB_XML;
	}

	public static String getPackage() {
		return Package;
	}
}
