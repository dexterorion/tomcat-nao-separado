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

package org.apache.el.lang;

import org.apache.el.util.MessageFactory;

/**
 * A helper class of Arithmetic defined by the EL Specification
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 */
public abstract class ELArithmetic {

	private static final ELArithmeticBigDecimalDelegate BIGDECIMAL = new ELArithmeticBigDecimalDelegate();

	private static final ELArithmeticBigIntegerDelegate BIGINTEGER = new ELArithmeticBigIntegerDelegate();

	private static final ELArithmeticDoubleDelegate DOUBLE = new ELArithmeticDoubleDelegate();

	private static final ELArithmeticLongDelegate LONG = new ELArithmeticLongDelegate();

	private static final Long ZERO = Long.valueOf(0);

	public static final Number add(final Object obj0, final Object obj1) {
		if (obj0 == null && obj1 == null) {
			return Long.valueOf(0);
		}

		final ELArithmetic delegate;
		if (BIGDECIMAL.matches(obj0, obj1))
			delegate = BIGDECIMAL;
		else if (DOUBLE.matches(obj0, obj1)) {
			if (BIGINTEGER.matches(obj0, obj1))
				delegate = BIGDECIMAL;
			else
				delegate = DOUBLE;
		} else if (BIGINTEGER.matches(obj0, obj1))
			delegate = BIGINTEGER;
		else
			delegate = LONG;

		Number num0 = delegate.coerce(obj0);
		Number num1 = delegate.coerce(obj1);

		return delegate.add(num0, num1);
	}

	public static final Number mod(final Object obj0, final Object obj1) {
		if (obj0 == null && obj1 == null) {
			return Long.valueOf(0);
		}

		final ELArithmetic delegate;
		if (BIGDECIMAL.matches(obj0, obj1))
			delegate = DOUBLE;
		else if (DOUBLE.matches(obj0, obj1))
			delegate = DOUBLE;
		else if (BIGINTEGER.matches(obj0, obj1))
			delegate = BIGINTEGER;
		else
			delegate = LONG;

		Number num0 = delegate.coerce(obj0);
		Number num1 = delegate.coerce(obj1);

		return delegate.mod(num0, num1);
	}

	public static final Number subtract(final Object obj0, final Object obj1) {
		if (obj0 == null && obj1 == null) {
			return Long.valueOf(0);
		}

		final ELArithmetic delegate;
		if (BIGDECIMAL.matches(obj0, obj1))
			delegate = BIGDECIMAL;
		else if (DOUBLE.matches(obj0, obj1)) {
			if (BIGINTEGER.matches(obj0, obj1))
				delegate = BIGDECIMAL;
			else
				delegate = DOUBLE;
		} else if (BIGINTEGER.matches(obj0, obj1))
			delegate = BIGINTEGER;
		else
			delegate = LONG;

		Number num0 = delegate.coerce(obj0);
		Number num1 = delegate.coerce(obj1);

		return delegate.subtract(num0, num1);
	}

	public static final Number divide(final Object obj0, final Object obj1) {
		if (obj0 == null && obj1 == null) {
			return ZERO;
		}

		final ELArithmetic delegate;
		if (BIGDECIMAL.matches(obj0, obj1))
			delegate = BIGDECIMAL;
		else if (BIGINTEGER.matches(obj0, obj1))
			delegate = BIGDECIMAL;
		else
			delegate = DOUBLE;

		Number num0 = delegate.coerce(obj0);
		Number num1 = delegate.coerce(obj1);

		return delegate.divide(num0, num1);
	}

	public static final Number multiply(final Object obj0, final Object obj1) {
		if (obj0 == null && obj1 == null) {
			return Long.valueOf(0);
		}

		final ELArithmetic delegate;
		if (BIGDECIMAL.matches(obj0, obj1))
			delegate = BIGDECIMAL;
		else if (DOUBLE.matches(obj0, obj1)) {
			if (BIGINTEGER.matches(obj0, obj1))
				delegate = BIGDECIMAL;
			else
				delegate = DOUBLE;
		} else if (BIGINTEGER.matches(obj0, obj1))
			delegate = BIGINTEGER;
		else
			delegate = LONG;

		Number num0 = delegate.coerce(obj0);
		Number num1 = delegate.coerce(obj1);

		return delegate.multiply(num0, num1);
	}

	public static final boolean isNumber(final Object obj) {
		return (obj != null && isNumberType(obj.getClass()));
	}

	public static final boolean isNumberType(final Class<?> type) {
		return type == Long.TYPE || type == Double.TYPE || type == Byte.TYPE
				|| type == Short.TYPE || type == Integer.TYPE
				|| type == Float.TYPE || Number.class.isAssignableFrom(type);
	}

	/**
     *
     */
	public ELArithmetic() {
		super();
	}

	protected abstract Number add(final Number num0, final Number num1);

	protected abstract Number multiply(final Number num0, final Number num1);

	protected abstract Number subtract(final Number num0, final Number num1);

	protected abstract Number mod(final Number num0, final Number num1);

	protected abstract Number coerce(final Number num);

	protected final Number coerce(final Object obj) {

		if (isNumber(obj)) {
			return coerce((Number) obj);
		}
		if (obj == null || "".equals(obj)) {
			return coerce(ZERO);
		}
		if (obj instanceof String) {
			return coerce((String) obj);
		}
		if (obj instanceof Character) {
			return coerce(Short.valueOf((short) ((Character) obj).charValue()));
		}

		throw new IllegalArgumentException(MessageFactory.get("error.convert",
				obj, obj.getClass(), "Number"));
	}

	protected abstract Number coerce(final String str);

	protected abstract Number divide(final Number num0, final Number num1);

	protected abstract boolean matches(final Object obj0, final Object obj1);

}
