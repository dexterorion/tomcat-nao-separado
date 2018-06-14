package org.apache.el.lang;

public final class ELArithmeticLongDelegate extends ELArithmetic {

	@Override
	protected Number add(Number num0, Number num1) {
		return Long.valueOf(num0.longValue() + num1.longValue());
	}

	@Override
	protected Number coerce(Number num) {
		if (num instanceof Long)
			return num;
		return Long.valueOf(num.longValue());
	}

	@Override
	protected Number coerce(String str) {
		return Long.valueOf(str);
	}

	@Override
	protected Number divide(Number num0, Number num1) {
		return Long.valueOf(num0.longValue() / num1.longValue());
	}

	@Override
	protected Number mod(Number num0, Number num1) {
		return Long.valueOf(num0.longValue() % num1.longValue());
	}

	@Override
	protected Number subtract(Number num0, Number num1) {
		return Long.valueOf(num0.longValue() - num1.longValue());
	}

	@Override
	protected Number multiply(Number num0, Number num1) {
		return Long.valueOf(num0.longValue() * num1.longValue());
	}

	@Override
	public boolean matches(Object obj0, Object obj1) {
		return (obj0 instanceof Long || obj1 instanceof Long);
	}
}