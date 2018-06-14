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
 * <p>Representation of a message destination for a web application, as
 * represented in a <code>&lt;message-destination&gt;</code> element
 * in the deployment descriptor.</p>
 *
 * @author Craig R. McClanahan
 * @since Tomcat 5.0
 */
public class MessageDestination extends ResourceBase {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------- Properties


    /**
     * The display name of this destination.
     */
    private String displayName = null;

    public String getDisplayName() {
        return (this.getDisplayNameData());
    }

    public void setDisplayName(String displayName) {
        this.setDisplayNameData(displayName);
    }


    /**
     * The large icon of this destination.
     */
    private String largeIcon = null;

    public String getLargeIcon() {
        return (this.getLargeIconData());
    }

    public void setLargeIcon(String largeIcon) {
        this.setLargeIconData(largeIcon);
    }


    /**
     * The small icon of this destination.
     */
    private String smallIcon = null;

    public String getSmallIcon() {
        return (this.getSmallIconData());
    }

    public void setSmallIcon(String smallIcon) {
        this.setSmallIconData(smallIcon);
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
        if (getDisplayNameData() != null) {
            sb.append(", displayName=");
            sb.append(getDisplayNameData());
        }
        if (getLargeIconData() != null) {
            sb.append(", largeIcon=");
            sb.append(getLargeIconData());
        }
        if (getSmallIconData() != null) {
            sb.append(", smallIcon=");
            sb.append(getSmallIconData());
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
        result = prime * result +
                ((getDisplayNameData() == null) ? 0 : getDisplayNameData().hashCode());
        result = prime * result +
                ((getLargeIconData() == null) ? 0 : getLargeIconData().hashCode());
        result = prime * result +
                ((getSmallIconData() == null) ? 0 : getSmallIconData().hashCode());
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
        MessageDestination other = (MessageDestination) obj;
        if (getDisplayNameData() == null) {
            if (other.getDisplayNameData() != null) {
                return false;
            }
        } else if (!getDisplayNameData().equals(other.getDisplayNameData())) {
            return false;
        }
        if (getLargeIconData() == null) {
            if (other.getLargeIconData() != null) {
                return false;
            }
        } else if (!getLargeIconData().equals(other.getLargeIconData())) {
            return false;
        }
        if (getSmallIconData() == null) {
            if (other.getSmallIconData() != null) {
                return false;
            }
        } else if (!getSmallIconData().equals(other.getSmallIconData())) {
            return false;
        }
        return true;
    }

	public String getDisplayNameData() {
		return displayName;
	}

	public void setDisplayNameData(String displayName) {
		this.displayName = displayName;
	}

	public String getLargeIconData() {
		return largeIcon;
	}

	public void setLargeIconData(String largeIcon) {
		this.largeIcon = largeIcon;
	}

	public String getSmallIconData() {
		return smallIcon;
	}

	public void setSmallIconData(String smallIcon) {
		this.smallIcon = smallIcon;
	}
}
