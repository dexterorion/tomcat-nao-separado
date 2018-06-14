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


/**
 * Representation of an EJB resource reference for a web application, as
 * represented in a <code>&lt;ejb-ref&gt;</code> element in the
 * deployment descriptor.
 *
 * @author Craig R. McClanahan
 * @author Peter Rossbach (pero@apache.org)
 */
public class ContextEjb extends ResourceBase {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------- Properties



    /**
     * The name of the EJB home implementation class.
     */
    private String home = null;

    public String getHome() {
        return (this.getHomeData());
    }

    public void setHome(String home) {
        this.setHomeData(home);
    }


    /**
     * The link to a J2EE EJB definition.
     */
    private String link = null;

    public String getLink() {
        return (this.getLinkData());
    }

    public void setLink(String link) {
        this.setLinkData(link);
    }

    /**
     * The name of the EJB remote implementation class.
     */
    private String remote = null;

    public String getRemote() {
        return (this.getRemoteData());
    }

    public void setRemote(String remote) {
        this.setRemoteData(remote);
    }

    
    // --------------------------------------------------------- Public Methods


    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("ContextEjb[");
        sb.append("name=");
        sb.append(getName());
        if (getDescription() != null) {
            sb.append(", description=");
            sb.append(getDescription());
        }
        if (getType() != null) {
            sb.append(", type=");
            sb.append(getType());
        }
        if (getHomeData() != null) {
            sb.append(", home=");
            sb.append(getHomeData());
        }
        if (getRemoteData() != null) {
            sb.append(", remote=");
            sb.append(getRemoteData());
        }
        if (getLinkData() != null) {
            sb.append(", link=");
            sb.append(getLinkData());
        }
        sb.append("]");
        return (sb.toString());

    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getHomeData() == null) ? 0 : getHomeData().hashCode());
        result = prime * result + ((getLinkData() == null) ? 0 : getLinkData().hashCode());
        result = prime * result + ((getRemoteData() == null) ? 0 : getRemoteData().hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContextEjb other = (ContextEjb) obj;
        if (getHomeData() == null) {
            if (other.getHomeData() != null) {
                return false;
            }
        } else if (!getHomeData().equals(other.getHomeData())) {
            return false;
        }
        if (getLinkData() == null) {
            if (other.getLinkData() != null) {
                return false;
            }
        } else if (!getLinkData().equals(other.getLinkData())) {
            return false;
        }
        if (getRemoteData() == null) {
            if (other.getRemoteData() != null) {
                return false;
            }
        } else if (!getRemoteData().equals(other.getRemoteData())) {
            return false;
        }
        return true;
    }

	public String getHomeData() {
		return home;
	}

	public void setHomeData(String home) {
		this.home = home;
	}

	public String getLinkData() {
		return link;
	}

	public void setLinkData(String link) {
		this.link = link;
	}

	public String getRemoteData() {
		return remote;
	}

	public void setRemoteData(String remote) {
		this.remote = remote;
	}
}
