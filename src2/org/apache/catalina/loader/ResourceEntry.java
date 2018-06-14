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


package org.apache.catalina.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 * Resource entry.
 *
 * @author Remy Maucherat
 */
public class ResourceEntry {


    /**
     * The "last modified" time of the origin file at the time this class
     * was loaded, in milliseconds since the epoch.
     */
	private long lastModified = -1;


    /**
     * Binary content of the resource.
     */
	private byte[] binaryContent = null;


    /**
     * Loaded class.
     */
	private volatile Class<?> loadedClass = null;


    /**
     * URL source from where the object was loaded.
     */
	private URL source = null;


    /**
     * URL of the codebase from where the object was loaded.
     */
	private URL codeBase = null;


    /**
     * Manifest (if the resource was loaded from a JAR).
     */
	private Manifest manifest = null;


    /**
     * Certificates (if the resource was loaded from a JAR).
     */
	private Certificate[] certificates = null;


	public Certificate[] getCertificates() {
		return certificates;
	}


	public void setCertificates(Certificate[] certificates) {
		this.certificates = certificates;
	}


	public Manifest getManifest() {
		return manifest;
	}


	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}


	public URL getCodeBase() {
		return codeBase;
	}


	public void setCodeBase(URL codeBase) {
		this.codeBase = codeBase;
	}


	public URL getSource() {
		return source;
	}


	public void setSource(URL source) {
		this.source = source;
	}


	public Class<?> getLoadedClass() {
		return loadedClass;
	}


	public void setLoadedClass(Class<?> loadedClass) {
		this.loadedClass = loadedClass;
	}


	public byte[] getBinaryContent() {
		return binaryContent;
	}


	public void setBinaryContent(byte[] binaryContent) {
		this.binaryContent = binaryContent;
	}


	public long getLastModified() {
		return lastModified;
	}


	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}


}

