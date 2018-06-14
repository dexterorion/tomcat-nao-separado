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

import java.util.Enumeration;

import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.apache.naming.factory.Constants30;

/**
 * Represents a reference handler for a web service.
 *
 * @author Fabien Carrion
 */

public class HandlerRef extends Reference {

	private static final long serialVersionUID = 1L;

	// -------------------------------------------------------------- Constants
	/**
	 * Default factory for this reference.
	 */
	private static final String DEFAULT_FACTORY = Constants30
			.getDefaultHandlerFactory();

	/**
	 * HandlerName address type.
	 */
	private static final String HANDLER_NAME = "handlername";

	/**
	 * Handler Classname address type.
	 */
	private static final String HANDLER_CLASS = "handlerclass";

	/**
	 * Handler Classname address type.
	 */
	private static final String HANDLER_LOCALPART = "handlerlocalpart";

	/**
	 * Handler Classname address type.
	 */
	private static final String HANDLER_NAMESPACE = "handlernamespace";

	/**
	 * Handler Classname address type.
	 */
	private static final String HANDLER_PARAMNAME = "handlerparamname";

	/**
	 * Handler Classname address type.
	 */
	private static final String HANDLER_PARAMVALUE = "handlerparamvalue";

	/**
	 * Handler SoapRole address type.
	 */
	private static final String HANDLER_SOAPROLE = "handlersoaprole";

	/**
	 * Handler PortName address type.
	 */
	private static final String HANDLER_PORTNAME = "handlerportname";

	// ----------------------------------------------------------- Constructors

	public HandlerRef(String refname, String handlerClass) {
		this(refname, handlerClass, null, null);
	}

	public HandlerRef(String refname, String handlerClass, String factory,
			String factoryLocation) {
		super(refname, factory, factoryLocation);
		StringRefAddr refAddr = null;
		if (refname != null) {
			refAddr = new StringRefAddr(HANDLER_NAME, refname);
			add(refAddr);
		}
		if (handlerClass != null) {
			refAddr = new StringRefAddr(HANDLER_CLASS, handlerClass);
			add(refAddr);
		}
	}

	// ----------------------------------------------------- Instance Variables

	// ------------------------------------------------------ Reference Methods

	/**
	 * Retrieves the class name of the factory of the object to which this
	 * reference refers.
	 */
	@Override
	public String getFactoryClassName() {
		String factory = super.getFactoryClassName();
		if (factory != null) {
			return factory;
		} else {
			factory = System.getProperty(Context.OBJECT_FACTORIES);
			if (factory != null) {
				return null;
			} else {
				return DEFAULT_FACTORY;
			}
		}
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Return a String rendering of this object.
	 */
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder("HandlerRef[");
		sb.append("className=");
		sb.append(getClassName());
		sb.append(",factoryClassLocation=");
		sb.append(getFactoryClassLocation());
		sb.append(",factoryClassName=");
		sb.append(getFactoryClassName());
		Enumeration<RefAddr> refAddrs = getAll();
		while (refAddrs.hasMoreElements()) {
			RefAddr refAddr = refAddrs.nextElement();
			sb.append(",{type=");
			sb.append(refAddr.getType());
			sb.append(",content=");
			sb.append(refAddr.getContent());
			sb.append("}");
		}
		sb.append("]");
		return (sb.toString());

	}

	public static String getHandlerLocalpart() {
		return HANDLER_LOCALPART;
	}

	public static String getHandlerNamespace() {
		return HANDLER_NAMESPACE;
	}

	public static String getHandlerParamname() {
		return HANDLER_PARAMNAME;
	}

	public static String getHandlerSoaprole() {
		return HANDLER_SOAPROLE;
	}

	public static String getHandlerPortname() {
		return HANDLER_PORTNAME;
	}

	public static String getHandlerParamvalue() {
		return HANDLER_PARAMVALUE;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public static String getDefaultFactory() {
		return DEFAULT_FACTORY;
	}

	public static String getHandlerName() {
		return HANDLER_NAME;
	}

	public static String getHandlerClass() {
		return HANDLER_CLASS;
	}

}
