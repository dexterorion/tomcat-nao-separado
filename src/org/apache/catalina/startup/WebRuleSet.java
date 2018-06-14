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

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;
import org.apache.tomcat.util.digester.SetNextRule;
import org.apache.tomcat.util.res.StringManager3;

/**
 * <p>
 * <strong>RuleSet</strong> for processing the contents of a web application
 * deployment descriptor (<code>/WEB-INF/web.xml</code>) resource.
 * </p>
 *
 * @author Craig R. McClanahan
 */
public class WebRuleSet extends RuleSetBase {

	/**
	 * The string resources for this package.
	 */
	private static final StringManager3 sm = StringManager3.getManager(Constants17
			.getPackage());

	// ----------------------------------------------------- Instance Variables

	/**
	 * The matching pattern prefix to use for recognizing our elements.
	 */
	private String prefix = null;

	/**
	 * The full pattern matching prefix, including the webapp or web-fragment
	 * component, to use for matching elements
	 */
	private String fullPrefix = null;

	/**
	 * Flag that indicates if this ruleset is for a web-fragment.xml file or for
	 * a web.xml file.
	 */
	private boolean fragment = false;

	/**
	 * The <code>SetSessionConfig</code> rule used to parse the web.xml
	 */
	private WebRuleSetSetSessionConfig sessionConfig = new WebRuleSetSetSessionConfig();

	/**
	 * The <code>SetLoginConfig</code> rule used to parse the web.xml
	 */
	private WebRuleSetSetLoginConfig loginConfig = new WebRuleSetSetLoginConfig();

	/**
	 * The <code>SetJspConfig</code> rule used to parse the web.xml
	 */
	private WebRuleSetSetJspConfig jspConfig = new WebRuleSetSetJspConfig();

	/**
	 * The <code>NameRule</code> rule used to parse the web.xml
	 */
	private WebRuleSetNameRule name = new WebRuleSetNameRule();

	/**
	 * The <code>AbsoluteOrderingRule</code> rule used to parse the web.xml
	 */
	private WebRuleSetAbsoluteOrderingRule absoluteOrdering;

	/**
	 * The <code>RelativeOrderingRule</code> rule used to parse the web.xml
	 */
	private WebRuleSetRelativeOrderingRule relativeOrdering;

	// ------------------------------------------------------------ Constructor

	/**
	 * Construct an instance of this <code>RuleSet</code> with the default
	 * matching pattern prefix and default fragment setting.
	 */
	public WebRuleSet() {

		this("", false);

	}

	/**
	 * Construct an instance of this <code>RuleSet</code> with the default
	 * matching pattern prefix.
	 */
	public WebRuleSet(boolean fragment) {

		this("", fragment);

	}

	/**
	 * Construct an instance of this <code>RuleSet</code> with the specified
	 * matching pattern prefix.
	 *
	 * @param prefix
	 *            Prefix for matching pattern rules (including the trailing
	 *            slash character)
	 */
	public WebRuleSet(String prefix, boolean fragment) {

		super();
		this.setNamespaceURI(null);
		this.prefix = prefix;
		this.fragment = fragment;

		if (fragment) {
			fullPrefix = prefix + "web-fragment";
		} else {
			fullPrefix = prefix + "web-app";
		}

		absoluteOrdering = new WebRuleSetAbsoluteOrderingRule(fragment);
		relativeOrdering = new WebRuleSetRelativeOrderingRule(fragment);
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * <p>
	 * Add the set of Rule instances defined in this RuleSet to the specified
	 * <code>Digester</code> instance, associating them with our namespace URI
	 * (if any). This method should only be called by a Digester instance.
	 * </p>
	 *
	 * @param digester
	 *            Digester instance to which the new Rule instances should be
	 *            added.
	 */
	@Override
	public void addRuleInstances(Digester digester) {
		digester.addRule(fullPrefix, new WebRuleSetSetPublicIdRule("setPublicId"));
		digester.addRule(fullPrefix, new WebRuleSetIgnoreAnnotationsRule());
		digester.addRule(fullPrefix, new WebRuleSetVersionRule());

		// Required for both fragments and non-fragments
		digester.addRule(fullPrefix + "/absolute-ordering", absoluteOrdering);
		digester.addRule(fullPrefix + "/ordering", relativeOrdering);

		if (fragment) {
			// web-fragment.xml
			digester.addRule(fullPrefix + "/name", name);
			digester.addCallMethod(fullPrefix + "/ordering/after/name",
					"addAfterOrdering", 0);
			digester.addCallMethod(fullPrefix + "/ordering/after/others",
					"addAfterOrderingOthers");
			digester.addCallMethod(fullPrefix + "/ordering/before/name",
					"addBeforeOrdering", 0);
			digester.addCallMethod(fullPrefix + "/ordering/before/others",
					"addBeforeOrderingOthers");
		} else {
			// web.xml
			digester.addCallMethod(fullPrefix + "/absolute-ordering/name",
					"addAbsoluteOrdering", 0);
			digester.addCallMethod(fullPrefix + "/absolute-ordering/others",
					"addAbsoluteOrderingOthers");
		}

		digester.addCallMethod(fullPrefix + "/context-param",
				"addContextParam", 2);
		digester.addCallParam(fullPrefix + "/context-param/param-name", 0);
		digester.addCallParam(fullPrefix + "/context-param/param-value", 1);

		digester.addCallMethod(fullPrefix + "/display-name", "setDisplayName",
				0);

		digester.addRule(fullPrefix + "/distributable",
				new WebRuleSetSetDistributableRule());

		configureNamingRules(digester);

		digester.addObjectCreate(fullPrefix + "/error-page",
				"org.apache.catalina.deploy.ErrorPage");
		digester.addSetNext(fullPrefix + "/error-page", "addErrorPage",
				"org.apache.catalina.deploy.ErrorPage");

		digester.addCallMethod(fullPrefix + "/error-page/error-code",
				"setErrorCode", 0);
		digester.addCallMethod(fullPrefix + "/error-page/exception-type",
				"setExceptionType", 0);
		digester.addCallMethod(fullPrefix + "/error-page/location",
				"setLocation", 0);

		digester.addObjectCreate(fullPrefix + "/filter",
				"org.apache.catalina.deploy.FilterDef");
		digester.addSetNext(fullPrefix + "/filter", "addFilter",
				"org.apache.catalina.deploy.FilterDef");

		digester.addCallMethod(fullPrefix + "/filter/description",
				"setDescription", 0);
		digester.addCallMethod(fullPrefix + "/filter/display-name",
				"setDisplayName", 0);
		digester.addCallMethod(fullPrefix + "/filter/filter-class",
				"setFilterClass", 0);
		digester.addCallMethod(fullPrefix + "/filter/filter-name",
				"setFilterName", 0);
		digester.addCallMethod(fullPrefix + "/filter/icon/large-icon",
				"setLargeIcon", 0);
		digester.addCallMethod(fullPrefix + "/filter/icon/small-icon",
				"setSmallIcon", 0);
		digester.addCallMethod(fullPrefix + "/filter/async-supported",
				"setAsyncSupported", 0);

		digester.addCallMethod(fullPrefix + "/filter/init-param",
				"addInitParameter", 2);
		digester.addCallParam(fullPrefix + "/filter/init-param/param-name", 0);
		digester.addCallParam(fullPrefix + "/filter/init-param/param-value", 1);

		digester.addObjectCreate(fullPrefix + "/filter-mapping",
				"org.apache.catalina.deploy.FilterMap");
		digester.addSetNext(fullPrefix + "/filter-mapping", "addFilterMapping",
				"org.apache.catalina.deploy.FilterMap");

		digester.addCallMethod(fullPrefix + "/filter-mapping/filter-name",
				"setFilterName", 0);
		digester.addCallMethod(fullPrefix + "/filter-mapping/servlet-name",
				"addServletName", 0);
		digester.addCallMethod(fullPrefix + "/filter-mapping/url-pattern",
				"addURLPattern", 0);

		digester.addCallMethod(fullPrefix + "/filter-mapping/dispatcher",
				"setDispatcher", 0);

		digester.addCallMethod(fullPrefix + "/listener/listener-class",
				"addListener", 0);

		digester.addRule(fullPrefix + "/jsp-config", jspConfig);

		digester.addObjectCreate(fullPrefix + "/jsp-config/jsp-property-group",
				"org.apache.catalina.deploy.JspPropertyGroup");
		digester.addSetNext(fullPrefix + "/jsp-config/jsp-property-group",
				"addJspPropertyGroup",
				"org.apache.catalina.deploy.JspPropertyGroup");
		digester.addCallMethod(
				fullPrefix
						+ "/jsp-config/jsp-property-group/deferred-syntax-allowed-as-literal",
				"setDeferredSyntax", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/el-ignored", "setElIgnored",
				0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/include-coda",
				"addIncludeCoda", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/include-prelude",
				"addIncludePrelude", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/is-xml", "setIsXml", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/page-encoding",
				"setPageEncoding", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/scripting-invalid",
				"setScriptingInvalid", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/trim-directive-whitespaces",
				"setTrimWhitespace", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/url-pattern",
				"addUrlPattern", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/default-content-type",
				"setDefaultContentType", 0);
		digester.addCallMethod(fullPrefix
				+ "/jsp-config/jsp-property-group/buffer", "setBuffer", 0);
		digester.addCallMethod(
				fullPrefix
						+ "/jsp-config/jsp-property-group/error-on-undeclared-namespace",
				"setErrorOnUndeclaredNamespace", 0);

		digester.addRule(fullPrefix + "/login-config", loginConfig);

		digester.addObjectCreate(fullPrefix + "/login-config",
				"org.apache.catalina.deploy.LoginConfig");
		digester.addSetNext(fullPrefix + "/login-config", "setLoginConfig",
				"org.apache.catalina.deploy.LoginConfig");

		digester.addCallMethod(fullPrefix + "/login-config/auth-method",
				"setAuthMethod", 0);
		digester.addCallMethod(fullPrefix + "/login-config/realm-name",
				"setRealmName", 0);
		digester.addCallMethod(fullPrefix
				+ "/login-config/form-login-config/form-error-page",
				"setErrorPage", 0);
		digester.addCallMethod(fullPrefix
				+ "/login-config/form-login-config/form-login-page",
				"setLoginPage", 0);

		digester.addCallMethod(fullPrefix + "/mime-mapping", "addMimeMapping",
				2);
		digester.addCallParam(fullPrefix + "/mime-mapping/extension", 0);
		digester.addCallParam(fullPrefix + "/mime-mapping/mime-type", 1);

		digester.addObjectCreate(fullPrefix + "/security-constraint",
				"org.apache.catalina.deploy.SecurityConstraint");
		digester.addSetNext(fullPrefix + "/security-constraint",
				"addSecurityConstraint",
				"org.apache.catalina.deploy.SecurityConstraint");

		digester.addRule(fullPrefix + "/security-constraint/auth-constraint",
				new WebRuleSetSetAuthConstraintRule());
		digester.addCallMethod(fullPrefix
				+ "/security-constraint/auth-constraint/role-name",
				"addAuthRole", 0);
		digester.addCallMethod(
				fullPrefix + "/security-constraint/display-name",
				"setDisplayName", 0);
		digester.addCallMethod(
				fullPrefix
						+ "/security-constraint/user-data-constraint/transport-guarantee",
				"setUserConstraint", 0);

		digester.addObjectCreate(fullPrefix
				+ "/security-constraint/web-resource-collection",
				"org.apache.catalina.deploy.SecurityCollection");
		digester.addSetNext(fullPrefix
				+ "/security-constraint/web-resource-collection",
				"addCollection",
				"org.apache.catalina.deploy.SecurityCollection");
		digester.addCallMethod(fullPrefix
				+ "/security-constraint/web-resource-collection/http-method",
				"addMethod", 0);
		digester.addCallMethod(
				fullPrefix
						+ "/security-constraint/web-resource-collection/http-method-omission",
				"addOmittedMethod", 0);
		digester.addCallMethod(fullPrefix
				+ "/security-constraint/web-resource-collection/url-pattern",
				"addPattern", 0);
		digester.addCallMethod(
				fullPrefix
						+ "/security-constraint/web-resource-collection/web-resource-name",
				"setName", 0);

		digester.addCallMethod(fullPrefix + "/security-role/role-name",
				"addSecurityRole", 0);

		digester.addRule(fullPrefix + "/servlet", new WebRuleSetServletDefCreateRule());
		digester.addSetNext(fullPrefix + "/servlet", "addServlet",
				"org.apache.catalina.deploy.ServletDef");

		digester.addCallMethod(fullPrefix + "/servlet/init-param",
				"addInitParameter", 2);
		digester.addCallParam(fullPrefix + "/servlet/init-param/param-name", 0);
		digester.addCallParam(fullPrefix + "/servlet/init-param/param-value", 1);

		digester.addCallMethod(fullPrefix + "/servlet/jsp-file", "setJspFile",
				0);
		digester.addCallMethod(fullPrefix + "/servlet/load-on-startup",
				"setLoadOnStartup", 0);
		digester.addCallMethod(fullPrefix + "/servlet/run-as/role-name",
				"setRunAs", 0);

		digester.addObjectCreate(fullPrefix + "/servlet/security-role-ref",
				"org.apache.catalina.deploy.SecurityRoleRef");
		digester.addSetNext(fullPrefix + "/servlet/security-role-ref",
				"addSecurityRoleRef",
				"org.apache.catalina.deploy.SecurityRoleRef");
		digester.addCallMethod(fullPrefix
				+ "/servlet/security-role-ref/role-link", "setLink", 0);
		digester.addCallMethod(fullPrefix
				+ "/servlet/security-role-ref/role-name", "setName", 0);

		digester.addCallMethod(fullPrefix + "/servlet/servlet-class",
				"setServletClass", 0);
		digester.addCallMethod(fullPrefix + "/servlet/servlet-name",
				"setServletName", 0);

		digester.addObjectCreate(fullPrefix + "/servlet/multipart-config",
				"org.apache.catalina.deploy.MultipartDef");
		digester.addSetNext(fullPrefix + "/servlet/multipart-config",
				"setMultipartDef", "org.apache.catalina.deploy.MultipartDef");
		digester.addCallMethod(fullPrefix
				+ "/servlet/multipart-config/location", "setLocation", 0);
		digester.addCallMethod(fullPrefix
				+ "/servlet/multipart-config/max-file-size", "setMaxFileSize",
				0);
		digester.addCallMethod(fullPrefix
				+ "/servlet/multipart-config/max-request-size",
				"setMaxRequestSize", 0);
		digester.addCallMethod(fullPrefix
				+ "/servlet/multipart-config/file-size-threshold",
				"setFileSizeThreshold", 0);

		digester.addCallMethod(fullPrefix + "/servlet/async-supported",
				"setAsyncSupported", 0);
		digester.addCallMethod(fullPrefix + "/servlet/enabled", "setEnabled", 0);

		digester.addRule(fullPrefix + "/servlet-mapping",
				new WebRuleSetCallMethodMultiRule("addServletMapping", 2, 0));
		digester.addCallParam(fullPrefix + "/servlet-mapping/servlet-name", 1);
		digester.addRule(fullPrefix + "/servlet-mapping/url-pattern",
				new WebRuleSetCallParamMultiRule(0));

		digester.addRule(fullPrefix + "/session-config", sessionConfig);
		digester.addObjectCreate(fullPrefix + "/session-config",
				"org.apache.catalina.deploy.SessionConfig");
		digester.addSetNext(fullPrefix + "/session-config", "setSessionConfig",
				"org.apache.catalina.deploy.SessionConfig");
		digester.addCallMethod(fullPrefix + "/session-config/session-timeout",
				"setSessionTimeout", 0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/name", "setCookieName", 0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/domain", "setCookieDomain", 0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/path", "setCookiePath", 0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/comment", "setCookieComment",
				0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/http-only",
				"setCookieHttpOnly", 0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/secure", "setCookieSecure", 0);
		digester.addCallMethod(fullPrefix
				+ "/session-config/cookie-config/max-age", "setCookieMaxAge", 0);
		digester.addCallMethod(fullPrefix + "/session-config/tracking-mode",
				"addSessionTrackingMode", 0);

		// Taglibs pre Servlet 2.4
		digester.addRule(fullPrefix + "/taglib", new WebRuleSetTaglibLocationRule(false));
		digester.addCallMethod(fullPrefix + "/taglib", "addTaglib", 2);
		digester.addCallParam(fullPrefix + "/taglib/taglib-location", 1);
		digester.addCallParam(fullPrefix + "/taglib/taglib-uri", 0);

		// Taglibs Servlet 2.4 onwards
		digester.addRule(fullPrefix + "/jsp-config/taglib",
				new WebRuleSetTaglibLocationRule(true));
		digester.addCallMethod(fullPrefix + "/jsp-config/taglib", "addTaglib",
				2);
		digester.addCallParam(
				fullPrefix + "/jsp-config/taglib/taglib-location", 1);
		digester.addCallParam(fullPrefix + "/jsp-config/taglib/taglib-uri", 0);

		digester.addCallMethod(fullPrefix + "/welcome-file-list/welcome-file",
				"addWelcomeFile", 0);

		digester.addCallMethod(fullPrefix
				+ "/locale-encoding-mapping-list/locale-encoding-mapping",
				"addLocaleEncodingMapping", 2);
		digester.addCallParam(
				fullPrefix
						+ "/locale-encoding-mapping-list/locale-encoding-mapping/locale",
				0);
		digester.addCallParam(
				fullPrefix
						+ "/locale-encoding-mapping-list/locale-encoding-mapping/encoding",
				1);

		digester.addRule(fullPrefix + "/post-construct",
				new WebRuleSetLifecycleCallbackRule("addPostConstructMethods", 2, true));
		digester.addCallParam(fullPrefix
				+ "/post-construct/lifecycle-callback-class", 0);
		digester.addCallParam(fullPrefix
				+ "/post-construct/lifecycle-callback-method", 1);

		digester.addRule(fullPrefix + "/pre-destroy",
				new WebRuleSetLifecycleCallbackRule("addPreDestroyMethods", 2, false));
		digester.addCallParam(fullPrefix
				+ "/pre-destroy/lifecycle-callback-class", 0);
		digester.addCallParam(fullPrefix
				+ "/pre-destroy/lifecycle-callback-method", 1);
	}

	protected void configureNamingRules(Digester digester) {
		// ejb-local-ref
		digester.addObjectCreate(fullPrefix + "/ejb-local-ref",
				"org.apache.catalina.deploy.ContextLocalEjb");
		digester.addSetNext(fullPrefix + "/ejb-local-ref", "addEjbLocalRef",
				"org.apache.catalina.deploy.ContextLocalEjb");
		digester.addCallMethod(fullPrefix + "/ejb-local-ref/description",
				"setDescription", 0);
		digester.addCallMethod(fullPrefix + "/ejb-local-ref/ejb-link",
				"setLink", 0);
		digester.addCallMethod(fullPrefix + "/ejb-local-ref/ejb-ref-name",
				"setName", 0);
		digester.addCallMethod(fullPrefix + "/ejb-local-ref/ejb-ref-type",
				"setType", 0);
		digester.addCallMethod(fullPrefix + "/ejb-local-ref/local", "setLocal",
				0);
		digester.addCallMethod(fullPrefix + "/ejb-local-ref/local-home",
				"setHome", 0);
		digester.addRule(fullPrefix + "/ejb-local-ref/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/ejb-local-ref/");

		// ejb-ref
		digester.addObjectCreate(fullPrefix + "/ejb-ref",
				"org.apache.catalina.deploy.ContextEjb");
		digester.addSetNext(fullPrefix + "/ejb-ref", "addEjbRef",
				"org.apache.catalina.deploy.ContextEjb");
		digester.addCallMethod(fullPrefix + "/ejb-ref/description",
				"setDescription", 0);
		digester.addCallMethod(fullPrefix + "/ejb-ref/ejb-link", "setLink", 0);
		digester.addCallMethod(fullPrefix + "/ejb-ref/ejb-ref-name", "setName",
				0);
		digester.addCallMethod(fullPrefix + "/ejb-ref/ejb-ref-type", "setType",
				0);
		digester.addCallMethod(fullPrefix + "/ejb-ref/home", "setHome", 0);
		digester.addCallMethod(fullPrefix + "/ejb-ref/remote", "setRemote", 0);
		digester.addRule(fullPrefix + "/ejb-ref/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/ejb-ref/");

		// env-entry
		digester.addObjectCreate(fullPrefix + "/env-entry",
				"org.apache.catalina.deploy.ContextEnvironment");
		digester.addSetNext(fullPrefix + "/env-entry", "addEnvEntry",
				"org.apache.catalina.deploy.ContextEnvironment");
		digester.addRule(fullPrefix + "/env-entry", new WebRuleSetSetOverrideRule());
		digester.addCallMethod(fullPrefix + "/env-entry/description",
				"setDescription", 0);
		digester.addCallMethod(fullPrefix + "/env-entry/env-entry-name",
				"setName", 0);
		digester.addCallMethod(fullPrefix + "/env-entry/env-entry-type",
				"setType", 0);
		digester.addCallMethod(fullPrefix + "/env-entry/env-entry-value",
				"setValue", 0);
		digester.addRule(fullPrefix + "/env-entry/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/env-entry/");

		// resource-env-ref
		digester.addObjectCreate(fullPrefix + "/resource-env-ref",
				"org.apache.catalina.deploy.ContextResourceEnvRef");
		digester.addSetNext(fullPrefix + "/resource-env-ref",
				"addResourceEnvRef",
				"org.apache.catalina.deploy.ContextResourceEnvRef");
		digester.addCallMethod(fullPrefix
				+ "/resource-env-ref/resource-env-ref-name", "setName", 0);
		digester.addCallMethod(fullPrefix
				+ "/resource-env-ref/resource-env-ref-type", "setType", 0);
		digester.addRule(fullPrefix + "/resource-env-ref/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/resource-env-ref/");

		// message-destination
		digester.addObjectCreate(fullPrefix + "/message-destination",
				"org.apache.catalina.deploy.MessageDestination");
		digester.addSetNext(fullPrefix + "/message-destination",
				"addMessageDestination",
				"org.apache.catalina.deploy.MessageDestination");
		digester.addCallMethod(fullPrefix + "/message-destination/description",
				"setDescription", 0);
		digester.addCallMethod(
				fullPrefix + "/message-destination/display-name",
				"setDisplayName", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination/icon/large-icon", "setLargeIcon", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination/icon/small-icon", "setSmallIcon", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination/message-destination-name", "setName", 0);
		digester.addRule(fullPrefix + "/message-destination/mapped-name",
				new WebRuleSetMappedNameRule());

		// message-destination-ref
		digester.addObjectCreate(fullPrefix + "/message-destination-ref",
				"org.apache.catalina.deploy.MessageDestinationRef");
		digester.addSetNext(fullPrefix + "/message-destination-ref",
				"addMessageDestinationRef",
				"org.apache.catalina.deploy.MessageDestinationRef");
		digester.addCallMethod(fullPrefix
				+ "/message-destination-ref/description", "setDescription", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination-ref/message-destination-link",
				"setLink", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination-ref/message-destination-ref-name",
				"setName", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination-ref/message-destination-type",
				"setType", 0);
		digester.addCallMethod(fullPrefix
				+ "/message-destination-ref/message-destination-usage",
				"setUsage", 0);
		digester.addRule(fullPrefix + "/message-destination-ref/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/message-destination-ref/");

		// resource-ref
		digester.addObjectCreate(fullPrefix + "/resource-ref",
				"org.apache.catalina.deploy.ContextResource");
		digester.addSetNext(fullPrefix + "/resource-ref", "addResourceRef",
				"org.apache.catalina.deploy.ContextResource");
		digester.addCallMethod(fullPrefix + "/resource-ref/description",
				"setDescription", 0);
		digester.addCallMethod(fullPrefix + "/resource-ref/res-auth",
				"setAuth", 0);
		digester.addCallMethod(fullPrefix + "/resource-ref/res-ref-name",
				"setName", 0);
		digester.addCallMethod(fullPrefix + "/resource-ref/res-sharing-scope",
				"setScope", 0);
		digester.addCallMethod(fullPrefix + "/resource-ref/res-type",
				"setType", 0);
		digester.addRule(fullPrefix + "/resource-ref/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/resource-ref/");

		// service-ref
		digester.addObjectCreate(fullPrefix + "/service-ref",
				"org.apache.catalina.deploy.ContextService");
		digester.addSetNext(fullPrefix + "/service-ref", "addServiceRef",
				"org.apache.catalina.deploy.ContextService");
		digester.addCallMethod(fullPrefix + "/service-ref/description",
				"setDescription", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/display-name",
				"setDisplayname", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/icon/large-icon",
				"setLargeIcon", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/icon/small-icon",
				"setSmallIcon", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/service-ref-name",
				"setName", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/service-interface",
				"setInterface", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/service-ref-type",
				"setType", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/wsdl-file",
				"setWsdlfile", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/jaxrpc-mapping-file",
				"setJaxrpcmappingfile", 0);
		digester.addRule(fullPrefix + "/service-ref/service-qname",
				new WebRuleSetServiceQnameRule());

		digester.addRule(fullPrefix + "/service-ref/port-component-ref",
				new WebRuleSetCallMethodMultiRule("addPortcomponent", 2, 1));
		digester.addCallParam(fullPrefix
				+ "/service-ref/port-component-ref/service-endpoint-interface",
				0);
		digester.addRule(fullPrefix
				+ "/service-ref/port-component-ref/port-component-link",
				new WebRuleSetCallParamMultiRule(1));

		digester.addObjectCreate(fullPrefix + "/service-ref/handler",
				"org.apache.catalina.deploy.ContextHandler");
		digester.addRule(fullPrefix + "/service-ref/handler", new SetNextRule(
				"addHandler", "org.apache.catalina.deploy.ContextHandler"));

		digester.addCallMethod(
				fullPrefix + "/service-ref/handler/handler-name", "setName", 0);
		digester.addCallMethod(fullPrefix
				+ "/service-ref/handler/handler-class", "setHandlerclass", 0);

		digester.addCallMethod(fullPrefix + "/service-ref/handler/init-param",
				"setProperty", 2);
		digester.addCallParam(fullPrefix
				+ "/service-ref/handler/init-param/param-name", 0);
		digester.addCallParam(fullPrefix
				+ "/service-ref/handler/init-param/param-value", 1);

		digester.addRule(fullPrefix + "/service-ref/handler/soap-header",
				new WebRuleSetSoapHeaderRule());

		digester.addCallMethod(fullPrefix + "/service-ref/handler/soap-role",
				"addSoapRole", 0);
		digester.addCallMethod(fullPrefix + "/service-ref/handler/port-name",
				"addPortName", 0);
		digester.addRule(fullPrefix + "/service-ref/mapped-name",
				new WebRuleSetMappedNameRule());
		configureInjectionRules(digester, "web-app/service-ref/");
	}

	protected void configureInjectionRules(Digester digester, String base) {

		digester.addCallMethod(prefix + base + "injection-target",
				"addInjectionTarget", 2);
		digester.addCallParam(prefix + base
				+ "injection-target/injection-target-class", 0);
		digester.addCallParam(prefix + base
				+ "injection-target/injection-target-name", 1);

	}

	/**
	 * Reset counter used for validating the web.xml file.
	 */
	public void recycle() {
		jspConfig.setJspConfigSet(false);
		sessionConfig.setSessionConfigSet(false);
		loginConfig.setLoginConfigSet(false);
		name.setNameSet(false);
		absoluteOrdering.setAbsoluteOrderingSet(false);
		relativeOrdering.setRelativeOrderingSet(false);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getFullPrefix() {
		return fullPrefix;
	}

	public void setFullPrefix(String fullPrefix) {
		this.fullPrefix = fullPrefix;
	}

	public boolean isFragment() {
		return fragment;
	}

	public void setFragment(boolean fragment) {
		this.fragment = fragment;
	}

	public WebRuleSetSetSessionConfig getSessionConfig() {
		return sessionConfig;
	}

	public void setSessionConfig(WebRuleSetSetSessionConfig sessionConfig) {
		this.sessionConfig = sessionConfig;
	}

	public WebRuleSetSetLoginConfig getLoginConfig() {
		return loginConfig;
	}

	public void setLoginConfig(WebRuleSetSetLoginConfig loginConfig) {
		this.loginConfig = loginConfig;
	}

	public WebRuleSetSetJspConfig getJspConfig() {
		return jspConfig;
	}

	public void setJspConfig(WebRuleSetSetJspConfig jspConfig) {
		this.jspConfig = jspConfig;
	}

	public WebRuleSetNameRule getName() {
		return name;
	}

	public void setName(WebRuleSetNameRule name) {
		this.name = name;
	}

	public WebRuleSetAbsoluteOrderingRule getAbsoluteOrdering() {
		return absoluteOrdering;
	}

	public void setAbsoluteOrdering(WebRuleSetAbsoluteOrderingRule absoluteOrdering) {
		this.absoluteOrdering = absoluteOrdering;
	}

	public WebRuleSetRelativeOrderingRule getRelativeOrdering() {
		return relativeOrdering;
	}

	public void setRelativeOrdering(WebRuleSetRelativeOrderingRule relativeOrdering) {
		this.relativeOrdering = relativeOrdering;
	}

	public static StringManager3 getSm() {
		return sm;
	}
}
