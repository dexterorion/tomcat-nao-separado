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

/** Fileinfo
 *
 * @author Mladen Turk
 */
public class FileInfo {

    /** Allocates memory and closes lingering handles in the specified pool */
	private long pool;
    /** The bitmask describing valid fields of this apr_finfo_t structure
     *  including all available 'wanted' fields and potentially more */
	private int valid;
    /** The access permissions of the file.  Mimics Unix access rights. */
	private int protection;
    /** The type of file.  One of APR_REG, APR_DIR, APR_CHR, APR_BLK, APR_PIPE,
     * APR_LNK or APR_SOCK.  If the type is undetermined, the value is APR_NOFILE.
     * If the type cannot be determined, the value is APR_UNKFILE.
     */
	private int filetype;
    /** The user id that owns the file */
	private int user;
    /** The group id that owns the file */
	private int group;
    /** The inode of the file. */
	private int inode;
    /** The id of the device the file is on. */
	private int device;
    /** The number of hard links to the file. */
	private int nlink;
    /** The size of the file */
	private long size;
    /** The storage size consumed by the file */
	private long csize;
    /** The time the file was last accessed */
	private long atime;
    /** The time the file was last modified */
	private long mtime;
    /** The time the file was created, or the inode was last changed */
	private long ctime;
    /** The pathname of the file (possibly unrooted) */
	private String fname;
    /** The file's name (no path) in filesystem case */
	private String name;
    /** The file's handle, if accessed (can be submitted to apr_duphandle) */
	private long filehand;
	public int getFiletype() {
		return filetype;
	}
	public void setFiletype(int filetype) {
		this.filetype = filetype;
	}
	public int getUser() {
		return user;
	}
	public void setUser(int user) {
		this.user = user;
	}
	public int getGroup() {
		return group;
	}
	public void setGroup(int group) {
		this.group = group;
	}
	public int getInode() {
		return inode;
	}
	public void setInode(int inode) {
		this.inode = inode;
	}
	public int getDevice() {
		return device;
	}
	public void setDevice(int device) {
		this.device = device;
	}
	public int getNlink() {
		return nlink;
	}
	public void setNlink(int nlink) {
		this.nlink = nlink;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public long getCsize() {
		return csize;
	}
	public void setCsize(long csize) {
		this.csize = csize;
	}
	public long getAtime() {
		return atime;
	}
	public void setAtime(long atime) {
		this.atime = atime;
	}
	public long getMtime() {
		return mtime;
	}
	public void setMtime(long mtime) {
		this.mtime = mtime;
	}
	public long getCtime() {
		return ctime;
	}
	public void setCtime(long ctime) {
		this.ctime = ctime;
	}
	public String getFname() {
		return fname;
	}
	public void setFname(String fname) {
		this.fname = fname;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getFilehand() {
		return filehand;
	}
	public void setFilehand(long filehand) {
		this.filehand = filehand;
	}
	public long getPool() {
		return pool;
	}
	public void setPool(long pool) {
		this.pool = pool;
	}
	public int getValid() {
		return valid;
	}
	public void setValid(int valid) {
		this.valid = valid;
	}
	public int getProtection() {
		return protection;
	}
	public void setProtection(int protection) {
		this.protection = protection;
	}
}
