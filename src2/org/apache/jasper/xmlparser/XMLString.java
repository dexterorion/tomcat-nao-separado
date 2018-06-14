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

/**
 * This class is used as a structure to pass text contained in the underlying
 * character buffer of the scanner. The offset and length fields allow the
 * buffer to be re-used without creating new character arrays.
 * <p>
 * <strong>Note:</strong> Methods that are passed an XMLString structure should
 * consider the contents read-only and not make any modifications to the
 * contents of the buffer. The method receiving this structure should also not
 * modify the offset and length if this structure (or the values of this
 * structure) are passed to another method.
 * <p>
 * <strong>Note:</strong> Methods that are passed an XMLString structure are
 * required to copy the information out of the buffer if it is to be saved for
 * use beyond the scope of the method. The contents of the structure are
 * volatile and the contents of the character buffer cannot be assured once the
 * method that is passed this structure returns. Therefore, methods passed this
 * structure should not save any reference to the structure or the character
 * array contained in the structure.
 *
 * @author Eric Ye, IBM
 * @author Andy Clark, IBM
 */
public class XMLString {

	//
	// Data
	//

	/** The character array. */
	private char[] ch;

	/** The offset into the character array. */
	private int offset;

	/** The length of characters from the offset. */
	private int length;

	//
	// Constructors
	//

	/** Default constructor. */
	public XMLString() {
	}

	//
	// Public methods
	//

	/**
	 * Initializes the contents of the XMLString structure with the specified
	 * values.
	 * 
	 * @param ch
	 *            The character array.
	 * @param offset
	 *            The offset into the character array.
	 * @param length
	 *            The length of characters from the offset.
	 */
	public void setValues(char[] ch, int offset, int length) {
		this.setChData(ch);
		this.setOffsetData(offset);
		this.setLengthData(length);
	}

	/**
	 * Initializes the contents of the XMLString structure with copies of the
	 * given string structure.
	 * <p>
	 * <strong>Note:</strong> This does not copy the character array; only the
	 * reference to the array is copied.
	 * 
	 * @param s
	 */
	public void setValues(XMLString s) {
		setValues(s.getChData(), s.getOffsetData(), s.getLengthData());
	}

	/** Resets all of the values to their defaults. */
	public void clear() {
		this.setChData(null);
		this.setOffsetData(0);
		this.setLengthData(-1);
	}

	/**
	 * Returns true if the contents of this XMLString structure and the
	 * specified string are equal.
	 * 
	 * @param s
	 *            The string to compare.
	 */
	public boolean equals(String s) {
		if (s == null) {
			return false;
		}
		if (getLengthData() != s.length()) {
			return false;
		}

		// is this faster than call s.toCharArray first and compare the
		// two arrays directly, which will possibly involve creating a
		// new char array object.
		for (int i = 0; i < getLengthData(); i++) {
			if (getChData()[getOffsetData() + i] != s.charAt(i)) {
				return false;
			}
		}

		return true;
	}

	//
	// Object methods
	//

	/** Returns a string representation of this object. */
	@Override
	public String toString() {
		return getLengthData() > 0 ? new String(getChData(), getOffsetData(), getLengthData()) : "";
	}

	public char[] getCh() {
		return getChData();
	}

	public void setCh(char[] ch) {
		this.setChData(ch);
	}

	public int getOffset() {
		return getOffsetData();
	}

	public void setOffset(int offset) {
		this.setOffsetData(offset);
	}

	public int getLength() {
		return getLengthData();
	}

	public void setLength(int length) {
		this.setLengthData(length);
	}
	
	public void setCh(char c, int position){
		this.getChData()[position] = c;
	}

	public char[] getChData() {
		return ch;
	}

	public void setChData(char[] ch) {
		this.ch = ch;
	}

	public int getOffsetData() {
		return offset;
	}

	public void setOffsetData(int offset) {
		this.offset = offset;
	}

	public int getLengthData() {
		return length;
	}

	public void setLengthData(int length) {
		this.length = length;
	}

}
