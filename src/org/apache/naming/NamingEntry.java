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

package org.apache.naming;

/**
 * Represents a binding in a NamingContext.
 *
 * @author Remy Maucherat
 */

public class NamingEntry {

	// -------------------------------------------------------------- Constants

	private static final int ENTRY = 0;
	private static final int LINK_REF = 1;
	private static final int REFERENCE = 2;

	private static final int CONTEXT = 10;

	// ----------------------------------------------------------- Constructors

	public NamingEntry(String name, Object value, int type) {
		this.setNameData(name);
		this.setValue(value);
		this.setType(type);
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * The type instance variable is used to avoid using RTTI when doing
	 * lookups.
	 */
	private int type;
	private String name;
	private Object value;

	// --------------------------------------------------------- Object Methods

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NamingEntry) {
			return getNameData().equals(((NamingEntry) obj).getNameData());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getNameData().hashCode();
	}

	public static int getEntry() {
		return ENTRY;
	}

	public static int getLinkRef() {
		return LINK_REF;
	}

	public static int getReference() {
		return REFERENCE;
	}

	public static int getContext() {
		return CONTEXT;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getName() {
		return getNameData();
	}

	public void setName(String name) {
		this.setNameData(name);
	}

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}

}
