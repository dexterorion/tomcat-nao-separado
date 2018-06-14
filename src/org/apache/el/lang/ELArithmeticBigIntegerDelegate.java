package org.apache.el.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class ELArithmeticBigIntegerDelegate extends ELArithmetic {

	@Override
	protected Number add(Number num0, Number num1) {
		return ((BigInteger) num0).add((BigInteger) num1);
	}

	@Override
	protected Number coerce(Number num) {
		if (num instanceof BigInteger)
			return num;
		return new BigInteger(num.toString());
	}

	@Override
	protected Number coerce(String str) {
		return new BigInteger(str);
	}

	@Override
	protected Number divide(Number num0, Number num1) {
		return (new BigDecimal((BigInteger) num0)).divide(new BigDecimal(
				(BigInteger) num1), BigDecimal.ROUND_HALF_UP);
	}

	@Override
	protected Number multiply(Number num0, Number num1) {
		return ((BigInteger) num0).multiply((BigInteger) num1);
	}

	@Override
	protected Number mod(Number num0, Number num1) {
		return ((BigInteger) num0).mod((BigInteger) num1);
	}

	@Override
	protected Number subtract(Number num0, Number num1) {
		return ((BigInteger) num0).subtract((BigInteger) num1);
	}

	@Override
	public boolean matches(Object obj0, Object obj1) {
		return (obj0 instanceof BigInteger || obj1 instanceof BigInteger);
	}
}