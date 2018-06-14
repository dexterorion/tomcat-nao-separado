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


package org.apache.catalina.deploy;


import java.io.Serializable;

import org.apache.catalina.util.RequestUtil;


/**
 * Representation of a login configuration element for a web application,
 * as represented in a <code>&lt;login-config&gt;</code> element in the
 * deployment descriptor.
 *
 * @author Craig R. McClanahan
 */
public class LoginConfig implements Serializable {


    private static final long serialVersionUID = 1L;


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new LoginConfig with default properties.
     */
    public LoginConfig() {

        super();

    }


    /**
     * Construct a new LoginConfig with the specified properties.
     *
     * @param authMethod The authentication method
     * @param realmName The realm name
     * @param loginPage The login page URI
     * @param errorPage The error page URI
     */
    public LoginConfig(String authMethod, String realmName,
                       String loginPage, String errorPage) {

        super();
        setAuthMethod(authMethod);
        setRealmName(realmName);
        setLoginPage(loginPage);
        setErrorPage(errorPage);

    }


    // ------------------------------------------------------------- Properties


    /**
     * The authentication method to use for application login.  Must be
     * BASIC, DIGEST, FORM, or CLIENT-CERT.
     */
    private String authMethod = null;

    public String getAuthMethod() {
        return (this.getAuthMethodData());
    }

    public void setAuthMethod(String authMethod) {
        this.setAuthMethodData(authMethod);
    }


    /**
     * The context-relative URI of the error page for form login.
     */
    private String errorPage = null;

    public String getErrorPage() {
        return (this.getErrorPageData());
    }

    public void setErrorPage(String errorPage) {
        //        if ((errorPage == null) || !errorPage.startsWith("/"))
        //            throw new IllegalArgumentException
        //                ("Error Page resource path must start with a '/'");
        this.setErrorPageData(RequestUtil.URLDecode(errorPage));
    }


    /**
     * The context-relative URI of the login page for form login.
     */
    private String loginPage = null;

    public String getLoginPage() {
        return (this.getLoginPageData());
    }

    public void setLoginPage(String loginPage) {
        //        if ((loginPage == null) || !loginPage.startsWith("/"))
        //            throw new IllegalArgumentException
        //                ("Login Page resource path must start with a '/'");
        this.setLoginPageData(RequestUtil.URLDecode(loginPage));
    }


    /**
     * The realm name used when challenging the user for authentication
     * credentials.
     */
    private String realmName = null;

    public String getRealmName() {
        return (this.getRealmNameData());
    }

    public void setRealmName(String realmName) {
        this.setRealmNameData(realmName);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("LoginConfig[");
        sb.append("authMethod=");
        sb.append(getAuthMethodData());
        if (getRealmNameData() != null) {
            sb.append(", realmName=");
            sb.append(getRealmNameData());
        }
        if (getLoginPageData() != null) {
            sb.append(", loginPage=");
            sb.append(getLoginPageData());
        }
        if (getErrorPageData() != null) {
            sb.append(", errorPage=");
            sb.append(getErrorPageData());
        }
        sb.append("]");
        return (sb.toString());

    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getAuthMethodData() == null) ? 0 : getAuthMethodData().hashCode());
        result = prime * result
                + ((getErrorPageData() == null) ? 0 : getErrorPageData().hashCode());
        result = prime * result
                + ((getLoginPageData() == null) ? 0 : getLoginPageData().hashCode());
        result = prime * result
                + ((getRealmNameData() == null) ? 0 : getRealmNameData().hashCode());
        return result;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof LoginConfig))
            return false;
        LoginConfig other = (LoginConfig) obj;
        if (getAuthMethodData() == null) {
            if (other.getAuthMethodData() != null)
                return false;
        } else if (!getAuthMethodData().equals(other.getAuthMethodData()))
            return false;
        if (getErrorPageData() == null) {
            if (other.getErrorPageData() != null)
                return false;
        } else if (!getErrorPageData().equals(other.getErrorPageData()))
            return false;
        if (getLoginPageData() == null) {
            if (other.getLoginPageData() != null)
                return false;
        } else if (!getLoginPageData().equals(other.getLoginPageData()))
            return false;
        if (getRealmNameData() == null) {
            if (other.getRealmNameData() != null)
                return false;
        } else if (!getRealmNameData().equals(other.getRealmNameData()))
            return false;
        return true;
    }


	public String getAuthMethodData() {
		return authMethod;
	}


	public void setAuthMethodData(String authMethod) {
		this.authMethod = authMethod;
	}


	public String getErrorPageData() {
		return errorPage;
	}


	public void setErrorPageData(String errorPage) {
		this.errorPage = errorPage;
	}


	public String getLoginPageData() {
		return loginPage;
	}


	public void setLoginPageData(String loginPage) {
		this.loginPage = loginPage;
	}


	public String getRealmNameData() {
		return realmName;
	}


	public void setRealmNameData(String realmName) {
		this.realmName = realmName;
	}


}
