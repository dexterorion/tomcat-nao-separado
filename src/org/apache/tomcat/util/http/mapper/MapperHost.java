package org.apache.tomcat.util.http.mapper;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MapperHost extends MapperMapElement {

	private volatile MapperContextList contextList;

	/**
	 * Link to the "real" Host, shared by all aliases.
	 */
	private final MapperHost realHost;

	/**
	 * Links to all registered aliases, for easy enumeration. This field is
	 * available only in the "real" Host. In an alias this field is
	 * <code>null</code>.
	 */
	private final List<MapperHost> aliases;

	/**
	 * Creates an object for primary Host
	 */
	public MapperHost(String name, Object host) {
		super(name, host);
		this.realHost = this;
		this.setContextListData(new MapperContextList());
		this.aliases = new CopyOnWriteArrayList<MapperHost>();
	}

	/**
	 * Creates an object for an Alias
	 */
	public MapperHost(String alias, MapperHost realHost) {
		super(alias, realHost.getObject());
		this.realHost = realHost;
		this.setContextListData(realHost.getContextListData());
		this.aliases = null;
	}

	public boolean isAlias() {
		return getRealHostData() != this;
	}

	public MapperHost getRealHost() {
		return getRealHostData();
	}

	public String getRealHostName() {
		return getRealHostData().getName();
	}

	public Collection<MapperHost> getAliases() {
		return getAliasesData();
	}

	public void addAlias(MapperHost alias) {
		getAliasesData().add(alias);
	}

	public void addAliases(Collection<? extends MapperHost> c) {
		getAliasesData().addAll(c);
	}

	public void removeAlias(MapperHost alias) {
		getAliasesData().remove(alias);
	}

	public MapperContextList getContextList() {
		return getContextListData();
	}

	public void setContextList(MapperContextList contextList) {
		this.setContextListData(contextList);
	}

	public MapperContextList getContextListData() {
		return contextList;
	}

	public void setContextListData(MapperContextList contextList) {
		this.contextList = contextList;
	}

	public MapperHost getRealHostData() {
		return realHost;
	}

	public List<MapperHost> getAliasesData() {
		return aliases;
	}

}