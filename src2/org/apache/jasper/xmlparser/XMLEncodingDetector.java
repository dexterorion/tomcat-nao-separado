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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.jasper.xmlparser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.jar.JarFile;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.compiler.ErrorDispatcher;
import org.apache.jasper.compiler.JspUtil;
import org.apache.jasper.compiler.Localizer;

public class XMLEncodingDetector {

	private InputStream stream;
	private String encoding;
	private boolean isEncodingSetInProlog;
	private boolean isBomPresent;
	private int skip;
	private Boolean isBigEndian;
	private Reader reader;

	// org.apache.xerces.impl.XMLEntityManager fields
	private static final int DEFAULT_BUFFER_SIZE = 2048;
	private static final int DEFAULT_XMLDECL_BUFFER_SIZE = 64;
	private boolean fAllowJavaEncodings;
	private SymbolTable fSymbolTable;
	private XMLEncodingDetector fCurrentEntity;
	private int fBufferSize = DEFAULT_BUFFER_SIZE;

	// org.apache.xerces.impl.XMLEntityManager.ScannedEntity fields
	private int lineNumber = 1;
	private int columnNumber = 1;
	private boolean literal;
	private char[] ch = new char[DEFAULT_BUFFER_SIZE];
	private int position;
	private int count;
	private boolean mayReadChunks = false;

	// org.apache.xerces.impl.XMLScanner fields
	private XMLString fString = new XMLString();
	private XMLStringBuffer fStringBuffer = new XMLStringBuffer();
	private XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();
	private static final String fVersionSymbol = "version";
	private static final String fEncodingSymbol = "encoding";
	private static final String fStandaloneSymbol = "standalone";

	// org.apache.xerces.impl.XMLDocumentFragmentScannerImpl fields
	private int fMarkupDepth = 0;
	private String[] fStrings = new String[3];

	private ErrorDispatcher err;

	/**
	 * Constructor
	 */
	public XMLEncodingDetector() {
		setfSymbolTableData(new SymbolTable());
		setfCurrentEntityData(this);
	}

	/**
	 * Autodetects the encoding of the XML document supplied by the given input
	 * stream.
	 *
	 * Encoding autodetection is done according to the XML 1.0 specification,
	 * Appendix F.1: Detection Without External Encoding Information.
	 *
	 * @return Two-element array, where the first element (of type
	 *         java.lang.String) contains the name of the (auto)detected
	 *         encoding, and the second element (of type java.lang.Boolean)
	 *         specifies whether the encoding was specified using the 'encoding'
	 *         attribute of an XML prolog (TRUE) or autodetected (FALSE).
	 */
	public static Object[] getEncoding(String fname, JarFile jarFile,
			JspCompilationContext ctxt, ErrorDispatcher err)
			throws IOException, JasperException {
		InputStream inStream = JspUtil
				.getInputStream(fname, jarFile, ctxt, err);
		XMLEncodingDetector detector = new XMLEncodingDetector();
		Object[] ret = detector.getEncoding(inStream, err);
		inStream.close();

		return ret;
	}

	private Object[] getEncoding(InputStream in, ErrorDispatcher err)
			throws IOException, JasperException {
		this.setStreamData(in);
		this.setErrData(err);
		createInitialReader();
		scanXMLDecl();

		return new Object[] { this.getEncodingData(),
				Boolean.valueOf(this.isEncodingSetInPrologData()),
				Boolean.valueOf(this.isBomPresentData()), Integer.valueOf(this.getSkipData()) };
	}

	// stub method
	public void endEntity() {
	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.startEntity()
	private void createInitialReader() throws IOException, JasperException {

		// wrap this stream in XMLEncodingDetectorRewindableInputStream
		setStreamData(new XMLEncodingDetectorRewindableInputStream(this, getStreamData()));

		// perform auto-detect of encoding if necessary
		if (getEncodingData() == null) {
			// read first four bytes and determine encoding
			final byte[] b4 = new byte[4];
			int count = 0;
			for (; count < 4; count++) {
				b4[count] = (byte) getStreamData().read();
			}
			if (count == 4) {
				Object[] encodingDesc = getEncodingName(b4, count);
				setEncodingData((String) (encodingDesc[0]));
				setIsBigEndianData((Boolean) (encodingDesc[1]));

				if (encodingDesc.length > 3) {
					setBomPresentData(((Boolean) (encodingDesc[2])).booleanValue());
					setSkipData(((Integer) (encodingDesc[3])).intValue());
				} else {
					setBomPresentData(true);
					setSkipData(((Integer) (encodingDesc[2])).intValue());
				}

				getStreamData().reset();
				// Special case UTF-8 files with BOM created by Microsoft
				// tools. It's more efficient to consume the BOM than make
				// the reader perform extra checks. -Ac
				if (count > 2 && getEncodingData().equals("UTF-8")) {
					int b0 = b4[0] & 0xFF;
					int b1 = b4[1] & 0xFF;
					int b2 = b4[2] & 0xFF;
					if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
						// ignore first three bytes...
						long skipped = getStreamData().skip(3);
						if (skipped != 3) {
							throw new IOException(
									Localizer
											.getMessage("xmlParser.skipBomFail"));
						}
					}
				}
				setReaderData(createReader(getStreamData(), getEncodingData(), getIsBigEndianData()));
			} else {
				setReaderData(createReader(getStreamData(), getEncodingData(), getIsBigEndianData()));
			}
		}
	}

	/**
	 * Creates a reader capable of reading the given input stream in the
	 * specified encoding.
	 *
	 * @param inputStream
	 *            The input stream.
	 * @param encoding
	 *            The encoding name that the input stream is encoded using. If
	 *            the user has specified that Java encoding names are allowed,
	 *            then the encoding name may be a Java encoding name; otherwise,
	 *            it is an ianaEncoding name.
	 * @param isBigEndian
	 *            For encodings (like uCS-4), whose names cannot specify a byte
	 *            order, this tells whether the order is bigEndian. null means
	 *            unknown or not relevant.
	 *
	 * @return Returns a reader.
	 */
	private Reader createReader(InputStream inputStream, String encoding,
			Boolean isBigEndian) throws IOException, JasperException {

		// normalize encoding name
		if (encoding == null) {
			encoding = "UTF-8";
		}

		// try to use an optimized reader
		String ENCODING = encoding.toUpperCase(Locale.ENGLISH);
		if (ENCODING.equals("UTF-8")) {
			return new UTF8Reader(inputStream, fBufferSize);
		}
		if (ENCODING.equals("US-ASCII")) {
			return new ASCIIReader(inputStream, fBufferSize);
		}
		if (ENCODING.equals("ISO-10646-UCS-4")) {
			if (isBigEndian != null) {
				boolean isBE = isBigEndian.booleanValue();
				if (isBE) {
					return new UCSReader(inputStream, UCSReader.getUcs4be());
				} else {
					return new UCSReader(inputStream, UCSReader.getUcs4le());
				}
			} else {
				getErrData().jspError("jsp.error.xml.encodingByteOrderUnsupported",
						encoding);
			}
		}
		if (ENCODING.equals("ISO-10646-UCS-2")) {
			if (isBigEndian != null) { // sould never happen with this
										// encoding...
				boolean isBE = isBigEndian.booleanValue();
				if (isBE) {
					return new UCSReader(inputStream, UCSReader.getUcs2be());
				} else {
					return new UCSReader(inputStream, UCSReader.getUcs2le());
				}
			} else {
				getErrData().jspError("jsp.error.xml.encodingByteOrderUnsupported",
						encoding);
			}
		}

		// check for valid name
		boolean validIANA = XMLChar.isValidIANAEncoding(encoding);
		boolean validJava = XMLChar.isValidJavaEncoding(encoding);
		if (!validIANA || (isfAllowJavaEncodingsData() && !validJava)) {
			getErrData().jspError("jsp.error.xml.encodingDeclInvalid", encoding);
			// NOTE: AndyH suggested that, on failure, we use ISO Latin 1
			// because every byte is a valid ISO Latin 1 character.
			// It may not translate correctly but if we failed on
			// the encoding anyway, then we're expecting the content
			// of the document to be bad. This will just prevent an
			// invalid UTF-8 sequence to be detected. This is only
			// important when continue-after-fatal-error is turned
			// on. -Ac
			encoding = "ISO-8859-1";
		}

		// try to use a Java reader
		String javaEncoding = EncodingMap.getIANA2JavaMapping(ENCODING);
		if (javaEncoding == null) {
			if (isfAllowJavaEncodingsData()) {
				javaEncoding = encoding;
			} else {
				getErrData().jspError("jsp.error.xml.encodingDeclInvalid", encoding);
				// see comment above.
				javaEncoding = "ISO8859_1";
			}
		}
		return new InputStreamReader(inputStream, javaEncoding);

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.getEncodingName
	/**
	 * Returns the IANA encoding name that is auto-detected from the bytes
	 * specified, with the endian-ness of that encoding where appropriate.
	 *
	 * @param b4
	 *            The first four bytes of the input.
	 * @param count
	 *            The number of bytes actually read.
	 * @return a 2-element array: the first element, an IANA-encoding string,
	 *         the second element a Boolean which is true iff the document is
	 *         big endian, false if it's little-endian, and null if the
	 *         distinction isn't relevant.
	 */
	private Object[] getEncodingName(byte[] b4, int count) {

		if (count < 2) {
			return new Object[] { "UTF-8", null, Boolean.FALSE,
					Integer.valueOf(0) };
		}

		// UTF-16, with BOM
		int b0 = b4[0] & 0xFF;
		int b1 = b4[1] & 0xFF;
		if (b0 == 0xFE && b1 == 0xFF) {
			// UTF-16, big-endian
			return new Object[] { "UTF-16BE", Boolean.TRUE, Integer.valueOf(2) };
		}
		if (b0 == 0xFF && b1 == 0xFE) {
			// UTF-16, little-endian
			return new Object[] { "UTF-16LE", Boolean.FALSE, Integer.valueOf(2) };
		}

		// default to UTF-8 if we don't have enough bytes to make a
		// good determination of the encoding
		if (count < 3) {
			return new Object[] { "UTF-8", null, Boolean.FALSE,
					Integer.valueOf(0) };
		}

		// UTF-8 with a BOM
		int b2 = b4[2] & 0xFF;
		if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
			return new Object[] { "UTF-8", null, Integer.valueOf(3) };
		}

		// default to UTF-8 if we don't have enough bytes to make a
		// good determination of the encoding
		if (count < 4) {
			return new Object[] { "UTF-8", null, Integer.valueOf(0) };
		}

		// other encodings
		int b3 = b4[3] & 0xFF;
		if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x3C) {
			// UCS-4, big endian (1234)
			return new Object[] { "ISO-10646-UCS-4", Boolean.TRUE,
					Integer.valueOf(4) };
		}
		if (b0 == 0x3C && b1 == 0x00 && b2 == 0x00 && b3 == 0x00) {
			// UCS-4, little endian (4321)
			return new Object[] { "ISO-10646-UCS-4", Boolean.FALSE,
					Integer.valueOf(4) };
		}
		if (b0 == 0x00 && b1 == 0x00 && b2 == 0x3C && b3 == 0x00) {
			// UCS-4, unusual octet order (2143)
			// REVISIT: What should this be?
			return new Object[] { "ISO-10646-UCS-4", null, Integer.valueOf(4) };
		}
		if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x00) {
			// UCS-4, unusual octect order (3412)
			// REVISIT: What should this be?
			return new Object[] { "ISO-10646-UCS-4", null, Integer.valueOf(4) };
		}
		if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
			// UTF-16, big-endian, no BOM
			// (or could turn out to be UCS-2...
			// REVISIT: What should this be?
			return new Object[] { "UTF-16BE", Boolean.TRUE, Integer.valueOf(4) };
		}
		if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
			// UTF-16, little-endian, no BOM
			// (or could turn out to be UCS-2...
			return new Object[] { "UTF-16LE", Boolean.FALSE, Integer.valueOf(4) };
		}
		if (b0 == 0x4C && b1 == 0x6F && b2 == 0xA7 && b3 == 0x94) {
			// EBCDIC
			// a la xerces1, return CP037 instead of EBCDIC here
			return new Object[] { "CP037", null, Integer.valueOf(4) };
		}

		// default encoding
		return new Object[] { "UTF-8", null, Boolean.FALSE, Integer.valueOf(0) };

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.isExternal
	/** Returns true if the current entity being scanned is external. */
	public boolean isExternal() {
		return true;
	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.peekChar
	/**
	 * Returns the next character on the input.
	 * <p>
	 * <strong>Note:</strong> The character is <em>not</em> consumed.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 */
	public int peekChar() throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		}

		// peek at character
		int c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];

		// return peeked character
		if (getfCurrentEntityData().isExternal()) {
			return c != '\r' ? c : '\n';
		} else {
			return c;
		}

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.scanChar
	/**
	 * Returns the next character on the input.
	 * <p>
	 * <strong>Note:</strong> The character is consumed.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 */
	public int scanChar() throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		}

		// scan character
		int c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
		getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1); 
		boolean external = false;
		if (c == '\n'
				|| (c == '\r' && (external = getfCurrentEntityData().isExternal()))) {
			getfCurrentEntityData().setLineNumberData(getfCurrentEntityData().getLineNumberData() + 1);
			getfCurrentEntityData().setColumnNumberData(1);
			if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
				getfCurrentEntityData().getChData()[0] = (char) c;
				load(1, false);
			}
			if (c == '\r' && external) {
				if (getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()] != '\n') {
//					getfCurrentEntityData().position = getfCurrentEntityData().position - 1;
				}
				else{
					getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
				}
				c = '\n';
			}
		}

		// return character that was scanned
		getfCurrentEntityData().setColumnNumberData(getfCurrentEntityData().getColumnNumberData() + 1);
		return c;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.scanName
	/**
	 * Returns a string matching the Name production appearing immediately on
	 * the input as a symbol, or null if no Name string is present.
	 * <p>
	 * <strong>Note:</strong> The Name characters are consumed.
	 * <p>
	 * <strong>Note:</strong> The string returned must be a symbol. The
	 * SymbolTable can be used for this purpose.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 *
	 * @see SymbolTable
	 * @see XMLChar#isName
	 * @see XMLChar#isNameStart
	 */
	public String scanName() throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		}

		// scan name
		int offset = getfCurrentEntityData().getPositionData();
		if (XMLChar.isNameStart(getfCurrentEntityData().getChData()[offset])) {
			getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
			if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
				getfCurrentEntityData().getChData()[0] = getfCurrentEntityData().getChData()[offset];
				offset = 0;
				if (load(1, false)) {
					getfCurrentEntityData().setColumnNumberData(
							getfCurrentEntityData().getColumnNumberData() + 1);
					String symbol = getfSymbolTableData().addSymbol(getfCurrentEntityData().getChData(),
							0, 1);
					return symbol;
				}
			}
			while (XMLChar.isName(getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()])) {
				getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
				if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
					int length = getfCurrentEntityData().getPositionData() - offset;
					if (length == fBufferSize) {
						// bad luck we have to resize our buffer
						char[] tmp = new char[fBufferSize * 2];
						System.arraycopy(getfCurrentEntityData().getChData(), offset, tmp, 0,
								length);
						getfCurrentEntityData().setChData(tmp);
						fBufferSize *= 2;
					} else {
						System.arraycopy(getfCurrentEntityData().getChData(), offset,
								getfCurrentEntityData().getChData(), 0, length);
					}
					offset = 0;
					if (load(length, false)) {
						break;
					}
				}
			}
		}
		int length = getfCurrentEntityData().getPositionData() - offset;
		getfCurrentEntityData()
				.setColumnNumberData(getfCurrentEntityData().getColumnNumberData() + length);

		// return name
		String symbol = null;
		if (length > 0) {
			symbol = getfSymbolTableData().addSymbol(getfCurrentEntityData().getChData(), offset, length);
		}
		return symbol;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.scanLiteral
	/**
	 * Scans a range of attribute value data, setting the fields of the
	 * XMLString structure, appropriately.
	 * <p>
	 * <strong>Note:</strong> The characters are consumed.
	 * <p>
	 * <strong>Note:</strong> This method does not guarantee to return the
	 * longest run of attribute value data. This method may return before the
	 * quote character due to reaching the end of the input buffer or any other
	 * reason.
	 * <p>
	 * <strong>Note:</strong> The fields contained in the XMLString structure
	 * are not guaranteed to remain valid upon subsequent calls to the entity
	 * scanner. Therefore, the caller is responsible for immediately using the
	 * returned character data or making a copy of the character data.
	 *
	 * @param quote
	 *            The quote character that signifies the end of the attribute
	 *            value data.
	 * @param content
	 *            The content structure to fill.
	 *
	 * @return Returns the next character on the input, if known. This value may
	 *         be -1 but this does <em>note</em> designate end of file.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 */
	public int scanLiteral(int quote, XMLString content) throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		} else if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData() - 1) {
			getfCurrentEntityData().getChData()[0] = getfCurrentEntityData().getChData()[getfCurrentEntityData().getCountData() - 1];
			load(1, false);
			getfCurrentEntityData().setPositionData(0);
		}

		// normalize newlines
		int offset = getfCurrentEntityData().getPositionData();
		int c = getfCurrentEntityData().getChData()[offset];
		int newlines = 0;
		boolean external = getfCurrentEntityData().isExternal();
		if (c == '\n' || (c == '\r' && external)) {
			do {
				c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
				getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
				if (c == '\r' && external) {
					newlines++;
					getfCurrentEntityData().setLineNumberData(
							getfCurrentEntityData().getLineNumberData() + 1);
					getfCurrentEntityData().setColumnNumberData(1);
					if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
						offset = 0;
						getfCurrentEntityData().setPositionData(newlines);
						if (load(newlines, false)) {
							break;
						}
					}
					if (getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()] == '\n') {
						getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
						offset++;
					}
					/*** NEWLINE NORMALIZATION ***/
					else {
						newlines++;
					}
					/***/
				} else if (c == '\n') {
					newlines++;
					getfCurrentEntityData().setLineNumberData(
							getfCurrentEntityData().getLineNumberData() + 1);
					getfCurrentEntityData().setColumnNumberData(1);
					if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
						offset = 0;
						getfCurrentEntityData().setPositionData(newlines);
						if (load(newlines, false)) {
							break;
						}
					}
				} else {
					getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() - 1);
					break;
				}
			} while (getfCurrentEntityData().getPositionData() < getfCurrentEntityData().getCountData() - 1);
			for (int i = offset; i < getfCurrentEntityData().getPositionData(); i++) {
				getfCurrentEntityData().getChData()[i] = '\n';
			}
			int length = getfCurrentEntityData().getPositionData() - offset;
			if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData() - 1) {
				content.setValues(getfCurrentEntityData().getChData(), offset, length);
				return -1;
			}
		}

		// scan literal value
		while (getfCurrentEntityData().getPositionData() < getfCurrentEntityData().getCountData()) {
			c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
			getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
			if ((c == quote && (!getfCurrentEntityData().isLiteralData() || external))
					|| c == '%' || !XMLChar.isContent(c)) {
				getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() - 1);
				break;
			}
		}
		int length = getfCurrentEntityData().getPositionData() - offset;
		getfCurrentEntityData().setColumnNumberData(
				getfCurrentEntityData().getColumnNumberData() + length - newlines);
		content.setValues(getfCurrentEntityData().getChData(), offset, length);

		// return next character
		if (getfCurrentEntityData().getPositionData() != getfCurrentEntityData().getCountData()) {
			c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
			// NOTE: We don't want to accidentally signal the
			// end of the literal if we're expanding an
			// entity appearing in the literal. -Ac
			if (c == quote && getfCurrentEntityData().isLiteralData()) {
				c = -1;
			}
		} else {
			c = -1;
		}
		return c;

	}

	/**
	 * Scans a range of character data up to the specified delimiter, setting
	 * the fields of the XMLString structure, appropriately.
	 * <p>
	 * <strong>Note:</strong> The characters are consumed.
	 * <p>
	 * <strong>Note:</strong> This assumes that the internal buffer is at least
	 * the same size, or bigger, than the length of the delimiter and that the
	 * delimiter contains at least one character.
	 * <p>
	 * <strong>Note:</strong> This method does not guarantee to return the
	 * longest run of character data. This method may return before the
	 * delimiter due to reaching the end of the input buffer or any other
	 * reason.
	 * <p>
	 * <strong>Note:</strong> The fields contained in the XMLString structure
	 * are not guaranteed to remain valid upon subsequent calls to the entity
	 * scanner. Therefore, the caller is responsible for immediately using the
	 * returned character data or making a copy of the character data.
	 *
	 * @param delimiter
	 *            The string that signifies the end of the character data to be
	 *            scanned.
	 * @param buffer
	 *            The data structure to fill.
	 *
	 * @return Returns true if there is more data to scan, false otherwise.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 */
	public boolean scanData(String delimiter, XMLStringBuffer buffer)
			throws IOException {

		boolean done = false;
		int delimLen = delimiter.length();
		char charAt0 = delimiter.charAt(0);
		boolean external = getfCurrentEntityData().isExternal();
		do {

			// load more characters, if needed

			if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
				load(0, true);
			} else if (getfCurrentEntityData().getPositionData() >= getfCurrentEntityData().getCountData()
					- delimLen) {
				System.arraycopy(getfCurrentEntityData().getChData(), getfCurrentEntityData().getPositionData(),
						getfCurrentEntityData().getChData(), 0, getfCurrentEntityData().getCountData()
								- getfCurrentEntityData().getPositionData());
				load(getfCurrentEntityData().getCountData() - getfCurrentEntityData().getPositionData(), false);
				getfCurrentEntityData().setPositionData(0);
			}
			if (getfCurrentEntityData().getPositionData() >= getfCurrentEntityData().getCountData() - delimLen) {
				// something must be wrong with the input: e.g., file ends an
				// unterminated comment
				int length = getfCurrentEntityData().getCountData() - getfCurrentEntityData().getPositionData();
				buffer.append(getfCurrentEntityData().getChData(), getfCurrentEntityData().getPositionData(),
						length);
				getfCurrentEntityData().setColumnNumberData(
						getfCurrentEntityData().getColumnNumberData() + getfCurrentEntityData().getCountData());
				getfCurrentEntityData().setPositionData(getfCurrentEntityData().getCountData());
				load(0, true);
				return false;
			}

			// normalize newlines
			int offset = getfCurrentEntityData().getPositionData();
			int c = getfCurrentEntityData().getChData()[offset];
			int newlines = 0;
			if (c == '\n' || (c == '\r' && external)) {
				do {
					c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
					getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
					if (c == '\r' && external) {
						newlines++;
						getfCurrentEntityData().setLineNumberData(
								getfCurrentEntityData().getLineNumberData() + 1);
						getfCurrentEntityData().setColumnNumberData(1);
						if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
							offset = 0;
							getfCurrentEntityData().setPositionData(newlines);
							if (load(newlines, false)) {
								break;
							}
						}
						if (getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()] == '\n') {
							getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
							offset++;
						}
						/*** NEWLINE NORMALIZATION ***/
						else {
							newlines++;
						}
					} else if (c == '\n') {
						newlines++;
						getfCurrentEntityData().setLineNumberData(
								getfCurrentEntityData().getLineNumberData() + 1);
						getfCurrentEntityData().setColumnNumberData(1);
						if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
							offset = 0;
							getfCurrentEntityData().setPositionData(newlines);
							getfCurrentEntityData().setCountData(newlines);
							if (load(newlines, false)) {
								break;
							}
						}
					} else {
						getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() - 1);
						break;
					}
				} while (getfCurrentEntityData().getPositionData() < getfCurrentEntityData().getCountData() - 1);
				for (int i = offset; i < getfCurrentEntityData().getPositionData(); i++) {
					getfCurrentEntityData().getChData()[i] = '\n';
				}
				int length = getfCurrentEntityData().getPositionData() - offset;
				if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData() - 1) {
					buffer.append(getfCurrentEntityData().getChData(), offset, length);
					return true;
				}
			}

			// iterate over buffer looking for delimiter
			OUTER: while (getfCurrentEntityData().getPositionData() < getfCurrentEntityData().getCountData()) {
				c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
				getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
				if (c == charAt0) {
					// looks like we just hit the delimiter
					int delimOffset = getfCurrentEntityData().getPositionData() - 1;
					for (int i = 1; i < delimLen; i++) {
						if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
							getfCurrentEntityData().setPositionData(
									getfCurrentEntityData().getPositionData() - i);
							break OUTER;
						}
						c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
						getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
						if (delimiter.charAt(i) != c) {
							getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() - 1);
							break;
						}
					}
					if (getfCurrentEntityData().getPositionData() == delimOffset + delimLen) {
						done = true;
						break;
					}
				} else if (c == '\n' || (external && c == '\r')) {
					getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() - 1);
					break;
				} else if (XMLChar.isInvalid(c)) {
					getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() -1);
					int length = getfCurrentEntityData().getPositionData() - offset;
					getfCurrentEntityData()
							.setColumnNumberData(
									getfCurrentEntityData().getColumnNumberData() + length - newlines);
					buffer.append(getfCurrentEntityData().getChData(), offset, length);
					return true;
				}
			}
			int length = getfCurrentEntityData().getPositionData() - offset;
			getfCurrentEntityData().setColumnNumberData(
					getfCurrentEntityData().getColumnNumberData() + length - newlines);
			if (done) {
				length -= delimLen;
			}
			buffer.append(getfCurrentEntityData().getChData(), offset, length);

			// return true if string was skipped
		} while (!done);
		return !done;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.skipChar
	/**
	 * Skips a character appearing immediately on the input.
	 * <p>
	 * <strong>Note:</strong> The character is consumed only if it matches the
	 * specified character.
	 *
	 * @param c
	 *            The character to skip.
	 *
	 * @return Returns true if the character was skipped.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 */
	public boolean skipChar(int c) throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		}

		// skip character
		int cc = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
		if (cc == c) {
			getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
			if (c == '\n') {
				getfCurrentEntityData().setLineNumberData(getfCurrentEntityData().getLineNumberData() + 1);
				getfCurrentEntityData().setColumnNumberData(1);
			} else {
				getfCurrentEntityData().setColumnNumberData(
						getfCurrentEntityData().getColumnNumberData() + 1);
			}
			return true;
		} else if (c == '\n' && cc == '\r' && getfCurrentEntityData().isExternal()) {
			// handle newlines
			if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
				getfCurrentEntityData().getChData()[0] = (char) cc;
				load(1, false);
			}
			getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
			if (getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()] == '\n') {
				getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
			}
			getfCurrentEntityData().setLineNumberData(getfCurrentEntityData().getLineNumberData() + 1);
			getfCurrentEntityData().setColumnNumberData(1);
			return true;
		}

		// character was not skipped
		return false;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.skipSpaces
	/**
	 * Skips space characters appearing immediately on the input.
	 * <p>
	 * <strong>Note:</strong> The characters are consumed only if they are space
	 * characters.
	 *
	 * @return Returns true if at least one space character was skipped.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 *
	 * @see XMLChar#isSpace
	 */
	public boolean skipSpaces() throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		}

		// skip spaces
		int c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
		if (XMLChar.isSpace(c)) {
			boolean external = getfCurrentEntityData().isExternal();
			do {
				boolean entityChanged = false;
				// handle newlines
				if (c == '\n' || (external && c == '\r')) {
					getfCurrentEntityData().setLineNumberData(
							getfCurrentEntityData().getLineNumberData() + 1);
					getfCurrentEntityData().setColumnNumberData(1);
					if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData() - 1) {
						getfCurrentEntityData().getChData()[0] = (char) c;
						entityChanged = load(1, true);
						if (!entityChanged)
							// the load change the position to be 1,
							// need to restore it when entity not changed
							getfCurrentEntityData().setPositionData(0);
					}
					if (c == '\r' && external) {
						// REVISIT: Does this need to be updated to fix the
						// #x0D ^#x0A newline normalization problem? -Ac
						getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
						if (getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()] != '\n') {
							getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() - 1);
						}
					}
				} else {
					getfCurrentEntityData().setColumnNumberData(
							getfCurrentEntityData().getColumnNumberData() + 1);
				}
				// load more characters, if needed
				if (!entityChanged)
					getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
				if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
					load(0, true);
				}
			} while (XMLChar
					.isSpace(c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()]));
			return true;
		}

		// no spaces were found
		return false;

	}

	/**
	 * Skips the specified string appearing immediately on the input.
	 * <p>
	 * <strong>Note:</strong> The characters are consumed only if they are space
	 * characters.
	 *
	 * @param s
	 *            The string to skip.
	 *
	 * @return Returns true if the string was skipped.
	 *
	 * @throws IOException
	 *             Thrown if i/o error occurs.
	 * @throws EOFException
	 *             Thrown on end of file.
	 */
	public boolean skipString(String s) throws IOException {

		// load more characters, if needed
		if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
			load(0, true);
		}

		// skip string
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			char c = getfCurrentEntityData().getChData()[getfCurrentEntityData().getPositionData()];
			getfCurrentEntityData().setPositionData(getfCurrentEntityData().getPositionData() + 1);
			if (c != s.charAt(i)) {
				getfCurrentEntityData().setPositionData(
						getfCurrentEntityData().getPositionData() - (i + 1));
				return false;
			}
			if (i < length - 1
					&& getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
				System.arraycopy(getfCurrentEntityData().getChData(), getfCurrentEntityData().getCountData() - i
						- 1, getfCurrentEntityData().getChData(), 0, i + 1);
				// REVISIT: Can a string to be skipped cross an
				// entity boundary? -Ac
				if (load(i + 1, false)) {
					getfCurrentEntityData().setPositionData(
							getfCurrentEntityData().getPositionData() - (i + 1));
					return false;
				}
			}
		}
		getfCurrentEntityData()
				.setColumnNumberData(getfCurrentEntityData().getColumnNumberData() + length);
		return true;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLEntityManager.EntityScanner.load
	/**
	 * Loads a chunk of text.
	 *
	 * @param offset
	 *            The offset into the character buffer to read the next batch of
	 *            characters.
	 * @param changeEntity
	 *            True if the load should change entities at the end of the
	 *            entity, otherwise leave the current entity in place and the
	 *            entity boundary will be signaled by the return value.
	 *
	 * @return Returns true if the entity changed as a result of this load
	 *         operation.
	 */
	public final boolean load(int offset, boolean changeEntity)
			throws IOException {

		// read characters
		int length = getfCurrentEntityData().isMayReadChunksData() ? (getfCurrentEntityData().getChData().length - offset)
				: (DEFAULT_XMLDECL_BUFFER_SIZE);
		int count = getfCurrentEntityData().getReaderData().read(getfCurrentEntityData().getChData(), offset,
				length);

		// reset count and position
		boolean entityChanged = false;
		if (count != -1) {
			if (count != 0) {
				getfCurrentEntityData().setCountData(count + offset);
				getfCurrentEntityData().setPositionData(offset);
			}
		}

		// end of this entity
		else {
			getfCurrentEntityData().setCountData(offset);
			getfCurrentEntityData().setPositionData(offset);
			entityChanged = true;
			if (changeEntity) {
				endEntity();
				if (getfCurrentEntityData() == null) {
					throw new EOFException();
				}
				// handle the trailing edges
				if (getfCurrentEntityData().getPositionData() == getfCurrentEntityData().getCountData()) {
					load(0, false);
				}
			}
		}

		return entityChanged;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLDocumentScannerImpl.dispatch
	private void scanXMLDecl() throws IOException, JasperException {

		if (skipString("<?xml")) {
			setfMarkupDepthData(getfMarkupDepthData() + 1);
			// NOTE: special case where document starts with a PI
			// whose name starts with "xml" (e.g. "xmlfoo")
			if (XMLChar.isName(peekChar())) {
				getfStringBufferData().clear();
				getfStringBufferData().append("xml");
				while (XMLChar.isName(peekChar())) {
					getfStringBufferData().append((char) scanChar());
				}
				String target = getfSymbolTableData().addSymbol(getfStringBufferData().getCh(),
						getfStringBufferData().getOffset(), getfStringBufferData().getLength());
				scanPIData(target, getfStringData());
			}

			// standard XML declaration
			else {
				scanXMLDeclOrTextDecl(false);
			}
		}
	}

	// Adapted from:
	// org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanXMLDeclOrTextDecl
	/**
	 * Scans an XML or text declaration.
	 * <p>
	 * 
	 * <pre>
	 * [23] XMLDecl ::= '&lt;?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
	 * [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
	 * [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
	 * [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
	 * [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
	 *                 | ('"' ('yes' | 'no') '"'))
	 * 
	 * [77] TextDecl ::= '&lt;?xml' VersionInfo? EncodingDecl S? '?>'
	 * </pre>
	 *
	 * @param scanningTextDecl
	 *            True if a text declaration is to be scanned instead of an XML
	 *            declaration.
	 */
	private void scanXMLDeclOrTextDecl(boolean scanningTextDecl)
			throws IOException, JasperException {

		// scan decl
		scanXMLDeclOrTextDecl(scanningTextDecl, getfStringsData());
		setfMarkupDepthData(getfMarkupDepthData() -1);

		// pseudo-attribute values
		String encodingPseudoAttr = getfStringsData()[1];

		// set encoding on reader
		if (encodingPseudoAttr != null) {
			setEncodingSetInPrologData(true);
			setEncodingData(encodingPseudoAttr);
		}
	}

	// Adapted from:
	// org.apache.xerces.impl.XMLScanner.scanXMLDeclOrTextDecl
	/**
	 * Scans an XML or text declaration.
	 * <p>
	 * 
	 * <pre>
	 * [23] XMLDecl ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
	 * [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
	 * [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
	 * [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
	 * [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
	 *                 | ('"' ('yes' | 'no') '"'))
	 * 
	 * [77] TextDecl ::= '<?xml' VersionInfo? EncodingDecl S? '?>'
	 * </pre>
	 *
	 * @param scanningTextDecl
	 *            True if a text declaration is to be scanned instead of an XML
	 *            declaration.
	 * @param pseudoAttributeValues
	 *            An array of size 3 to return the version, encoding and
	 *            standalone pseudo attribute values (in that order).
	 *
	 *            <strong>Note:</strong> This method uses fString, anything in
	 *            it at the time of calling is lost.
	 */
	private void scanXMLDeclOrTextDecl(boolean scanningTextDecl,
			String[] pseudoAttributeValues) throws IOException, JasperException {

		// pseudo-attribute values
		String version = null;
		String encoding = null;
		String standalone = null;

		// scan pseudo-attributes
		final int STATE_VERSION = 0;
		final int STATE_ENCODING = 1;
		final int STATE_STANDALONE = 2;
		final int STATE_DONE = 3;
		int state = STATE_VERSION;

		boolean dataFoundForTarget = false;
		boolean sawSpace = skipSpaces();
		while (peekChar() != '?') {
			dataFoundForTarget = true;
			String name = scanPseudoAttribute(scanningTextDecl, getfStringData());
			switch (state) {
			case STATE_VERSION: {
				if (name == fVersionSymbol) {
					if (!sawSpace) {
						reportFatalError(
								scanningTextDecl ? "jsp.error.xml.spaceRequiredBeforeVersionInTextDecl"
										: "jsp.error.xml.spaceRequiredBeforeVersionInXMLDecl",
								null);
					}
					version = getfStringData().toString();
					state = STATE_ENCODING;
					if (!version.equals("1.0")) {
						// REVISIT: XML REC says we should throw an error
						// in such cases.
						// some may object the throwing of fatalError.
						getErrData().jspError("jsp.error.xml.versionNotSupported",
								version);
					}
				} else if (name == fEncodingSymbol) {
					if (!scanningTextDecl) {
						getErrData().jspError("jsp.error.xml.versionInfoRequired");
					}
					if (!sawSpace) {
						reportFatalError(
								scanningTextDecl ? "jsp.error.xml.spaceRequiredBeforeEncodingInTextDecl"
										: "jsp.error.xml.spaceRequiredBeforeEncodingInXMLDecl",
								null);
					}
					encoding = getfStringData().toString();
					state = scanningTextDecl ? STATE_DONE : STATE_STANDALONE;
				} else {
					if (scanningTextDecl) {
						getErrData().jspError("jsp.error.xml.encodingDeclRequired");
					} else {
						getErrData().jspError("jsp.error.xml.versionInfoRequired");
					}
				}
				break;
			}
			case STATE_ENCODING: {
				if (name == fEncodingSymbol) {
					if (!sawSpace) {
						reportFatalError(
								scanningTextDecl ? "jsp.error.xml.spaceRequiredBeforeEncodingInTextDecl"
										: "jsp.error.xml.spaceRequiredBeforeEncodingInXMLDecl",
								null);
					}
					encoding = getfStringData().toString();
					state = scanningTextDecl ? STATE_DONE : STATE_STANDALONE;
					// TODO: check encoding name; set encoding on
					// entity scanner
				} else if (!scanningTextDecl && name == fStandaloneSymbol) {
					if (!sawSpace) {
						getErrData().jspError("jsp.error.xml.spaceRequiredBeforeStandalone");
					}
					standalone = getfStringData().toString();
					state = STATE_DONE;
					if (!standalone.equals("yes") && !standalone.equals("no")) {
						getErrData().jspError("jsp.error.xml.sdDeclInvalid");
					}
				} else {
					getErrData().jspError("jsp.error.xml.encodingDeclRequired");
				}
				break;
			}
			case STATE_STANDALONE: {
				if (name == fStandaloneSymbol) {
					if (!sawSpace) {
						getErrData().jspError("jsp.error.xml.spaceRequiredBeforeStandalone");
					}
					standalone = getfStringData().toString();
					state = STATE_DONE;
					if (!standalone.equals("yes") && !standalone.equals("no")) {
						getErrData().jspError("jsp.error.xml.sdDeclInvalid");
					}
				} else {
					getErrData().jspError("jsp.error.xml.encodingDeclRequired");
				}
				break;
			}
			default: {
				getErrData().jspError("jsp.error.xml.noMorePseudoAttributes");
			}
			}
			sawSpace = skipSpaces();
		}
		// REVISIT: should we remove this error reporting?
		if (scanningTextDecl && state != STATE_DONE) {
			getErrData().jspError("jsp.error.xml.morePseudoAttributes");
		}

		// If there is no data in the xml or text decl then we fail to report
		// error for version or encoding info above.
		if (scanningTextDecl) {
			if (!dataFoundForTarget && encoding == null) {
				getErrData().jspError("jsp.error.xml.encodingDeclRequired");
			}
		} else {
			if (!dataFoundForTarget && version == null) {
				getErrData().jspError("jsp.error.xml.versionInfoRequired");
			}
		}

		// end
		if (!skipChar('?')) {
			getErrData().jspError("jsp.error.xml.xmlDeclUnterminated");
		}
		if (!skipChar('>')) {
			getErrData().jspError("jsp.error.xml.xmlDeclUnterminated");

		}

		// fill in return array
		pseudoAttributeValues[0] = version;
		pseudoAttributeValues[1] = encoding;
		pseudoAttributeValues[2] = standalone;
	}

	// Adapted from:
	// org.apache.xerces.impl.XMLScanner.scanPseudoAttribute
	/**
	 * Scans a pseudo attribute.
	 *
	 * @param scanningTextDecl
	 *            True if scanning this pseudo-attribute for a TextDecl; false
	 *            if scanning XMLDecl. This flag is needed to report the correct
	 *            type of error.
	 * @param value
	 *            The string to fill in with the attribute value.
	 *
	 * @return The name of the attribute
	 *
	 *         <strong>Note:</strong> This method uses fStringBuffer2, anything
	 *         in it at the time of calling is lost.
	 */
	public String scanPseudoAttribute(boolean scanningTextDecl, XMLString value)
			throws IOException, JasperException {

		String name = scanName();
		if (name == null) {
			getErrData().jspError("jsp.error.xml.pseudoAttrNameExpected");
		}
		skipSpaces();
		if (!skipChar('=')) {
			reportFatalError(
					scanningTextDecl ? "jsp.error.xml.eqRequiredInTextDecl"
							: "jsp.error.xml.eqRequiredInXMLDecl", name);
		}
		skipSpaces();
		int quote = peekChar();
		if (quote != '\'' && quote != '"') {
			reportFatalError(
					scanningTextDecl ? "jsp.error.xml.quoteRequiredInTextDecl"
							: "jsp.error.xml.quoteRequiredInXMLDecl", name);
		}
		scanChar();
		int c = scanLiteral(quote, value);
		if (c != quote) {
			getfStringBuffer2Data().clear();
			do {
				getfStringBuffer2Data().append(value);
				if (c != -1) {
					if (c == '&' || c == '%' || c == '<' || c == ']') {
						getfStringBuffer2Data().append((char) scanChar());
					} else if (XMLChar.isHighSurrogate(c)) {
						scanSurrogates(getfStringBuffer2Data());
					} else if (XMLChar.isInvalid(c)) {
						String key = scanningTextDecl ? "jsp.error.xml.invalidCharInTextDecl"
								: "jsp.error.xml.invalidCharInXMLDecl";
						reportFatalError(key, Integer.toString(c, 16));
						scanChar();
					}
				}
				c = scanLiteral(quote, value);
			} while (c != quote);
			getfStringBuffer2Data().append(value);
			value.setValues(getfStringBuffer2Data());
		}
		if (!skipChar(quote)) {
			reportFatalError(
					scanningTextDecl ? "jsp.error.xml.closeQuoteMissingInTextDecl"
							: "jsp.error.xml.closeQuoteMissingInXMLDecl", name);
		}

		// return
		return name;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLScanner.scanPIData
	/**
	 * Scans a processing data. This is needed to handle the situation where a
	 * document starts with a processing instruction whose target name
	 * <em>starts with</em> "xml". (e.g. xmlfoo)
	 *
	 * <strong>Note:</strong> This method uses fStringBuffer, anything in it at
	 * the time of calling is lost.
	 *
	 * @param target
	 *            The PI target
	 * @param data
	 *            The string to fill in with the data
	 */
	private void scanPIData(String target, XMLString data) throws IOException,
			JasperException {

		// check target
		if (target.length() == 3) {
			char c0 = Character.toLowerCase(target.charAt(0));
			char c1 = Character.toLowerCase(target.charAt(1));
			char c2 = Character.toLowerCase(target.charAt(2));
			if (c0 == 'x' && c1 == 'm' && c2 == 'l') {
				getErrData().jspError("jsp.error.xml.reservedPITarget");
			}
		}

		// spaces
		if (!skipSpaces()) {
			if (skipString("?>")) {
				// we found the end, there is no data
				data.clear();
				return;
			} else {
				// if there is data there should be some space
				getErrData().jspError("jsp.error.xml.spaceRequiredInPI");
			}
		}

		getfStringBufferData().clear();
		// data
		if (scanData("?>", getfStringBufferData())) {
			do {
				int c = peekChar();
				if (c != -1) {
					if (XMLChar.isHighSurrogate(c)) {
						scanSurrogates(getfStringBufferData());
					} else if (XMLChar.isInvalid(c)) {
						getErrData().jspError("jsp.error.xml.invalidCharInPI",
								Integer.toHexString(c));
						scanChar();
					}
				}
			} while (scanData("?>", getfStringBufferData()));
		}
		data.setValues(getfStringBufferData());

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLScanner.scanSurrogates
	/**
	 * Scans surrogates and append them to the specified buffer.
	 * <p>
	 * <strong>Note:</strong> This assumes the current char has already been
	 * identified as a high surrogate.
	 *
	 * @param buf
	 *            The StringBuffer to append the read surrogates to.
	 * @return True if it succeeded.
	 */
	private boolean scanSurrogates(XMLStringBuffer buf) throws IOException,
			JasperException {

		int high = scanChar();
		int low = peekChar();
		if (!XMLChar.isLowSurrogate(low)) {
			getErrData().jspError("jsp.error.xml.invalidCharInContent",
					Integer.toString(high, 16));
			return false;
		}
		scanChar();

		// convert surrogates to supplemental character
		int c = XMLChar.supplemental((char) high, (char) low);

		// supplemental character must be a valid XML character
		if (!XMLChar.isValid(c)) {
			getErrData().jspError("jsp.error.xml.invalidCharInContent",
					Integer.toString(c, 16));
			return false;
		}

		// fill in the buffer
		buf.append((char) high);
		buf.append((char) low);

		return true;

	}

	// Adapted from:
	// org.apache.xerces.impl.XMLScanner.reportFatalError
	/**
	 * Convenience function used in all XML scanners.
	 */
	private void reportFatalError(String msgId, String arg)
			throws JasperException {
		getErrData().jspError(msgId, arg);
	}

	public InputStream getStream() {
		return getStreamData();
	}

	public void setStream(InputStream stream) {
		this.setStreamData(stream);
	}

	public String getEncoding() {
		return getEncodingData();
	}

	public void setEncoding(String encoding) {
		this.setEncodingData(encoding);
	}

	public boolean isEncodingSetInProlog() {
		return isEncodingSetInPrologData();
	}

	public void setEncodingSetInProlog(boolean isEncodingSetInProlog) {
		this.setEncodingSetInPrologData(isEncodingSetInProlog);
	}

	public boolean isBomPresent() {
		return isBomPresentData();
	}

	public void setBomPresent(boolean isBomPresent) {
		this.setBomPresentData(isBomPresent);
	}

	public int getSkip() {
		return getSkipData();
	}

	public void setSkip(int skip) {
		this.setSkipData(skip);
	}

	public Boolean getIsBigEndian() {
		return getIsBigEndianData();
	}

	public void setIsBigEndian(Boolean isBigEndian) {
		this.setIsBigEndianData(isBigEndian);
	}

	public Reader getReader() {
		return getReaderData();
	}

	public void setReader(Reader reader) {
		this.setReaderData(reader);
	}

	public boolean isfAllowJavaEncodings() {
		return isfAllowJavaEncodingsData();
	}

	public void setfAllowJavaEncodings(boolean fAllowJavaEncodings) {
		this.setfAllowJavaEncodingsData(fAllowJavaEncodings);
	}

	public SymbolTable getfSymbolTable() {
		return getfSymbolTableData();
	}

	public void setfSymbolTable(SymbolTable fSymbolTable) {
		this.setfSymbolTableData(fSymbolTable);
	}

	public XMLEncodingDetector getfCurrentEntity() {
		return getfCurrentEntityData();
	}

	public void setfCurrentEntity(XMLEncodingDetector fCurrentEntity) {
		this.setfCurrentEntityData(fCurrentEntity);
	}

	public int getfBufferSize() {
		return fBufferSize;
	}

	public void setfBufferSize(int fBufferSize) {
		this.fBufferSize = fBufferSize;
	}

	public int getLineNumber() {
		return getLineNumberData();
	}

	public void setLineNumber(int lineNumber) {
		this.setLineNumberData(lineNumber);
	}

	public int getColumnNumber() {
		return getColumnNumberData();
	}

	public void setColumnNumber(int columnNumber) {
		this.setColumnNumberData(columnNumber);
	}

	public boolean isLiteral() {
		return isLiteralData();
	}

	public void setLiteral(boolean literal) {
		this.setLiteralData(literal);
	}

	public char[] getCh() {
		return getChData();
	}

	public void setCh(char[] ch) {
		this.setChData(ch);
	}

	public int getPosition() {
		return getPositionData();
	}

	public void setPosition(int position) {
		this.setPositionData(position);
	}

	public int getCount() {
		return getCountData();
	}

	public void setCount(int count) {
		this.setCountData(count);
	}

	public boolean isMayReadChunks() {
		return isMayReadChunksData();
	}

	public void setMayReadChunks(boolean mayReadChunks) {
		this.setMayReadChunksData(mayReadChunks);
	}

	public XMLString getfString() {
		return getfStringData();
	}

	public void setfString(XMLString fString) {
		this.setfStringData(fString);
	}

	public XMLStringBuffer getfStringBuffer() {
		return getfStringBufferData();
	}

	public void setfStringBuffer(XMLStringBuffer fStringBuffer) {
		this.setfStringBufferData(fStringBuffer);
	}

	public XMLStringBuffer getfStringBuffer2() {
		return getfStringBuffer2Data();
	}

	public void setfStringBuffer2(XMLStringBuffer fStringBuffer2) {
		this.setfStringBuffer2Data(fStringBuffer2);
	}

	public int getfMarkupDepth() {
		return getfMarkupDepthData();
	}

	public void setfMarkupDepth(int fMarkupDepth) {
		this.setfMarkupDepthData(fMarkupDepth);
	}

	public String[] getfStrings() {
		return getfStringsData();
	}

	public void setfStrings(String[] fStrings) {
		this.setfStringsData(fStrings);
	}

	public ErrorDispatcher getErr() {
		return getErrData();
	}

	public void setErr(ErrorDispatcher err) {
		this.setErrData(err);
	}

	public static int getDefaultBufferSize() {
		return DEFAULT_BUFFER_SIZE;
	}

	public static int getDefaultXmldeclBufferSize() {
		return DEFAULT_XMLDECL_BUFFER_SIZE;
	}

	public static String getFversionsymbol() {
		return fVersionSymbol;
	}

	public static String getFencodingsymbol() {
		return fEncodingSymbol;
	}

	public static String getFstandalonesymbol() {
		return fStandaloneSymbol;
	}

	public InputStream getStreamData() {
		return stream;
	}

	public void setStreamData(InputStream stream) {
		this.stream = stream;
	}

	public String getEncodingData() {
		return encoding;
	}

	public void setEncodingData(String encoding) {
		this.encoding = encoding;
	}

	public boolean isEncodingSetInPrologData() {
		return isEncodingSetInProlog;
	}

	public void setEncodingSetInPrologData(boolean isEncodingSetInProlog) {
		this.isEncodingSetInProlog = isEncodingSetInProlog;
	}

	public boolean isBomPresentData() {
		return isBomPresent;
	}

	public void setBomPresentData(boolean isBomPresent) {
		this.isBomPresent = isBomPresent;
	}

	public int getSkipData() {
		return skip;
	}

	public void setSkipData(int skip) {
		this.skip = skip;
	}

	public Boolean getIsBigEndianData() {
		return isBigEndian;
	}

	public void setIsBigEndianData(Boolean isBigEndian) {
		this.isBigEndian = isBigEndian;
	}

	public Reader getReaderData() {
		return reader;
	}

	public void setReaderData(Reader reader) {
		this.reader = reader;
	}

	public boolean isfAllowJavaEncodingsData() {
		return fAllowJavaEncodings;
	}

	public void setfAllowJavaEncodingsData(boolean fAllowJavaEncodings) {
		this.fAllowJavaEncodings = fAllowJavaEncodings;
	}

	public SymbolTable getfSymbolTableData() {
		return fSymbolTable;
	}

	public void setfSymbolTableData(SymbolTable fSymbolTable) {
		this.fSymbolTable = fSymbolTable;
	}

	public XMLEncodingDetector getfCurrentEntityData() {
		return fCurrentEntity;
	}

	public void setfCurrentEntityData(XMLEncodingDetector fCurrentEntity) {
		this.fCurrentEntity = fCurrentEntity;
	}

	public int getLineNumberData() {
		return lineNumber;
	}

	public void setLineNumberData(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public int getColumnNumberData() {
		return columnNumber;
	}

	public void setColumnNumberData(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	public boolean isLiteralData() {
		return literal;
	}

	public void setLiteralData(boolean literal) {
		this.literal = literal;
	}

	public char[] getChData() {
		return ch;
	}

	public void setChData(char[] ch) {
		this.ch = ch;
	}

	public int getPositionData() {
		return position;
	}

	public void setPositionData(int position) {
		this.position = position;
	}

	public int getCountData() {
		return count;
	}

	public void setCountData(int count) {
		this.count = count;
	}

	public boolean isMayReadChunksData() {
		return mayReadChunks;
	}

	public void setMayReadChunksData(boolean mayReadChunks) {
		this.mayReadChunks = mayReadChunks;
	}

	public XMLString getfStringData() {
		return fString;
	}

	public void setfStringData(XMLString fString) {
		this.fString = fString;
	}

	public XMLStringBuffer getfStringBufferData() {
		return fStringBuffer;
	}

	public void setfStringBufferData(XMLStringBuffer fStringBuffer) {
		this.fStringBuffer = fStringBuffer;
	}

	public XMLStringBuffer getfStringBuffer2Data() {
		return fStringBuffer2;
	}

	public void setfStringBuffer2Data(XMLStringBuffer fStringBuffer2) {
		this.fStringBuffer2 = fStringBuffer2;
	}

	public int getfMarkupDepthData() {
		return fMarkupDepth;
	}

	public void setfMarkupDepthData(int fMarkupDepth) {
		this.fMarkupDepth = fMarkupDepth;
	}

	public String[] getfStringsData() {
		return fStrings;
	}

	public void setfStringsData(String[] fStrings) {
		this.fStrings = fStrings;
	}

	public ErrorDispatcher getErrData() {
		return err;
	}

	public void setErrData(ErrorDispatcher err) {
		this.err = err;
	}

}
