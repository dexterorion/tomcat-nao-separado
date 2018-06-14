package org.apache.el.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class ELArithmeticBigDecimalDelegate extends ELArithmetic {

	@Override
	protected Number add(Number num0, Number num1) {
		return ((BigDecimal) num0).add((BigDecimal) num1);
	}

	@Override
	protected Number coerce(Number num) {
		if (num instanceof BigDecimal)
			return num;
		if (num instanceof BigInteger)
			return new BigDecimal((BigInteger) num);
		return new BigDecimal(num.doubleValue());
	}

	@Override
	protected Number coerce(String str) {
		return new BigDecimal(str);
	}

	@Override
	protected Number divide(Number num0, Number num1) {
		return ((BigDecimal) num0).divide((BigDecimal) num1,
				BigDecimal.ROUND_HALF_UP);
	}

	@Override
	protected Number subtract(Number num0, Number num1) {
		return ((BigDecimal) num0).subtract((BigDecimal) num1);
	}

	@Override
	protected Number mod(Number num0, Number num1) {
		return new Double(num0.doubleValue() % num1.doubleValue());
	}

	@Override
	protected Number multiply(Number num0, Number num1) {
		return ((BigDecimal) num0).multiply((BigDecimal) num1);
	}

	@Override
	public boolean matches(Object obj0, Object obj1) {
		return (obj0 instanceof BigDecimal || obj1 instanceof BigDecimal);
	}
}