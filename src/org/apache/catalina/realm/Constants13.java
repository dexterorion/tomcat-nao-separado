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


package org.apache.catalina.realm;


/**
 * Manifest constants for this Java package.
 *
 *
 * @author Craig R. McClanahan
 */
public final class Constants13 {

	private static final String Package = "org.apache.catalina.realm";
    
        // Authentication methods for login configuration
	private static final String FORM_METHOD = "FORM";

    // Form based authentication constants
	private static final String FORM_ACTION = "/j_security_check";

    // User data constraints for transport guarantee
	private static final String NONE_TRANSPORT = "NONE";
	private static final String INTEGRAL_TRANSPORT = "INTEGRAL";
	private static final String CONFIDENTIAL_TRANSPORT = "CONFIDENTIAL";
	public static String getPackage() {
		return Package;
	}
	public static String getFormMethod() {
		return FORM_METHOD;
	}
	public static String getFormAction() {
		return FORM_ACTION;
	}
	public static String getNoneTransport() {
		return NONE_TRANSPORT;
	}
	public static String getIntegralTransport() {
		return INTEGRAL_TRANSPORT;
	}
	public static String getConfidentialTransport() {
		return CONFIDENTIAL_TRANSPORT;
	}

}
