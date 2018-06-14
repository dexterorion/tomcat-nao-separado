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

package org.apache.tomcat.util.http.mapper;

import org.apache.tomcat.util.buf.MessageBytes;

/**
 * Mapping data.
 *
 * @author Remy Maucherat
 */
public class MappingData {

	private Object host = null;
	private Object context = null;
	private int contextSlashCount = 0;
	private Object[] contexts = null;
	private Object wrapper = null;
	private boolean jspWildCard = false;

	private MessageBytes contextPath = MessageBytes.newInstance();
	private MessageBytes requestPath = MessageBytes.newInstance();
	private MessageBytes wrapperPath = MessageBytes.newInstance();
	private MessageBytes pathInfo = MessageBytes.newInstance();

	private MessageBytes redirectPath = MessageBytes.newInstance();

	public void recycle() {
		setHost(null);
		setContext(null);
		setContextSlashCount(0);
		setContexts(null);
		setWrapper(null);
		setJspWildCard(false);
		contextPath.recycle();
		requestPath.recycle();
		wrapperPath.recycle();
		pathInfo.recycle();
		redirectPath.recycle();
	}

	public Object getHost() {
		return host;
	}

	public void setHost(Object host) {
		this.host = host;
	}

	public Object getContext() {
		return context;
	}

	public void setContext(Object context) {
		this.context = context;
	}

	public int getContextSlashCount() {
		return contextSlashCount;
	}

	public void setContextSlashCount(int contextSlashCount) {
		this.contextSlashCount = contextSlashCount;
	}

	public Object[] getContexts() {
		return contexts;
	}

	public void setContexts(Object[] contexts) {
		this.contexts = contexts;
	}

	public Object getWrapper() {
		return wrapper;
	}

	public void setWrapper(Object wrapper) {
		this.wrapper = wrapper;
	}

	public boolean isJspWildCard() {
		return jspWildCard;
	}

	public void setJspWildCard(boolean jspWildCard) {
		this.jspWildCard = jspWildCard;
	}

	public MessageBytes getContextPath() {
		return contextPath;
	}

	public void setContextPath(MessageBytes contextPath) {
		this.contextPath = contextPath;
	}

	public MessageBytes getRequestPath() {
		return requestPath;
	}

	public void setRequestPath(MessageBytes requestPath) {
		this.requestPath = requestPath;
	}

	public MessageBytes getWrapperPath() {
		return wrapperPath;
	}

	public void setWrapperPath(MessageBytes wrapperPath) {
		this.wrapperPath = wrapperPath;
	}

	public MessageBytes getPathInfo() {
		return pathInfo;
	}

	public void setPathInfo(MessageBytes pathInfo) {
		this.pathInfo = pathInfo;
	}

	public MessageBytes getRedirectPath() {
		return redirectPath;
	}

	public void setRedirectPath(MessageBytes redirectPath) {
		this.redirectPath = redirectPath;
	}

}
