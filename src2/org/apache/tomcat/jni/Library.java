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

/**
 * Library
 *
 * @author Mladen Turk
 */
public final class Library {

	/* Default library names */
	private static String[] NAMES = { "tcnative-1", "libtcnative-1" };
	/*
	 * A handle to the unique Library singleton instance.
	 */
	private static Library _instance = null;

	public Library() throws Exception {
		boolean loaded = false;
		StringBuilder err = new StringBuilder();
		for (int i = 0; i < NAMES.length; i++) {
			try {
				System.loadLibrary(NAMES[i]);
				loaded = true;
			} catch (Throwable t) {
				if (t instanceof ThreadDeath) {
					throw (ThreadDeath) t;
				}
				if (t instanceof VirtualMachineError) {
					throw (VirtualMachineError) t;
				}
				String name = System.mapLibraryName(NAMES[i]);
				String path = System.getProperty("java.library.path");
				String sep = System.getProperty("path.separator");
				String[] paths = path.split(sep);
				for (int j = 0; j < paths.length; j++) {
					java.io.File fd = new java.io.File(paths[j], name);
					if (fd.exists()) {
						t.printStackTrace();
					}
				}
				if (i > 0)
					err.append(", ");
				err.append(t.getMessage());
			}
			if (loaded)
				break;
		}
		if (!loaded) {
			err.append('(');
			err.append(System.getProperty("java.library.path"));
			err.append(')');
			throw new UnsatisfiedLinkError(err.toString());
		}
	}

	private Library(String libraryName) {
		System.loadLibrary(libraryName);
	}

	/*
	 * create global TCN's APR pool This has to be the first call to TCN
	 * library.
	 */
	private static native boolean initialize();

	/*
	 * destroy global TCN's APR pool This has to be the last call to TCN
	 * library.
	 */
	public static native void terminate();

	/* Internal function for loading APR Features */
	private static native boolean has(int what);

	/* Internal function for loading APR Features */
	private static native int version(int what);

	/* Internal function for loading APR sizes */
	private static native int size(int what);

	/* TCN_MAJOR_VERSION */
	private static int TCN_MAJOR_VERSION = 0;
	/* TCN_MINOR_VERSION */
	private static int TCN_MINOR_VERSION = 0;
	/* TCN_PATCH_VERSION */
	private static int TCN_PATCH_VERSION = 0;
	/* TCN_IS_DEV_VERSION */
	private static int TCN_IS_DEV_VERSION = 0;
	/* APR_MAJOR_VERSION */
	private static int APR_MAJOR_VERSION = 0;
	/* APR_MINOR_VERSION */
	private static int APR_MINOR_VERSION = 0;
	/* APR_PATCH_VERSION */
	private static int APR_PATCH_VERSION = 0;
	/* APR_IS_DEV_VERSION */
	private static int APR_IS_DEV_VERSION = 0;

	/* TCN_VERSION_STRING */
	private static native String versionString();

	/* APR_VERSION_STRING */
	private static native String aprVersionString();

	/* APR Feature Macros */
	private static boolean APR_HAVE_IPV6 = false;
	private static boolean APR_HAS_SHARED_MEMORY = false;
	private static boolean APR_HAS_THREADS = false;
	private static boolean APR_HAS_SENDFILE = false;
	private static boolean APR_HAS_MMAP = false;
	private static boolean APR_HAS_FORK = false;
	private static boolean APR_HAS_RANDOM = false;
	private static boolean APR_HAS_OTHER_CHILD = false;
	private static boolean APR_HAS_DSO = false;
	private static boolean APR_HAS_SO_ACCEPTFILTER = false;
	private static boolean APR_HAS_UNICODE_FS = false;
	private static boolean APR_HAS_PROC_INVOKED = false;
	private static boolean APR_HAS_USER = false;
	private static boolean APR_HAS_LARGE_FILES = false;
	private static boolean APR_HAS_XTHREAD_FILES = false;
	private static boolean APR_HAS_OS_UUID = false;
	/* Are we big endian? */
	private static boolean APR_IS_BIGENDIAN = false;
	/*
	 * APR sets APR_FILES_AS_SOCKETS to 1 on systems where it is possible to
	 * poll on files/pipes.
	 */
	private static boolean APR_FILES_AS_SOCKETS = false;
	/*
	 * This macro indicates whether or not EBCDIC is the native character set.
	 */
	private static boolean APR_CHARSET_EBCDIC = false;
	/*
	 * Is the TCP_NODELAY socket option inherited from listening sockets?
	 */
	private static boolean APR_TCP_NODELAY_INHERITED = false;
	/*
	 * Is the O_NONBLOCK flag inherited from listening sockets?
	 */
	private static boolean APR_O_NONBLOCK_INHERITED = false;

	private static int APR_SIZEOF_VOIDP;
	private static int APR_PATH_MAX;
	private static int APRMAXHOSTLEN;
	private static int APR_MAX_IOVEC_SIZE;
	private static int APR_MAX_SECS_TO_LINGER;
	private static int APR_MMAP_THRESHOLD;
	private static int APR_MMAP_LIMIT;

	/* return global TCN's APR pool */
	public static native long globalPool();

	/**
	 * Setup any APR internal data structures. This MUST be the first function
	 * called for any APR library.
	 * 
	 * @param libraryName
	 *            the name of the library to load
	 */
	public static boolean initialize(String libraryName) throws Exception {
		if (_instance == null) {
			if (libraryName == null)
				_instance = new Library();
			else
				_instance = new Library(libraryName);
			TCN_MAJOR_VERSION = version(0x01);
			TCN_MINOR_VERSION = version(0x02);
			TCN_PATCH_VERSION = version(0x03);
			TCN_IS_DEV_VERSION = version(0x04);
			APR_MAJOR_VERSION = version(0x11);
			APR_MINOR_VERSION = version(0x12);
			APR_PATCH_VERSION = version(0x13);
			APR_IS_DEV_VERSION = version(0x14);

			APR_SIZEOF_VOIDP = size(1);
			APR_PATH_MAX = size(2);
			APRMAXHOSTLEN = size(3);
			APR_MAX_IOVEC_SIZE = size(4);
			APR_MAX_SECS_TO_LINGER = size(5);
			APR_MMAP_THRESHOLD = size(6);
			APR_MMAP_LIMIT = size(7);

			APR_HAVE_IPV6 = has(0);
			APR_HAS_SHARED_MEMORY = has(1);
			APR_HAS_THREADS = has(2);
			APR_HAS_SENDFILE = has(3);
			APR_HAS_MMAP = has(4);
			APR_HAS_FORK = has(5);
			APR_HAS_RANDOM = has(6);
			APR_HAS_OTHER_CHILD = has(7);
			APR_HAS_DSO = has(8);
			APR_HAS_SO_ACCEPTFILTER = has(9);
			APR_HAS_UNICODE_FS = has(10);
			APR_HAS_PROC_INVOKED = has(11);
			APR_HAS_USER = has(12);
			APR_HAS_LARGE_FILES = has(13);
			APR_HAS_XTHREAD_FILES = has(14);
			APR_HAS_OS_UUID = has(15);
			APR_IS_BIGENDIAN = has(16);
			APR_FILES_AS_SOCKETS = has(17);
			APR_CHARSET_EBCDIC = has(18);
			APR_TCP_NODELAY_INHERITED = has(19);
			APR_O_NONBLOCK_INHERITED = has(20);
			if (APR_MAJOR_VERSION < 1) {
				throw new UnsatisfiedLinkError("Unsupported APR Version ("
						+ aprVersionString() + ")");
			}
			if (!APR_HAS_THREADS) {
				throw new UnsatisfiedLinkError("Missing APR_HAS_THREADS");
			}
		}
		return initialize();
	}

	public static String[] getNAMES() {
		return NAMES;
	}

	public static void setNAMES(String[] nAMES) {
		NAMES = nAMES;
	}

	public static Library get_instance() {
		return _instance;
	}

	public static void set_instance(Library _instance) {
		Library._instance = _instance;
	}

	public static int getTCN_MAJOR_VERSION() {
		return TCN_MAJOR_VERSION;
	}

	public static void setTCN_MAJOR_VERSION(int tCN_MAJOR_VERSION) {
		TCN_MAJOR_VERSION = tCN_MAJOR_VERSION;
	}

	public static int getTCN_MINOR_VERSION() {
		return TCN_MINOR_VERSION;
	}

	public static void setTCN_MINOR_VERSION(int tCN_MINOR_VERSION) {
		TCN_MINOR_VERSION = tCN_MINOR_VERSION;
	}

	public static int getTCN_PATCH_VERSION() {
		return TCN_PATCH_VERSION;
	}

	public static void setTCN_PATCH_VERSION(int tCN_PATCH_VERSION) {
		TCN_PATCH_VERSION = tCN_PATCH_VERSION;
	}

	public static int getTCN_IS_DEV_VERSION() {
		return TCN_IS_DEV_VERSION;
	}

	public static void setTCN_IS_DEV_VERSION(int tCN_IS_DEV_VERSION) {
		TCN_IS_DEV_VERSION = tCN_IS_DEV_VERSION;
	}

	public static int getAPR_MAJOR_VERSION() {
		return APR_MAJOR_VERSION;
	}

	public static void setAPR_MAJOR_VERSION(int aPR_MAJOR_VERSION) {
		APR_MAJOR_VERSION = aPR_MAJOR_VERSION;
	}

	public static int getAPR_MINOR_VERSION() {
		return APR_MINOR_VERSION;
	}

	public static void setAPR_MINOR_VERSION(int aPR_MINOR_VERSION) {
		APR_MINOR_VERSION = aPR_MINOR_VERSION;
	}

	public static int getAPR_PATCH_VERSION() {
		return APR_PATCH_VERSION;
	}

	public static void setAPR_PATCH_VERSION(int aPR_PATCH_VERSION) {
		APR_PATCH_VERSION = aPR_PATCH_VERSION;
	}

	public static int getAPR_IS_DEV_VERSION() {
		return APR_IS_DEV_VERSION;
	}

	public static void setAPR_IS_DEV_VERSION(int aPR_IS_DEV_VERSION) {
		APR_IS_DEV_VERSION = aPR_IS_DEV_VERSION;
	}

	public static boolean isAPR_HAVE_IPV6() {
		return APR_HAVE_IPV6;
	}

	public static void setAPR_HAVE_IPV6(boolean aPR_HAVE_IPV6) {
		APR_HAVE_IPV6 = aPR_HAVE_IPV6;
	}

	public static boolean isAPR_HAS_SHARED_MEMORY() {
		return APR_HAS_SHARED_MEMORY;
	}

	public static void setAPR_HAS_SHARED_MEMORY(boolean aPR_HAS_SHARED_MEMORY) {
		APR_HAS_SHARED_MEMORY = aPR_HAS_SHARED_MEMORY;
	}

	public static boolean isAPR_HAS_THREADS() {
		return APR_HAS_THREADS;
	}

	public static void setAPR_HAS_THREADS(boolean aPR_HAS_THREADS) {
		APR_HAS_THREADS = aPR_HAS_THREADS;
	}

	public static boolean isAPR_HAS_SENDFILE() {
		return APR_HAS_SENDFILE;
	}

	public static void setAPR_HAS_SENDFILE(boolean aPR_HAS_SENDFILE) {
		APR_HAS_SENDFILE = aPR_HAS_SENDFILE;
	}

	public static boolean isAPR_HAS_MMAP() {
		return APR_HAS_MMAP;
	}

	public static void setAPR_HAS_MMAP(boolean aPR_HAS_MMAP) {
		APR_HAS_MMAP = aPR_HAS_MMAP;
	}

	public static boolean isAPR_HAS_FORK() {
		return APR_HAS_FORK;
	}

	public static void setAPR_HAS_FORK(boolean aPR_HAS_FORK) {
		APR_HAS_FORK = aPR_HAS_FORK;
	}

	public static boolean isAPR_HAS_RANDOM() {
		return APR_HAS_RANDOM;
	}

	public static void setAPR_HAS_RANDOM(boolean aPR_HAS_RANDOM) {
		APR_HAS_RANDOM = aPR_HAS_RANDOM;
	}

	public static boolean isAPR_HAS_OTHER_CHILD() {
		return APR_HAS_OTHER_CHILD;
	}

	public static void setAPR_HAS_OTHER_CHILD(boolean aPR_HAS_OTHER_CHILD) {
		APR_HAS_OTHER_CHILD = aPR_HAS_OTHER_CHILD;
	}

	public static boolean isAPR_HAS_DSO() {
		return APR_HAS_DSO;
	}

	public static void setAPR_HAS_DSO(boolean aPR_HAS_DSO) {
		APR_HAS_DSO = aPR_HAS_DSO;
	}

	public static boolean isAPR_HAS_SO_ACCEPTFILTER() {
		return APR_HAS_SO_ACCEPTFILTER;
	}

	public static void setAPR_HAS_SO_ACCEPTFILTER(
			boolean aPR_HAS_SO_ACCEPTFILTER) {
		APR_HAS_SO_ACCEPTFILTER = aPR_HAS_SO_ACCEPTFILTER;
	}

	public static boolean isAPR_HAS_UNICODE_FS() {
		return APR_HAS_UNICODE_FS;
	}

	public static void setAPR_HAS_UNICODE_FS(boolean aPR_HAS_UNICODE_FS) {
		APR_HAS_UNICODE_FS = aPR_HAS_UNICODE_FS;
	}

	public static boolean isAPR_HAS_PROC_INVOKED() {
		return APR_HAS_PROC_INVOKED;
	}

	public static void setAPR_HAS_PROC_INVOKED(boolean aPR_HAS_PROC_INVOKED) {
		APR_HAS_PROC_INVOKED = aPR_HAS_PROC_INVOKED;
	}

	public static boolean isAPR_HAS_USER() {
		return APR_HAS_USER;
	}

	public static void setAPR_HAS_USER(boolean aPR_HAS_USER) {
		APR_HAS_USER = aPR_HAS_USER;
	}

	public static boolean isAPR_HAS_LARGE_FILES() {
		return APR_HAS_LARGE_FILES;
	}

	public static void setAPR_HAS_LARGE_FILES(boolean aPR_HAS_LARGE_FILES) {
		APR_HAS_LARGE_FILES = aPR_HAS_LARGE_FILES;
	}

	public static boolean isAPR_HAS_XTHREAD_FILES() {
		return APR_HAS_XTHREAD_FILES;
	}

	public static void setAPR_HAS_XTHREAD_FILES(boolean aPR_HAS_XTHREAD_FILES) {
		APR_HAS_XTHREAD_FILES = aPR_HAS_XTHREAD_FILES;
	}

	public static boolean isAPR_HAS_OS_UUID() {
		return APR_HAS_OS_UUID;
	}

	public static void setAPR_HAS_OS_UUID(boolean aPR_HAS_OS_UUID) {
		APR_HAS_OS_UUID = aPR_HAS_OS_UUID;
	}

	public static boolean isAPR_IS_BIGENDIAN() {
		return APR_IS_BIGENDIAN;
	}

	public static void setAPR_IS_BIGENDIAN(boolean aPR_IS_BIGENDIAN) {
		APR_IS_BIGENDIAN = aPR_IS_BIGENDIAN;
	}

	public static boolean isAPR_FILES_AS_SOCKETS() {
		return APR_FILES_AS_SOCKETS;
	}

	public static void setAPR_FILES_AS_SOCKETS(boolean aPR_FILES_AS_SOCKETS) {
		APR_FILES_AS_SOCKETS = aPR_FILES_AS_SOCKETS;
	}

	public static boolean isAPR_CHARSET_EBCDIC() {
		return APR_CHARSET_EBCDIC;
	}

	public static void setAPR_CHARSET_EBCDIC(boolean aPR_CHARSET_EBCDIC) {
		APR_CHARSET_EBCDIC = aPR_CHARSET_EBCDIC;
	}

	public static boolean isAPR_TCP_NODELAY_INHERITED() {
		return APR_TCP_NODELAY_INHERITED;
	}

	public static void setAPR_TCP_NODELAY_INHERITED(
			boolean aPR_TCP_NODELAY_INHERITED) {
		APR_TCP_NODELAY_INHERITED = aPR_TCP_NODELAY_INHERITED;
	}

	public static boolean isAPR_O_NONBLOCK_INHERITED() {
		return APR_O_NONBLOCK_INHERITED;
	}

	public static void setAPR_O_NONBLOCK_INHERITED(
			boolean aPR_O_NONBLOCK_INHERITED) {
		APR_O_NONBLOCK_INHERITED = aPR_O_NONBLOCK_INHERITED;
	}

	public static int getAPR_SIZEOF_VOIDP() {
		return APR_SIZEOF_VOIDP;
	}

	public static void setAPR_SIZEOF_VOIDP(int aPR_SIZEOF_VOIDP) {
		APR_SIZEOF_VOIDP = aPR_SIZEOF_VOIDP;
	}

	public static int getAPR_PATH_MAX() {
		return APR_PATH_MAX;
	}

	public static void setAPR_PATH_MAX(int aPR_PATH_MAX) {
		APR_PATH_MAX = aPR_PATH_MAX;
	}

	public static int getAPRMAXHOSTLEN() {
		return APRMAXHOSTLEN;
	}

	public static void setAPRMAXHOSTLEN(int aPRMAXHOSTLEN) {
		APRMAXHOSTLEN = aPRMAXHOSTLEN;
	}

	public static int getAPR_MAX_IOVEC_SIZE() {
		return APR_MAX_IOVEC_SIZE;
	}

	public static void setAPR_MAX_IOVEC_SIZE(int aPR_MAX_IOVEC_SIZE) {
		APR_MAX_IOVEC_SIZE = aPR_MAX_IOVEC_SIZE;
	}

	public static int getAPR_MAX_SECS_TO_LINGER() {
		return APR_MAX_SECS_TO_LINGER;
	}

	public static void setAPR_MAX_SECS_TO_LINGER(int aPR_MAX_SECS_TO_LINGER) {
		APR_MAX_SECS_TO_LINGER = aPR_MAX_SECS_TO_LINGER;
	}

	public static int getAPR_MMAP_THRESHOLD() {
		return APR_MMAP_THRESHOLD;
	}

	public static void setAPR_MMAP_THRESHOLD(int aPR_MMAP_THRESHOLD) {
		APR_MMAP_THRESHOLD = aPR_MMAP_THRESHOLD;
	}

	public static int getAPR_MMAP_LIMIT() {
		return APR_MMAP_LIMIT;
	}

	public static void setAPR_MMAP_LIMIT(int aPR_MMAP_LIMIT) {
		APR_MMAP_LIMIT = aPR_MMAP_LIMIT;
	}
}
