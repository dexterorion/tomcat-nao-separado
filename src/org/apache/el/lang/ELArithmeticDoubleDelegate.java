package org.apache.el.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class ELArithmeticDoubleDelegate extends ELArithmetic {

	@Override
	protected Number add(Number num0, Number num1) {
		// could only be one of these
		if (num0 instanceof BigDecimal) {
			return ((BigDecimal) num0).add(new BigDecimal(num1
					.doubleValue()));
		} else if (num1 instanceof BigDecimal) {
			return ((new BigDecimal(num0.doubleValue())
					.add((BigDecimal) num1)));
		}
		return new Double(num0.doubleValue() + num1.doubleValue());
	}

	@Override
	protected Number coerce(Number num) {
		if (num instanceof Double)
			return num;
		if (num instanceof BigInteger)
			return new BigDecimal((BigInteger) num);
		return new Double(num.doubleValue());
	}

	@Override
	protected Number coerce(String str) {
		return new Double(str);
	}

	@Override
	protected Number divide(Number num0, Number num1) {
		return new Double(num0.doubleValue() / num1.doubleValue());
	}

	@Override
	protected Number mod(Number num0, Number num1) {
		return new Double(num0.doubleValue() % num1.doubleValue());
	}

	@Override
	protected Number subtract(Number num0, Number num1) {
		// could only be one of these
		if (num0 instanceof BigDecimal) {
			return ((BigDecimal) num0).subtract(new BigDecimal(num1
					.doubleValue()));
		} else if (num1 instanceof BigDecimal) {
			return ((new BigDecimal(num0.doubleValue())
					.subtract((BigDecimal) num1)));
		}
		return new Double(num0.doubleValue() - num1.doubleValue());
	}

	@Override
	protected Number multiply(Number num0, Number num1) {
		// could only be one of these
		if (num0 instanceof BigDecimal) {
			return ((BigDecimal) num0).multiply(new BigDecimal(num1
					.doubleValue()));
		} else if (num1 instanceof BigDecimal) {
			return ((new BigDecimal(num0.doubleValue())
					.multiply((BigDecimal) num1)));
		}
		return new Double(num0.doubleValue() * num1.doubleValue());
	}

	@Override
	public boolean matches(Object obj0, Object obj1) {
		return (obj0 instanceof Double
				|| obj1 instanceof Double
				|| obj0 instanceof Float
				|| obj1 instanceof Float
				|| (obj0 instanceof String && ELSupport
						.isStringFloat((String) obj0)) || (obj1 instanceof String && ELSupport
				.isStringFloat((String) obj1)));
	}
}