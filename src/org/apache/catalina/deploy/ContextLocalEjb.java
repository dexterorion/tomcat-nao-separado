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
 * Representation of a local EJB resource reference for a web application, as
 * represented in a <code>&lt;ejb-local-ref&gt;</code> element in the
 * deployment descriptor.
 *
 * @author Craig R. McClanahan
 * @author Peter Rossbach (pero@apache.org)
 */
public class ContextLocalEjb extends ResourceBase {

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
     * The name of the EJB local implementation class.
     */
    private String local = null;

    public String getLocal() {
        return (this.getLocalData());
    }

    public void setLocal(String local) {
        this.setLocalData(local);
    }

    
    // --------------------------------------------------------- Public Methods


    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("ContextLocalEjb[");
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
        if (getLinkData() != null) {
            sb.append(", link=");
            sb.append(getLinkData());
        }
        if (getLocalData() != null) {
            sb.append(", local=");
            sb.append(getLocalData());
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
        result = prime * result + ((getLocalData() == null) ? 0 : getLocalData().hashCode());
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
        ContextLocalEjb other = (ContextLocalEjb) obj;
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
        if (getLocalData() == null) {
            if (other.getLocalData() != null) {
                return false;
            }
        } else if (!getLocalData().equals(other.getLocalData())) {
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

	public String getLocalData() {
		return local;
	}

	public void setLocalData(String local) {
		this.local = local;
	}
}
