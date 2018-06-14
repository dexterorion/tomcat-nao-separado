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


package org.apache.naming.factory;


/**
 * Static constants for this package.
 */

public final class Constants30 {

	private static final String Package = "org.apache.naming.factory";

	private static final String DEFAULT_RESOURCE_FACTORY = 
        Package + ".ResourceFactory";

	private static final String DEFAULT_RESOURCE_LINK_FACTORY = 
        Package + ".ResourceLinkFactory";

	private static final String DEFAULT_TRANSACTION_FACTORY = 
        Package + ".TransactionFactory";

	private static final String DEFAULT_RESOURCE_ENV_FACTORY = 
        Package + ".ResourceEnvFactory";

	private static final String DEFAULT_EJB_FACTORY = 
        Package + ".EjbFactory";

	private static final String DEFAULT_SERVICE_FACTORY = 
        Package + ".webservices.ServiceRefFactory";

	private static final String DEFAULT_HANDLER_FACTORY = 
        Package + ".HandlerFactory";

	private static final String DBCP_DATASOURCE_FACTORY = 
        "org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory";

	private static final String OPENEJB_EJB_FACTORY = 
        Package + ".OpenEjbFactory";

	private static final String FACTORY = "factory";

	public static String getDefaultResourceFactory() {
		return DEFAULT_RESOURCE_FACTORY;
	}

	public static String getDefaultResourceLinkFactory() {
		return DEFAULT_RESOURCE_LINK_FACTORY;
	}

	public static String getDefaultTransactionFactory() {
		return DEFAULT_TRANSACTION_FACTORY;
	}

	public static String getDefaultResourceEnvFactory() {
		return DEFAULT_RESOURCE_ENV_FACTORY;
	}

	public static String getDefaultEjbFactory() {
		return DEFAULT_EJB_FACTORY;
	}

	public static String getDefaultServiceFactory() {
		return DEFAULT_SERVICE_FACTORY;
	}

	public static String getDefaultHandlerFactory() {
		return DEFAULT_HANDLER_FACTORY;
	}

	public static String getDbcpDatasourceFactory() {
		return DBCP_DATASOURCE_FACTORY;
	}

	public static String getOpenejbEjbFactory() {
		return OPENEJB_EJB_FACTORY;
	}

	public static String getFactory() {
		return FACTORY;
	}

}
