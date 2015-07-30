package net.enilink.composition.properties.komma;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ConversionUtil {
	/**
	 * A mapping of known primitive wrappers.
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrappers;

	static {
		primitiveWrappers = new HashMap<Class<?>, Class<?>>();
		primitiveWrappers.put(boolean.class, Boolean.class);
		primitiveWrappers.put(char.class, Character.class);
		primitiveWrappers.put(byte.class, Byte.class);
		primitiveWrappers.put(short.class, Short.class);
		primitiveWrappers.put(int.class, Integer.class);
		primitiveWrappers.put(long.class, Long.class);
		primitiveWrappers.put(float.class, Float.class);
		primitiveWrappers.put(double.class, Double.class);
	}

	/**
	 * Determines primitive wrapper class for a primitive type.
	 * 
	 * @return Wrapper class, or "null" if class is no primitive type.
	 */
	public static Class<?> wrapperType(Class<?> type) {
		if (type.isPrimitive()) {
			return primitiveWrappers.get(type);
		}
		return type;
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
	public static String stringValue(Object value, boolean trim) {
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
	public static long longValue(Object value) throws NumberFormatException {
		if (value == null)
			return 0;

		Class<?> c = value.getClass();

		if (c.getSuperclass() == Number.class) {
			return ((Number) value).longValue();
		}

		if (c == Character.class) {
			return ((Character) value).charValue();
		}

		if (c == Boolean.class) {
			return ((Boolean) value).booleanValue() ? 1 : 0;
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
	public static double doubleValue(Object value) throws NumberFormatException {
		if (value == null)
			return 0.0;

		Class<?> c = value.getClass();

		if (c.getSuperclass() == Number.class) {
			return ((Number) value).doubleValue();
		}

		if (c == Character.class) {
			return ((Character) value).charValue();
		}

		if (c == Boolean.class) {
			return ((Boolean) value).booleanValue() ? 1 : 0;
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
	public static BigDecimal bigDecValue(Object value)
			throws NumberFormatException {
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
			return BigDecimal.valueOf(((Character) value).charValue());
		}

		if (c == Boolean.class) {
			return BigDecimal.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
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
	public static BigInteger bigIntValue(Object value)
			throws NumberFormatException {
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
			return BigInteger.valueOf(((Character) value).charValue());
		}

		if (c == Boolean.class) {
			return BigInteger.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
		}

		return new BigInteger(stringValue(value, true));
	}

	/*
	 * FIXME: check for String-conversion! old - String set: true, String empty:
	 * false new - String set and "true" (case insensitive): true, else: false
	 */
	public static Boolean booleanValue(Object value) {
		if (value == null)
			return Boolean.FALSE;
		if ((value instanceof Boolean) && Boolean.FALSE.equals(value)
				|| (value instanceof Number)
				&& ((Number) value).intValue() == 0
				|| (value instanceof String)
				&& !Boolean.parseBoolean((String) value)
				|| (value instanceof Character)
				&& ((Character) value).charValue() == 0) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
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
	public static Object convertValue(Class<?> toType, Object value,
			Object defaultValue) {
		Object result;

		if (value != null) {
			if (toType == Integer.class || toType == Integer.TYPE) {
				result = new Integer((int) longValue(value));
			} else if (toType == Double.class || toType == Double.TYPE) {
				result = new Double(doubleValue(value));
			} else if (toType == Boolean.class || toType == Boolean.TYPE) {
				result = booleanValue(value) ? Boolean.TRUE : Boolean.FALSE;
			} else if (toType == Byte.class || toType == Byte.TYPE) {
				result = new Byte((byte) longValue(value));
			} else if (toType == Character.class || toType == Character.TYPE) {
				result = new Character((char) longValue(value));
			} else if (toType == Short.class || toType == Short.TYPE) {
				result = new Short((short) longValue(value));
			} else if (toType == Long.class || toType == Long.TYPE) {
				result = new Long(longValue(value));
			} else if (toType == Float.class || toType == Float.TYPE) {
				result = new Float(doubleValue(value));
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
