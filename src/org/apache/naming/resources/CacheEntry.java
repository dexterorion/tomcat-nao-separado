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

package org.apache.naming.resources;

import javax.naming.directory.DirContext;

/**
 * Implements a cache entry.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
public class CacheEntry {

	// ------------------------------------------------- Instance Variables

	private long timestamp = -1;
	private String name = null;
	private ResourceAttributes attributes = null;
	private Resource resource = null;
	private DirContext context = null;
	private boolean exists = true;
	private long accessCount = 0;
	private int size = 1;

	// ----------------------------------------------------- Public Methods

	public void recycle() {
		setTimestamp(-1);
		name = null;
		attributes = null;
		resource = null;
		context = null;
		exists = true;
		setAccessCount(0);
		setSize(1);
	}

	@Override
	public String toString() {
		return ("Cache entry: " + name + "\n" + "Exists: " + exists + "\n"
				+ "Attributes: " + attributes + "\n" + "Resource: " + resource
				+ "\n" + "Context: " + context);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getAccessCount() {
		return accessCount;
	}

	public void setAccessCount(long accessCount) {
		this.accessCount = accessCount;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ResourceAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(ResourceAttributes attributes) {
		this.attributes = attributes;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public DirContext getContext() {
		return context;
	}

	public void setContext(DirContext context) {
		this.context = context;
	}

	public boolean isExists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}

}
