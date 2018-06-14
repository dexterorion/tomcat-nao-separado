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

package org.apache.catalina.core;

import javax.servlet.descriptor.TaglibDescriptor;

public class ApplicationTaglibDescriptor implements TaglibDescriptor {

    private String location;
    private String uri;
    
    public ApplicationTaglibDescriptor(String location, String uri) {
        this.setLocationData(location);
        this.setUriData(uri);
    }

    @Override
    public String getTaglibLocation() {
        return getLocationData();
    }

    @Override
    public String getTaglibURI() {
        return getUriData();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getLocationData() == null) ? 0 : getLocationData().hashCode());
        result = prime * result + ((getUriData() == null) ? 0 : getUriData().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ApplicationTaglibDescriptor)) {
            return false;
        }
        ApplicationTaglibDescriptor other = (ApplicationTaglibDescriptor) obj;
        if (getLocationData() == null) {
            if (other.getLocationData() != null) {
                return false;
            }
        } else if (!getLocationData().equals(other.getLocationData())) {
            return false;
        }
        if (getUriData() == null) {
            if (other.getUriData() != null) {
                return false;
            }
        } else if (!getUriData().equals(other.getUriData())) {
            return false;
        }
        return true;
    }

	public String getLocationData() {
		return location;
	}

	public void setLocationData(String location) {
		this.location = location;
	}

	public String getUriData() {
		return uri;
	}

	public void setUriData(String uri) {
		this.uri = uri;
	}

}
