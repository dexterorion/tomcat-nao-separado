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
 * SSL
 *
 * @author Mladen Turk
 */

public final class SSL {

	/*
	 * Type definitions mostly from mod_ssl
	 */
	private static final int UNSET = -1;
	/*
	 * Define the certificate algorithm types
	 */
	private static final int SSL_ALGO_UNKNOWN = 0;
	private static final int SSL_ALGO_RSA = (1 << 0);
	private static final int SSL_ALGO_DSA = (1 << 1);
	private static final int SSL_ALGO_ALL = (SSL_ALGO_RSA | SSL_ALGO_DSA);

	private static final int SSL_AIDX_RSA = 0;
	private static final int SSL_AIDX_DSA = 1;
	private static final int SSL_AIDX_MAX = 2;
	/*
	 * Define IDs for the temporary RSA keys and DH params
	 */

	private static final int SSL_TMP_KEY_RSA_512 = 0;
	private static final int SSL_TMP_KEY_RSA_1024 = 1;
	private static final int SSL_TMP_KEY_RSA_2048 = 2;
	private static final int SSL_TMP_KEY_RSA_4096 = 3;
	private static final int SSL_TMP_KEY_DH_512 = 4;
	private static final int SSL_TMP_KEY_DH_1024 = 5;
	private static final int SSL_TMP_KEY_DH_2048 = 6;
	private static final int SSL_TMP_KEY_DH_4096 = 7;
	private static final int SSL_TMP_KEY_MAX = 8;

	/*
	 * Define the SSL options
	 */
	private static final int SSL_OPT_NONE = 0;
	private static final int SSL_OPT_RELSET = (1 << 0);
	private static final int SSL_OPT_STDENVVARS = (1 << 1);
	private static final int SSL_OPT_EXPORTCERTDATA = (1 << 3);
	private static final int SSL_OPT_FAKEBASICAUTH = (1 << 4);
	private static final int SSL_OPT_STRICTREQUIRE = (1 << 5);
	private static final int SSL_OPT_OPTRENEGOTIATE = (1 << 6);
	private static final int SSL_OPT_ALL = (SSL_OPT_STDENVVARS
			| SSL_OPT_EXPORTCERTDATA | SSL_OPT_FAKEBASICAUTH
			| SSL_OPT_STRICTREQUIRE | SSL_OPT_OPTRENEGOTIATE);

	/*
	 * Define the SSL Protocol options
	 */
	private static final int SSL_PROTOCOL_NONE = 0;
	private static final int SSL_PROTOCOL_SSLV2 = (1 << 0);
	private static final int SSL_PROTOCOL_SSLV3 = (1 << 1);
	private static final int SSL_PROTOCOL_TLSV1 = (1 << 2);
	private static final int SSL_PROTOCOL_TLSV1_1 = (1 << 3);
	private static final int SSL_PROTOCOL_TLSV1_2 = (1 << 4);
	private static final int SSL_PROTOCOL_ALL = (SSL_PROTOCOL_TLSV1
			| SSL_PROTOCOL_TLSV1_1 | SSL_PROTOCOL_TLSV1_2);

	/*
	 * Define the SSL verify levels
	 */
	private static final int SSL_CVERIFY_UNSET = UNSET;
	private static final int SSL_CVERIFY_NONE = 0;
	private static final int SSL_CVERIFY_OPTIONAL = 1;
	private static final int SSL_CVERIFY_REQUIRE = 2;
	private static final int SSL_CVERIFY_OPTIONAL_NO_CA = 3;

	/*
	 * Use either SSL_VERIFY_NONE or SSL_VERIFY_PEER, the last 2 options are
	 * 'ored' with SSL_VERIFY_PEER if they are desired
	 */
	private static final int SSL_VERIFY_NONE = 0;
	private static final int SSL_VERIFY_PEER = 1;
	private static final int SSL_VERIFY_FAIL_IF_NO_PEER_CERT = 2;
	private static final int SSL_VERIFY_CLIENT_ONCE = 4;
	private static final int SSL_VERIFY_PEER_STRICT = (SSL_VERIFY_PEER | SSL_VERIFY_FAIL_IF_NO_PEER_CERT);

	private static final int SSL_OP_MICROSOFT_SESS_ID_BUG = 0x00000001;
	private static final int SSL_OP_NETSCAPE_CHALLENGE_BUG = 0x00000002;
	private static final int SSL_OP_NETSCAPE_REUSE_CIPHER_CHANGE_BUG = 0x00000008;
	private static final int SSL_OP_SSLREF2_REUSE_CERT_TYPE_BUG = 0x00000010;
	private static final int SSL_OP_MICROSOFT_BIG_SSLV3_BUFFER = 0x00000020;
	private static final int SSL_OP_MSIE_SSLV2_RSA_PADDING = 0x00000040;
	private static final int SSL_OP_SSLEAY_080_CLIENT_DH_BUG = 0x00000080;
	private static final int SSL_OP_TLS_D5_BUG = 0x00000100;
	private static final int SSL_OP_TLS_BLOCK_PADDING_BUG = 0x00000200;

	/*
	 * Disable SSL 3.0/TLS 1.0 CBC vulnerability workaround that was added in
	 * OpenSSL 0.9.6d. Usually (depending on the application protocol) the
	 * workaround is not needed. Unfortunately some broken SSL/TLS
	 * implementations cannot handle it at all, which is why we include it in
	 * SSL_OP_ALL.
	 */
	private static final int SSL_OP_DONT_INSERT_EMPTY_FRAGMENTS = 0x00000800;

	/*
	 * SSL_OP_ALL: various bug workarounds that should be rather harmless. This
	 * used to be 0x000FFFFFL before 0.9.7.
	 */
	private static final int SSL_OP_ALL = 0x00000FFF;
	/* As server, disallow session resumption on renegotiation */
	private static final int SSL_OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION = 0x00010000;
	/* Don't use compression even if supported */
	private static final int SSL_OP_NO_COMPRESSION = 0x00020000;
	/* Permit unsafe legacy renegotiation */
	private static final int SSL_OP_ALLOW_UNSAFE_LEGACY_RENEGOTIATION = 0x00040000;
	/* If set, always create a new key when using tmp_eddh parameters */
	private static final int SSL_OP_SINGLE_ECDH_USE = 0x00080000;
	/* If set, always create a new key when using tmp_dh parameters */
	private static final int SSL_OP_SINGLE_DH_USE = 0x00100000;
	/*
	 * Set to always use the tmp_rsa key when doing RSA operations, even when
	 * this violates protocol specs
	 */
	private static final int SSL_OP_EPHEMERAL_RSA = 0x00200000;
	/*
	 * Set on servers to choose the cipher according to the server's preferences
	 */
	private static final int SSL_OP_CIPHER_SERVER_PREFERENCE = 0x00400000;
	/*
	 * If set, a server will allow a client to issue a SSLv3.0 version number as
	 * latest version supported in the premaster secret, even when TLSv1.0
	 * (version 3.1) was announced in the client hello. Normally this is
	 * forbidden to prevent version rollback attacks.
	 */
	private static final int SSL_OP_TLS_ROLLBACK_BUG = 0x00800000;

	private static final int SSL_OP_NO_SSLv2 = 0x01000000;
	private static final int SSL_OP_NO_SSLv3 = 0x02000000;
	private static final int SSL_OP_NO_TLSv1 = 0x04000000;

	// SSL_OP_PKCS1_CHECK_1 and SSL_OP_PKCS1_CHECK_2 flags are unsupported
	// in the current version of OpenSSL library. See ssl.h changes in commit
	// 7409d7ad517650db332ae528915a570e4e0ab88b (30 Apr 2011) of OpenSSL.
	/**
	 * @deprecated Unsupported in the current version of OpenSSL
	 */
	@Deprecated
	private static final int SSL_OP_PKCS1_CHECK_1 = 0x08000000;
	/**
	 * @deprecated Unsupported in the current version of OpenSSL
	 */
	@Deprecated
	private static final int SSL_OP_PKCS1_CHECK_2 = 0x10000000;
	private static final int SSL_OP_NETSCAPE_CA_DN_BUG = 0x20000000;
	private static final int SSL_OP_NETSCAPE_DEMO_CIPHER_CHANGE_BUG = 0x40000000;

	private static final int SSL_CRT_FORMAT_UNDEF = 0;
	private static final int SSL_CRT_FORMAT_ASN1 = 1;
	private static final int SSL_CRT_FORMAT_TEXT = 2;
	private static final int SSL_CRT_FORMAT_PEM = 3;
	private static final int SSL_CRT_FORMAT_NETSCAPE = 4;
	private static final int SSL_CRT_FORMAT_PKCS12 = 5;
	private static final int SSL_CRT_FORMAT_SMIME = 6;
	private static final int SSL_CRT_FORMAT_ENGINE = 7;

	private static final int SSL_MODE_CLIENT = 0;
	private static final int SSL_MODE_SERVER = 1;
	private static final int SSL_MODE_COMBINED = 2;

	private static final int SSL_SHUTDOWN_TYPE_UNSET = 0;
	private static final int SSL_SHUTDOWN_TYPE_STANDARD = 1;
	private static final int SSL_SHUTDOWN_TYPE_UNCLEAN = 2;
	private static final int SSL_SHUTDOWN_TYPE_ACCURATE = 3;

	private static final int SSL_INFO_SESSION_ID = 0x0001;
	private static final int SSL_INFO_CIPHER = 0x0002;
	private static final int SSL_INFO_CIPHER_USEKEYSIZE = 0x0003;
	private static final int SSL_INFO_CIPHER_ALGKEYSIZE = 0x0004;
	private static final int SSL_INFO_CIPHER_VERSION = 0x0005;
	private static final int SSL_INFO_CIPHER_DESCRIPTION = 0x0006;
	private static final int SSL_INFO_PROTOCOL = 0x0007;

	/*
	 * To obtain the CountryName of the Client Certificate Issuer use the
	 * SSL_INFO_CLIENT_I_DN + SSL_INFO_DN_COUNTRYNAME
	 */
	private static final int SSL_INFO_CLIENT_S_DN = 0x0010;
	private static final int SSL_INFO_CLIENT_I_DN = 0x0020;
	private static final int SSL_INFO_SERVER_S_DN = 0x0040;
	private static final int SSL_INFO_SERVER_I_DN = 0x0080;

	private static final int SSL_INFO_DN_COUNTRYNAME = 0x0001;
	private static final int SSL_INFO_DN_STATEORPROVINCENAME = 0x0002;
	private static final int SSL_INFO_DN_LOCALITYNAME = 0x0003;
	private static final int SSL_INFO_DN_ORGANIZATIONNAME = 0x0004;
	private static final int SSL_INFO_DN_ORGANIZATIONALUNITNAME = 0x0005;
	private static final int SSL_INFO_DN_COMMONNAME = 0x0006;
	private static final int SSL_INFO_DN_TITLE = 0x0007;
	private static final int SSL_INFO_DN_INITIALS = 0x0008;
	private static final int SSL_INFO_DN_GIVENNAME = 0x0009;
	private static final int SSL_INFO_DN_SURNAME = 0x000A;
	private static final int SSL_INFO_DN_DESCRIPTION = 0x000B;
	private static final int SSL_INFO_DN_UNIQUEIDENTIFIER = 0x000C;
	private static final int SSL_INFO_DN_EMAILADDRESS = 0x000D;

	private static final int SSL_INFO_CLIENT_M_VERSION = 0x0101;
	private static final int SSL_INFO_CLIENT_M_SERIAL = 0x0102;
	private static final int SSL_INFO_CLIENT_V_START = 0x0103;
	private static final int SSL_INFO_CLIENT_V_END = 0x0104;
	private static final int SSL_INFO_CLIENT_A_SIG = 0x0105;
	private static final int SSL_INFO_CLIENT_A_KEY = 0x0106;
	private static final int SSL_INFO_CLIENT_CERT = 0x0107;
	private static final int SSL_INFO_CLIENT_V_REMAIN = 0x0108;

	private static final int SSL_INFO_SERVER_M_VERSION = 0x0201;
	private static final int SSL_INFO_SERVER_M_SERIAL = 0x0202;
	private static final int SSL_INFO_SERVER_V_START = 0x0203;
	private static final int SSL_INFO_SERVER_V_END = 0x0204;
	private static final int SSL_INFO_SERVER_A_SIG = 0x0205;
	private static final int SSL_INFO_SERVER_A_KEY = 0x0206;
	private static final int SSL_INFO_SERVER_CERT = 0x0207;
	/*
	 * Return client certificate chain. Add certificate chain number to that
	 * flag (0 ... verify depth)
	 */
	private static final int SSL_INFO_CLIENT_CERT_CHAIN = 0x0400;

	/* Return OpenSSL version number */
	private static native int version();

	/* Return OpenSSL version string */
	public static native String versionString();

	/**
	 * Initialize OpenSSL support. This function needs to be called once for the
	 * lifetime of JVM. Library.init() has to be called before.
	 * 
	 * @param engine
	 *            Support for external a Crypto Device ("engine"), usually a
	 *            hardware accelerator card for crypto operations.
	 * @return APR status code
	 */
	public static native int initialize(String engine);

	/**
	 * Get the status of FIPS Mode.
	 *
	 * @return FIPS_mode return code. It is <code>0</code> if OpenSSL is not in
	 *         FIPS mode, <code>1</code> if OpenSSL is in FIPS Mode.
	 * @throws Exception
	 *             If tcnative was not compiled with FIPS Mode available.
	 * @see <a href="http://wiki.openssl.org/index.php/FIPS_mode%28%29">OpenSSL
	 *      method FIPS_mode()</a>
	 */
	public static native int fipsModeGet() throws Exception;

	/**
	 * Enable/Disable FIPS Mode.
	 *
	 * @param mode
	 *            1 - enable, 0 - disable
	 *
	 * @return FIPS_mode_set return code
	 * @throws Exception
	 *             If tcnative was not compiled with FIPS Mode available, or if
	 *             {@code FIPS_mode_set()} call returned an error value.
	 * @see <a
	 *      href="http://wiki.openssl.org/index.php/FIPS_mode_set%28%29">OpenSSL
	 *      method FIPS_mode_set()</a>
	 */
	public static native int fipsModeSet(int mode) throws Exception;

	/**
	 * Add content of the file to the PRNG
	 * 
	 * @param filename
	 *            Filename containing random data. If null the default file will
	 *            be tested. The seed file is $RANDFILE if that environment
	 *            variable is set, $HOME/.rnd otherwise. In case both files are
	 *            unavailable builtin random seed generator is used.
	 */
	public static native boolean randLoad(String filename);

	/**
	 * Writes a number of random bytes (currently 1024) to file
	 * <code>filename</code> which can be used to initialize the PRNG by calling
	 * randLoad in a later session.
	 * 
	 * @param filename
	 *            Filename to save the data
	 */
	public static native boolean randSave(String filename);

	/**
	 * Creates random data to filename
	 * 
	 * @param filename
	 *            Filename to save the data
	 * @param len
	 *            The length of random sequence in bytes
	 * @param base64
	 *            Output the data in Base64 encoded format
	 */
	public static native boolean randMake(String filename, int len,
			boolean base64);

	/**
	 * Sets global random filename.
	 * 
	 * @param filename
	 *            Filename to use. If set it will be used for SSL initialization
	 *            and all contexts where explicitly not set.
	 */
	public static native void randSet(String filename);

	/**
	 * Initialize new BIO
	 * 
	 * @param pool
	 *            The pool to use.
	 * @param callback
	 *            BIOCallback to use
	 * @return New BIO handle
	 */
	public static native long newBIO(long pool, BIOCallback callback)
			throws Exception;

	/**
	 * Close BIO and dereference callback object
	 * 
	 * @param bio
	 *            BIO to close and destroy.
	 * @return APR Status code
	 */
	public static native int closeBIO(long bio);

	/**
	 * Set global Password callback for obtaining passwords.
	 * 
	 * @param callback
	 *            PasswordCallback implementation to use.
	 */
	public static native void setPasswordCallback(PasswordCallback callback);

	/**
	 * Set global Password for decrypting certificates and keys.
	 * 
	 * @param password
	 *            Password to use.
	 */
	public static native void setPassword(String password);

	/**
	 * Generate temporary RSA key. <br />
	 * Index can be one of:
	 * 
	 * <PRE>
	 * SSL_TMP_KEY_RSA_512
	 * SSL_TMP_KEY_RSA_1024
	 * SSL_TMP_KEY_RSA_2048
	 * SSL_TMP_KEY_RSA_4096
	 * </PRE>
	 * 
	 * By default 512 and 1024 keys are generated on startup. You can use a low
	 * priority thread to generate them on the fly.
	 * 
	 * @param idx
	 *            temporary key index.
	 */
	public static native boolean generateRSATempKey(int idx);

	/**
	 * Load temporary DSA key from file <br />
	 * Index can be one of:
	 * 
	 * <PRE>
	 * SSL_TMP_KEY_DH_512
	 * SSL_TMP_KEY_DH_1024
	 * SSL_TMP_KEY_DH_2048
	 * SSL_TMP_KEY_DH_4096
	 * </PRE>
	 * 
	 * @param idx
	 *            temporary key index.
	 * @param file
	 *            File containing DH params.
	 */
	public static native boolean loadDSATempKey(int idx, String file);

	/**
	 * Return last SSL error string
	 */
	public static native String getLastError();

	/**
	 * Return true if all the requested SSL_OP_* are supported by OpenSSL.
	 * 
	 * <i>Note that for versions of tcnative &lt; 1.1.25, this method will
	 * return <code>true</code> if and only if <code>op</code>=
	 * {@link #SSL_OP_ALLOW_UNSAFE_LEGACY_RENEGOTIATION} and tcnative supports
	 * that flag.</i>
	 *
	 * @param op
	 *            Bitwise-OR of all SSL_OP_* to test.
	 * 
	 * @return true if all SSL_OP_* are supported by OpenSSL library.
	 */
	public static native boolean hasOp(int op);

	public static int getUnset() {
		return UNSET;
	}

	public static int getSslAlgoUnknown() {
		return SSL_ALGO_UNKNOWN;
	}

	public static int getSslAlgoRsa() {
		return SSL_ALGO_RSA;
	}

	public static int getSslAlgoDsa() {
		return SSL_ALGO_DSA;
	}

	public static int getSslAlgoAll() {
		return SSL_ALGO_ALL;
	}

	public static int getSslAidxRsa() {
		return SSL_AIDX_RSA;
	}

	public static int getSslAidxDsa() {
		return SSL_AIDX_DSA;
	}

	public static int getSslAidxMax() {
		return SSL_AIDX_MAX;
	}

	public static int getSslTmpKeyRsa512() {
		return SSL_TMP_KEY_RSA_512;
	}

	public static int getSslTmpKeyRsa1024() {
		return SSL_TMP_KEY_RSA_1024;
	}

	public static int getSslTmpKeyRsa2048() {
		return SSL_TMP_KEY_RSA_2048;
	}

	public static int getSslTmpKeyRsa4096() {
		return SSL_TMP_KEY_RSA_4096;
	}

	public static int getSslTmpKeyDh512() {
		return SSL_TMP_KEY_DH_512;
	}

	public static int getSslTmpKeyDh1024() {
		return SSL_TMP_KEY_DH_1024;
	}

	public static int getSslTmpKeyDh2048() {
		return SSL_TMP_KEY_DH_2048;
	}

	public static int getSslTmpKeyDh4096() {
		return SSL_TMP_KEY_DH_4096;
	}

	public static int getSslTmpKeyMax() {
		return SSL_TMP_KEY_MAX;
	}

	public static int getSslOptNone() {
		return SSL_OPT_NONE;
	}

	public static int getSslOptRelset() {
		return SSL_OPT_RELSET;
	}

	public static int getSslOptStdenvvars() {
		return SSL_OPT_STDENVVARS;
	}

	public static int getSslOptExportcertdata() {
		return SSL_OPT_EXPORTCERTDATA;
	}

	public static int getSslOptFakebasicauth() {
		return SSL_OPT_FAKEBASICAUTH;
	}

	public static int getSslOptStrictrequire() {
		return SSL_OPT_STRICTREQUIRE;
	}

	public static int getSslOptOptrenegotiate() {
		return SSL_OPT_OPTRENEGOTIATE;
	}

	public static int getSslOptAll() {
		return SSL_OPT_ALL;
	}

	public static int getSslProtocolNone() {
		return SSL_PROTOCOL_NONE;
	}

	public static int getSslProtocolSslv2() {
		return SSL_PROTOCOL_SSLV2;
	}

	public static int getSslProtocolSslv3() {
		return SSL_PROTOCOL_SSLV3;
	}

	public static int getSslProtocolTlsv1() {
		return SSL_PROTOCOL_TLSV1;
	}

	public static int getSslProtocolTlsv11() {
		return SSL_PROTOCOL_TLSV1_1;
	}

	public static int getSslProtocolTlsv12() {
		return SSL_PROTOCOL_TLSV1_2;
	}

	public static int getSslProtocolAll() {
		return SSL_PROTOCOL_ALL;
	}

	public static int getSslCverifyUnset() {
		return SSL_CVERIFY_UNSET;
	}

	public static int getSslCverifyNone() {
		return SSL_CVERIFY_NONE;
	}

	public static int getSslCverifyOptional() {
		return SSL_CVERIFY_OPTIONAL;
	}

	public static int getSslCverifyRequire() {
		return SSL_CVERIFY_REQUIRE;
	}

	public static int getSslCverifyOptionalNoCa() {
		return SSL_CVERIFY_OPTIONAL_NO_CA;
	}

	public static int getSslVerifyNone() {
		return SSL_VERIFY_NONE;
	}

	public static int getSslVerifyPeer() {
		return SSL_VERIFY_PEER;
	}

	public static int getSslVerifyFailIfNoPeerCert() {
		return SSL_VERIFY_FAIL_IF_NO_PEER_CERT;
	}

	public static int getSslVerifyClientOnce() {
		return SSL_VERIFY_CLIENT_ONCE;
	}

	public static int getSslVerifyPeerStrict() {
		return SSL_VERIFY_PEER_STRICT;
	}

	public static int getSslOpMicrosoftSessIdBug() {
		return SSL_OP_MICROSOFT_SESS_ID_BUG;
	}

	public static int getSslOpNetscapeChallengeBug() {
		return SSL_OP_NETSCAPE_CHALLENGE_BUG;
	}

	public static int getSslOpNetscapeReuseCipherChangeBug() {
		return SSL_OP_NETSCAPE_REUSE_CIPHER_CHANGE_BUG;
	}

	public static int getSslOpSslref2ReuseCertTypeBug() {
		return SSL_OP_SSLREF2_REUSE_CERT_TYPE_BUG;
	}

	public static int getSslOpMicrosoftBigSslv3Buffer() {
		return SSL_OP_MICROSOFT_BIG_SSLV3_BUFFER;
	}

	public static int getSslOpMsieSslv2RsaPadding() {
		return SSL_OP_MSIE_SSLV2_RSA_PADDING;
	}

	public static int getSslOpSsleay080ClientDhBug() {
		return SSL_OP_SSLEAY_080_CLIENT_DH_BUG;
	}

	public static int getSslOpTlsD5Bug() {
		return SSL_OP_TLS_D5_BUG;
	}

	public static int getSslOpTlsBlockPaddingBug() {
		return SSL_OP_TLS_BLOCK_PADDING_BUG;
	}

	public static int getSslOpDontInsertEmptyFragments() {
		return SSL_OP_DONT_INSERT_EMPTY_FRAGMENTS;
	}

	public static int getSslOpAll() {
		return SSL_OP_ALL;
	}

	public static int getSslOpNoSessionResumptionOnRenegotiation() {
		return SSL_OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION;
	}

	public static int getSslOpNoCompression() {
		return SSL_OP_NO_COMPRESSION;
	}

	public static int getSslOpAllowUnsafeLegacyRenegotiation() {
		return SSL_OP_ALLOW_UNSAFE_LEGACY_RENEGOTIATION;
	}

	public static int getSslOpSingleEcdhUse() {
		return SSL_OP_SINGLE_ECDH_USE;
	}

	public static int getSslOpSingleDhUse() {
		return SSL_OP_SINGLE_DH_USE;
	}

	public static int getSslOpEphemeralRsa() {
		return SSL_OP_EPHEMERAL_RSA;
	}

	public static int getSslOpCipherServerPreference() {
		return SSL_OP_CIPHER_SERVER_PREFERENCE;
	}

	public static int getSslOpTlsRollbackBug() {
		return SSL_OP_TLS_ROLLBACK_BUG;
	}

	public static int getSslOpNoSslv2() {
		return SSL_OP_NO_SSLv2;
	}

	public static int getSslOpNoSslv3() {
		return SSL_OP_NO_SSLv3;
	}

	public static int getSslOpNoTlsv1() {
		return SSL_OP_NO_TLSv1;
	}

	public static int getSslOpPkcs1Check1() {
		return SSL_OP_PKCS1_CHECK_1;
	}

	public static int getSslOpPkcs1Check2() {
		return SSL_OP_PKCS1_CHECK_2;
	}

	public static int getSslOpNetscapeCaDnBug() {
		return SSL_OP_NETSCAPE_CA_DN_BUG;
	}

	public static int getSslOpNetscapeDemoCipherChangeBug() {
		return SSL_OP_NETSCAPE_DEMO_CIPHER_CHANGE_BUG;
	}

	public static int getSslCrtFormatUndef() {
		return SSL_CRT_FORMAT_UNDEF;
	}

	public static int getSslCrtFormatAsn1() {
		return SSL_CRT_FORMAT_ASN1;
	}

	public static int getSslCrtFormatText() {
		return SSL_CRT_FORMAT_TEXT;
	}

	public static int getSslCrtFormatPem() {
		return SSL_CRT_FORMAT_PEM;
	}

	public static int getSslCrtFormatNetscape() {
		return SSL_CRT_FORMAT_NETSCAPE;
	}

	public static int getSslCrtFormatPkcs12() {
		return SSL_CRT_FORMAT_PKCS12;
	}

	public static int getSslCrtFormatSmime() {
		return SSL_CRT_FORMAT_SMIME;
	}

	public static int getSslCrtFormatEngine() {
		return SSL_CRT_FORMAT_ENGINE;
	}

	public static int getSslModeClient() {
		return SSL_MODE_CLIENT;
	}

	public static int getSslModeServer() {
		return SSL_MODE_SERVER;
	}

	public static int getSslModeCombined() {
		return SSL_MODE_COMBINED;
	}

	public static int getSslShutdownTypeUnset() {
		return SSL_SHUTDOWN_TYPE_UNSET;
	}

	public static int getSslShutdownTypeStandard() {
		return SSL_SHUTDOWN_TYPE_STANDARD;
	}

	public static int getSslShutdownTypeUnclean() {
		return SSL_SHUTDOWN_TYPE_UNCLEAN;
	}

	public static int getSslShutdownTypeAccurate() {
		return SSL_SHUTDOWN_TYPE_ACCURATE;
	}

	public static int getSslInfoSessionId() {
		return SSL_INFO_SESSION_ID;
	}

	public static int getSslInfoCipher() {
		return SSL_INFO_CIPHER;
	}

	public static int getSslInfoCipherUsekeysize() {
		return SSL_INFO_CIPHER_USEKEYSIZE;
	}

	public static int getSslInfoCipherAlgkeysize() {
		return SSL_INFO_CIPHER_ALGKEYSIZE;
	}

	public static int getSslInfoCipherVersion() {
		return SSL_INFO_CIPHER_VERSION;
	}

	public static int getSslInfoCipherDescription() {
		return SSL_INFO_CIPHER_DESCRIPTION;
	}

	public static int getSslInfoProtocol() {
		return SSL_INFO_PROTOCOL;
	}

	public static int getSslInfoClientSDn() {
		return SSL_INFO_CLIENT_S_DN;
	}

	public static int getSslInfoClientIDn() {
		return SSL_INFO_CLIENT_I_DN;
	}

	public static int getSslInfoServerSDn() {
		return SSL_INFO_SERVER_S_DN;
	}

	public static int getSslInfoServerIDn() {
		return SSL_INFO_SERVER_I_DN;
	}

	public static int getSslInfoDnCountryname() {
		return SSL_INFO_DN_COUNTRYNAME;
	}

	public static int getSslInfoDnStateorprovincename() {
		return SSL_INFO_DN_STATEORPROVINCENAME;
	}

	public static int getSslInfoDnLocalityname() {
		return SSL_INFO_DN_LOCALITYNAME;
	}

	public static int getSslInfoDnOrganizationname() {
		return SSL_INFO_DN_ORGANIZATIONNAME;
	}

	public static int getSslInfoDnOrganizationalunitname() {
		return SSL_INFO_DN_ORGANIZATIONALUNITNAME;
	}

	public static int getSslInfoDnCommonname() {
		return SSL_INFO_DN_COMMONNAME;
	}

	public static int getSslInfoDnTitle() {
		return SSL_INFO_DN_TITLE;
	}

	public static int getSslInfoDnInitials() {
		return SSL_INFO_DN_INITIALS;
	}

	public static int getSslInfoDnGivenname() {
		return SSL_INFO_DN_GIVENNAME;
	}

	public static int getSslInfoDnSurname() {
		return SSL_INFO_DN_SURNAME;
	}

	public static int getSslInfoDnDescription() {
		return SSL_INFO_DN_DESCRIPTION;
	}

	public static int getSslInfoDnUniqueidentifier() {
		return SSL_INFO_DN_UNIQUEIDENTIFIER;
	}

	public static int getSslInfoDnEmailaddress() {
		return SSL_INFO_DN_EMAILADDRESS;
	}

	public static int getSslInfoClientMVersion() {
		return SSL_INFO_CLIENT_M_VERSION;
	}

	public static int getSslInfoClientMSerial() {
		return SSL_INFO_CLIENT_M_SERIAL;
	}

	public static int getSslInfoClientVStart() {
		return SSL_INFO_CLIENT_V_START;
	}

	public static int getSslInfoClientVEnd() {
		return SSL_INFO_CLIENT_V_END;
	}

	public static int getSslInfoClientASig() {
		return SSL_INFO_CLIENT_A_SIG;
	}

	public static int getSslInfoClientAKey() {
		return SSL_INFO_CLIENT_A_KEY;
	}

	public static int getSslInfoClientCert() {
		return SSL_INFO_CLIENT_CERT;
	}

	public static int getSslInfoClientVRemain() {
		return SSL_INFO_CLIENT_V_REMAIN;
	}

	public static int getSslInfoServerMVersion() {
		return SSL_INFO_SERVER_M_VERSION;
	}

	public static int getSslInfoServerMSerial() {
		return SSL_INFO_SERVER_M_SERIAL;
	}

	public static int getSslInfoServerVStart() {
		return SSL_INFO_SERVER_V_START;
	}

	public static int getSslInfoServerVEnd() {
		return SSL_INFO_SERVER_V_END;
	}

	public static int getSslInfoServerASig() {
		return SSL_INFO_SERVER_A_SIG;
	}

	public static int getSslInfoServerAKey() {
		return SSL_INFO_SERVER_A_KEY;
	}

	public static int getSslInfoServerCert() {
		return SSL_INFO_SERVER_CERT;
	}

	public static int getSslInfoClientCertChain() {
		return SSL_INFO_CLIENT_CERT_CHAIN;
	}

}
