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
 *
 */
package org.apache.tomcat.util.bcel.classfile;

import java.io.DataInput;
import java.io.IOException;

/**
 * @author <A HREF="mailto:dbrosius@qis.net">D. Brosius</A>
 * @since 6.0
 */
public abstract class ElementValue {
	private final int type;

	private final ConstantPool cpool;

	public ElementValue(int type, ConstantPool cpool) {
		this.type = type;
		this.cpool = cpool;
	}

	public abstract String stringifyValue();

	private static final int STRING = 's';

	private static final int ENUM_CONSTANT = 'e';

	private static final int CLASS = 'c';

	private static final int ANNOTATION = '@';

	private static final int ARRAY = '[';

	private static final int PRIMITIVE_INT = 'I';

	private static final int PRIMITIVE_BYTE = 'B';

	private static final int PRIMITIVE_CHAR = 'C';

	private static final int PRIMITIVE_DOUBLE = 'D';

	private static final int PRIMITIVE_FLOAT = 'F';

	private static final int PRIMITIVE_LONG = 'J';

	private static final int PRIMITIVE_SHORT = 'S';

	private static final int PRIMITIVE_BOOLEAN = 'Z';

	public static ElementValue readElementValue(DataInput dis,
			ConstantPool cpool) throws IOException {
		byte type = dis.readByte();
		switch (type) {
		case 'B': // byte
			return new SimpleElementValue(PRIMITIVE_BYTE,
					dis.readUnsignedShort(), cpool);
		case 'C': // char
			return new SimpleElementValue(PRIMITIVE_CHAR,
					dis.readUnsignedShort(), cpool);
		case 'D': // double
			return new SimpleElementValue(PRIMITIVE_DOUBLE,
					dis.readUnsignedShort(), cpool);
		case 'F': // float
			return new SimpleElementValue(PRIMITIVE_FLOAT,
					dis.readUnsignedShort(), cpool);
		case 'I': // int
			return new SimpleElementValue(PRIMITIVE_INT,
					dis.readUnsignedShort(), cpool);
		case 'J': // long
			return new SimpleElementValue(PRIMITIVE_LONG,
					dis.readUnsignedShort(), cpool);
		case 'S': // short
			return new SimpleElementValue(PRIMITIVE_SHORT,
					dis.readUnsignedShort(), cpool);
		case 'Z': // boolean
			return new SimpleElementValue(PRIMITIVE_BOOLEAN,
					dis.readUnsignedShort(), cpool);
		case 's': // String
			return new SimpleElementValue(STRING, dis.readUnsignedShort(),
					cpool);
		case 'e': // Enum constant
			dis.readUnsignedShort(); // Unused type_index
			return new EnumElementValue(ENUM_CONSTANT, dis.readUnsignedShort(),
					cpool);
		case 'c': // Class
			return new ClassElementValue(CLASS, dis.readUnsignedShort(), cpool);
		case '@': // Annotation
			// TODO isRuntimeVisible
			return new AnnotationElementValue(ANNOTATION, new AnnotationEntry(
					dis, cpool), cpool);
		case '[': // Array
			int numArrayVals = dis.readUnsignedShort();
			ElementValue[] evalues = new ElementValue[numArrayVals];
			for (int j = 0; j < numArrayVals; j++) {
				evalues[j] = ElementValue.readElementValue(dis, cpool);
			}
			return new ArrayElementValue(ARRAY, evalues, cpool);
		default:
			throw new ClassFormatException(
					"Unexpected element value kind in annotation: " + type);
		}
	}

	public int getType() {
		return type;
	}

	public ConstantPool getCpool() {
		return cpool;
	}

	public static int getString() {
		return STRING;
	}

	public static int getEnumConstant() {
		return ENUM_CONSTANT;
	}

	public static int getClassVariable() {
		return CLASS;
	}

	public static int getAnnotation() {
		return ANNOTATION;
	}

	public static int getArray() {
		return ARRAY;
	}

	public static int getPrimitiveInt() {
		return PRIMITIVE_INT;
	}

	public static int getPrimitiveByte() {
		return PRIMITIVE_BYTE;
	}

	public static int getPrimitiveChar() {
		return PRIMITIVE_CHAR;
	}

	public static int getPrimitiveDouble() {
		return PRIMITIVE_DOUBLE;
	}

	public static int getPrimitiveFloat() {
		return PRIMITIVE_FLOAT;
	}

	public static int getPrimitiveLong() {
		return PRIMITIVE_LONG;
	}

	public static int getPrimitiveShort() {
		return PRIMITIVE_SHORT;
	}

	public static int getPrimitiveBoolean() {
		return PRIMITIVE_BOOLEAN;
	}

}
