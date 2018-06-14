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
package org.apache.tomcat.util.buf;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * This class is used to represent a subarray of bytes in an HTTP message. It
 * represents all request/response elements. The byte/char conversions are
 * delayed and cached. Everything is recyclable.
 *
 * The object can represent a byte[], a char[], or a (sub) String. All
 * operations can be made in case sensitive mode or not.
 *
 * @author dac@eng.sun.com
 * @author James Todd [gonzo@eng.sun.com]
 * @author Costin Manolache
 */
public final class MessageBytes implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	// primary type ( whatever is set as original value )
	private int type = T_NULL;

	private static final int T_NULL = 0;
	/**
	 * getType() is T_STR if the the object used to create the MessageBytes was
	 * a String
	 */
	private static final int T_STR = 1;
	/**
	 * getType() is T_STR if the the object used to create the MessageBytes was
	 * a byte[]
	 */
	private static final int T_BYTES = 2;
	/**
	 * getType() is T_STR if the the object used to create the MessageBytes was
	 * a char[]
	 */
	private static final int T_CHARS = 3;

	private int hashCode = 0;
	// did we computed the hashcode ?
	private boolean hasHashCode = false;

	// Internal objects to represent array + offset, and specific methods
	private final ByteChunk byteC = new ByteChunk();
	private final CharChunk charC = new CharChunk();

	// String
	private String strValue;
	// true if a String value was computed. Probably not needed,
	// strValue!=null is the same
	private boolean hasStrValue = false;

	/**
	 * Creates a new, uninitialized MessageBytes object. Use static
	 * newInstance() in order to allow future hooks.
	 */
	public MessageBytes() {
	}

	/**
	 * Construct a new MessageBytes instance
	 */
	public static MessageBytes newInstance() {
		return factory.newInstance();
	}

	/**
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public MessageBytes getClone() {
		try {
			return (MessageBytes) this.clone();
		} catch (Exception ex) {
			return null;
		}
	}

	public boolean isNull() {
		// should we check also hasStrValue ???
		return getByteCData().isNull() && getCharCData().isNull() && !isHasStrValueData();
		// bytes==null && strValue==null;
	}

	/**
	 * Resets the message bytes to an uninitialized (NULL) state.
	 */
	public void recycle() {
		setTypeData(T_NULL);
		getByteCData().recycle();
		getCharCData().recycle();

		setStrValueData(null);

		setHasStrValueData(false);
		setHasHashCodeData(false);
		setHasIntValueData(false);
		setHasLongValueData(false);
	}

	/**
	 * Sets the content to the specified subarray of bytes.
	 *
	 * @param b
	 *            the bytes
	 * @param off
	 *            the start offset of the bytes
	 * @param len
	 *            the length of the bytes
	 */
	public void setBytes(byte[] b, int off, int len) {
		getByteCData().setBytes(b, off, len);
		setTypeData(T_BYTES);
		setHasStrValueData(false);
		setHasHashCodeData(false);
		setHasIntValueData(false);
		setHasLongValueData(false);
	}

	/**
	 * Set the encoding. If the object was constructed from bytes[]. any
	 * previous conversion is reset. If no encoding is set, we'll use 8859-1.
	 * 
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public void setCharset(Charset charset) {
		if (!getByteCData().isNull()) {
			// if the encoding changes we need to reset the conversion results
			getCharCData().recycle();
			setHasStrValueData(false);
		}
		getByteCData().setCharset(charset);
	}

	/**
	 * Sets the content to be a char[]
	 *
	 * @param c
	 *            the bytes
	 * @param off
	 *            the start offset of the bytes
	 * @param len
	 *            the length of the bytes
	 */
	public void setChars(char[] c, int off, int len) {
		getCharCData().setChars(c, off, len);
		setTypeData(T_CHARS);
		setHasStrValueData(false);
		setHasHashCodeData(false);
		setHasIntValueData(false);
		setHasLongValueData(false);
	}

	/**
	 * Set the content to be a string
	 */
	public void setString(String s) {
		setStrValueData(s);
		setHasHashCodeData(false);
		setHasIntValueData(false);
		setHasLongValueData(false);
		if (s == null) {
			setHasStrValueData(false);
			setTypeData(T_NULL);
		} else {
			setHasStrValueData(true);
			setTypeData(T_STR);
		}
	}

	// -------------------- Conversion and getters --------------------

	/**
	 * Compute the string value
	 */
	@Override
	public String toString() {
		if (isHasStrValueData()) {
			return getStrValueData();
		}

		switch (getTypeData()) {
		case T_CHARS:
			setStrValueData(getCharCData().toString());
			setHasStrValueData(true);
			return getStrValueData();
		case T_BYTES:
			setStrValueData(getByteCData().toString());
			setHasStrValueData(true);
			return getStrValueData();
		}
		return null;
	}

	// ----------------------------------------
	/**
	 * Return the type of the original content. Can be T_STR, T_BYTES, T_CHARS
	 * or T_NULL
	 */
	public int getType() {
		return getTypeData();
	}

	/**
	 * Returns the byte chunk, representing the byte[] and offset/length. Valid
	 * only if T_BYTES or after a conversion was made.
	 */
	public ByteChunk getByteChunk() {
		return getByteCData();
	}

	/**
	 * Returns the char chunk, representing the char[] and offset/length. Valid
	 * only if T_CHARS or after a conversion was made.
	 */
	public CharChunk getCharChunk() {
		return getCharCData();
	}

	/**
	 * Returns the string value. Valid only if T_STR or after a conversion was
	 * made.
	 */
	public String getString() {
		return getStrValueData();
	}

	/**
	 * Do a char->byte conversion.
	 */
	public void toBytes() {
		if (!getByteCData().isNull()) {
			setTypeData(T_BYTES);
			return;
		}
		toString();
		setTypeData(T_BYTES);
		byte bb[] = getStrValueData().getBytes(Charset.defaultCharset());
		getByteCData().setBytes(bb, 0, bb.length);
	}

	/**
	 * Convert to char[] and fill the CharChunk. XXX Not optimized - it converts
	 * to String first.
	 */
	public void toChars() {
		if (!getCharCData().isNull()) {
			setTypeData(T_CHARS);
			return;
		}
		// inefficient
		toString();
		setTypeData(T_CHARS);
		char cc[] = getStrValueData().toCharArray();
		getCharCData().setChars(cc, 0, cc.length);
	}

	/**
	 * Returns the length of the original buffer. Note that the length in bytes
	 * may be different from the length in chars.
	 */
	public int getLength() {
		if (getTypeData() == T_BYTES) {
			return getByteCData().getLength();
		}
		if (getTypeData() == T_CHARS) {
			return getCharCData().getLength();
		}
		if (getTypeData() == T_STR) {
			return getStrValueData().length();
		}
		toString();
		if (getStrValueData() == null) {
			return 0;
		}
		return getStrValueData().length();
	}

	// -------------------- equals --------------------

	/**
	 * Compares the message bytes to the specified String object.
	 * 
	 * @param s
	 *            the String to compare
	 * @return true if the comparison succeeded, false otherwise
	 */
	public boolean equals(String s) {
		switch (getTypeData()) {
		case T_STR:
			if (getStrValueData() == null) {
				return s == null;
			}
			return getStrValueData().equals(s);
		case T_CHARS:
			return getCharCData().equals(s);
		case T_BYTES:
			return getByteCData().equals(s);
		default:
			return false;
		}
	}

	/**
	 * Compares the message bytes to the specified String object.
	 * 
	 * @param s
	 *            the String to compare
	 * @return true if the comparison succeeded, false otherwise
	 */
	public boolean equalsIgnoreCase(String s) {
		switch (getTypeData()) {
		case T_STR:
			if (getStrValueData() == null) {
				return s == null;
			}
			return getStrValueData().equalsIgnoreCase(s);
		case T_CHARS:
			return getCharCData().equalsIgnoreCase(s);
		case T_BYTES:
			return getByteCData().equalsIgnoreCase(s);
		default:
			return false;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MessageBytes) {
			return equals((MessageBytes) obj);
		}
		return false;
	}

	public boolean equals(MessageBytes mb) {
		switch (getTypeData()) {
		case T_STR:
			return mb.equals(getStrValueData());
		}

		if (mb.getTypeData() != T_CHARS && mb.getTypeData() != T_BYTES) {
			// it's a string or int/date string value
			return equals(mb.toString());
		}

		// mb is either CHARS or BYTES.
		// this is either CHARS or BYTES
		// Deal with the 4 cases ( in fact 3, one is symmetric)

		if (mb.getTypeData() == T_CHARS && getTypeData() == T_CHARS) {
			return getCharCData().equals(mb.getCharCData());
		}
		if (mb.getTypeData() == T_BYTES && getTypeData() == T_BYTES) {
			return getByteCData().equals(mb.getByteCData());
		}
		if (mb.getTypeData() == T_CHARS && getTypeData() == T_BYTES) {
			return getByteCData().equals(mb.getCharCData());
		}
		if (mb.getTypeData() == T_BYTES && getTypeData() == T_CHARS) {
			return mb.getByteCData().equals(getCharCData());
		}
		// can't happen
		return true;
	}

	/**
	 * Returns true if the message bytes starts with the specified string.
	 * 
	 * @param s
	 *            the string
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public boolean startsWith(String s) {
		switch (getTypeData()) {
		case T_STR:
			return getStrValueData().startsWith(s);
		case T_CHARS:
			return getCharCData().startsWith(s);
		case T_BYTES:
			return getByteCData().startsWith(s);
		default:
			return false;
		}
	}

	/**
	 * Returns true if the message bytes starts with the specified string.
	 * 
	 * @param s
	 *            the string
	 * @param pos
	 *            The start position
	 */
	public boolean startsWithIgnoreCase(String s, int pos) {
		switch (getTypeData()) {
		case T_STR:
			if (getStrValueData() == null) {
				return false;
			}
			if (getStrValueData().length() < pos + s.length()) {
				return false;
			}

			for (int i = 0; i < s.length(); i++) {
				if (Ascii.toLower(s.charAt(i)) != Ascii.toLower(getStrValueData()
						.charAt(pos + i))) {
					return false;
				}
			}
			return true;
		case T_CHARS:
			return getCharCData().startsWithIgnoreCase(s, pos);
		case T_BYTES:
			return getByteCData().startsWithIgnoreCase(s, pos);
		default:
			return false;
		}
	}

	// -------------------- Hash code --------------------
	@Override
	public int hashCode() {
		if (isHasHashCodeData()) {
			return getHashCodeData();
		}
		int code = 0;

		code = hash();
		setHashCodeData(code);
		setHasHashCodeData(true);
		return code;
	}

	// normal hash.
	private int hash() {
		int code = 0;
		switch (getTypeData()) {
		case T_STR:
			// We need to use the same hash function
			for (int i = 0; i < getStrValueData().length(); i++) {
				code = code * 37 + getStrValueData().charAt(i);
			}
			return code;
		case T_CHARS:
			return getCharCData().hash();
		case T_BYTES:
			return getByteCData().hash();
		default:
			return 0;
		}
	}

	/**
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public int indexOf(char c) {
		return indexOf(c, 0);
	}

	// Inefficient initial implementation. Will be replaced on the next
	// round of tune-up
	public int indexOf(String s, int starting) {
		toString();
		return getStrValueData().indexOf(s, starting);
	}

	// Inefficient initial implementation. Will be replaced on the next
	// round of tune-up
	public int indexOf(String s) {
		return indexOf(s, 0);
	}

	public int indexOfIgnoreCase(String s, int starting) {
		toString();
		String upper = getStrValueData().toUpperCase(Locale.ENGLISH);
		String sU = s.toUpperCase(Locale.ENGLISH);
		return upper.indexOf(sU, starting);
	}

	/**
	 * Returns true if the message bytes starts with the specified string.
	 * 
	 * @param c
	 *            the character
	 * @param starting
	 *            The start position
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public int indexOf(char c, int starting) {
		switch (getTypeData()) {
		case T_STR:
			return getStrValueData().indexOf(c, starting);
		case T_CHARS:
			return getCharCData().indexOf(c, starting);
		case T_BYTES:
			return getByteCData().indexOf(c, starting);
		default:
			return -1;
		}
	}

	/**
	 * Copy the src into this MessageBytes, allocating more space if needed
	 */
	public void duplicate(MessageBytes src) throws IOException {
		switch (src.getType()) {
		case MessageBytes.T_BYTES:
			setTypeData(T_BYTES);
			ByteChunk bc = src.getByteChunk();
			getByteCData().allocate(2 * bc.getLength(), -1);
			getByteCData().append(bc);
			break;
		case MessageBytes.T_CHARS:
			setTypeData(T_CHARS);
			CharChunk cc = src.getCharChunk();
			getCharCData().allocate(2 * cc.getLength(), -1);
			getCharCData().append(cc);
			break;
		case MessageBytes.T_STR:
			setTypeData(T_STR);
			String sc = src.getString();
			this.setString(sc);
			break;
		}
	}

	// -------------------- Deprecated code --------------------
	// efficient int, long and date
	// XXX used only for headers - shouldn't be
	// stored here.
	private int intValue;
	private boolean hasIntValue = false;
	private long longValue;
	private boolean hasLongValue = false;

	/**
	 * Set the buffer to the representation of an int
	 * 
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public void setInt(int i) {
		getByteCData().allocate(16, 32);
		int current = i;
		byte[] buf = getByteCData().getBuffer();
		int start = 0;
		int end = 0;
		if (i == 0) {
			buf[end++] = (byte) '0';
		}
		if (i < 0) {
			current = -i;
			buf[end++] = (byte) '-';
		}
		while (current > 0) {
			int digit = current % 10;
			current = current / 10;
			buf[end++] = HexUtils.getHex(digit);
		}
		getByteCData().setOffset(0);
		getByteCData().setEnd(end);
		// Inverting buffer
		end--;
		if (i < 0) {
			start++;
		}
		while (end > start) {
			byte temp = buf[start];
			buf[start] = buf[end];
			buf[end] = temp;
			start++;
			end--;
		}
		setIntValueData(i);
		setHasStrValueData(false);
		setHasHashCodeData(false);
		setHasIntValueData(true);
		setHasLongValueData(false);
		setTypeData(T_BYTES);
	}

	/**
	 * Set the buffer to the representation of an long
	 */
	public void setLong(long l) {
		getByteCData().allocate(32, 64);
		long current = l;
		byte[] buf = getByteCData().getBuffer();
		int start = 0;
		int end = 0;
		if (l == 0) {
			buf[end++] = (byte) '0';
		}
		if (l < 0) {
			current = -l;
			buf[end++] = (byte) '-';
		}
		while (current > 0) {
			int digit = (int) (current % 10);
			current = current / 10;
			buf[end++] = HexUtils.getHex(digit);
		}
		getByteCData().setOffset(0);
		getByteCData().setEnd(end);
		// Inverting buffer
		end--;
		if (l < 0) {
			start++;
		}
		while (end > start) {
			byte temp = buf[start];
			buf[start] = buf[end];
			buf[end] = temp;
			start++;
			end--;
		}
		setLongValueData(l);
		setHasStrValueData(false);
		setHasHashCodeData(false);
		setHasIntValueData(false);
		setHasLongValueData(true);
		setTypeData(T_BYTES);
	}

	// Used for headers conversion
	/**
	 * Convert the buffer to an int, cache the value
	 * 
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public int getInt() {
		if (isHasIntValueData()) {
			return getIntValueData();
		}

		switch (getTypeData()) {
		case T_BYTES:
			setIntValueData(getByteCData().getInt());
			break;
		default:
			setIntValueData(Integer.parseInt(toString()));
		}
		setHasIntValueData(true);
		return getIntValueData();
	}

	// Used for headers conversion
	/**
	 * Convert the buffer to an long, cache the value
	 */
	public long getLong() {
		if (isHasLongValueData()) {
			return getLongValueData();
		}

		switch (getTypeData()) {
		case T_BYTES:
			setLongValueData(getByteCData().getLong());
			break;
		default:
			setLongValueData(Long.parseLong(toString()));
		}

		setHasLongValueData(true);
		return getLongValueData();

	}

	// -------------------- Future may be different --------------------

	private static MessageBytesMessageBytesFactory factory = new MessageBytesMessageBytesFactory();

	/**
	 * @deprecated Unused. Will be removed in Tomcat 8.0.x onwards.
	 */
	@Deprecated
	public static void setFactory(MessageBytesMessageBytesFactory mbf) {
		factory = mbf;
	}

	public int getHashCode() {
		return getHashCodeData();
	}

	public void setHashCode(int hashCode) {
		this.setHashCodeData(hashCode);
	}

	public boolean isHasHashCode() {
		return isHasHashCodeData();
	}

	public void setHasHashCode(boolean hasHashCode) {
		this.setHasHashCodeData(hasHashCode);
	}

	public String getStrValue() {
		return getStrValueData();
	}

	public void setStrValue(String strValue) {
		this.setStrValueData(strValue);
	}

	public boolean isHasStrValue() {
		return isHasStrValueData();
	}

	public void setHasStrValue(boolean hasStrValue) {
		this.setHasStrValueData(hasStrValue);
	}

	public int getIntValue() {
		return getIntValueData();
	}

	public void setIntValue(int intValue) {
		this.setIntValueData(intValue);
	}

	public boolean isHasIntValue() {
		return isHasIntValueData();
	}

	public void setHasIntValue(boolean hasIntValue) {
		this.setHasIntValueData(hasIntValue);
	}

	public long getLongValue() {
		return getLongValueData();
	}

	public void setLongValue(long longValue) {
		this.setLongValueData(longValue);
	}

	public boolean isHasLongValue() {
		return isHasLongValueData();
	}

	public void setHasLongValue(boolean hasLongValue) {
		this.setHasLongValueData(hasLongValue);
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public static int gettNull() {
		return T_NULL;
	}

	public static int gettStr() {
		return T_STR;
	}

	public static int gettBytes() {
		return T_BYTES;
	}

	public static int gettChars() {
		return T_CHARS;
	}

	public ByteChunk getByteC() {
		return getByteCData();
	}

	public CharChunk getCharC() {
		return getCharCData();
	}

	public static MessageBytesMessageBytesFactory getFactory() {
		return factory;
	}

	public void setType(int type) {
		this.setTypeData(type);
	}

	public int getTypeData() {
		return type;
	}

	public void setTypeData(int type) {
		this.type = type;
	}

	public int getHashCodeData() {
		return hashCode;
	}

	public void setHashCodeData(int hashCode) {
		this.hashCode = hashCode;
	}

	public boolean isHasHashCodeData() {
		return hasHashCode;
	}

	public void setHasHashCodeData(boolean hasHashCode) {
		this.hasHashCode = hasHashCode;
	}

	public ByteChunk getByteCData() {
		return byteC;
	}

	public CharChunk getCharCData() {
		return charC;
	}

	public String getStrValueData() {
		return strValue;
	}

	public void setStrValueData(String strValue) {
		this.strValue = strValue;
	}

	public boolean isHasStrValueData() {
		return hasStrValue;
	}

	public void setHasStrValueData(boolean hasStrValue) {
		this.hasStrValue = hasStrValue;
	}

	public int getIntValueData() {
		return intValue;
	}

	public void setIntValueData(int intValue) {
		this.intValue = intValue;
	}

	public boolean isHasIntValueData() {
		return hasIntValue;
	}

	public void setHasIntValueData(boolean hasIntValue) {
		this.hasIntValue = hasIntValue;
	}

	public long getLongValueData() {
		return longValue;
	}

	public void setLongValueData(long longValue) {
		this.longValue = longValue;
	}

	public boolean isHasLongValueData() {
		return hasLongValue;
	}

	public void setHasLongValueData(boolean hasLongValue) {
		this.hasLongValue = hasLongValue;
	}

}
