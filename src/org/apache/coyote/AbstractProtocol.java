/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.coyote;

import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.modeler.Registry2;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.AbstractEndpointHandler;
import org.apache.tomcat.util.res.StringManager3;

public abstract class AbstractProtocol<S> implements ProtocolHandler,
        MBeanRegistration {

    /**
     * The string manager for this package.
     */
    private static final StringManager3 sm =
        StringManager3.getManager(Constants24.getPackage());


    /**
     * Counter used to generate unique JMX names for connectors using automatic
     * port binding.
     */
    private static final AtomicInteger nameCounter = new AtomicInteger(0);


    /**
     * Name of MBean for the Global Request Processor.
     */
    private ObjectName rgOname = null;

    
    /**
     * Name of MBean for the ThreadPool.
     */
    private ObjectName tpOname = null;

    
    /**
     * Unique ID for this connector. Only used if the connector is configured
     * to use a random port as the port will change if stop(), start() is
     * called.
     */
    private int nameIndex = 0;


    /**
     * Endpoint that provides low-level network I/O - must be matched to the
     * ProtocolHandler implementation (ProtocolHandler using BIO, requires BIO
     * Endpoint etc.).
     */
    private AbstractEndpoint<S> endpoint = null;

    
    // ----------------------------------------------- Generic property handling

    /**
     * Generic property setter used by the digester. Other code should not need
     * to use this. The digester will only use this method if it can't find a
     * more specific setter. That means the property belongs to the Endpoint,
     * the ServerSocketFactory or some other lower level component. This method
     * ensures that it is visible to both.
     */
    public boolean setProperty(String name, String value) {
        return endpoint.setProperty(name, value);
    }


    /**
     * Generic property getter used by the digester. Other code should not need
     * to use this.
     */
    public String getProperty(String name) {
        return endpoint.getProperty(name);
    }


    // ------------------------------- Properties managed by the ProtocolHandler
    
    /**
     * The adapter provides the link between the ProtocolHandler and the
     * connector.
     */
    private Adapter adapter;
    @Override
    public void setAdapter(Adapter adapter) { this.adapter = adapter; }
    @Override
    public Adapter getAdapter() { return adapter; }


    /**
     * The maximum number of idle processors that will be retained in the cache
     * and re-used with a subsequent request. The default is 200. A value of -1
     * means unlimited. In the unlimited case, the theoretical maximum number of
     * cached Processor objects is {@link #getMaxConnections()} although it will
     * usually be closer to {@link #getMaxThreads()}.
     */
    private int processorCache = 200;
    public int getProcessorCache() { return this.processorCache; }
    public void setProcessorCache(int processorCache) {
        this.processorCache = processorCache;
    }


    /**
     * When client certificate information is presented in a form other than
     * instances of {@link java.security.cert.X509Certificate} it needs to be
     * converted before it can be used and this property controls which JSSE
     * provider is used to perform the conversion. For example it is used with
     * the AJP connectors, the HTTP APR connector and with the
     * {@link org.apache.catalina.valves.SSLValve}. If not specified, the
     * default provider will be used. 
     */
    private String clientCertProvider = null;
    public String getClientCertProvider() { return clientCertProvider; }
    public void setClientCertProvider(String s) { this.clientCertProvider = s; }


    @Override
    public boolean isAprRequired() {
        return false;
    }


    // ---------------------- Properties that are passed through to the EndPoint

    @Override
    public Executor getExecutor() { return endpoint.getExecutor(); }
    public void setExecutor(Executor executor) {
        endpoint.setExecutor(executor);
    }


    public int getMaxThreads() { return endpoint.getMaxThreads(); }
    public void setMaxThreads(int maxThreads) {
        endpoint.setMaxThreads(maxThreads);
    }
    
    public int getMaxConnections() { return endpoint.getMaxConnections(); }
    public void setMaxConnections(int maxConnections) {
        endpoint.setMaxConnections(maxConnections);
    }


    public int getMinSpareThreads() { return endpoint.getMinSpareThreads(); }
    public void setMinSpareThreads(int minSpareThreads) {
        endpoint.setMinSpareThreads(minSpareThreads);
    }


    public int getThreadPriority() { return endpoint.getThreadPriority(); }
    public void setThreadPriority(int threadPriority) {
        endpoint.setThreadPriority(threadPriority);
    }


    public int getBacklog() { return endpoint.getBacklog(); }
    public void setBacklog(int backlog) { endpoint.setBacklog(backlog); }


    public boolean getTcpNoDelay() { return endpoint.getTcpNoDelay(); }
    public void setTcpNoDelay(boolean tcpNoDelay) {
        endpoint.setTcpNoDelay(tcpNoDelay);
    }


    public int getSoLinger() { return endpoint.getSoLinger(); }
    public void setSoLinger(int soLinger) { endpoint.setSoLinger(soLinger); }


    public int getKeepAliveTimeout() { return endpoint.getKeepAliveTimeout(); }
    public void setKeepAliveTimeout(int keepAliveTimeout) {
        endpoint.setKeepAliveTimeout(keepAliveTimeout);
    }

    public InetAddress getAddress() { return endpoint.getAddress(); }
    public void setAddress(InetAddress ia) {
        endpoint.setAddress(ia);
    }


    public int getPort() { return endpoint.getPort(); }
    public void setPort(int port) {
        endpoint.setPort(port);
    }


    public int getLocalPort() { return endpoint.getLocalPort(); }

    /*
     * When Tomcat expects data from the client, this is the time Tomcat will
     * wait for that data to arrive before closing the connection.
     */
    public int getConnectionTimeout() {
        // Note that the endpoint uses the alternative name
        return endpoint.getSoTimeout();
    }
    public void setConnectionTimeout(int timeout) {
        // Note that the endpoint uses the alternative name
        endpoint.setSoTimeout(timeout);
    }

    /*
     * Alternative name for connectionTimeout property
     */
    public int getSoTimeout() {
        return getConnectionTimeout();
    }
    public void setSoTimeout(int timeout) {
        setConnectionTimeout(timeout);
    }

    public int getMaxHeaderCount() {
        return endpoint.getMaxHeaderCount();
    }
    public void setMaxHeaderCount(int maxHeaderCount) {
        endpoint.setMaxHeaderCount(maxHeaderCount);
    }

    public long getConnectionCount() {
        return endpoint.getConnectionCount();
    }


    // ---------------------------------------------------------- Public methods

    public synchronized int getNameIndex() {
        if (nameIndex == 0) {
            nameIndex = nameCounter.incrementAndGet();
        }

        return nameIndex;
    }


    /**
     * The name will be prefix-address-port if address is non-null and
     * prefix-port if the address is null. The name will be appropriately quoted
     * so it can be used directly in an ObjectName.
     */
    public String getName() {
        StringBuilder name = new StringBuilder(getNamePrefix());
        name.append('-');
        if (getAddress() != null) {
            name.append(getAddress().getHostAddress());
            name.append('-');
        }
        int port = getPort();
        if (port == 0) {
            // Auto binding is in use. Check if port is known
            name.append("auto-");
            name.append(getNameIndex());
            port = getLocalPort();
            if (port != -1) {
                name.append('-');
                name.append(port);
            }
        } else {
            name.append(port);
        }
        return ObjectName.quote(name.toString());
    }

    
    // -------------------------------------------------------- Abstract methods
    
    /**
     * Concrete implementations need to provide access to their logger to be
     * used by the abstract classes.
     */
    protected abstract Log getLog();
    
    
    /**
     * Obtain the prefix to be used when construction a name for this protocol
     * handler. The name will be prefix-address-port.
     */
    protected abstract String getNamePrefix();


    /**
     * Obtain the name of the protocol, (Http, Ajp, etc.). Used with JMX.
     */
    protected abstract String getProtocolName();


    /**
     * Obtain the handler associated with the underlying Endpoint
     */
    protected abstract AbstractEndpointHandler getHandler();


    // ----------------------------------------------------- JMX related methods

    private String domain;
    private ObjectName oname;
    private MBeanServer mserver;

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        oname = name;
        mserver = server;
        domain = name.getDomain();
        return name;
    }

    @Override
    public void postRegister(Boolean registrationDone) {
        // NOOP
    }

    @Override
    public void preDeregister() throws Exception {
        // NOOP
    }

    @Override
    public void postDeregister() {
        // NOOP
    }

    private ObjectName createObjectName() throws MalformedObjectNameException {
        // Use the same domain as the connector
        domain = adapter.getDomain();
        
        if (domain == null) {
            return null;
        }

        StringBuilder name = new StringBuilder(getDomain());
        name.append(":type=ProtocolHandler,port=");
        int port = getPort();
        if (port > 0) {
            name.append(getPort());
        } else {
            name.append("auto-");
            name.append(getNameIndex());
        }
        InetAddress address = getAddress();
        if (address != null) {
            name.append(",address=");
            name.append(ObjectName.quote(address.getHostAddress()));
        }
        return new ObjectName(name.toString());
    }


    // ------------------------------------------------------- Lifecycle methods

    /*
     * NOTE: There is no maintenance of state or checking for valid transitions
     * within this class. It is expected that the connector will maintain state
     * and prevent invalid state transitions.
     */

    @Override
    public void init() throws Exception {
        if (getLog().isInfoEnabled())
            getLog().info(sm.getString("abstractProtocolHandler.init",
                    getName()));

        if (oname == null) {
            // Component not pre-registered so register it
            oname = createObjectName();
            if (oname != null) {
                Registry2.getRegistry(null, null).registerComponent(this, oname,
                    null);
            }
        }

        if (this.domain != null) {
            try {
                tpOname = new ObjectName(domain + ":" +
                        "type=ThreadPool,name=" + getName());
                Registry2.getRegistry(null, null).registerComponent(endpoint,
                        tpOname, null);
            } catch (Exception e) {
                getLog().error(sm.getString(
                        "abstractProtocolHandler.mbeanRegistrationFailed",
                        tpOname, getName()), e);
            }
            rgOname=new ObjectName(domain +
                    ":type=GlobalRequestProcessor,name=" + getName());
            Registry2.getRegistry(null, null).registerComponent(
                    getHandler().getGlobal(), rgOname, null );
        }

        String endpointName = getName();
        endpoint.setName(endpointName.substring(1, endpointName.length()-1));

        try {
            endpoint.init();
        } catch (Exception ex) {
            getLog().error(sm.getString("abstractProtocolHandler.initError",
                    getName()), ex);
            throw ex;
        }
    }


    @Override
    public void start() throws Exception {
        if (getLog().isInfoEnabled())
            getLog().info(sm.getString("abstractProtocolHandler.start",
                    getName()));
        try {
            endpoint.start();
        } catch (Exception ex) {
            getLog().error(sm.getString("abstractProtocolHandler.startError",
                    getName()), ex);
            throw ex;
        }
    }


    @Override
    public void pause() throws Exception {
        if(getLog().isInfoEnabled())
            getLog().info(sm.getString("abstractProtocolHandler.pause",
                    getName()));
        try {
            endpoint.pause();
        } catch (Exception ex) {
            getLog().error(sm.getString("abstractProtocolHandler.pauseError",
                    getName()), ex);
            throw ex;
        }
    }

    @Override
    public void resume() throws Exception {
        if(getLog().isInfoEnabled())
            getLog().info(sm.getString("abstractProtocolHandler.resume",
                    getName()));
        try {
            endpoint.resume();
        } catch (Exception ex) {
            getLog().error(sm.getString("abstractProtocolHandler.resumeError",
                    getName()), ex);
            throw ex;
        }
    }


    @Override
    public void stop() throws Exception {
        if(getLog().isInfoEnabled())
            getLog().info(sm.getString("abstractProtocolHandler.stop",
                    getName()));
        try {
            endpoint.stop();
        } catch (Exception ex) {
            getLog().error(sm.getString("abstractProtocolHandler.stopError",
                    getName()), ex);
            throw ex;
        }
    }


    @Override
    public void destroy() {
        if(getLog().isInfoEnabled()) {
            getLog().info(sm.getString("abstractProtocolHandler.destroy",
                    getName()));
        }
        try {
            endpoint.destroy();
        } catch (Exception e) {
            getLog().error(sm.getString("abstractProtocolHandler.destroyError",
                    getName()), e);
        }
        
        if (oname != null) {
            Registry2.getRegistry(null, null).unregisterComponent(oname);
        }

        if (tpOname != null)
            Registry2.getRegistry(null, null).unregisterComponent(tpOname);
        if (rgOname != null)
            Registry2.getRegistry(null, null).unregisterComponent(rgOname);
    }


	public ObjectName getRgOname() {
		return rgOname;
	}


	public void setRgOname(ObjectName rgOname) {
		this.rgOname = rgOname;
	}


	public ObjectName getTpOname() {
		return tpOname;
	}


	public void setTpOname(ObjectName tpOname) {
		this.tpOname = tpOname;
	}


	public AbstractEndpoint<S> getEndpoint() {
		return endpoint;
	}


	public void setEndpoint(AbstractEndpoint<S> endpoint) {
		this.endpoint = endpoint;
	}


	public ObjectName getOname() {
		return oname;
	}


	public void setOname(ObjectName oname) {
		this.oname = oname;
	}


	public MBeanServer getMserver() {
		return mserver;
	}


	public void setMserver(MBeanServer mserver) {
		this.mserver = mserver;
	}


	public static StringManager3 getSm() {
		return sm;
	}


	public static AtomicInteger getNamecounter() {
		return nameCounter;
	}


	public void setNameIndex(int nameIndex) {
		this.nameIndex = nameIndex;
	}


	public void setDomain(String domain) {
		this.domain = domain;
	}
}
