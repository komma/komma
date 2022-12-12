/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 
 * @author Ken Wenzel
 */
public class ValueUtils {
	protected Map<Class<?>, ValueType> typeMap = new IdentityHashMap<Class<?>, ValueType>(
			16);
	protected Map<Class<?>, Object> defaultValues = new IdentityHashMap<Class<?>, Object>(
			16);

	private static ValueUtils instance = null;

	protected ValueUtils() {
		typeMap.put(Boolean.class, ValueType.BOOLEAN);
		typeMap.put(Boolean.TYPE, ValueType.BOOLEAN);
		typeMap.put(Byte.class, ValueType.BYTE);
		typeMap.put(Byte.TYPE, ValueType.BYTE);
		typeMap.put(Short.class, ValueType.SHORT);
		typeMap.put(Short.TYPE, ValueType.SHORT);
		typeMap.put(Integer.class, ValueType.INTEGER);
		typeMap.put(Integer.TYPE, ValueType.INTEGER);
		typeMap.put(Long.class, ValueType.LONG);
		typeMap.put(Long.TYPE, ValueType.LONG);
		typeMap.put(BigInteger.class, ValueType.BIGINTEGER);
		typeMap.put(Float.class, ValueType.FLOAT);
		typeMap.put(Float.TYPE, ValueType.FLOAT);
		typeMap.put(Double.class, ValueType.DOUBLE);
		typeMap.put(Double.TYPE, ValueType.DOUBLE);
		typeMap.put(BigDecimal.class, ValueType.BIGDECIMAL);

		defaultValues.put(Boolean.TYPE, Boolean.FALSE);
		defaultValues.put(Boolean.class, Boolean.FALSE);
		defaultValues.put(Byte.TYPE, (byte) 0);
		defaultValues.put(Byte.class, (byte) 0);
		defaultValues.put(Character.TYPE, (char) 0);
		defaultValues.put(Character.class, (char) 0);
		defaultValues.put(Short.TYPE, (short) 0);
		defaultValues.put(Short.class, (short) 0);
		defaultValues.put(Integer.TYPE, 0);
		defaultValues.put(Integer.class, 0);
		defaultValues.put(Long.TYPE, 0L);
		defaultValues.put(Long.class, 0L);
		defaultValues.put(Float.TYPE, 0.0f);
		defaultValues.put(Float.class, 0.0f);
		defaultValues.put(Double.TYPE, 0.0);
		defaultValues.put(Double.class, 0.0);
	}

	public static synchronized ValueUtils getInstance() {
		if (instance == null)
			instance = new ValueUtils();
		return instance;
	}

	public ValueType getType(Object value) {
		ValueType result = ValueType.ANY;

		if (value != null) {
			Class<?> c = value.getClass();
			result = typeMap.get(c);
			if (result == null)
				result = ValueType.ANY;
		}

		return result;
	}

	/**
	 * Returns the constant from the NumericTypes interface that best expresses
	 * the type of an operation, which can be either numeric or not, on the two
	 * given types.
	 * 
	 * @param t1
	 *            one argument type to an operator
	 * @param t2
	 *            the other argument type
	 * @param canBeNonNumeric
	 *            whether the operator can be interpreted as non-numeric
	 * 
	 * @return the appropriate constant from the NumericTypes interface
	 */
	public ValueType getNumericType(ValueType t1, ValueType t2,
			boolean canBeNonNumeric) {
		if (t1 == t2) {
			return t1;
		}

		if (canBeNonNumeric
				&& ((t1 == ValueType.ANY) || (t2 == ValueType.ANY)
						|| (t1 == ValueType.CHARACTER) || (t2 == ValueType.CHARACTER))) {
			return ValueType.ANY;
		}

		if (t1 == ValueType.ANY) {
			t1 = ValueType.DOUBLE; // Try to interpret strings as doubles...
		}

		if (t2 == ValueType.ANY) {
			t2 = ValueType.DOUBLE; // Try to interpret strings as doubles...
		}

		if (t1.compareTo(ValueType.FLOAT) >= 0) {
			if (t2.compareTo(ValueType.FLOAT) >= 0) {
				return ValueType.max(t1, t2);
			}

			if (t2.compareTo(ValueType.INTEGER) < 0) {
				return t1;
			}

			if (t2 == ValueType.BIGINTEGER) {
				return ValueType.BIGDECIMAL;
			}

			return ValueType.max(ValueType.DOUBLE, t2);
		} else if (t2.compareTo(ValueType.FLOAT) >= 0) {
			if (t1.compareTo(ValueType.INTEGER) < 0) {
				return t2;
			}

			if (t1 == ValueType.BIGINTEGER) {
				return ValueType.BIGDECIMAL;
			}
			return ValueType.max(ValueType.DOUBLE, t2);
		} else {
			return ValueType.max(t1, t2);
		}
	}

	public int compareWithConversion(Object v1, Object v2) {
		return compareWithConversion(v1, v2, null);
	}

	@SuppressWarnings("unchecked")
	public int compareWithConversion(Object v1, Object v2, Number epsilon) {
		int result;

		if (v1 == v2) {
			result = 0;
		} else {
			ValueType t1 = getType(v1), t2 = getType(v2), type = getNumericType(
					t1, t2, true);

			switch (type) {
			case BIGINTEGER:
				result = bigIntValue(v1).compareTo(bigIntValue(v2));
				break;

			case BIGDECIMAL:
				result = bigDecValue(v1).compareTo(bigDecValue(v2));
				break;

			case ANY:
				if ((t1 == ValueType.ANY) && (t2 == ValueType.ANY)) {
					if (v1 instanceof Comparable) {
						result = ((Comparable) v1).compareTo(v2);
						break;
					} else {
						throw new RuntimeException("invalid comparison: " + v1
								+ " <-> " + v2);
					}
				}
				// else fall through
			case FLOAT:
			case DOUBLE:
				double dv1 = doubleValue(v1),
				dv2 = doubleValue(v2);

				if (epsilon != null) {
					double diff = Math.abs(dv1 - dv2);
					result = (diff < Math.abs(epsilon.doubleValue())) ? 0
							: ((dv1 < dv2) ? -1 : 1);
				} else {
					result = (dv1 == dv2) ? 0 : ((dv1 < dv2) ? -1 : 1);
				}

				break;

			default:
				long lv1 = longValue(v1),
				lv2 = longValue(v2);

				result = (lv1 == lv2) ? 0 : ((lv1 < lv2) ? -1 : 1);
				break;
			}
		}
		return result;
	}

	/**
	 * Evaluates the given object as a String and trims it if the trim flag is
	 * true.
	 * 
	 * @param value
	 *            an object to interpret as a String
	 * @param trim
	 *            if true this returned string value is trimmed of whitespace
	 *            using String.trim().
	 * 
	 * @return the String value implied by the given object as returned by the
	 *         toString() method, or "null" if the object is null.
	 */
	public String stringValue(Object value, boolean trim) {
		String result;

		if (value == null) {
			result = "";
		} else {
			result = value.toString();

			if (trim) {
				result = result.trim();
			}
		}

		return result;
	}

	/**
	 * Evaluates the given object as a long integer.
	 * 
	 * @param value
	 *            an object to interpret as a long integer
	 * 
	 * @return the long integer value implied by the given object
	 * 
	 * @throws NumberFormatException
	 *             if the given object can't be understood as a long integer
	 */
	public long longValue(Object value) throws NumberFormatException {
		if (value == null)
			return 0;

		Class<?> c = value.getClass();

		if (c.getSuperclass() == Number.class) {
			return ((Number) value).longValue();
		}

		if (c == Character.class) {
			return (Character) value;
		}

		if (c == Boolean.class) {
			return (Boolean) value ? 1 : 0;
		}

		return Long.parseLong(stringValue(value, true));
	}

	/**
	 * Evaluates the given object as a double-precision floating-point number.
	 * 
	 * @param value
	 *            an object to interpret as a double
	 * 
	 * @return the double value implied by the given object
	 * 
	 * @throws NumberFormatException
	 *             if the given object can't be understood as a double
	 */
	public double doubleValue(Object value) throws NumberFormatException {
		if (value == null)
			return 0.0;

		Class<?> c = value.getClass();

		if (c.getSuperclass() == Number.class) {
			return ((Number) value).doubleValue();
		}

		if (c == Character.class) {
			return (Character) value;
		}

		if (c == Boolean.class) {
			return (Boolean) value ? 1 : 0;
		}

		String s = stringValue(value, true);
		return (s.length() == 0) ? 0.0 : Double.parseDouble(s);
	}

	/**
	 * Evaluates the given object as a BigDecimal.
	 * 
	 * @param value
	 *            an object to interpret as a BigDecimal
	 * 
	 * @return the BigDecimal value implied by the given object
	 * 
	 * @throws NumberFormatException
	 *             if the given object can't be understood as a BigDecimal
	 */
	public BigDecimal bigDecValue(Object value) throws NumberFormatException {
		if (value == null)
			return new BigDecimal(0);

		Class<?> c = value.getClass();

		if (c == BigDecimal.class) {
			return (BigDecimal) value;
		}

		if (c == BigInteger.class) {
			return new BigDecimal((BigInteger) value);
		}

		if (c.getSuperclass() == Number.class) {
			return new BigDecimal(((Number) value).doubleValue());
		}

		if (c == Character.class) {
			return BigDecimal.valueOf((Character) value);
		}

		if (c == Boolean.class) {
			return BigDecimal.valueOf((Boolean) value ? 1 : 0);
		}

		return new BigDecimal(stringValue(value, true));
	}

	/**
	 * Evaluates the given object as a BigInteger.
	 * 
	 * @param value
	 *            an object to interpret as a BigInteger
	 * 
	 * @return the BigInteger value implied by the given object
	 * 
	 * @throws NumberFormatException
	 *             if the given object can't be understood as a BigInteger
	 */
	public BigInteger bigIntValue(Object value) throws NumberFormatException {
		if (value == null)
			return new BigInteger(new byte[] { 0 });

		Class<?> c = value.getClass();

		if (c == BigInteger.class) {
			return (BigInteger) value;
		}

		if (c == BigDecimal.class) {
			return ((BigDecimal) value).toBigInteger();
		}

		if (c.getSuperclass() == Number.class) {
			return BigInteger.valueOf(((Number) value).longValue());
		}

		if (c == Character.class) {
			return BigInteger.valueOf((Character) value);
		}

		if (c == Boolean.class) {
			return BigInteger.valueOf((Boolean) value ? 1 : 0);
		}

		return new BigInteger(stringValue(value, true));
	}

	/*
	 * FIXME: check for String-conversion! old - String set: true, String empty:
	 * false new - String set and "true" (case insensitive): true, else: false
	 */
	public Boolean booleanValue(Object value) {
		if (value == null)
			return Boolean.FALSE;
		if ((value instanceof Boolean) && Boolean.FALSE.equals(value)
				|| (value instanceof Number)
				&& ((Number) value).intValue() == 0
				|| (value instanceof String)
				&& !Boolean.parseBoolean((String) value)
				|| (value instanceof Character)
				&& (Character) value == 0) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	/**
	 * Adds the two objects given. For numeric values this will add them
	 * numerically; for one of the arguments being non-numeric this will
	 * catenate them as Strings.
	 * 
	 * @param v1
	 *            First addend
	 * @param v2
	 *            Second addend
	 * 
	 * @return v1 + v2 or concatenated String
	 */
	public Object add(Object v1, Object v2) {
		ValueType t1 = getType(v1), t2 = getType(v2);
		ValueType type = getNumericType(t1, t2, true);

		switch (type) {
		case BIGINTEGER:
			return bigIntValue(v1).add(bigIntValue(v2));

		case BIGDECIMAL:
			return bigDecValue(v1).add(bigDecValue(v2));

		case FLOAT:
		case DOUBLE:
			return createReal(type, doubleValue(v1) + doubleValue(v2));

		case ANY:
			return stringValue(v1, false) + stringValue(v2, false);

		default:
			return createInteger(type, longValue(v1) + longValue(v2));
		}
	}

	/**
	 * Subtracts v2 from v1.
	 * 
	 * @param v1
	 *            Value to be subtracted from.
	 * @param v2
	 *            Value to be subtracted from v1.
	 * 
	 * @return v1 - v2
	 */
	public Object subtract(Object v1, Object v2) {
		ValueType type = getNumericType(getType(v1), getType(v2), false);

		switch (type) {
		case BIGINTEGER:
			return bigIntValue(v1).subtract(bigIntValue(v2));

		case BIGDECIMAL:
			return bigDecValue(v1).subtract(bigDecValue(v2));

		case FLOAT:
		case DOUBLE:
			return createReal(type, doubleValue(v1) - doubleValue(v2));

		default:
			return createInteger(type, longValue(v1) - longValue(v2));
		}
	}

	/**
	 * Negates v1.
	 * 
	 * @param v1
	 *            Value to be negated.
	 * 
	 * @return -v1
	 */
	public Object negate(Object v1) {
		ValueType type = getType(v1);

		switch (type) {
		case BIGINTEGER:
			return bigIntValue(v1).negate();

		case BIGDECIMAL:
			return bigDecValue(v1).negate();

		case FLOAT:
		case DOUBLE:
			return createReal(type, -doubleValue(v1));

		default:
			return createInteger(type, -longValue(v1));
		}
	}

	/**
	 * Multiplies v1 times v2.
	 * 
	 * @param v1
	 *            First multiplicand
	 * @param v2
	 *            Second multiplicand
	 * 
	 * @return v1 * v2
	 */
	public Object multiply(Object v1, Object v2) {
		ValueType type = getNumericType(getType(v1), getType(v2), false);

		switch (type) {
		case BIGINTEGER:
			return bigIntValue(v1).multiply(bigIntValue(v2));

		case BIGDECIMAL:
			return bigDecValue(v1).multiply(bigDecValue(v2));

		case FLOAT:
		case DOUBLE:
			return createReal(type, doubleValue(v1) * doubleValue(v2));

		default:
			return createInteger(type, longValue(v1) * longValue(v2));
		}
	}

	/**
	 * Divides the first argument by the second
	 * 
	 * @param v1
	 *            Dividend
	 * @param v2
	 *            Divisor
	 * 
	 * @return Value of v1 / v2.
	 */
	public Object divide(Object v1, Object v2) {
		ValueType type = getNumericType(getType(v1), getType(v2), false);

		switch (type) {
		case BIGINTEGER:
			return bigIntValue(v1).divide(bigIntValue(v2));

		case BIGDECIMAL:
			return bigDecValue(v1).divide(bigDecValue(v2),
					RoundingMode.HALF_EVEN);

		case FLOAT:
		case DOUBLE:
			return createReal(type, doubleValue(v1) / doubleValue(v2));

		default:
			return createInteger(type, longValue(v1) / longValue(v2));
		}
	}

	/**
	 * Returns the number of the first argument, v1, divided by the second
	 * number's remainder (modulus).
	 * 
	 * @param v1
	 *            Dividend
	 * @param v2
	 *            Divisor
	 * 
	 * @return Remainder of dividing v1 / v2.
	 */
	public Object remainder(Object v1, Object v2) {
		ValueType type = getNumericType(getType(v1), getType(v2), false);

		switch (type) {
		case BIGDECIMAL:
		case BIGINTEGER:
			return bigIntValue(v1).remainder(bigIntValue(v2));

		default:
			return createInteger(type, longValue(v1) % longValue(v2));
		}
	}

	/**
	 * Returns a new Number object of an appropriate type to hold the given
	 * integer value. The type of the returned object is consistent with the
	 * given type argument, which is a constant from the NumericTypes interface.
	 * 
	 * @param type
	 *            the nominal numeric type of the result
	 * @param value
	 *            the integer value to convert to a Number object
	 * 
	 * @return a Number object with the given value, of type implied by the type
	 *         argument
	 */
	public Number createInteger(ValueType type, long value) {
		switch (type) {
		case BOOLEAN:
		case CHARACTER:
		case INTEGER:
			return (int) value;

		case FLOAT:

			if ((long) (float) value == value) {
				return (float) value;
			}

			// else fall through:
		case DOUBLE:

			if ((long) (double) value == value) {
				return (double) value;
			}

			// else fall through:
		case LONG:
			return value;

		case BYTE:
			return (byte) value;

		case SHORT:
			return (short) value;

		default:
			return BigInteger.valueOf(value);
		}
	}

	/**
	 * Returns a new Number object of an appropriate type to hold the given real
	 * value. The type of the returned object is always either Float or Double,
	 * and is only Float if the given type tag (a constant from the NumericTypes
	 * interface) is FLOAT.
	 * 
	 * @param type
	 *            the nominal numeric type of the result
	 * @param value
	 *            the real value to convert to a Number object
	 * 
	 * @return a Number object with the given value, of type implied by the type
	 *         argument
	 */
	public Number createReal(ValueType type, double value) {
		if (type == ValueType.FLOAT) {
			return (float) value;
		}

		return value;
	}

	/**
	 * Returns the value converted numerically to the given class type.
	 * 
	 * @param toType
	 *            class type to be converted to
	 * @param value
	 *            an object to be converted to the given type
	 * @param defaultValue
	 *            value returned in the event that no conversion is possible to
	 *            the given type
	 * 
	 * @return converted value of the type given, or defaultValue if the value
	 *         cannot be converted to the given type.
	 */
	public Object convertValue(Class<?> toType, Object value,
			Object defaultValue) {
		Object result;

		if (value != null) {
			if (toType == Integer.class || toType == Integer.TYPE) {
				result = (int) longValue(value);
			} else if (toType == Double.class || toType == Double.TYPE) {
				result = doubleValue(value);
			} else if (toType == Boolean.class || toType == Boolean.TYPE) {
				result = booleanValue(value) ? Boolean.TRUE : Boolean.FALSE;
			} else if (toType == Byte.class || toType == Byte.TYPE) {
				result = (byte) longValue(value);
			} else if (toType == Character.class || toType == Character.TYPE) {
				result = (char) longValue(value);
			} else if (toType == Short.class || toType == Short.TYPE) {
				result = (short) longValue(value);
			} else if (toType == Long.class || toType == Long.TYPE) {
				result = longValue(value);
			} else if (toType == Float.class || toType == Float.TYPE) {
				result = (float) doubleValue(value);
			} else if (toType == BigInteger.class) {
				result = bigIntValue(value);
			} else if (toType == BigDecimal.class) {
				result = bigDecValue(value);
			} else if (toType == String.class) {
				result = stringValue(value, false);
			} else {
				result = value;
			}
		} else {
			result = defaultValue;
		}

		return result;
	}
}
