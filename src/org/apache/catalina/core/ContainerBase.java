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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.tomcat.util.res.StringManager3;

/**
 * Abstract implementation of the <b>Container</b> interface, providing common
 * functionality required by nearly every implementation. Classes extending this
 * base class must implement <code>getInfo()</code>, and may implement a
 * replacement for <code>invoke()</code>.
 * <p>
 * All subclasses of this abstract base class will include support for a
 * Pipeline object that defines the processing to be performed for each request
 * received by the <code>invoke()</code> method of this class, utilizing the
 * "Chain of Responsibility" design pattern. A subclass should encapsulate its
 * own processing functionality as a <code>Valve</code>, and configure this
 * Valve into the pipeline by calling <code>setBasic()</code>.
 * <p>
 * This implementation fires property change events, per the JavaBeans design
 * pattern, for changes in singleton properties. In addition, it fires the
 * following <code>ContainerEvent</code> events to listeners who register
 * themselves with <code>addContainerListener()</code>:
 * <table border=1>
 * <tr>
 * <th>Type</th>
 * <th>Data</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td align=center><code>addChild</code></td>
 * <td align=center><code>Container</code></td>
 * <td>Child container added to this Container.</td>
 * </tr>
 * <tr>
 * <td align=center><code>{@link #getPipeline() pipeline}.addValve</code></td>
 * <td align=center><code>Valve</code></td>
 * <td>Valve added to this Container.</td>
 * </tr>
 * <tr>
 * <td align=center><code>removeChild</code></td>
 * <td align=center><code>Container</code></td>
 * <td>Child container removed from this Container.</td>
 * </tr>
 * <tr>
 * <td align=center><code>{@link #getPipeline() pipeline}.removeValve</code></td>
 * <td align=center><code>Valve</code></td>
 * <td>Valve removed from this Container.</td>
 * </tr>
 * <tr>
 * <td align=center><code>start</code></td>
 * <td align=center><code>null</code></td>
 * <td>Container was started.</td>
 * </tr>
 * <tr>
 * <td align=center><code>stop</code></td>
 * <td align=center><code>null</code></td>
 * <td>Container was stopped.</td>
 * </tr>
 * </table>
 * Subclasses that fire additional events should document them in the class
 * comments of the implementation class.
 * 
 * TODO: Review synchronisation around background processing. See bug 47024.
 * 
 * @author Craig R. McClanahan
 */
public abstract class ContainerBase extends LifecycleMBeanBase implements
		Container {

	private static final Log log = LogFactory
			.getLog(ContainerBase.class);

	/**
	 * The child Containers belonging to this Container, keyed by name.
	 */
	private HashMap<String, Container> children = new HashMap<String, Container>();

	/**
	 * The processor delay for this component.
	 */
	private int backgroundProcessorDelay = -1;

	/**
	 * The container event listeners for this Container. Implemented as a
	 * CopyOnWriteArrayList since listeners may invoke methods to add/remove
	 * themselves or other listeners and with a ReadWriteLock that would trigger
	 * a deadlock.
	 */
	private List<ContainerListener> listeners = new CopyOnWriteArrayList<ContainerListener>();

	/**
	 * The Loader implementation with which this Container is associated.
	 */
	private Loader loader = null;

	/**
	 * The Logger implementation with which this Container is associated.
	 */
	private Log logger = null;

	/**
	 * Associated logger name.
	 */
	private String logName = null;

	/**
	 * The Manager implementation with which this Container is associated.
	 */
	private Manager manager = null;

	/**
	 * The cluster with which this Container is associated.
	 */
	private Cluster cluster = null;

	/**
	 * The human-readable name of this Container.
	 */
	private String name = null;

	/**
	 * The parent Container to which this Container is a child.
	 */
	private Container parent = null;

	/**
	 * The parent class loader to be configured when we install a Loader.
	 */
	private ClassLoader parentClassLoader = null;

	/**
	 * The Pipeline object with which this Container is associated.
	 */
	private Pipeline pipeline = new StandardPipeline(this);

	/**
	 * The Realm with which this Container is associated.
	 */
	private volatile Realm realm = null;

	/**
	 * Lock used to control access to the Realm.
	 */
	private final ReadWriteLock realmLock = new ReentrantReadWriteLock();

	/**
	 * The resources DirContext object with which this Container is associated.
	 */
	private DirContext resources = null;

	/**
	 * The string manager for this package.
	 */
	static final StringManager3 sm = StringManager3.getManager(Constants3
			.getPackage());

	/**
	 * Will children be started automatically when they are added.
	 */
	private boolean startChildren = true;

	/**
	 * The property change support for this component.
	 */
	private PropertyChangeSupport support = new PropertyChangeSupport(this);

	/**
	 * The background thread.
	 */
	private Thread thread = null;

	/**
	 * The background thread completion semaphore.
	 */
	private volatile boolean threadDone = false;

	/**
	 * The access log to use for requests normally handled by this container
	 * that have been handled earlier in the processing chain.
	 */
	private volatile AccessLog accessLog = null;
	private volatile boolean accessLogScanComplete = false;

	/**
	 * The number of threads available to process start and stop events for any
	 * children associated with this container.
	 */
	private int startStopThreads = 1;
	private ThreadPoolExecutor startStopExecutor;

	// ------------------------------------------------------------- Properties

	@Override
	public int getStartStopThreads() {
		return getStartStopThreadsData();
	}

	/**
	 * Handles the special values.
	 */
	private int getStartStopThreadsInternal() {
		int result = getStartStopThreads();

		// Positive values are unchanged
		if (result > 0) {
			return result;
		}

		// Zero == Runtime.getRuntime().availableProcessors()
		// -ve == Runtime.getRuntime().availableProcessors() + value
		// These two are the same
		result = Runtime.getRuntime().availableProcessors() + result;
		if (result < 1) {
			result = 1;
		}
		return result;
	}

	@Override
	public void setStartStopThreads(int startStopThreads) {
		this.setStartStopThreadsData(startStopThreads);

		// Use local copies to ensure thread safety
		ThreadPoolExecutor executor = getStartStopExecutorData();
		if (executor != null) {
			int newThreads = getStartStopThreadsInternal();
			executor.setMaximumPoolSize(newThreads);
			executor.setCorePoolSize(newThreads);
		}
	}

	/**
	 * Get the delay between the invocation of the backgroundProcess method on
	 * this container and its children. Child containers will not be invoked if
	 * their delay value is not negative (which would mean they are using their
	 * own thread). Setting this to a positive value will cause a thread to be
	 * spawn. After waiting the specified amount of time, the thread will invoke
	 * the executePeriodic method on this container and all its children.
	 */
	@Override
	public int getBackgroundProcessorDelay() {
		return getBackgroundProcessorDelayData();
	}

	/**
	 * Set the delay between the invocation of the execute method on this
	 * container and its children.
	 * 
	 * @param delay
	 *            The delay in seconds between the invocation of
	 *            backgroundProcess methods
	 */
	@Override
	public void setBackgroundProcessorDelay(int delay) {
		setBackgroundProcessorDelayData(delay);
	}

	/**
	 * Return descriptive information about this Container implementation and
	 * the corresponding version number, in the format
	 * <code>&lt;description&gt;/&lt;version&gt;</code>.
	 */
	@Override
	public String getInfo() {
		return this.getClass().getName();
	}

	/**
	 * Return the Loader with which this Container is associated. If there is no
	 * associated Loader, return the Loader associated with our parent Container
	 * (if any); otherwise, return <code>null</code>.
	 */
	@Override
	public Loader getLoader() {

		if (getLoaderData() != null)
			return (getLoaderData());
		if (getParentData() != null)
			return (getParentData().getLoader());
		return (null);

	}

	/**
	 * Set the Loader with which this Container is associated.
	 *
	 * @param loader
	 *            The newly associated loader
	 */
	@Override
	public synchronized void setLoader(Loader loader) {

		// Change components if necessary
		Loader oldLoader = this.getLoaderData();
		if (oldLoader == loader)
			return;
		this.setLoaderData(loader);

		// Stop the old component if necessary
		if (getState().isAvailable() && (oldLoader != null)
				&& (oldLoader instanceof Lifecycle)) {
			try {
				((Lifecycle) oldLoader).stop();
			} catch (LifecycleException e) {
				log.error("ContainerBase.setLoader: stop: ", e);
			}
		}

		// Start the new component if necessary
		if (loader != null)
			loader.setContainer(this);
		if (getState().isAvailable() && (loader != null)
				&& (loader instanceof Lifecycle)) {
			try {
				((Lifecycle) loader).start();
			} catch (LifecycleException e) {
				log.error("ContainerBase.setLoader: start: ", e);
			}
		}

		// Report this property change to interested listeners
		getSupportData().firePropertyChange("loader", oldLoader, this.getLoaderData());

	}

	/**
	 * Return the Logger for this Container.
	 */
	@Override
	public Log getLogger() {

		if (getLoggerData() != null)
			return (getLoggerData());
		setLoggerData(LogFactory.getLog(logName()));
		return (getLoggerData());

	}

	/**
	 * Return the Manager with which this Container is associated. If there is
	 * no associated Manager, return the Manager associated with our parent
	 * Container (if any); otherwise return <code>null</code>.
	 */
	@Override
	public Manager getManager() {

		if (getManagerData() != null)
			return (getManagerData());
		if (getParentData() != null)
			return (getParentData().getManager());
		return (null);

	}

	/**
	 * Set the Manager with which this Container is associated.
	 *
	 * @param manager
	 *            The newly associated Manager
	 */
	@Override
	public synchronized void setManager(Manager manager) {

		// Change components if necessary
		Manager oldManager = this.getManagerData();
		if (oldManager == manager)
			return;
		this.setManagerData(manager);

		// Stop the old component if necessary
		if (getState().isAvailable() && (oldManager != null)
				&& (oldManager instanceof Lifecycle)) {
			try {
				((Lifecycle) oldManager).stop();
			} catch (LifecycleException e) {
				log.error("ContainerBase.setManager: stop: ", e);
			}
		}

		// Start the new component if necessary
		if (manager != null)
			manager.setContainer(this);
		if (getState().isAvailable() && (manager != null)
				&& (manager instanceof Lifecycle)) {
			try {
				((Lifecycle) manager).start();
			} catch (LifecycleException e) {
				log.error("ContainerBase.setManager: start: ", e);
			}
		}

		// Report this property change to interested listeners
		getSupportData().firePropertyChange("manager", oldManager, this.getManagerData());

	}

	/**
	 * Return an object which may be utilized for mapping to this component.
	 */
	@Deprecated
	@Override
	public Object getMappingObject() {
		return this;
	}

	/**
	 * Return the Cluster with which this Container is associated. If there is
	 * no associated Cluster, return the Cluster associated with our parent
	 * Container (if any); otherwise return <code>null</code>.
	 */
	@Override
	public Cluster getCluster() {
		if (getClusterData() != null)
			return (getClusterData());

		if (getParentData() != null)
			return (getParentData().getCluster());

		return (null);
	}

	/**
	 * Set the Cluster with which this Container is associated.
	 *
	 * @param cluster
	 *            The newly associated Cluster
	 */
	@Override
	public synchronized void setCluster(Cluster cluster) {
		// Change components if necessary
		Cluster oldCluster = this.getClusterData();
		if (oldCluster == cluster)
			return;
		this.setClusterData(cluster);

		// Stop the old component if necessary
		if (getState().isAvailable() && (oldCluster != null)
				&& (oldCluster instanceof Lifecycle)) {
			try {
				((Lifecycle) oldCluster).stop();
			} catch (LifecycleException e) {
				log.error("ContainerBase.setCluster: stop: ", e);
			}
		}

		// Start the new component if necessary
		if (cluster != null)
			cluster.setContainer(this);

		if (getState().isAvailable() && (cluster != null)
				&& (cluster instanceof Lifecycle)) {
			try {
				((Lifecycle) cluster).start();
			} catch (LifecycleException e) {
				log.error("ContainerBase.setCluster: start: ", e);
			}
		}

		// Report this property change to interested listeners
		getSupportData().firePropertyChange("cluster", oldCluster, this.getClusterData());
	}

	/**
	 * Return a name string (suitable for use by humans) that describes this
	 * Container. Within the set of child containers belonging to a particular
	 * parent, Container names must be unique.
	 */
	@Override
	public String getName() {

		return (getNameData());

	}

	/**
	 * Set a name string (suitable for use by humans) that describes this
	 * Container. Within the set of child containers belonging to a particular
	 * parent, Container names must be unique.
	 *
	 * @param name
	 *            New name of this container
	 *
	 * @exception IllegalStateException
	 *                if this Container has already been added to the children
	 *                of a parent Container (after which the name may not be
	 *                changed)
	 */
	@Override
	public void setName(String name) {

		String oldName = this.getNameData();
		this.setNameData(name);
		getSupportData().firePropertyChange("name", oldName, this.getNameData());
	}

	/**
	 * Return if children of this container will be started automatically when
	 * they are added to this container.
	 */
	public boolean getStartChildren() {

		return (isStartChildrenData());

	}

	/**
	 * Set if children of this container will be started automatically when they
	 * are added to this container.
	 *
	 * @param startChildren
	 *            New value of the startChildren flag
	 */
	public void setStartChildren(boolean startChildren) {

		boolean oldStartChildren = this.isStartChildrenData();
		this.setStartChildrenData(startChildren);
		getSupportData().firePropertyChange("startChildren", oldStartChildren,
				this.isStartChildrenData());
	}

	/**
	 * Return the Container for which this Container is a child, if there is
	 * one. If there is no defined parent, return <code>null</code>.
	 */
	@Override
	public Container getParent() {

		return (getParentData());

	}

	/**
	 * Set the parent Container to which this Container is being added as a
	 * child. This Container may refuse to become attached to the specified
	 * Container by throwing an exception.
	 *
	 * @param container
	 *            Container to which this Container is being added as a child
	 *
	 * @exception IllegalArgumentException
	 *                if this Container refuses to become attached to the
	 *                specified Container
	 */
	@Override
	public void setParent(Container container) {

		Container oldParent = this.getParentData();
		this.setParentData(container);
		getSupportData().firePropertyChange("parent", oldParent, this.getParentData());

	}

	/**
	 * Return the parent class loader (if any) for this web application. This
	 * call is meaningful only <strong>after</strong> a Loader has been
	 * configured.
	 */
	@Override
	public ClassLoader getParentClassLoader() {
		if (getParentClassLoaderData() != null)
			return (getParentClassLoaderData());
		if (getParentData() != null) {
			return (getParentData().getParentClassLoader());
		}
		return (ClassLoader.getSystemClassLoader());

	}

	/**
	 * Set the parent class loader (if any) for this web application. This call
	 * is meaningful only <strong>before</strong> a Loader has been configured,
	 * and the specified value (if non-null) should be passed as an argument to
	 * the class loader constructor.
	 *
	 *
	 * @param parent
	 *            The new parent class loader
	 */
	@Override
	public void setParentClassLoader(ClassLoader parent) {
		ClassLoader oldParentClassLoader = this.getParentClassLoaderData();
		this.setParentClassLoaderData(parent);
		getSupportData().firePropertyChange("parentClassLoader", oldParentClassLoader,
				this.getParentClassLoaderData());

	}

	/**
	 * Return the Pipeline object that manages the Valves associated with this
	 * Container.
	 */
	@Override
	public Pipeline getPipeline() {

		return (this.getPipelineData());

	}

	/**
	 * Return the Realm with which this Container is associated. If there is no
	 * associated Realm, return the Realm associated with our parent Container
	 * (if any); otherwise return <code>null</code>.
	 */
	@Override
	public Realm getRealm() {

		Lock l = getRealmLockData().readLock();
		try {
			l.lock();
			if (getRealmData() != null)
				return (getRealmData());
			if (getParentData() != null)
				return (getParentData().getRealm());
			return null;
		} finally {
			l.unlock();
		}
	}

	protected Realm getRealmInternal() {
		Lock l = getRealmLockData().readLock();
		try {
			l.lock();
			return getRealmData();
		} finally {
			l.unlock();
		}
	}

	/**
	 * Set the Realm with which this Container is associated.
	 *
	 * @param realm
	 *            The newly associated Realm
	 */
	@Override
	public void setRealm(Realm realm) {

		Lock l = getRealmLockData().writeLock();

		try {
			l.lock();

			// Change components if necessary
			Realm oldRealm = this.getRealmData();
			if (oldRealm == realm)
				return;
			this.setRealmData(realm);

			// Stop the old component if necessary
			if (getState().isAvailable() && (oldRealm != null)
					&& (oldRealm instanceof Lifecycle)) {
				try {
					((Lifecycle) oldRealm).stop();
				} catch (LifecycleException e) {
					log.error("ContainerBase.setRealm: stop: ", e);
				}
			}

			// Start the new component if necessary
			if (realm != null)
				realm.setContainer(this);
			if (getState().isAvailable() && (realm != null)
					&& (realm instanceof Lifecycle)) {
				try {
					((Lifecycle) realm).start();
				} catch (LifecycleException e) {
					log.error("ContainerBase.setRealm: start: ", e);
				}
			}

			// Report this property change to interested listeners
			getSupportData().firePropertyChange("realm", oldRealm, this.getRealmData());
		} finally {
			l.unlock();
		}

	}

	/**
	 * Return the resources DirContext object with which this Container is
	 * associated. If there is no associated resources object, return the
	 * resources associated with our parent Container (if any); otherwise return
	 * <code>null</code>.
	 */
	@Override
	public DirContext getResources() {
		if (getResourcesData() != null)
			return (getResourcesData());
		if (getParentData() != null)
			return (getParentData().getResources());
		return (null);

	}

	/**
	 * Set the resources DirContext object with which this Container is
	 * associated.
	 *
	 * @param resources
	 *            The newly associated DirContext
	 */
	@Override
	public synchronized void setResources(DirContext resources) {
		// Called from StandardContext.setResources()
		// <- StandardContext.start()
		// <- ContainerBase.addChildInternal()

		// Change components if necessary
		DirContext oldResources = this.getResourcesData();
		if (oldResources == resources)
			return;
		Hashtable<String, String> env = new Hashtable<String, String>();
		if (getParent() != null)
			env.put(ProxyDirContext.getHost(), getParent().getName());
		env.put(ProxyDirContext.getContext(), getName());
		this.setResourcesData(new ProxyDirContext(env, resources));
		// Report this property change to interested listeners
		getSupportData().firePropertyChange("resources", oldResources, this.getResourcesData());

	}

	// ------------------------------------------------------ Container Methods

	/**
	 * Add a new child Container to those associated with this Container, if
	 * supported. Prior to adding this Container to the set of children, the
	 * child's <code>setParent()</code> method must be called, with this
	 * Container as an argument. This method may thrown an
	 * <code>IllegalArgumentException</code> if this Container chooses not to be
	 * attached to the specified Container, in which case it is not added
	 *
	 * @param child
	 *            New child Container to be added
	 *
	 * @exception IllegalArgumentException
	 *                if this exception is thrown by the
	 *                <code>setParent()</code> method of the child Container
	 * @exception IllegalArgumentException
	 *                if the new child does not have a name unique from that of
	 *                existing children of this Container
	 * @exception IllegalStateException
	 *                if this Container does not support child Containers
	 */
	@Override
	public void addChild(Container child) {
		if (Globals.isSecurityEnabled()) {
			PrivilegedAction<Void> dp = new ContainerBasePrivilegedAddChild(this, child);
			AccessController.doPrivileged(dp);
		} else {
			addChildInternal(child);
		}
	}

	void addChildInternal(Container child) {

		if (log.isDebugEnabled())
			log.debug("Add child " + child + " " + this);
		synchronized (getChildrenData()) {
			if (getChildrenData().get(child.getName()) != null)
				throw new IllegalArgumentException("addChild:  Child name '"
						+ child.getName() + "' is not unique");
			child.setParent(this); // May throw IAE
			getChildrenData().put(child.getName(), child);
		}

		// Start child
		// Don't do this inside sync block - start can be a slow process and
		// locking the children object can cause problems elsewhere
		if ((getState().isAvailable() || LifecycleState.STARTING_PREP
				.equals(getState())) && isStartChildrenData()) {
			try {
				child.start();
			} catch (LifecycleException e) {
				log.error("ContainerBase.addChild: start: ", e);
				throw new IllegalStateException(
						"ContainerBase.addChild: start: " + e);
			}
		}

		fireContainerEvent(ADD_CHILD_EVENT, child);
	}

	/**
	 * Add a container event listener to this component.
	 *
	 * @param listener
	 *            The listener to add
	 */
	@Override
	public void addContainerListener(ContainerListener listener) {
		getListenersData().add(listener);
	}

	/**
	 * Add a property change listener to this component.
	 *
	 * @param listener
	 *            The listener to add
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {

		getSupportData().addPropertyChangeListener(listener);

	}

	/**
	 * Return the child Container, associated with this Container, with the
	 * specified name (if any); otherwise, return <code>null</code>
	 *
	 * @param name
	 *            Name of the child Container to be retrieved
	 */
	@Override
	public Container findChild(String name) {

		if (name == null)
			return (null);
		synchronized (getChildrenData()) {
			return getChildrenData().get(name);
		}

	}

	/**
	 * Return the set of children Containers associated with this Container. If
	 * this Container has no children, a zero-length array is returned.
	 */
	@Override
	public Container[] findChildren() {

		synchronized (getChildrenData()) {
			Container results[] = new Container[getChildrenData().size()];
			return getChildrenData().values().toArray(results);
		}

	}

	/**
	 * Return the set of container listeners associated with this Container. If
	 * this Container has no registered container listeners, a zero-length array
	 * is returned.
	 */
	@Override
	public ContainerListener[] findContainerListeners() {
		ContainerListener[] results = new ContainerListener[0];
		return getListenersData().toArray(results);
	}

	/**
	 * Process the specified Request, to produce the corresponding Response, by
	 * invoking the first Valve in our pipeline (if any), or the basic Valve
	 * otherwise.
	 *
	 * @param request
	 *            Request to be processed
	 * @param response
	 *            Response to be produced
	 *
	 * @exception IllegalStateException
	 *                if neither a pipeline or a basic Valve have been
	 *                configured for this Container
	 * @exception IOException
	 *                if an input/output error occurred while processing
	 * @exception ServletException
	 *                if a ServletException was thrown while processing this
	 *                request
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {

		getPipelineData().getFirst().invoke(request, response);

	}

	/**
	 * Remove an existing child Container from association with this parent
	 * Container.
	 *
	 * @param child
	 *            Existing child Container to be removed
	 */
	@Override
	public void removeChild(Container child) {

		if (child == null) {
			return;
		}

		synchronized (getChildrenData()) {
			if (getChildrenData().get(child.getName()) == null)
				return;
			getChildrenData().remove(child.getName());
		}

		try {
			if (child.getState().isAvailable()) {
				child.stop();
			}
		} catch (LifecycleException e) {
			log.error("ContainerBase.removeChild: stop: ", e);
		}

		fireContainerEvent(REMOVE_CHILD_EVENT, child);

		try {
			// child.destroy() may have already been called which would have
			// triggered this call. If that is the case, no need to destroy the
			// child again.
			if (!LifecycleState.DESTROYING.equals(child.getState())) {
				child.destroy();
			}
		} catch (LifecycleException e) {
			log.error("ContainerBase.removeChild: destroy: ", e);
		}

	}

	/**
	 * Remove a container event listener from this component.
	 *
	 * @param listener
	 *            The listener to remove
	 */
	@Override
	public void removeContainerListener(ContainerListener listener) {
		getListenersData().remove(listener);
	}

	/**
	 * Remove a property change listener from this component.
	 *
	 * @param listener
	 *            The listener to remove
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {

		getSupportData().removePropertyChangeListener(listener);

	}

	@Override
	protected void initInternal() throws LifecycleException {
		BlockingQueue<Runnable> startStopQueue = new LinkedBlockingQueue<Runnable>();
		setStartStopExecutorData(new ThreadPoolExecutor(
				getStartStopThreadsInternal(), getStartStopThreadsInternal(),
				10, TimeUnit.SECONDS, startStopQueue,
				new ContainerBaseStartStopThreadFactory(getName()
						+ "-startStop-")));
		getStartStopExecutorData().allowCoreThreadTimeOut(true);
		super.initInternal();
	}

	/**
	 * Start this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void startInternal() throws LifecycleException {

		// Start our subordinate components, if any
		if ((getLoaderData() != null) && (getLoaderData() instanceof Lifecycle))
			((Lifecycle) getLoaderData()).start();
		setLoggerData(null);
		getLogger();
		if ((getManagerData() != null) && (getManagerData() instanceof Lifecycle))
			((Lifecycle) getManagerData()).start();
		if ((getClusterData() != null) && (getClusterData() instanceof Lifecycle))
			((Lifecycle) getClusterData()).start();
		Realm realm = getRealmInternal();
		if ((realm != null) && (realm instanceof Lifecycle))
			((Lifecycle) realm).start();
		if ((getResourcesData() != null) && (getResourcesData() instanceof Lifecycle))
			((Lifecycle) getResourcesData()).start();

		// Start our child containers, if any
		Container children[] = findChildren();
		List<Future<Void>> results = new ArrayList<Future<Void>>();
		for (int i = 0; i < children.length; i++) {
			results.add(getStartStopExecutorData().submit(new ContainerBaseStartChild(
					children[i])));
		}

		boolean fail = false;
		for (Future<Void> result : results) {
			try {
				result.get();
			} catch (Exception e) {
				log.error(sm.getString("containerBase.threadedStartFailed"), e);
				fail = true;
			}

		}
		if (fail) {
			throw new LifecycleException(
					sm.getString("containerBase.threadedStartFailed"));
		}

		// Start the Valves in our pipeline (including the basic), if any
		if (getPipelineData() instanceof Lifecycle)
			((Lifecycle) getPipelineData()).start();

		setState(LifecycleState.STARTING);

		// Start our thread
		threadStart();

	}

	/**
	 * Stop this component and implement the requirements of
	 * {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
	 *
	 * @exception LifecycleException
	 *                if this component detects a fatal error that prevents this
	 *                component from being used
	 */
	@Override
	protected synchronized void stopInternal() throws LifecycleException {

		// Stop our thread
		threadStop();

		setState(LifecycleState.STOPPING);

		// Stop the Valves in our pipeline (including the basic), if any
		if (getPipelineData() instanceof Lifecycle
				&& ((Lifecycle) getPipelineData()).getState().isAvailable()) {
			((Lifecycle) getPipelineData()).stop();
		}

		// Stop our child containers, if any
		Container children[] = findChildren();
		List<Future<Void>> results = new ArrayList<Future<Void>>();
		for (int i = 0; i < children.length; i++) {
			results.add(getStartStopExecutorData().submit(new ContainerBaseStopChild(
					children[i])));
		}

		boolean fail = false;
		for (Future<Void> result : results) {
			try {
				result.get();
			} catch (Exception e) {
				log.error(sm.getString("containerBase.threadedStopFailed"), e);
				fail = true;
			}
		}
		if (fail) {
			throw new LifecycleException(
					sm.getString("containerBase.threadedStopFailed"));
		}

		// Stop our subordinate components, if any
		if ((getResourcesData() != null) && (getResourcesData() instanceof Lifecycle)) {
			((Lifecycle) getResourcesData()).stop();
		}
		Realm realm = getRealmInternal();
		if ((realm != null) && (realm instanceof Lifecycle)) {
			((Lifecycle) realm).stop();
		}
		if ((getClusterData() != null) && (getClusterData() instanceof Lifecycle)) {
			((Lifecycle) getClusterData()).stop();
		}
		if ((getManagerData() != null) && (getManagerData() instanceof Lifecycle)
				&& ((Lifecycle) getManagerData()).getState().isAvailable()) {
			((Lifecycle) getManagerData()).stop();
		}
		if ((getLoaderData() != null) && (getLoaderData() instanceof Lifecycle)) {
			((Lifecycle) getLoaderData()).stop();
		}
	}

	@Override
	protected void destroyInternal() throws LifecycleException {

		if ((getManagerData() != null) && (getManagerData() instanceof Lifecycle)) {
			((Lifecycle) getManagerData()).destroy();
		}
		Realm realm = getRealmInternal();
		if ((realm != null) && (realm instanceof Lifecycle)) {
			((Lifecycle) realm).destroy();
		}
		if ((getClusterData() != null) && (getClusterData() instanceof Lifecycle)) {
			((Lifecycle) getClusterData()).destroy();
		}
		if ((getLoaderData() != null) && (getLoaderData() instanceof Lifecycle)) {
			((Lifecycle) getLoaderData()).destroy();
		}

		// Stop the Valves in our pipeline (including the basic), if any
		if (getPipelineData() instanceof Lifecycle) {
			((Lifecycle) getPipelineData()).destroy();
		}

		// Remove children now this container is being destroyed
		for (Container child : findChildren()) {
			removeChild(child);
		}

		// Required if the child is destroyed directly.
		if (getParentData() != null) {
			getParentData().removeChild(this);
		}

		// If init fails, this may be null
		if (getStartStopExecutorData() != null) {
			getStartStopExecutorData().shutdownNow();
		}

		super.destroyInternal();
	}

	/**
	 * Check this container for an access log and if none is found, look to the
	 * parent. If there is no parent and still none is found, use the NoOp
	 * access log.
	 */
	@Override
	public void logAccess(Request request, Response response, long time,
			boolean useDefault) {

		boolean logged = false;

		if (getAccessLog() != null) {
			getAccessLog().log(request, response, time);
			logged = true;
		}

		if (getParent() != null) {
			// No need to use default logger once request/response has been
			// logged
			// once
			getParent().logAccess(request, response, time,
					(useDefault && !logged));
		}
	}

	@Override
	public AccessLog getAccessLog() {

		if (isAccessLogScanCompleteData()) {
			return getAccessLogData();
		}

		AccessLogAdapter adapter = null;
		Valve valves[] = getPipeline().getValves();
		for (Valve valve : valves) {
			if (valve instanceof AccessLog) {
				if (adapter == null) {
					adapter = new AccessLogAdapter((AccessLog) valve);
				} else {
					adapter.add((AccessLog) valve);
				}
			}
		}
		if (adapter != null) {
			setAccessLogData(adapter);
		}
		setAccessLogScanCompleteData(true);
		return getAccessLogData();
	}

	// ------------------------------------------------------- Pipeline Methods

	/**
	 * Convenience method, intended for use by the digester to simplify the
	 * process of adding Valves to containers. See
	 * {@link Pipeline#addValve(Valve)} for full details. Components other than
	 * the digester should use {@link #getPipeline()}.{@link #addValve(Valve)}
	 * in case a future implementation provides an alternative method for the
	 * digester to use.
	 *
	 * @param valve
	 *            Valve to be added
	 *
	 * @exception IllegalArgumentException
	 *                if this Container refused to accept the specified Valve
	 * @exception IllegalArgumentException
	 *                if the specified Valve refuses to be associated with this
	 *                Container
	 * @exception IllegalStateException
	 *                if the specified Valve is already associated with a
	 *                different Container
	 */
	public synchronized void addValve(Valve valve) {

		getPipelineData().addValve(valve);
	}

	/**
	 * Execute a periodic task, such as reloading, etc. This method will be
	 * invoked inside the classloading context of this container. Unexpected
	 * throwables will be caught and logged.
	 */
	@Override
	public void backgroundProcess() {

		if (!getState().isAvailable())
			return;

		if (getClusterData() != null) {
			try {
				getClusterData().backgroundProcess();
			} catch (Exception e) {
				log.warn(sm.getString(
						"containerBase.backgroundProcess.cluster", getClusterData()), e);
			}
		}
		if (getLoaderData() != null) {
			try {
				getLoaderData().backgroundProcess();
			} catch (Exception e) {
				log.warn(sm.getString("containerBase.backgroundProcess.loader",
						getLoaderData()), e);
			}
		}
		if (getManagerData() != null) {
			try {
				getManagerData().backgroundProcess();
			} catch (Exception e) {
				log.warn(sm.getString(
						"containerBase.backgroundProcess.manager", getManagerData()), e);
			}
		}
		Realm realm = getRealmInternal();
		if (realm != null) {
			try {
				realm.backgroundProcess();
			} catch (Exception e) {
				log.warn(sm.getString("containerBase.backgroundProcess.realm",
						realm), e);
			}
		}
		Valve current = getPipelineData().getFirst();
		while (current != null) {
			try {
				current.backgroundProcess();
			} catch (Exception e) {
				log.warn(sm.getString("containerBase.backgroundProcess.valve",
						current), e);
			}
			current = current.getNext();
		}
		fireLifecycleEvent(Lifecycle.PERIODIC_EVENT, null);
	}

	// ------------------------------------------------------ Protected Methods

	/**
	 * Notify all container event listeners that a particular event has occurred
	 * for this Container. The default implementation performs this notification
	 * synchronously using the calling thread.
	 *
	 * @param type
	 *            Event type
	 * @param data
	 *            Event data
	 */
	@Override
	public void fireContainerEvent(String type, Object data) {

		if (getListenersData().size() < 1)
			return;

		ContainerEvent event = new ContainerEvent(this, type, data);
		// Note for each uses an iterator internally so this is safe
		for (ContainerListener listener : getListenersData()) {
			listener.containerEvent(event);
		}
	}

	/**
	 * Return the abbreviated name of this container for logging messages
	 */
	protected String logName() {

		if (getLogNameData() != null) {
			return getLogNameData();
		}
		String loggerName = null;
		Container current = this;
		while (current != null) {
			String name = current.getName();
			if ((name == null) || (name.equals(""))) {
				name = "/";
			} else if (name.startsWith("##")) {
				name = "/" + name;
			}
			loggerName = "[" + name + "]"
					+ ((loggerName != null) ? ("." + loggerName) : "");
			current = current.getParent();
		}
		setLogNameData(ContainerBase.class.getName() + "." + loggerName);
		return getLogNameData();

	}

	// -------------------- JMX and Registration --------------------

	@Override
	protected String getDomainInternal() {
		return MBeanUtils.getDomain(this);
	}

	public ObjectName[] getChildren() {
		ObjectName result[] = new ObjectName[getChildrenData().size()];
		Iterator<Container> it = getChildrenData().values().iterator();
		int i = 0;
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof ContainerBase) {
				result[i++] = ((ContainerBase) next).getObjectName();
			}
		}
		return result;
	}

	// -------------------- Background Thread --------------------

	/**
	 * Start the background thread that will periodically check for session
	 * timeouts.
	 */
	protected void threadStart() {

		if (getThreadData() != null)
			return;
		if (getBackgroundProcessorDelayData() <= 0)
			return;

		setThreadDoneData(false);
		String threadName = "ContainerBackgroundProcessor[" + toString() + "]";
		setThreadData(new Thread(
				new ContainerBaseContainerBackgroundProcessor(this), threadName));
		getThreadData().setDaemon(true);
		getThreadData().start();

	}

	/**
	 * Stop the background thread that is periodically checking for session
	 * timeouts.
	 */
	protected void threadStop() {

		if (getThreadData() == null)
			return;

		setThreadDoneData(true);
		getThreadData().interrupt();
		try {
			getThreadData().join();
		} catch (InterruptedException e) {
			// Ignore
		}

		setThreadData(null);

	}

	public List<ContainerListener> getListeners() {
		return getListenersData();
	}

	public void setListeners(List<ContainerListener> listeners) {
		this.setListenersData(listeners);
	}

	public String getLogName() {
		return getLogNameData();
	}

	public void setLogName(String logName) {
		this.setLogNameData(logName);
	}

	public PropertyChangeSupport getSupport() {
		return getSupportData();
	}

	public void setSupport(PropertyChangeSupport support) {
		this.setSupportData(support);
	}

	public Thread getThread() {
		return getThreadData();
	}

	public void setThread(Thread thread) {
		this.setThreadData(thread);
	}

	public boolean isThreadDone() {
		return isThreadDoneData();
	}

	public void setThreadDone(boolean threadDone) {
		this.setThreadDoneData(threadDone);
	}

	public boolean isAccessLogScanComplete() {
		return isAccessLogScanCompleteData();
	}

	public void setAccessLogScanComplete(boolean accessLogScanComplete) {
		this.setAccessLogScanCompleteData(accessLogScanComplete);
	}

	public ThreadPoolExecutor getStartStopExecutor() {
		return getStartStopExecutorData();
	}

	public void setStartStopExecutor(ThreadPoolExecutor startStopExecutor) {
		this.setStartStopExecutorData(startStopExecutor);
	}

	public static Log getLog() {
		return log;
	}

	public ReadWriteLock getRealmLock() {
		return getRealmLockData();
	}

	public static StringManager3 getSm() {
		return sm;
	}

	public void setChildren(HashMap<String, Container> children) {
		this.setChildrenData(children);
	}

	public void setLogger(Log logger) {
		this.setLoggerData(logger);
	}

	public void setPipeline(Pipeline pipeline) {
		this.setPipelineData(pipeline);
	}

	public void setAccessLog(AccessLog accessLog) {
		this.setAccessLogData(accessLog);
	}
	
	public void setResourcesVariable(DirContext value){
		this.setResourcesData(value);
	}

	public HashMap<String, Container> getChildrenData() {
		return children;
	}

	public void setChildrenData(HashMap<String, Container> children) {
		this.children = children;
	}

	public int getBackgroundProcessorDelayData() {
		return backgroundProcessorDelay;
	}

	public void setBackgroundProcessorDelayData(int backgroundProcessorDelay) {
		this.backgroundProcessorDelay = backgroundProcessorDelay;
	}

	public List<ContainerListener> getListenersData() {
		return listeners;
	}

	public void setListenersData(List<ContainerListener> listeners) {
		this.listeners = listeners;
	}

	public Loader getLoaderData() {
		return loader;
	}

	public void setLoaderData(Loader loader) {
		this.loader = loader;
	}

	public Log getLoggerData() {
		return logger;
	}

	public void setLoggerData(Log logger) {
		this.logger = logger;
	}

	public String getLogNameData() {
		return logName;
	}

	public void setLogNameData(String logName) {
		this.logName = logName;
	}

	public Manager getManagerData() {
		return manager;
	}

	public void setManagerData(Manager manager) {
		this.manager = manager;
	}

	public Cluster getClusterData() {
		return cluster;
	}

	public void setClusterData(Cluster cluster) {
		this.cluster = cluster;
	}

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}

	public Container getParentData() {
		return parent;
	}

	public void setParentData(Container parent) {
		this.parent = parent;
	}

	public ClassLoader getParentClassLoaderData() {
		return parentClassLoader;
	}

	public void setParentClassLoaderData(ClassLoader parentClassLoader) {
		this.parentClassLoader = parentClassLoader;
	}

	public Pipeline getPipelineData() {
		return pipeline;
	}

	public void setPipelineData(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	public Realm getRealmData() {
		return realm;
	}

	public void setRealmData(Realm realm) {
		this.realm = realm;
	}

	public ReadWriteLock getRealmLockData() {
		return realmLock;
	}

	public DirContext getResourcesData() {
		return resources;
	}

	public void setResourcesData(DirContext resources) {
		this.resources = resources;
	}

	public boolean isStartChildrenData() {
		return startChildren;
	}

	public void setStartChildrenData(boolean startChildren) {
		this.startChildren = startChildren;
	}

	public PropertyChangeSupport getSupportData() {
		return support;
	}

	public void setSupportData(PropertyChangeSupport support) {
		this.support = support;
	}

	public Thread getThreadData() {
		return thread;
	}

	public void setThreadData(Thread thread) {
		this.thread = thread;
	}

	public boolean isThreadDoneData() {
		return threadDone;
	}

	public void setThreadDoneData(boolean threadDone) {
		this.threadDone = threadDone;
	}

	public AccessLog getAccessLogData() {
		return accessLog;
	}

	public void setAccessLogData(AccessLog accessLog) {
		this.accessLog = accessLog;
	}

	public boolean isAccessLogScanCompleteData() {
		return accessLogScanComplete;
	}

	public void setAccessLogScanCompleteData(boolean accessLogScanComplete) {
		this.accessLogScanComplete = accessLogScanComplete;
	}

	public int getStartStopThreadsData() {
		return startStopThreads;
	}

	public void setStartStopThreadsData(int startStopThreads) {
		this.startStopThreads = startStopThreads;
	}

	public ThreadPoolExecutor getStartStopExecutorData() {
		return startStopExecutor;
	}

	public void setStartStopExecutorData(ThreadPoolExecutor startStopExecutor) {
		this.startStopExecutor = startStopExecutor;
	}

}
