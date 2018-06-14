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
 * <p>Representation of a message destination reference for a web application,
 * as represented in a <code>&lt;message-destination-ref&gt;</code> element
 * in the deployment descriptor.</p>
 *
 * @author Craig R. McClanahan
 * @since Tomcat 5.0
 */
public class MessageDestinationRef extends ResourceBase {

    private static final long serialVersionUID = 1L;
    
    // ------------------------------------------------------------- Properties


    /**
     * The link of this destination ref.
     */
    private String link = null;

    public String getLink() {
        return (this.getLinkData());
    }

    public void setLink(String link) {
        this.setLinkData(link);
    }


    /**
     * The usage of this destination ref.
     */
    private String usage = null;

    public String getUsage() {
        return (this.getUsageData());
    }

    public void setUsage(String usage) {
        this.setUsageData(usage);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("MessageDestination[");
        sb.append("name=");
        sb.append(getName());
        if (getLinkData() != null) {
            sb.append(", link=");
            sb.append(getLinkData());
        }
        if (getType() != null) {
            sb.append(", type=");
            sb.append(getType());
        }
        if (getUsageData() != null) {
            sb.append(", usage=");
            sb.append(getUsageData());
        }
        if (getDescription() != null) {
            sb.append(", description=");
            sb.append(getDescription());
        }
        sb.append("]");
        return (sb.toString());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getLinkData() == null) ? 0 : getLinkData().hashCode());
        result = prime * result + ((getUsageData() == null) ? 0 : getUsageData().hashCode());
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
        MessageDestinationRef other = (MessageDestinationRef) obj;
        if (getLinkData() == null) {
            if (other.getLinkData() != null) {
                return false;
            }
        } else if (!getLinkData().equals(other.getLinkData())) {
            return false;
        }
        if (getUsageData() == null) {
            if (other.getUsageData() != null) {
                return false;
            }
        } else if (!getUsageData().equals(other.getUsageData())) {
            return false;
        }
        return true;
    }

	public String getLinkData() {
		return link;
	}

	public void setLinkData(String link) {
		this.link = link;
	}

	public String getUsageData() {
		return usage;
	}

	public void setUsageData(String usage) {
		this.usage = usage;
	}
}
