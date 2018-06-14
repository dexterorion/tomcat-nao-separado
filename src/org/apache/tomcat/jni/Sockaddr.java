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

package org.apache.tomcat.jni;

/** Sockaddr
 *
 * @author Mladen Turk
 */
public class Sockaddr {

   /** The pool to use... */
	private long pool;
    /** The hostname */
	private String hostname;
    /** Either a string of the port number or the service name for the port */
	private String servname;
    /** The numeric port */
	private int port;
    /** The family */
	private int family;
    /** If multiple addresses were found by apr_sockaddr_info_get(), this
     *  points to a representation of the next address. */
	private long next;
	public long getPool() {
		return pool;
	}
	public void setPool(long pool) {
		this.pool = pool;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getServname() {
		return servname;
	}
	public void setServname(String servname) {
		this.servname = servname;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getFamily() {
		return family;
	}
	public void setFamily(int family) {
		this.family = family;
	}
	public long getNext() {
		return next;
	}
	public void setNext(long next) {
		this.next = next;
	}

}
