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


/**
 * Representation of a the multipart configuration for a servlet.
 */
public class MultipartDef implements Serializable {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------- Properties
    private String location;
   
    public String getLocation() {
        return getLocationData();
    }

    public void setLocation(String location) {
        this.setLocationData(location);
    }
    
    
    private String maxFileSize;

    public String getMaxFileSize() {
        return getMaxFileSizeData();
    }

    public void setMaxFileSize(String maxFileSize) {
        this.setMaxFileSizeData(maxFileSize);
    }
    
    
    private String maxRequestSize;

    public String getMaxRequestSize() {
        return getMaxRequestSizeData();
    }

    public void setMaxRequestSize(String maxRequestSize) {
        this.setMaxRequestSizeData(maxRequestSize);
    }

    
    private String fileSizeThreshold;
    
    public String getFileSizeThreshold() {
        return getFileSizeThresholdData();
    }

    public void setFileSizeThreshold(String fileSizeThreshold) {
        this.setFileSizeThresholdData(fileSizeThreshold);
    }


    // ---------------------------------------------------------- Object methods

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((getFileSizeThresholdData() == null) ? 0 : getFileSizeThresholdData()
                        .hashCode());
        result = prime * result
                + ((getLocationData() == null) ? 0 : getLocationData().hashCode());
        result = prime * result
                + ((getMaxFileSizeData() == null) ? 0 : getMaxFileSizeData().hashCode());
        result = prime * result
                + ((getMaxRequestSizeData() == null) ? 0 : getMaxRequestSizeData().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MultipartDef)) {
            return false;
        }
        MultipartDef other = (MultipartDef) obj;
        if (getFileSizeThresholdData() == null) {
            if (other.getFileSizeThresholdData() != null) {
                return false;
            }
        } else if (!getFileSizeThresholdData().equals(other.getFileSizeThresholdData())) {
            return false;
        }
        if (getLocationData() == null) {
            if (other.getLocationData() != null) {
                return false;
            }
        } else if (!getLocationData().equals(other.getLocationData())) {
            return false;
        }
        if (getMaxFileSizeData() == null) {
            if (other.getMaxFileSizeData() != null) {
                return false;
            }
        } else if (!getMaxFileSizeData().equals(other.getMaxFileSizeData())) {
            return false;
        }
        if (getMaxRequestSizeData() == null) {
            if (other.getMaxRequestSizeData() != null) {
                return false;
            }
        } else if (!getMaxRequestSizeData().equals(other.getMaxRequestSizeData())) {
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

	public String getMaxFileSizeData() {
		return maxFileSize;
	}

	public void setMaxFileSizeData(String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public String getMaxRequestSizeData() {
		return maxRequestSize;
	}

	public void setMaxRequestSizeData(String maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	public String getFileSizeThresholdData() {
		return fileSizeThreshold;
	}

	public void setFileSizeThresholdData(String fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

}
