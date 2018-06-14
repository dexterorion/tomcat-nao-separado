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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Representation of an Context element
 *
 * @author Peter Rossbach (pero@apache.org)
 */
public class ResourceBase implements Serializable, Injectable {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------- Properties


    /**
     * The description of this resource.
     */
    private String description = null;

    public String getDescription() {
        return (this.getDescriptionData());
    }

    public void setDescription(String description) {
        this.setDescriptionData(description);
    }



    /**
     * The name of this resource.
     */
    private String name = null;

    @Override
    public String getName() {
        return (this.getNameData());
    }

    public void setName(String name) {
        this.setNameData(name);
    }


    /**
     * The name of the resource implementation class.
     */
    private String type = null;

    public String getType() {
        return (this.getTypeData());
    }

    public void setType(String type) {
        this.setTypeData(type);
    }


    /**
     * Holder for our configured properties.
     */
    private final HashMap<String, Object> properties =
            new HashMap<String, Object>();

    /**
     * Return a configured property.
     */
    public Object getProperty(String name) {
        return getPropertiesData().get(name);
    }

    /**
     * Set a configured property.
     */
    public void setProperty(String name, Object value) {
        getPropertiesData().put(name, value);
    }

    /** 
     * Remove a configured property.
     */
    public void removeProperty(String name) {
        getPropertiesData().remove(name);
    }

    /**
     * List properties.
     */
    public Iterator<String> listProperties() {
        return getPropertiesData().keySet().iterator();
    }

    private final List<InjectionTarget> injectionTargets = new ArrayList<InjectionTarget>();

    @Override
    public void addInjectionTarget(String injectionTargetName, String jndiName) {
        InjectionTarget target = new InjectionTarget(injectionTargetName, jndiName);
        getInjectionTargetsData().add(target);
    }

    @Override
    public List<InjectionTarget> getInjectionTargets() {
        return getInjectionTargetsData();
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result +
                ((getDescriptionData() == null) ? 0 : getDescriptionData().hashCode());
        result = prime * result +
                ((getInjectionTargetsData() == null) ? 0 : getInjectionTargetsData().hashCode());
        result = prime * result + ((getNameData() == null) ? 0 : getNameData().hashCode());
        result = prime * result +
                ((getPropertiesData() == null) ? 0 : getPropertiesData().hashCode());
        result = prime * result + ((getTypeData() == null) ? 0 : getTypeData().hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourceBase other = (ResourceBase) obj;
        if (getDescriptionData() == null) {
            if (other.getDescriptionData() != null) {
                return false;
            }
        } else if (!getDescriptionData().equals(other.getDescriptionData())) {
            return false;
        }
        if (getInjectionTargetsData() == null) {
            if (other.getInjectionTargetsData() != null) {
                return false;
            }
        } else if (!getInjectionTargetsData().equals(other.getInjectionTargetsData())) {
            return false;
        }
        if (getNameData() == null) {
            if (other.getNameData() != null) {
                return false;
            }
        } else if (!getNameData().equals(other.getNameData())) {
            return false;
        }
        if (getPropertiesData() == null) {
            if (other.getPropertiesData() != null) {
                return false;
            }
        } else if (!getPropertiesData().equals(other.getPropertiesData())) {
            return false;
        }
        if (getTypeData() == null) {
            if (other.getTypeData() != null) {
                return false;
            }
        } else if (!getTypeData().equals(other.getTypeData())) {
            return false;
        }
        return true;
    }


    // -------------------------------------------------------- Package Methods

    /**
     * The NamingResources with which we are associated (if any).
     */
    private NamingResources resources = null;

    public NamingResources getNamingResources() {
        return (this.getResourcesData());
    }

    public void setNamingResources(NamingResources resources) {
        this.setResourcesData(resources);
    }

	public String getDescriptionData() {
		return description;
	}

	public void setDescriptionData(String description) {
		this.description = description;
	}

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}

	public String getTypeData() {
		return type;
	}

	public void setTypeData(String type) {
		this.type = type;
	}

	public HashMap<String, Object> getPropertiesData() {
		return properties;
	}

	public List<InjectionTarget> getInjectionTargetsData() {
		return injectionTargets;
	}

	public NamingResources getResourcesData() {
		return resources;
	}

	public void setResourcesData(NamingResources resources) {
		this.resources = resources;
	}
}
