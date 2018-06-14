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
 * Representation of an application environment entry, as represented in
 * an <code>&lt;env-entry&gt;</code> element in the deployment descriptor.
 *
 * @author Craig R. McClanahan
 */
public class ContextEnvironment extends ResourceBase {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------- Properties


    /**
     * Does this environment entry allow overrides by the application
     * deployment descriptor?
     */
    private boolean override = true;

    public boolean getOverride() {
        return (this.isOverrideData());
    }

    public void setOverride(boolean override) {
        this.setOverrideData(override);
    }


    /**
     * The value of this environment entry.
     */
    private String value = null;

    public String getValue() {
        return (this.getValueData());
    }

    public void setValue(String value) {
        this.setValueData(value);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("ContextEnvironment[");
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
        if (getValueData() != null) {
            sb.append(", value=");
            sb.append(getValueData());
        }
        sb.append(", override=");
        sb.append(isOverrideData());
        sb.append("]");
        return (sb.toString());

    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (isOverrideData() ? 1231 : 1237);
        result = prime * result + ((getValueData() == null) ? 0 : getValueData().hashCode());
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
        ContextEnvironment other = (ContextEnvironment) obj;
        if (isOverrideData() != other.isOverrideData()) {
            return false;
        }
        if (getValueData() == null) {
            if (other.getValueData() != null) {
                return false;
            }
        } else if (!getValueData().equals(other.getValueData())) {
            return false;
        }
        return true;
    }

	public boolean isOverrideData() {
		return override;
	}

	public void setOverrideData(boolean override) {
		this.override = override;
	}

	public String getValueData() {
		return value;
	}

	public void setValueData(String value) {
		this.value = value;
	}
}
