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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Representation of a handler reference for a web service, as
 * represented in a <code>&lt;handler&gt;</code> element in the
 * deployment descriptor.
 *
 * @author Fabien Carrion
 */
public class ContextHandler extends ResourceBase {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------- Properties


    /**
     * The Handler reference class.
     */
    private String handlerclass = null;

    public String getHandlerclass() {
        return (this.getHandlerclassData());
    }

    public void setHandlerclass(String handlerclass) {
        this.setHandlerclassData(handlerclass);
    }

    /**
     * A list of QName specifying the SOAP Headers the handler will work on. 
     * -namespace and locapart values must be found inside the WSDL.
     *
     * A service-qname is composed by a namespaceURI and a localpart.
     *
     * soapHeader[0] : namespaceURI
     * soapHeader[1] : localpart
     */
    private final HashMap<String, String> soapHeaders =
            new HashMap<String, String>();

    public Iterator<String> getLocalparts() {
        return getSoapHeadersData().keySet().iterator();
    }

    public String getNamespaceuri(String localpart) {
        return getSoapHeadersData().get(localpart);
    }

    public void addSoapHeaders(String localpart, String namespaceuri) {
        getSoapHeadersData().put(localpart, namespaceuri);
    }

    /**
     * Set a configured property.
     */
    public void setProperty(String name, String value) {
        this.setProperty(name, (Object) value);
    }

    /**
     * The soapRole.
     */
    private final ArrayList<String> soapRoles = new ArrayList<String>();

    public String getSoapRole(int i) {
        return this.getSoapRolesData().get(i);
    }

    public int getSoapRolesSize() {
        return this.getSoapRolesData().size();
    }

    public void addSoapRole(String soapRole) {
        this.getSoapRolesData().add(soapRole);
    }

    /**
     * The portName.
     */
    private final ArrayList<String> portNames = new ArrayList<String>();

    public String getPortName(int i) {
        return this.getPortNamesData().get(i);
    }

    public int getPortNamesSize() {
        return this.getPortNamesData().size();
    }

    public void addPortName(String portName) {
        this.getPortNamesData().add(portName);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("ContextHandler[");
        sb.append("name=");
        sb.append(getName());
        if (getHandlerclassData() != null) {
            sb.append(", class=");
            sb.append(getHandlerclassData());
        }
        if (this.getSoapHeadersData() != null) {
            sb.append(", soap-headers=");
            sb.append(this.getSoapHeadersData());
        }
        if (this.getSoapRolesSize() > 0) {
            sb.append(", soap-roles=");
            sb.append(getSoapRolesData());
        }
        if (this.getPortNamesSize() > 0) {
            sb.append(", port-name=");
            sb.append(getPortNamesData());
        }
        if (this.listProperties() != null) {
            sb.append(", init-param=");
            sb.append(this.listProperties());
        }
        sb.append("]");
        return (sb.toString());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result +
                ((getHandlerclassData() == null) ? 0 : getHandlerclassData().hashCode());
        result = prime * result +
                ((getPortNamesData() == null) ? 0 : getPortNamesData().hashCode());
        result = prime * result +
                ((getSoapHeadersData() == null) ? 0 : getSoapHeadersData().hashCode());
        result = prime * result +
                ((getSoapRolesData() == null) ? 0 : getSoapRolesData().hashCode());
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
        ContextHandler other = (ContextHandler) obj;
        if (getHandlerclassData() == null) {
            if (other.getHandlerclassData() != null) {
                return false;
            }
        } else if (!getHandlerclassData().equals(other.getHandlerclassData())) {
            return false;
        }
        if (getPortNamesData() == null) {
            if (other.getPortNamesData() != null) {
                return false;
            }
        } else if (!getPortNamesData().equals(other.getPortNamesData())) {
            return false;
        }
        if (getSoapHeadersData() == null) {
            if (other.getSoapHeadersData() != null) {
                return false;
            }
        } else if (!getSoapHeadersData().equals(other.getSoapHeadersData())) {
            return false;
        }
        if (getSoapRolesData() == null) {
            if (other.getSoapRolesData() != null) {
                return false;
            }
        } else if (!getSoapRolesData().equals(other.getSoapRolesData())) {
            return false;
        }
        return true;
    }

	public String getHandlerclassData() {
		return handlerclass;
	}

	public void setHandlerclassData(String handlerclass) {
		this.handlerclass = handlerclass;
	}

	public HashMap<String, String> getSoapHeadersData() {
		return soapHeaders;
	}

	public ArrayList<String> getSoapRolesData() {
		return soapRoles;
	}

	public ArrayList<String> getPortNamesData() {
		return portNames;
	}
}
