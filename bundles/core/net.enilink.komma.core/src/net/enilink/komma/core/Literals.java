package net.enilink.komma.core;

import java.util.Locale;

/**
 * Helper methods to work with {@link ILiteral} instances.
 * 
 */
public class Literals {
	/**
	 * Indicates whether two literals are "equal to".
	 * 
	 * @param literal
	 *            A literal instance
	 * @param other
	 *            Another literal with which to compare
	 * @return <code>true</code> if the literal is the same as the
	 *         <code>other</code> literal; <code>false</code> otherwise
	 */
	public static boolean equals(ILiteral literal, ILiteral other) {
		URI datatype = literal.getDatatype();
		URI otherDatatype = other.getDatatype();
		if (datatype == null) {
			if (otherDatatype != null) {
				return false;
			}
		} else if (!datatype.equals(otherDatatype))
			return false;
		String language = literal.getLanguage();
		String otherLanguage = other.getLanguage();
		if (language == null) {
			if (otherLanguage != null) {
				return false;
			}
		} else if (!language.equals(otherLanguage)) {
			return false;
		}
		return literal.getLabel().equals(other.getLabel());
	}

	/**
	 * Indicates whether some literal and some other object are "equal to".
	 * 
	 * @param literal
	 *            A literal instance
	 * @param obj
	 *            Another object with which to compare
	 * @return <code>true</code> if the literal is the same as the obj argument;
	 *         <code>false</code> otherwise
	 */
	public static boolean equals(ILiteral literal, Object obj) {
		if (literal == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof ILiteral)) {
			return false;
		}
		return equals(literal, (ILiteral) obj);
	}

	/**
	 * Compute the {@link Object#hashCode()} for a given literal.
	 * 
	 * @param literal
	 *            The literal
	 * @return The hash code of the given literal
	 */
	public static int hashCode(ILiteral literal) {
		final int prime = 31;
		int result = 1;
		URI datatype = literal.getDatatype();
		result = prime * result + ((datatype == null) ? 0 : datatype.hashCode());
		String language = literal.getLanguage();
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + literal.getLabel().hashCode();
		return result;
	}

	/**
	 * Build a Turtle-compatible string representation for a given literal.
	 * 
	 * @param literal
	 *            The literal
	 * @return A string representation for the given literal
	 */
	public static String toString(ILiteral literal) {
		StringBuilder result = new StringBuilder("\"");
		escapeTurtle(result, literal.getLabel());
		result.append("\"");
		String language = literal.getLanguage();
		if (language != null) {
			result.append("@").append(language);
		} else {
			URI datatype = literal.getDatatype();
			if (datatype != null) {
				result.append("^^<").append(datatype.toString()).append(">");
			}
		}
		return result.toString();
	}

	/**
	 * Escape the given string for the use within quotation marks for building a
	 * Turtle-compatible string representation.
	 * 
	 * @param str
	 *            The string that should be escaped
	 * @return The escaped string
	 */
	public static String escapeTurtle(String str) {
		return escapeTurtle(new StringBuilder(), str).toString();
	}

	private static String toHex(char ch) {
		return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
	}

	private static StringBuilder escapeTurtle(StringBuilder sb, String str) {
		if (str == null) {
			return sb;
		}
		int length = str.length();
		for (int i = 0; i < length; i++) {
			char ch = str.charAt(i);

			// handle unicode
			if (ch > 0xfff) {
				sb.append("\\u" + toHex(ch));
			} else if (ch > 0xff) {
				sb.append("\\u0" + toHex(ch));
			} else if (ch > 0x7f) {
				sb.append("\\u00" + toHex(ch));
			} else if (ch < 32) {
				switch (ch) {
				case '\b':
					sb.append('\\');
					sb.append('b');
					break;
				case '\n':
					sb.append('\\');
					sb.append('n');
					break;
				case '\t':
					sb.append('\\');
					sb.append('t');
					break;
				case '\f':
					sb.append('\\');
					sb.append('f');
					break;
				case '\r':
					sb.append('\\');
					sb.append('r');
					break;
				default:
					if (ch > 0xf) {
						sb.append("\\u00" + toHex(ch));
					} else {
						sb.append("\\u000" + toHex(ch));
					}
					break;
				}
			} else {
				switch (ch) {
				case '"':
					sb.append('\\');
					sb.append('"');
					break;
				case '\\':
					sb.append('\\');
					sb.append('\\');
					break;
				default:
					sb.append(ch);
					break;
				}
			}
		}
		return sb;
	}

	private Literals() {
	}
}
